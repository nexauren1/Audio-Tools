const FIREBASE_JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/" +
  "securetoken@system.gserviceaccount.com";

let jwksCache = null;
let jwksExpiresAt = 0;

function corsHeaders() {
  return {
    "access-control-allow-origin": "*",
    "access-control-allow-methods": "GET,POST,OPTIONS",
    "access-control-allow-headers": "Authorization,Content-Type"
  };
}

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      ...corsHeaders()
    }
  });
}

function html(body, status = 200) {
  return new Response(body, {
    status,
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "no-store",
      ...corsHeaders()
    }
  });
}

function nowSeconds() {
  return Math.floor(Date.now() / 1000);
}

function base64UrlToBytes(value) {
  const normalized = value
    .replace(/-/g, "+")
    .replace(/_/g, "/")
    .padEnd(Math.ceil(value.length / 4) * 4, "=");

  const binary = atob(normalized);
  const bytes = new Uint8Array(binary.length);

  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }

  return bytes;
}

function decodeBase64UrlJson(value) {
  return JSON.parse(
    new TextDecoder().decode(base64UrlToBytes(value))
  );
}

async function getFirebaseJwks() {
  const now = Date.now();

  if (jwksCache && jwksExpiresAt > now) {
    return jwksCache;
  }

  const response = await fetch(FIREBASE_JWKS_URL, {
    headers: {
      accept: "application/json"
    }
  });

  if (!response.ok) {
    throw new Error(
      "Firebase public keys could not be loaded."
    );
  }

  const data = await response.json();
  const cacheControl =
    response.headers.get("cache-control") || "";
  const match =
    cacheControl.match(/max-age=(\\d+)/i);

  const maxAgeSeconds = match
    ? Number(match[1])
    : 3600;

  jwksCache = data.keys || [];
  jwksExpiresAt = now + maxAgeSeconds * 1000;

  return jwksCache;
}

async function verifyFirebaseIdToken(
  idToken,
  env
) {
  const parts = idToken.split(".");

  if (parts.length !== 3) {
    throw new Error("Invalid Firebase token.");
  }

  const [headerPart, payloadPart, signaturePart] =
    parts;

  const header = decodeBase64UrlJson(headerPart);
  const payload = decodeBase64UrlJson(payloadPart);

  if (header.alg !== "RS256" || !header.kid) {
    throw new Error(
      "Invalid Firebase token header."
    );
  }

  const projectId =
    env.FIREBASE_PROJECT_ID || "audio-tools-e725e";

  const now = nowSeconds();

  if (
    typeof payload.exp !== "number" ||
    payload.exp <= now
  ) {
    throw new Error("Firebase token expired.");
  }

  if (
    typeof payload.iat !== "number" ||
    payload.iat > now + 60
  ) {
    throw new Error(
      "Invalid Firebase token issue time."
    );
  }

  if (
    payload.aud !== projectId ||
    payload.iss !==
      "https://securetoken.google.com/" +
      projectId
  ) {
    throw new Error(
      "Firebase token audience or issuer is invalid."
    );
  }

  if (
    typeof payload.sub !== "string" ||
    payload.sub.trim() === ""
  ) {
    throw new Error(
      "Firebase token subject is invalid."
    );
  }

  if (
    typeof payload.auth_time !== "number" ||
    payload.auth_time > now + 60
  ) {
    throw new Error(
      "Firebase token auth time is invalid."
    );
  }

  const keys = await getFirebaseJwks();
  let jwk = keys.find(
    (key) => key.kid === header.kid
  );

  if (!jwk) {
    jwksExpiresAt = 0;
    const refreshed = await getFirebaseJwks();

    jwk = refreshed.find(
      (key) => key.kid === header.kid
    );
  }

  if (!jwk) {
    throw new Error(
      "Firebase signing key not found."
    );
  }

  const publicKey = await crypto.subtle.importKey(
    "jwk",
    jwk,
    {
      name: "RSASSA-PKCS1-v1_5",
      hash: "SHA-256"
    },
    false,
    ["verify"]
  );

  const valid = await crypto.subtle.verify(
    {
      name: "RSASSA-PKCS1-v1_5"
    },
    publicKey,
    base64UrlToBytes(signaturePart),
    new TextEncoder().encode(
      headerPart + "." + payloadPart
    )
  );

  if (!valid) {
    throw new Error(
      "Firebase token signature is invalid."
    );
  }

  return {
    uid: payload.sub,
    email:
      typeof payload.email === "string"
        ? payload.email
        : null
  };
}

function getBearerToken(request) {
  const value =
    request.headers.get("authorization") || "";

  if (!value.startsWith("Bearer ")) {
    return null;
  }

  return value.slice(7).trim() || null;
}

