const FIREBASE_JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

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

  const match = cacheControl.match(/max-age=(\\d+)/i);
  const maxAgeSeconds = match
    ? Number(match[1])
    : 3600;

  jwksCache = data.keys || [];
  jwksExpiresAt = now + maxAgeSeconds * 1000;

  return jwksCache;
}

async function verifyFirebaseIdToken(idToken, env) {
  const parts = idToken.split(".");

  if (parts.length !== 3) {
    throw new Error("Invalid Firebase token.");
  }

  const [headerPart, payloadPart, signaturePart] = parts;

  const header = decodeBase64UrlJson(headerPart);
  const payload = decodeBase64UrlJson(payloadPart);

  if (header.alg !== "RS256" || !header.kid) {
    throw new Error("Invalid Firebase token header.");
  }

  const projectId = env.FIREBASE_PROJECT_ID;

  if (!projectId) {
    throw new Error(
      "FIREBASE_PROJECT_ID is not configured."
    );
  }

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
      "https://securetoken.google.com/" + projectId
  ) {
    throw new Error(
      "Firebase token audience or issuer is invalid."
    );
  }

  if (
    typeof payload.sub !== "string" ||
    payload.sub.trim() === ""
  ) {
    throw new Error("Firebase token subject is invalid.");
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
  const jwk = keys.find(
    (key) => key.kid === header.kid
  );

  if (!jwk) {
    jwksExpiresAt = 0;
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
    email: typeof payload.email === "string"
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
    return json(
      { error: "Authentication required." },
      401
    );
  }

  try {
    return await verifyFirebaseIdToken(token, env);
  } catch (error) {
    console.error(
      "Firebase verification failed",
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
    "(firebase_uid, plan, expires_at, " +
    "updated_at, last_order_id) " +
    "VALUES (?, 'FREE', NULL, ?, NULL)"
  )
    .bind(uid, timestamp)
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
    Number(entitlement.expires_at || 0) >
      nowSeconds();
}

async function getPayPalAccessToken(env) {
  const clientId = env.PAYPAL_CLIENT_ID;
  const clientSecret = env.PAYPAL_CLIENT_SECRET;

  if (!clientId || !clientSecret) {
    throw new Error(
      "PayPal credentials are not configured."
    );
  }

  const response = await fetch(
    "https://api-m.sandbox.paypal.com/v1/oauth2/token",
    {
      method: "POST",
      headers: {
        authorization:
          "Basic " +
          btoa(clientId + ":" + clientSecret),
        "content-type":
          "application/x-www-form-urlencoded",
        accept: "application/json"
      },
      body: "grant_type=client_credentials"
    }
  );

  if (!response.ok) {
    console.error(
      "PayPal token error",
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

async function createPayPalOrder(request, env, user) {
  const entitlement =
    await ensureEntitlement(env.DB, user.uid);

  if (isPro(entitlement)) {
    return json({
      ok: true,
      alreadyPro: true,
      plan: "PRO",
      expiresAt:
        Number(entitlement.expires_at)
    });
  }

  const amount =
    Number(env.PRO_PRICE_USD || "5.00")
      .toFixed(2);

  const durationDays =
    Math.max(
      1,
      Number(env.PRO_DURATION_DAYS || "30")
    );

  const origin = new URL(request.url).origin;
  const accessToken =
    await getPayPalAccessToken(env);

  const requestId =
    "at-" + crypto.randomUUID();

  const payload = {
    intent: "CAPTURE",
    purchase_units: [
      {
        reference_id:
          "at-" + crypto.randomUUID(),
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
    "https://api-m.sandbox.paypal.com/v2/checkout/orders",
    {
      method: "POST",
      headers: {
        authorization:
          "Bearer " + accessToken,
        "content-type": "application/json",
        accept: "application/json",
        "paypal-request-id": requestId
      },
      body: JSON.stringify(payload)
    }
  );

  const data = await response.json();

  if (!response.ok) {
    console.error(
      "PayPal create order error",
      response.status,
      data
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
    console.error(
      "PayPal create order missing approval URL"
    );

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
    "VALUES (?, ?, ?, ?, ?, 'CREATED', ?)"
  )
    .bind(
      user.uid,
      data.id,
      "pro_30d",
      amount,
      "USD",
      nowSeconds()
    )
    .run();

  return json({
    ok: true,
    alreadyPro: false,
    orderId: data.id,
    approvalUrl,
    planId: "pro_30d",
    amount,
    currency: "USD",
    durationDays
  });
}

function captureAmountMatches(data, expectedAmount) {
  const unit = data?.purchase_units?.[0];
  const capture = unit?.payments?.captures?.[0];

  if (!capture || capture.status !== "COMPLETED") {
    return false;
  }

  const amount = capture.amount || {};

  return amount.currency_code === "USD" &&
    Number(amount.value).toFixed(2) ===
      expectedAmount;
}

async function capturePayPalOrder(request, env, user) {
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

  const existingEntitlement =
    await ensureEntitlement(env.DB, user.uid);

  if (order.status === "COMPLETED") {
    return json({
      ok: true,
      alreadyCaptured: true,
      plan: existingEntitlement.plan,
      expiresAt:
        Number(existingEntitlement.expires_at || 0)
    });
  }

  const expectedAmount =
    Number(order.amount).toFixed(2);

  const accessToken =
    await getPayPalAccessToken(env);

  const response = await fetch(
    "https://api-m.sandbox.paypal.com/v2/checkout/orders/" +
    encodeURIComponent(orderId) +
    "/capture",
    {
      method: "POST",
      headers: {
        authorization:
          "Bearer " + accessToken,
        "content-type": "application/json",
        accept: "application/json",
        "paypal-request-id":
          "capture-" + crypto.randomUUID()
      },
      body: "{}"
    }
  );

  const data = await response.json();

  if (!response.ok) {
    console.error(
      "PayPal capture error",
      response.status,
      data
    );

    return json(
      {
        error:
          "PayPal could not capture the order."
      },
      502
    );
  }

  if (!captureAmountMatches(data, expectedAmount)) {
    console.error(
      "PayPal capture validation failed"
    );

    return json(
      { error: "Payment validation failed." },
      400
    );
  }

  const currentExpiry =
    Number(existingEntitlement.expires_at || 0);

  const base = Math.max(
    nowSeconds(),
    currentExpiry
  );

  const durationDays =
    Math.max(
      1,
      Number(env.PRO_DURATION_DAYS || "30")
    );

  const expiresAt =
    base + durationDays * 24 * 60 * 60;

  const timestamp = nowSeconds();

  await env.DB.batch([
    env.DB.prepare(
      "UPDATE paypal_orders SET " +
      "status = 'COMPLETED', captured_at = ? " +
      "WHERE order_id = ? AND firebase_uid = ?"
    )
      .bind(timestamp, orderId, user.uid),

    env.DB.prepare(
      "INSERT INTO entitlements " +
      "(firebase_uid, plan, expires_at, " +
      "updated_at, last_order_id) " +
      "VALUES (?, 'PRO', ?, ?, ?) " +
      "ON CONFLICT(firebase_uid) DO UPDATE SET " +
      "plan = 'PRO', " +
      "expires_at = excluded.expires_at, " +
      "updated_at = excluded.updated_at, " +
      "last_order_id = excluded.last_order_id"
    )
      .bind(
        user.uid,
        expiresAt,
        timestamp,
        orderId
      )
  ]);

  return json({
    ok: true,
    alreadyCaptured: false,
    plan: "PRO",
    expiresAt
  });
}

async function entitlement(request, env, user) {
  const current =
    await ensureEntitlement(env.DB, user.uid);

  let plan = current.plan || "FREE";
  let expiresAt =
    Number(current.expires_at || 0);

  if (
    plan === "PRO" &&
    expiresAt > 0 &&
    expiresAt <= nowSeconds()
  ) {
    plan = "FREE";
    expiresAt = 0;

    await env.DB.prepare(
      "UPDATE entitlements SET " +
      "plan = 'FREE', expires_at = NULL, " +
      "updated_at = ? " +
      "WHERE firebase_uid = ?"
    )
      .bind(nowSeconds(), user.uid)
      .run();
  }

  return json({
    ok: true,
    plan,
    isPro: plan === "PRO",
    expiresAt: expiresAt || null
  });
}

function returnPage(request) {
  const url = new URL(request.url);
  const orderId =
    url.searchParams.get("token") || "";

  const safeOrderId =
    orderId.replace(/[^a-zA-Z0-9_-]/g, "");

  const deepLink =
    "audiotools://paypal/complete?token=" +
    encodeURIComponent(safeOrderId);

  const linkHtml = safeOrderId
    ? "<p><a href='" +
      deepLink +
      "' style='display:inline-block;" +
      "padding:14px 18px;border-radius:12px;" +
      "background:#1667d9;color:#fff;" +
      "text-decoration:none'>" +
      "Voltar ao Audio Tools</a></p>"
    : "<p>Não foi encontrado o identificador do pedido.</p>";

  return html(
    "<!doctype html>" +
    "<html><head>" +
    "<meta charset='utf-8'>" +
    "<meta name='viewport' " +
    "content='width=device-width,initial-scale=1'>" +
    "<title>Audio Tools - PayPal</title>" +
    "</head><body style='font-family:system-ui;" +
    "padding:32px;max-width:560px;margin:auto'>" +
    "<h1>Pagamento recebido</h1>" +
    "<p>Volta ao Audio Tools para concluir " +
    "a ativação do Pro.</p>" +
    linkHtml +
    "</body></html>"
  );
}

function cancelPage() {
  return html(
    "<!doctype html>" +
    "<html><head>" +
    "<meta charset='utf-8'>" +
    "<meta name='viewport' " +
    "content='width=device-width,initial-scale=1'>" +
    "<title>Audio Tools - PayPal</title>" +
    "</head><body style='font-family:system-ui;" +
    "padding:32px;max-width:560px;margin:auto'>" +
    "<h1>Pagamento cancelado</h1>" +
    "<p>Nenhuma cobrança foi confirmada " +
    "pelo Audio Tools.</p>" +
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
          service:
            "audio-tools-payments",
          environment:
            env.PAYPAL_ENVIRONMENT || "sandbox"
        });
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