async function requireUser(request, env) {
  const token = getBearerToken(request);

  if (!token) {
    throw new Response(
      JSON.stringify({
        error: "Authentication required."
      }),
      {
        status: 401,
        headers: {
          "content-type":
            "application/json; charset=utf-8",
          ...corsHeaders()
        }
      }
    );
  }

  try {
    return await verifyFirebaseIdToken(
      token,
      env
    );
  } catch (error) {
    console.error(
      "Firebase token verification failed",
      error
    );

    throw new Response(
      JSON.stringify({
        error: "Invalid authentication token."
      }),
      {
        status: 401,
        headers: {
          "content-type":
            "application/json; charset=utf-8",
          ...corsHeaders()
        }
      }
    );
  }
}

function paypalBaseUrl(env) {
  return env.PAYPAL_ENVIRONMENT === "live"
    ? "https://api-m.paypal.com"
    : "https://api-m.sandbox.paypal.com";
}

function proPrice(env) {
  const value =
    Number(env.PRO_PRICE_USD || "5.00");

  if (!Number.isFinite(value) || value <= 0) {
    throw new Error(
      "PRO_PRICE_USD is invalid."
    );
  }

  return value.toFixed(2);
}

function proDurationDays(env) {
  const value =
    Number(env.PRO_DURATION_DAYS || "30");

  if (!Number.isInteger(value) || value < 1) {
    throw new Error(
      "PRO_DURATION_DAYS is invalid."
    );
  }

  return value;
}

async function ensureEntitlement(db, uid) {
  const existing = await db.prepare(
    "SELECT firebase_uid, plan, expires_at, " +
    "updated_at, last_order_id " +
    "FROM entitlements WHERE firebase_uid = ?"
  )
    .bind(uid)
    .first();

  if (existing) {
    return existing;
  }

  const timestamp = nowSeconds();

  await db.prepare(
    "INSERT OR IGNORE INTO entitlements " +
    "(firebase_uid, plan, starts_at, expires_at, " +
    "status, source, updated_at, last_order_id) " +
    "VALUES (?, 'FREE', ?, NULL, 'ACTIVE', " +
    "'SYSTEM', ?, NULL)"
  )
    .bind(uid, timestamp, timestamp)
    .run();

  return db.prepare(
    "SELECT firebase_uid, plan, expires_at, " +
    "updated_at, last_order_id " +
    "FROM entitlements WHERE firebase_uid = ?"
  )
    .bind(uid)
    .first();
}

function isPro(entitlement) {
  return entitlement &&
    entitlement.plan === "PRO" &&
    entitlement.status === "ACTIVE" &&
    Number(entitlement.expires_at || 0) >
      nowSeconds();
}

async function getPayPalAccessToken(env) {
  const clientId = env.PAYPAL_CLIENT_ID;
  const clientSecret =
    env.PAYPAL_CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    throw new Error(
      "PayPal credentials are not configured."
    );
  }

  const response = await fetch(
    paypalBaseUrl(env) +
      "/v1/oauth2/token",
    {
      method: "POST",
      headers: {
        authorization:
          "Basic " +
          btoa(
            clientId + ":" + clientSecret
          ),
        "content-type":
          "application/x-www-form-urlencoded",
        accept: "application/json"
      },
      body: "grant_type=client_credentials"
    }
  );

  if (!response.ok) {
    console.error(
      "PayPal OAuth failed",
      response.status
    );

    throw new Error(
      "PayPal authentication failed."
    );
  }

  const data = await response.json();

  if (!data.access_token) {
    throw new Error(
      "PayPal did not return an access token."
    );
  }

  return data.access_token;
}

async function createPayPalOrder(
  request,
  env,
  user
) {
  const entitlement =
    await ensureEntitlement(
      env.DB,
      user.uid
    );

  if (isPro(entitlement)) {
    return json({
      ok: true,
      alreadyPro: true,
      plan: "PRO",
      expiresAt:
        Number(entitlement.expires_at)
    });
  }

  const amount = proPrice(env);
  const durationDays =
    proDurationDays(env);

  const accessToken =
    await getPayPalAccessToken(env);

  const origin =
    new URL(request.url).origin;

  const orderPayload = {
    intent: "CAPTURE",
    purchase_units: [
      {
        reference_id:
          "audio-tools-" +
          crypto.randomUUID(),
        description:
          "Audio Tools Pro - " +
          durationDays +
          " days",
        amount: {
          currency_code: "USD",
          value: amount
        }
      }
    ],
    application_context: {
      brand_name: "Audio Tools",
      user_action: "PAY_NOW",
      return_url:
        origin + "/api/paypal/return",
      cancel_url:
        origin + "/api/paypal/cancel"
    }
  };

  const response = await fetch(
    paypalBaseUrl(env) +
      "/v2/checkout/orders",
    {
      method: "POST",
      headers: {
        authorization:
          "Bearer " + accessToken,
        "content-type":
          "application/json",
        accept: "application/json",
        "paypal-request-id":
          "create-" +
          crypto.randomUUID()
      },
      body: JSON.stringify(orderPayload)
    }
  );

  const data = await response.json();

  if (!response.ok) {
    console.error(
      "PayPal order creation failed",
      response.status
    );

    return json(
      {
        error:
          "PayPal could not create the order."
      },
      502
    );
  }

  const approvalUrl =
    (data.links || []).find(
      (link) => link.rel === "approve"
    )?.href || null;

  if (!data.id || !approvalUrl) {
    return json(
      {
        error:
          "PayPal returned an incomplete order."
      },
      502
    );
  }

  await env.DB.prepare(
    "INSERT INTO paypal_orders " +
    "(firebase_uid, order_id, plan_id, amount, " +
    "currency, status, created_at) " +
    "VALUES (?, ?, 'PRO', ?, 'USD', 'CREATED', ?)"
  )
    .bind(
      user.uid,
      data.id,
      amount,
      nowSeconds()
    )
    .run();

  return json({
    ok: true,
    orderId: data.id,
    approvalUrl,
    plan: "PRO",
    amount,
    currency: "USD",
    durationDays
  });
}

function captureAmountMatches(
  data,
  expectedAmount
) {
  const unit =
    data?.purchase_units?.[0];

  const capture =
    unit?.payments?.captures?.[0];

  if (!capture) {
    return false;
  }

  if (capture.status !== "COMPLETED") {
    return false;
  }

  const amount =
    capture.amount || {};

  return (
    amount.currency_code === "USD" &&
    Number(amount.value).toFixed(2) ===
      expectedAmount
  );
}

async function capturePayPalOrder(
  request,
  env,
  user
) {
  const body = await request
    .json()
    .catch(() => ({}));

  const orderId =
    typeof body.orderId === "string"
      ? body.orderId.trim()
      : "";

  if (!orderId) {
    return json(
      { error: "orderId is required." },
      400
    );
  }

  const order = await env.DB.prepare(
    "SELECT * FROM paypal_orders " +
    "WHERE order_id = ? AND firebase_uid = ?"
  )
    .bind(orderId, user.uid)
    .first();

  if (!order) {
    return json(
      { error: "PayPal order not found." },
      404
    );
  }

  const entitlement =
    await ensureEntitlement(
      env.DB,
      user.uid
    );

  if (order.status === "COMPLETED") {
    return json({
      ok: true,
      alreadyCaptured: true,
      plan: entitlement.plan,
      expiresAt:
        Number(entitlement.expires_at || 0)
    });
  }

  const accessToken =
    await getPayPalAccessToken(env);

  const response = await fetch(
    paypalBaseUrl(env) +
      "/v2/checkout/orders/" +
      encodeURIComponent(orderId) +
      "/capture",
    {
      method: "POST",
      headers: {
        authorization:
          "Bearer " + accessToken,
        "content-type":
          "application/json",
        accept: "application/json",
        "paypal-request-id":
          "capture-" +
          crypto.randomUUID()
      },
      body: "{}"
    }
  );

  const data = await response.json();

  if (!response.ok) {
    console.error(
      "PayPal capture failed",
      response.status
    );

    return json(
      {
        error:
          "PayPal could not capture the order."
      },
      502
    );
  }

  const expectedAmount =
    Number(order.amount).toFixed(2);

  if (
    !captureAmountMatches(
      data,
      expectedAmount
    )
  ) {
    return json(
      { error: "Payment validation failed." },
      400
    );
  }

  const now = nowSeconds();
  const existingExpiry =
    Number(entitlement.expires_at || 0);

  const base =
    Math.max(now, existingExpiry);

  const expiresAt =
    base +
    proDurationDays(env) * 86400;

  await env.DB.batch([
    env.DB.prepare(
      "UPDATE paypal_orders SET " +
      "status = 'COMPLETED', captured_at = ? " +
      "WHERE order_id = ? AND firebase_uid = ?"
    )
      .bind(now, orderId, user.uid),

    env.DB.prepare(
      "UPDATE entitlements SET " +
      "plan = 'PRO', starts_at = ?, " +
      "expires_at = ?, status = 'ACTIVE', " +
      "source = 'PAYPAL', updated_at = ?, " +
      "last_order_id = ? " +
      "WHERE firebase_uid = ?"
    )
      .bind(
        now,
        expiresAt,
        now,
        orderId,
        user.uid
      )
  ]);

  return json({
    ok: true,
    plan: "PRO",
    expiresAt
  });
}

async function entitlement(
  request,
  env,
  user
) {
  const current =
    await ensureEntitlement(
      env.DB,
      user.uid
    );

  let plan = current.plan || "FREE";
  let expiresAt =
    Number(current.expires_at || 0);
  let status =
    current.status || "ACTIVE";

  if (
    plan === "PRO" &&
    expiresAt > 0 &&
    expiresAt <= nowSeconds()
  ) {
    plan = "FREE";
    expiresAt = 0;
    status = "ACTIVE";

    await env.DB.prepare(
      "UPDATE entitlements SET " +
      "plan = 'FREE', expires_at = NULL, " +
      "status = 'ACTIVE', updated_at = ? " +
      "WHERE firebase_uid = ?"
    )
      .bind(nowSeconds(), user.uid)
      .run();
  }

  return json({
    ok: true,
    plan,
    isPro: plan === "PRO" &&
      status === "ACTIVE",
    expiresAt: expiresAt || null
  });
}

function plans(env) {
  return json({
    ok: true,
    plans: [
      {
        id: "FREE",
        name: "Free",
        priceUsd: 0,
        durationDays: null
      },
      {
        id: "PRO",
        name: "Pro",
        priceUsd:
          Number(proPrice(env)),
        durationDays:
          proDurationDays(env)
      }
    ]
  });
}

function returnPage(request) {
  const url = new URL(request.url);
  const token =
    url.searchParams.get("token") || "";

  const safeToken =
    token.replace(
      /[^a-zA-Z0-9_-]/g,
      ""
    );

  const deepLink =
    "audiotools://paypal/complete?token=" +
    encodeURIComponent(safeToken);

  const returnLink = safeToken
    ? "<p><a href='" +
      deepLink +
      "' style='display:inline-block;" +
      "padding:14px 18px;border-radius:12px;" +
      "background:#2F6BFF;color:#fff;" +
      "text-decoration:none'>" +
      "Voltar ao Audio Tools</a></p>"
    : "<p>Pedido não identificado.</p>";

  return html(
    "<!doctype html><html><head>" +
    "<meta charset='utf-8'>" +
    "<meta name='viewport' " +
    "content='width=device-width,initial-scale=1'>" +
    "<title>Audio Tools Pro</title>" +
    "</head><body style='" +
    "font-family:system-ui;padding:32px;" +
    "max-width:560px;margin:auto'>" +
    "<h1>Pagamento aprovado</h1>" +
    "<p>Volta ao Audio Tools para terminar " +
    "a ativação do Pro.</p>" +
    returnLink +
    "</body></html>"
  );
}

function cancelPage() {
  return html(
    "<!doctype html><html><head>" +
    "<meta charset='utf-8'>" +
    "<meta name='viewport' " +
    "content='width=device-width,initial-scale=1'>" +
    "<title>Audio Tools Pro</title>" +
    "</head><body style='" +
    "font-family:system-ui;padding:32px;" +
    "max-width:560px;margin:auto'>" +
    "<h1>Pagamento cancelado</h1>" +
    "<p>Nenhuma compra foi confirmada.</p>" +
    "</body></html>"
  );
}

export default {
  async fetch(request, env) {
    if (request.method === "OPTIONS") {
      return new Response(null, {
        status: 204,
        headers: corsHeaders()
      });
    }

    const url = new URL(request.url);

    try {
      if (
        request.method === "GET" &&
        url.pathname === "/"
      ) {
        return json({
          ok: true,
          service: "audio-tools",
          environment:
            env.PAYPAL_ENVIRONMENT ||
            "sandbox",
          version: "1"
        });
      }

      if (
        request.method === "GET" &&
        url.pathname === "/api/plans"
      ) {
        return plans(env);
      }

      if (
        request.method === "GET" &&
        url.pathname ===
          "/api/paypal/return"
      ) {
        return returnPage(request);
      }

      if (
        request.method === "GET" &&
        url.pathname ===
          "/api/paypal/cancel"
      ) {
        return cancelPage();
      }

      const user =
        await requireUser(request, env);

      if (
        request.method === "GET" &&
        url.pathname ===
          "/api/entitlement"
      ) {
        return entitlement(
          request,
          env,
          user
        );
      }

      if (
        request.method === "POST" &&
        url.pathname ===
          "/api/paypal/create-order"
      ) {
        return createPayPalOrder(
          request,
          env,
          user
        );
      }

      if (
        request.method === "POST" &&
        url.pathname ===
          "/api/paypal/capture-order"
      ) {
        return capturePayPalOrder(
          request,
          env,
          user
        );
      }

      return json(
        { error: "Not found." },
        404
      );
    } catch (error) {
      if (error instanceof Response) {
        return error;
      }

      console.error(
        "Unhandled Worker error",
        error
      );

      return json(
        { error: "Internal server error." },
        500
      );
    }
  }
};
