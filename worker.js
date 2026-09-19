// Audio Tools billing: Free, Pro and Premium subscriptions.
const FIREBASE_JWKS_URL =
  "https://www.googleapis.com/service_accounts/v1/jwk/" +
  "securetoken@system.gserviceaccount.com";

let jwksCache = null;
let jwksExpiresAt = 0;

const PLAN_DEFINITIONS = {
  FREE: {
    name: "Free",
    priceUsd: "0.00",
    durationDays: null,
    billingInterval: "NONE",
    description:
      "Acesso gratuito às ferramentas disponíveis no plano Free.",
    includes: [
      "Ferramentas Free",
      "Processamento local disponível"
    ]
  },
  PRO: {
    name: "Pro",
    priceUsd: "5.00",
    durationDays: 30,
    billingInterval: "MONTH",
    description:
      "Mais ferramentas e recursos para quem trabalha com áudio com mais frequência.",
    includes: [
      "Tudo do Free",
      "Ferramentas Pro",
      "Novos recursos Pro"
    ]
  },
  PREMIUM: {
    name: "Premium",
    priceUsd: "10.00",
    durationDays: 30,
    billingInterval: "MONTH",
    description:
      "Acesso completo ao Pro e aos recursos Premium do Audio Tools.",
    includes: [
      "Tudo do Free",
      "Tudo do Pro",
      "Ferramentas Premium",
      "Novos recursos Premium"
    ]
  }
};

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

function isoToSeconds(value) {
  if (!value) {
    return null;
  }

  const time = Date.parse(value);

  return Number.isFinite(time)
    ? Math.floor(time / 1000)
    : null;
}

function base64UrlToBytes(value) {
  const normalized = value
    .replace(/-/g, "+")
    .replace(/_/g, "/")
    .padEnd(
      Math.ceil(value.length / 4) * 4,
      "="
    );

  const binary = atob(normalized);
  const bytes = new Uint8Array(
    binary.length
  );

  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }

  return bytes;
}

function decodeBase64UrlJson(value) {
  return JSON.parse(
    new TextDecoder().decode(
      base64UrlToBytes(value)
    )
  );
}

async function getFirebaseJwks() {
  const now = Date.now();

  if (
    jwksCache &&
    jwksExpiresAt > now
  ) {
    return jwksCache;
  }

  const response = await fetch(
    FIREBASE_JWKS_URL,
    {
      headers: {
        accept: "application/json"
      }
    }
  );

  if (!response.ok) {
    throw new Error(
      "Firebase public keys could not be loaded."
    );
  }

  const data = await response.json();
  const cacheControl =
    response.headers.get(
      "cache-control"
    ) || "";
  const match =
    cacheControl.match(
      /max-age=(\d+)/i
    );

  const maxAgeSeconds = match
    ? Number(match[1])
    : 3600;

  jwksCache = data.keys || [];
  jwksExpiresAt =
    now + maxAgeSeconds * 1000;

  return jwksCache;
}

async function verifyFirebaseIdToken(
  idToken,
  env
) {
  const parts = idToken.split(".");

  if (parts.length !== 3) {
    throw new Error(
      "Invalid Firebase token."
    );
  }

  const [
    headerPart,
    payloadPart,
    signaturePart
  ] = parts;

  const header =
    decodeBase64UrlJson(
      headerPart
    );
  const payload =
    decodeBase64UrlJson(
      payloadPart
    );

  if (
    header.alg !== "RS256" ||
    !header.kid
  ) {
    throw new Error(
      "Invalid Firebase token header."
    );
  }

  const projectId =
    env.FIREBASE_PROJECT_ID ||
    "audio-tools-e725";

  const now = nowSeconds();

  if (
    typeof payload.exp !== "number" ||
    payload.exp <= now
  ) {
    throw new Error(
      "Firebase token expired."
    );
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

  const keys =
    await getFirebaseJwks();

  let jwk = keys.find(
    (key) =>
      key.kid === header.kid
  );

  if (!jwk) {
    jwksExpiresAt = 0;

    const refreshed =
      await getFirebaseJwks();

    jwk = refreshed.find(
      (key) =>
        key.kid === header.kid
    );
  }

  if (!jwk) {
    throw new Error(
      "Firebase signing key not found."
    );
  }

  const publicKey =
    await crypto.subtle.importKey(
      "jwk",
      jwk,
      {
        name:
          "RSASSA-PKCS1-v1_5",
        hash: "SHA-256"
      },
      false,
      ["verify"]
    );

  const valid =
    await crypto.subtle.verify(
      {
        name:
          "RSASSA-PKCS1-v1_5"
      },
      publicKey,
      base64UrlToBytes(
        signaturePart
      ),
      new TextEncoder().encode(
        headerPart +
        "." +
        payloadPart
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
      typeof payload.email ===
      "string"
        ? payload.email
        : null
  };
}

function getBearerToken(request) {
  const value =
    request.headers.get(
      "authorization"
    ) || "";

  if (!value.startsWith(
    "Bearer "
  )) {
    return null;
  }

  return value.slice(7).trim() ||
    null;
}

async function requireUser(
  request,
  env
) {
  const token =
    getBearerToken(request);

  if (!token) {
    throw new Response(
      JSON.stringify({
        error:
          "Authentication required."
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
        error:
          "Invalid authentication token."
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
  return env.PAYPAL_ENVIRONMENT ===
    "live"
    ? "https://api-m.paypal.com"
    : "https://api-m.sandbox.paypal.com";
}

async function getPayPalAccessToken(
  env
) {
  const clientId =
    env.PAYPAL_CLIENT_ID;
  const clientSecret =
    env.PAYPAL_CLIENT_SECRET;

  if (
    !clientId ||
    !clientSecret
  ) {
    throw new Error(
      "PayPal credentials are not configured."
    );
  }

  const response =
    await fetch(
      paypalBaseUrl(env) +
        "/v1/oauth2/token",
      {
        method: "POST",
        headers: {
          authorization:
            "Basic " +
            btoa(
              clientId +
              ":" +
              clientSecret
            ),
          "content-type":
            "application/x-www-form-urlencoded",
          accept:
            "application/json"
        },
        body:
          "grant_type=client_credentials"
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

  const data =
    await response.json();

  if (!data.access_token) {
    throw new Error(
      "PayPal did not return an access token."
    );
  }

  return data.access_token;
}

async function tableExists(
  db,
  name
) {
  const result =
    await db.prepare(
      "SELECT name FROM sqlite_master " +
      "WHERE type = 'table' AND name = ?"
    )
      .bind(name)
      .first();

  return Boolean(result);
}

async function tableColumns(
  db,
  tableName
) {
  const result =
    await db.prepare(
      "PRAGMA table_info(" +
      tableName +
      ")"
    ).all();

  return new Set(
    (result.results || [])
      .map(
        (row) => row.name
      )
  );
}

async function rebuildPlansIfLegacy(
  db
) {
  if (
    !await tableExists(
      db,
      "plans"
    )
  ) {
    return;
  }

  const columns =
    await tableColumns(
      db,
      "plans"
    );

  if (
    !columns.has(
      "monthly_credits"
    )
  ) {
    return;
  }

  await db.prepare(
    "CREATE TABLE plans_v2 (" +
    "plan_id TEXT PRIMARY KEY," +
    "name TEXT NOT NULL," +
    "price_usd TEXT NOT NULL DEFAULT '0.00'," +
    "duration_days INTEGER," +
    "billing_interval TEXT NOT NULL DEFAULT 'NONE'," +
    "description TEXT NOT NULL DEFAULT ''," +
    "includes TEXT NOT NULL DEFAULT '[]'," +
    "paypal_product_id TEXT," +
    "paypal_plan_id TEXT," +
    "active INTEGER NOT NULL DEFAULT 1," +
    "created_at INTEGER NOT NULL," +
    "updated_at INTEGER NOT NULL)"
  ).run();

  await db.prepare(
    "INSERT INTO plans_v2 (" +
    "plan_id, name, price_usd, duration_days, " +
    "active, created_at, updated_at) " +
    "SELECT plan_id, name, price_usd, duration_days, " +
    "active, created_at, updated_at " +
    "FROM plans"
  ).run();

  await db.prepare(
    "DROP TABLE plans"
  ).run();

  await db.prepare(
    "ALTER TABLE plans_v2 " +
    "RENAME TO plans"
  ).run();
}

async function rebuildEntitlementsIfLegacy(
  db
) {
  if (
    !await tableExists(
      db,
      "entitlements"
    )
  ) {
    return;
  }

  const columns =
    await tableColumns(
      db,
      "entitlements"
    );

  if (
    !columns.has(
      "last_order_id"
    )
  ) {
    return;
  }

  await db.prepare(
    "CREATE TABLE entitlements_v2 (" +
    "firebase_uid TEXT PRIMARY KEY," +
    "plan TEXT NOT NULL DEFAULT 'FREE'," +
    "starts_at INTEGER," +
    "expires_at INTEGER," +
    "status TEXT NOT NULL DEFAULT 'ACTIVE'," +
    "source TEXT NOT NULL DEFAULT 'SYSTEM'," +
    "updated_at INTEGER NOT NULL," +
    "last_subscription_id TEXT)"
  ).run();

  await db.prepare(
    "INSERT INTO entitlements_v2 (" +
    "firebase_uid, plan, starts_at, expires_at, " +
    "status, source, updated_at) " +
    "SELECT firebase_uid, plan, starts_at, expires_at, " +
    "status, source, updated_at " +
    "FROM entitlements"
  ).run();

  await db.prepare(
    "DROP TABLE entitlements"
  ).run();

  await db.prepare(
    "ALTER TABLE entitlements_v2 " +
    "RENAME TO entitlements"
  ).run();
}

async function ensureDatabaseSchema(db) {
  await db.prepare(
    "CREATE TABLE IF NOT EXISTS schema_meta (" +
    "key TEXT PRIMARY KEY," +
    "value TEXT NOT NULL)"
  ).run();

  await db.prepare(
    "DROP TABLE IF EXISTS credit_transactions"
  ).run();

  await db.prepare(
    "DROP TABLE IF EXISTS credit_accounts"
  ).run();

  await db.prepare(
    "DROP TABLE IF EXISTS paypal_orders"
  ).run();

  await db.prepare(
    "DROP TABLE IF EXISTS tool_usage"
  ).run();

  await rebuildPlansIfLegacy(
    db
  );

  await rebuildEntitlementsIfLegacy(
    db
  );

  await db.prepare(
    "CREATE TABLE IF NOT EXISTS support_requests (" +
    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "firebase_uid TEXT NOT NULL," +
    "account_email TEXT NOT NULL DEFAULT ''," +
    "display_name TEXT NOT NULL DEFAULT ''," +
    "plan TEXT NOT NULL DEFAULT 'FREE'," +
    "request_type TEXT NOT NULL," +
    "subject TEXT NOT NULL," +
    "message TEXT NOT NULL," +
    "app_version TEXT NOT NULL DEFAULT ''," +
    "created_at INTEGER NOT NULL," +
    "status TEXT NOT NULL DEFAULT 'PENDING'," +
    "email_id TEXT," +
    "last_error TEXT," +
    "updated_at INTEGER NOT NULL)"
  ).run();

  await db.prepare(
    "CREATE INDEX IF NOT EXISTS " +
    "idx_support_requests_uid " +
    "ON support_requests(firebase_uid, created_at)"
  ).run();

  await db.prepare(
    "CREATE TABLE IF NOT EXISTS plans (" +
    "plan_id TEXT PRIMARY KEY," +
    "name TEXT NOT NULL," +
    "price_usd TEXT NOT NULL DEFAULT '0.00'," +
    "duration_days INTEGER," +
    "billing_interval TEXT NOT NULL DEFAULT 'NONE'," +
    "description TEXT NOT NULL DEFAULT ''," +
    "includes TEXT NOT NULL DEFAULT '[]'," +
    "paypal_product_id TEXT," +
    "paypal_plan_id TEXT," +
    "active INTEGER NOT NULL DEFAULT 1," +
    "created_at INTEGER NOT NULL," +
    "updated_at INTEGER NOT NULL)"
  ).run();

  let planColumns =
    await tableColumns(
      db,
      "plans"
    );

  const planAdditions = [
    [
      "billing_interval",
      "TEXT NOT NULL DEFAULT 'NONE'"
    ],
    [
      "description",
      "TEXT NOT NULL DEFAULT ''"
    ],
    [
      "includes",
      "TEXT NOT NULL DEFAULT '[]'"
    ],
    [
      "paypal_product_id",
      "TEXT"
    ],
    [
      "paypal_plan_id",
      "TEXT"
    ]
  ];

  for (
    const [name, definition]
    of planAdditions
  ) {
    if (!planColumns.has(name)) {
      await db.prepare(
        "ALTER TABLE plans ADD COLUMN " +
        name +
        " " +
        definition
      ).run();
    }
  }

  await db.prepare(
    "CREATE TABLE IF NOT EXISTS entitlements (" +
    "firebase_uid TEXT PRIMARY KEY," +
    "plan TEXT NOT NULL DEFAULT 'FREE'," +
    "starts_at INTEGER," +
    "expires_at INTEGER," +
    "status TEXT NOT NULL DEFAULT 'ACTIVE'," +
    "source TEXT NOT NULL DEFAULT 'SYSTEM'," +
    "updated_at INTEGER NOT NULL," +
    "last_subscription_id TEXT)"
  ).run();

  const entitlementColumns =
    await tableColumns(
      db,
      "entitlements"
    );

  if (
    !entitlementColumns.has(
      "last_subscription_id"
    )
  ) {
    await db.prepare(
      "ALTER TABLE entitlements ADD COLUMN " +
      "last_subscription_id TEXT"
    ).run();
  }

  await db.prepare(
    "CREATE TABLE IF NOT EXISTS paypal_subscriptions (" +
    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
    "firebase_uid TEXT NOT NULL," +
    "subscription_id TEXT NOT NULL UNIQUE," +
    "plan_id TEXT NOT NULL," +
    "paypal_plan_id TEXT NOT NULL," +
    "status TEXT NOT NULL DEFAULT 'APPROVAL_PENDING'," +
    "created_at INTEGER NOT NULL," +
    "updated_at INTEGER NOT NULL," +
    "approved_at INTEGER," +
    "current_period_end INTEGER," +
    "next_billing_time INTEGER," +
    "payer_id TEXT)"
  ).run();

  await db.prepare(
    "CREATE INDEX IF NOT EXISTS " +
    "idx_entitlements_plan " +
    "ON entitlements(plan)"
  ).run();

  await db.prepare(
    "CREATE INDEX IF NOT EXISTS " +
    "idx_entitlements_status " +
    "ON entitlements(status)"
  ).run();

  await db.prepare(
    "CREATE INDEX IF NOT EXISTS " +
    "idx_paypal_subscriptions_uid " +
    "ON paypal_subscriptions(firebase_uid)"
  ).run();

  await db.prepare(
    "CREATE INDEX IF NOT EXISTS " +
    "idx_paypal_subscriptions_status " +
    "ON paypal_subscriptions(status)"
  ).run();

  const timestamp =
    nowSeconds();

  for (
    const [planId, definition]
    of Object.entries(
      PLAN_DEFINITIONS
    )
  ) {
    await db.prepare(
      "INSERT OR IGNORE INTO plans (" +
      "plan_id, name, price_usd, duration_days, " +
      "billing_interval, description, includes, " +
      "active, created_at, updated_at) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, 1, ?, ?)"
    )
      .bind(
        planId,
        definition.name,
        definition.priceUsd,
        definition.durationDays,
        definition.billingInterval,
        definition.description,
        JSON.stringify(
          definition.includes
        ),
        timestamp,
        timestamp
      )
      .run();
  }

  for (
    const [planId, definition]
    of Object.entries(
      PLAN_DEFINITIONS
    )
  ) {
    await db.prepare(
      "UPDATE plans SET " +
      "name = ?, price_usd = ?, duration_days = ?, " +
      "billing_interval = ?, description = ?, " +
      "includes = ?, active = 1, updated_at = ? " +
      "WHERE plan_id = ?"
    )
      .bind(
        definition.name,
        definition.priceUsd,
        definition.durationDays,
        definition.billingInterval,
        definition.description,
        JSON.stringify(
          definition.includes
        ),
        timestamp,
        planId
      )
      .run();
  }

  await db.prepare(
    "INSERT OR REPLACE INTO schema_meta " +
    "(key, value) VALUES ('version', '3')"
  ).run();
}

async function ensureEntitlement(
  db,
  uid
) {
  const existing =
    await db.prepare(
      "SELECT firebase_uid, plan, starts_at, " +
      "expires_at, status, source, updated_at, " +
      "last_subscription_id " +
      "FROM entitlements " +
      "WHERE firebase_uid = ?"
    )
      .bind(uid)
      .first();

  if (existing) {
    return existing;
  }

  const timestamp =
    nowSeconds();

  await db.prepare(
    "INSERT OR IGNORE INTO entitlements (" +
    "firebase_uid, plan, starts_at, expires_at, " +
    "status, source, updated_at, " +
    "last_subscription_id) " +
    "VALUES (?, 'FREE', ?, NULL, 'ACTIVE', " +
    "'SYSTEM', ?, NULL)"
  )
    .bind(
      uid,
      timestamp,
      timestamp
    )
    .run();

  return db.prepare(
    "SELECT firebase_uid, plan, starts_at, " +
    "expires_at, status, source, updated_at, " +
    "last_subscription_id " +
    "FROM entitlements " +
    "WHERE firebase_uid = ?"
  )
    .bind(uid)
    .first();
}

function planRank(plan) {
  if (plan === "PREMIUM") {
    return 2;
  }

  if (plan === "PRO") {
    return 1;
  }

  return 0;
}

function hasPlanAccess(
  currentPlan,
  requiredPlan
) {
  return (
    planRank(currentPlan) >=
    planRank(requiredPlan)
  );
}

async function getPlan(
  db,
  planId
) {
  return db.prepare(
    "SELECT * FROM plans " +
    "WHERE plan_id = ? AND active = 1"
  )
    .bind(planId)
    .first();
}

function parsePlanIncludes(
  value
) {
  try {
    const parsed =
      JSON.parse(value || "[]");

    return Array.isArray(parsed)
      ? parsed
      : [];
  } catch {
    return [];
  }
}

async function updateEntitlementFromSubscription(
  db,
  uid,
  subscription
) {
  const status =
    subscription.status || "";

  const nextBillingTime =
    isoToSeconds(
      subscription?.billing_info
        ?.next_billing_time
    );

  const startTime =
    isoToSeconds(
      subscription.start_time
    ) || nowSeconds();

  const payerId =
    subscription?.subscriber
      ?.payer_id || null;

  const now =
    nowSeconds();

  const local =
    await db.prepare(
      "SELECT plan_id " +
      "FROM paypal_subscriptions " +
      "WHERE subscription_id = ? " +
      "AND firebase_uid = ?"
    )
      .bind(
        subscription.id,
        uid
      )
      .first();

  if (!local) {
    return null;
  }

  await db.prepare(
    "UPDATE paypal_subscriptions SET " +
    "status = ?, updated_at = ?, " +
    "approved_at = CASE " +
    "WHEN ? IN ('APPROVED', 'ACTIVE') " +
    "THEN COALESCE(approved_at, ?) " +
    "ELSE approved_at END, " +
    "current_period_end = ?, " +
    "next_billing_time = ?, " +
    "payer_id = ? " +
    "WHERE subscription_id = ? " +
    "AND firebase_uid = ?"
  )
    .bind(
      status,
      now,
      status,
      now,
      nextBillingTime,
      nextBillingTime,
      payerId,
      subscription.id,
      uid
    )
    .run();

  const active =
    (
      status === "ACTIVE" ||
      status === "APPROVED"
    ) &&
    nextBillingTime !== null &&
    nextBillingTime > now;

  const cancelledWithTime =
    status === "CANCELLED" &&
    nextBillingTime !== null &&
    nextBillingTime > now;

  const entitlement =
    await ensureEntitlement(
      db,
      uid
    );

  if (
    (
      active ||
      cancelledWithTime
    ) &&
    hasPlanAccess(
      local.plan_id,
      "PRO"
    )
  ) {
    await db.prepare(
      "UPDATE entitlements SET " +
      "plan = ?, starts_at = ?, expires_at = ?, " +
      "status = 'ACTIVE', source = 'PAYPAL', " +
      "updated_at = ?, last_subscription_id = ? " +
      "WHERE firebase_uid = ?"
    )
      .bind(
        local.plan_id,
        startTime,
        nextBillingTime,
        now,
        subscription.id,
        uid
      )
      .run();
  } else if (
    entitlement.plan ===
    local.plan_id
  ) {
    await db.prepare(
      "UPDATE entitlements SET " +
      "plan = 'FREE', starts_at = NULL, " +
      "expires_at = NULL, status = 'ACTIVE', " +
      "source = 'SYSTEM', updated_at = ? " +
      "WHERE firebase_uid = ?"
    )
      .bind(
        now,
        uid
      )
      .run();
  }

  return db.prepare(
    "SELECT firebase_uid, plan, starts_at, " +
    "expires_at, status, source, updated_at, " +
    "last_subscription_id " +
    "FROM entitlements " +
    "WHERE firebase_uid = ?"
  )
    .bind(uid)
    .first();
}

async function syncUserSubscription(
  db,
  env,
  uid
) {
  const entitlement =
    await ensureEntitlement(
      db,
      uid
    );

  if (
    !entitlement.last_subscription_id
  ) {
    return entitlement;
  }

  try {
    const accessToken =
      await getPayPalAccessToken(
        env
      );

    const response =
      await fetch(
        paypalBaseUrl(env) +
        "/v1/billing/subscriptions/" +
        encodeURIComponent(
          entitlement
            .last_subscription_id
        ),
        {
          method: "GET",
          headers: {
            authorization:
              "Bearer " +
              accessToken,
            accept:
              "application/json"
          }
        }
      );

    const data =
      await response.json();

    if (!response.ok) {
      console.error(
        "PayPal subscription lookup failed",
        response.status
      );
      return entitlement;
    }

    return (
      await updateEntitlementFromSubscription(
        db,
        uid,
        data
      )
    ) || entitlement;
  } catch (error) {
    console.error(
      "PayPal entitlement sync failed",
      error
    );

    return entitlement;
  }
}

async function listPlans(
  env
) {
  const result =
    await env.DB.prepare(
      "SELECT plan_id, name, price_usd, " +
      "duration_days, billing_interval, " +
      "description, includes, active " +
      "FROM plans WHERE active = 1 " +
      "ORDER BY CASE plan_id " +
      "WHEN 'FREE' THEN 1 " +
      "WHEN 'PRO' THEN 2 " +
      "WHEN 'PREMIUM' THEN 3 " +
      "ELSE 4 END"
    ).all();

  return json({
    ok: true,
    plans:
      (result.results || [])
        .map((plan) => ({
          id: plan.plan_id,
          name: plan.name,
          priceUsd:
            Number(
              plan.price_usd
            ),
          durationDays:
            plan.duration_days ===
            null
              ? null
              : Number(
                  plan.duration_days
                ),
          billingInterval:
            plan.billing_interval,
          description:
            plan.description,
          includes:
            parsePlanIncludes(
              plan.includes
            ),
          active:
            Number(plan.active) === 1
        }))
  });
}

async function createPayPalProduct(
  env,
  accessToken,
  plan
) {
  const response =
    await fetch(
      paypalBaseUrl(env) +
      "/v1/catalogs/products",
      {
        method: "POST",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          "content-type":
            "application/json",
          accept:
            "application/json",
          "paypal-request-id":
            "audio-tools-product-" +
            plan.plan_id
              .toLowerCase() +
            "-" +
            crypto.randomUUID()
        },
        body: JSON.stringify({
          name:
            "Audio Tools " +
            plan.name,
          description:
            plan.description,
          type: "SERVICE",
          category: "SOFTWARE"
        })
      }
    );

  const data =
    await response.json();

  if (
    !response.ok ||
    !data.id
  ) {
    console.error(
      "PayPal product creation failed",
      response.status,
      data
    );

    throw new Error(
      "PayPal could not create the subscription product."
    );
  }

  return data.id;
}

async function createPayPalBillingPlan(
  env,
  accessToken,
  plan,
  productId
) {
  const response =
    await fetch(
      paypalBaseUrl(env) +
      "/v1/billing/plans",
      {
        method: "POST",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          "content-type":
            "application/json",
          accept:
            "application/json",
          "paypal-request-id":
            "audio-tools-plan-" +
            plan.plan_id
              .toLowerCase() +
            "-" +
            crypto.randomUUID()
        },
        body: JSON.stringify({
          product_id:
            productId,
          name:
            "Audio Tools " +
            plan.name +
            " Monthly",
          description:
            plan.description,
          status: "ACTIVE",
          billing_cycles: [
            {
              frequency: {
                interval_unit:
                  "MONTH",
                interval_count:
                  1
              },
              tenure_type:
                "REGULAR",
              sequence: 1,
              total_cycles: 0,
              pricing_scheme: {
                fixed_price: {
                  value:
                    Number(
                      plan.price_usd
                    )
                      .toFixed(2),
                  currency_code:
                    "USD"
                }
              }
            }
          ],
          payment_preferences: {
            auto_bill_outstanding:
              true,
            payment_failure_threshold:
              2
          }
        })
      }
    );

  const data =
    await response.json();

  if (
    !response.ok ||
    !data.id
  ) {
    console.error(
      "PayPal billing plan creation failed",
      response.status,
      data
    );

    throw new Error(
      "PayPal could not create the subscription plan."
    );
  }

  return data.id;
}

async function ensurePayPalBillingPlan(
  env,
  plan
) {
  if (
    plan.paypal_plan_id
  ) {
    return plan.paypal_plan_id;
  }

  const accessToken =
    await getPayPalAccessToken(
      env
    );

  const productId =
    plan.paypal_product_id ||
    await createPayPalProduct(
      env,
      accessToken,
      plan
    );

  if (!plan.paypal_product_id) {
    await env.DB.prepare(
      "UPDATE plans SET " +
      "paypal_product_id = ?, updated_at = ? " +
      "WHERE plan_id = ?"
    )
      .bind(
        productId,
        nowSeconds(),
        plan.plan_id
      )
      .run();
  }

  const paypalPlanId =
    await createPayPalBillingPlan(
      env,
      accessToken,
      plan,
      productId
    );

  await env.DB.prepare(
    "UPDATE plans SET " +
    "paypal_plan_id = ?, updated_at = ? " +
    "WHERE plan_id = ?"
  )
    .bind(
      paypalPlanId,
      nowSeconds(),
      plan.plan_id
    )
    .run();

  return paypalPlanId;
}

async function createPayPalSubscription(
  request,
  env,
  user
) {
  const body =
    await request
      .json()
      .catch(() => ({}));

  const planId =
    typeof body.planId ===
    "string"
      ? body.planId
          .trim()
          .toUpperCase()
      : "";

  if (
    planId !== "PRO" &&
    planId !== "PREMIUM"
  ) {
    return json(
      {
        error:
          "Escolha um plano mensal válido."
      },
      400
    );
  }

  const current =
    await syncUserSubscription(
      env.DB,
      env,
      user.uid
    );

  if (
    current.plan !== "FREE" &&
    current.status === "ACTIVE" &&
    Number(
      current.expires_at || 0
    ) > nowSeconds()
  ) {
    return json(
      {
        error:
          "Esta conta já tem um plano pago ativo.",
        plan:
          current.plan,
        expiresAt:
          Number(
            current.expires_at
          )
      },
      409
    );
  }

  const plan =
    await getPlan(
      env.DB,
      planId
    );

  if (!plan) {
    return json(
      {
        error:
          "Plano não encontrado."
      },
      404
    );
  }

  const paypalPlanId =
    await ensurePayPalBillingPlan(
      env,
      plan
    );

  const accessToken =
    await getPayPalAccessToken(
      env
    );

  const origin =
    new URL(request.url)
      .origin;

  const payload = {
    plan_id:
      paypalPlanId,
    ...(user.email
      ? {
          subscriber: {
            email_address:
              user.email
          }
        }
      : {}),
    application_context: {
      brand_name:
        "Audio Tools",
      locale:
        "pt-PT",
      shipping_preference:
        "NO_SHIPPING",
      user_action:
        "SUBSCRIBE_NOW",
      return_url:
        origin +
        "/api/paypal/return",
      cancel_url:
        origin +
        "/api/paypal/cancel"
    }
  };

  const response =
    await fetch(
      paypalBaseUrl(env) +
      "/v1/billing/subscriptions",
      {
        method: "POST",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          "content-type":
            "application/json",
          accept:
            "application/json",
          "paypal-request-id":
            "audio-tools-subscription-" +
            crypto.randomUUID()
        },
        body:
          JSON.stringify(
            payload
          )
      }
    );

  const data =
    await response.json();

  if (!response.ok) {
    console.error(
      "PayPal subscription creation failed",
      response.status,
      data
    );

    return json(
      {
        error:
          "PayPal não conseguiu criar a assinatura.",
        details:
          data?.details?.[0]
            ?.description ||
          data?.message ||
          null
      },
      502
    );
  }

  const approvalUrl =
    (data.links || [])
      .find(
        (link) =>
          link.rel ===
          "approve"
      )?.href || null;

  if (
    !data.id ||
    !approvalUrl
  ) {
    return json(
      {
        error:
          "PayPal devolveu uma assinatura incompleta."
      },
      502
    );
  }

  const timestamp =
    nowSeconds();

  await env.DB.prepare(
    "INSERT INTO paypal_subscriptions (" +
    "firebase_uid, subscription_id, plan_id, " +
    "paypal_plan_id, status, created_at, " +
    "updated_at) VALUES (?, ?, ?, ?, ?, ?, ?)"
  )
    .bind(
      user.uid,
      data.id,
      planId,
      paypalPlanId,
      data.status ||
        "APPROVAL_PENDING",
      timestamp,
      timestamp
    )
    .run();

  return json({
    ok: true,
    subscriptionId:
      data.id,
    approvalUrl,
    plan:
      planId,
    amount:
      Number(
        plan.price_usd
      ),
    currency:
      "USD",
    billingInterval:
      plan.billing_interval
  });
}

async function getPayPalSubscription(
  env,
  accessToken,
  subscriptionId
) {
  const response =
    await fetch(
      paypalBaseUrl(env) +
      "/v1/billing/subscriptions/" +
      encodeURIComponent(
        subscriptionId
      ),
      {
        method: "GET",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          accept:
            "application/json"
        }
      }
    );

  const data =
    await response.json();

  if (!response.ok) {
    console.error(
      "PayPal subscription details failed",
      response.status,
      data
    );

    throw new Error(
      "Não foi possível verificar a assinatura no PayPal."
    );
  }

  return data;
}

async function activatePayPalSubscription(
  env,
  accessToken,
  subscriptionId
) {
  const response =
    await fetch(
      paypalBaseUrl(env) +
      "/v1/billing/subscriptions/" +
      encodeURIComponent(
        subscriptionId
      ) +
      "/activate",
      {
        method: "POST",
        headers: {
          authorization:
            "Bearer " +
            accessToken,
          "content-type":
            "application/json",
          accept:
            "application/json"
        },
        body:
          JSON.stringify({
            reason:
              "Customer approved the Audio Tools subscription."
          })
      }
    );

  if (
    response.status === 204 ||
    response.ok
  ) {
    return;
  }

  const data =
    await response.json()
      .catch(() => ({}));

  throw new Error(
    data?.details?.[0]
      ?.description ||
    data?.message ||
    "Não foi possível ativar a assinatura."
  );
}

async function activateSubscription(
  request,
  env,
  user
) {
  const body =
    await request
      .json()
      .catch(() => ({}));

  const subscriptionId =
    typeof body.subscriptionId ===
    "string"
      ? body.subscriptionId
          .trim()
      : "";

  if (!subscriptionId) {
    return json(
      {
        error:
          "subscriptionId is required."
      },
      400
    );
  }

  const local =
    await env.DB.prepare(
      "SELECT * FROM paypal_subscriptions " +
      "WHERE subscription_id = ? " +
      "AND firebase_uid = ?"
    )
      .bind(
        subscriptionId,
        user.uid
      )
      .first();

  if (!local) {
    return json(
      {
        error:
          "Assinatura não encontrada para esta conta."
      },
      404
    );
  }

  const accessToken =
    await getPayPalAccessToken(
      env
    );

  let subscription =
    await getPayPalSubscription(
      env,
      accessToken,
      subscriptionId
    );

  if (
    subscription.plan_id !==
    local.paypal_plan_id
  ) {
    return json(
      {
        error:
          "A assinatura não corresponde ao plano solicitado."
      },
      400
    );
  }

  if (
    subscription.status ===
    "APPROVED"
  ) {
    try {
      await activatePayPalSubscription(
        env,
        accessToken,
        subscriptionId
      );

      subscription =
        await getPayPalSubscription(
          env,
          accessToken,
          subscriptionId
        );
    } catch (error) {
      console.error(
        "PayPal activation failed",
        error
      );

      if (
        subscription.status !==
        "ACTIVE"
      ) {
        return json(
          {
            error:
              "PayPal ainda não ativou a assinatura.",
            details:
              error.message
          },
          502
        );
      }
    }
  }

  const entitlement =
    await updateEntitlementFromSubscription(
      env.DB,
      user.uid,
      subscription
    );

  if (!entitlement) {
    return json(
      {
        error:
          "Não foi possível atualizar o acesso."
      },
      500
    );
  }

  if (
    entitlement.plan !==
    local.plan_id
  ) {
    return json(
      {
        error:
          "Pagamento ainda não está ativo.",
        status:
          subscription.status
      },
      409
    );
  }

  return json({
    ok: true,
    plan:
      entitlement.plan,
    status:
      entitlement.status,
    expiresAt:
      entitlement.expires_at
        ? Number(
            entitlement.expires_at
          )
        : null,
    subscriptionId:
      subscriptionId
  });
}

async function entitlement(
  env,
  user
) {
  let current =
    await ensureEntitlement(
      env.DB,
      user.uid
    );

  const now =
    nowSeconds();

  if (
    current.plan !== "FREE" &&
    current.expires_at &&
    Number(
      current.expires_at
    ) <= now
  ) {
    await env.DB.prepare(
      "UPDATE entitlements SET " +
      "plan = 'FREE', starts_at = NULL, " +
      "expires_at = NULL, status = 'ACTIVE', " +
      "source = 'SYSTEM', updated_at = ? " +
      "WHERE firebase_uid = ?"
    )
      .bind(
        now,
        user.uid
      )
      .run();
  }

  current =
    await syncUserSubscription(
      env.DB,
      env,
      user.uid
    );

  return json({
    ok: true,
    plan:
      current.plan ||
      "FREE",
    isPro:
      hasPlanAccess(
        current.plan ||
          "FREE",
        "PRO"
      ) &&
      current.status ===
        "ACTIVE",
    isPremium:
      hasPlanAccess(
        current.plan ||
          "FREE",
        "PREMIUM"
      ) &&
      current.status ===
        "ACTIVE",
    expiresAt:
      current.expires_at
        ? Number(
            current.expires_at
          )
        : null,
    subscriptionId:
      current.last_subscription_id ||
      null
  });
}

function supportTypeLabel(
  type
) {
  switch (type) {
    case "COMPLAINT":
      return "Reclamação";
    case "SUGGESTION":
      return "Sugestão";
    default:
      return "Suporte";
  }
}

function escapeHtml(
  value
) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

async function sendSupportContact(
  request,
  env,
  user
) {
  const body =
    await request
      .json()
      .catch(() => ({}));

  const type =
    typeof body.type === "string"
      ? body.type
          .trim()
          .toUpperCase()
      : "SUPPORT";

  const subject =
    typeof body.subject === "string"
      ? body.subject
          .trim()
          .slice(0, 120)
      : "";

  const message =
    typeof body.message === "string"
      ? body.message
          .trim()
          .slice(0, 5000)
      : "";

  const name =
    typeof body.name === "string"
      ? body.name
          .trim()
          .slice(0, 120)
      : "";

  const appVersion =
    typeof body.appVersion === "string"
      ? body.appVersion
          .trim()
          .slice(0, 40)
      : "";

  if (
    ![
      "SUPPORT",
      "COMPLAINT",
      "SUGGESTION"
    ].includes(type)
  ) {
    return json(
      {
        error:
          "Tipo de pedido inválido."
      },
      400
    );
  }

  if (!subject) {
    return json(
      {
        error:
          "O assunto é obrigatório."
      },
      400
    );
  }

  if (message.length < 5) {
    return json(
      {
        error:
          "Escreve uma mensagem mais completa."
      },
      400
    );
  }

  const timestamp =
    nowSeconds();

  const recent =
    await env.DB.prepare(
      "SELECT COUNT(*) AS count " +
      "FROM support_requests " +
      "WHERE firebase_uid = ? " +
      "AND created_at >= ?"
    )
      .bind(
        user.uid,
        timestamp - 3600
      )
      .first();

  if (
    Number(
      recent?.count || 0
    ) >= 10
  ) {
    return json(
      {
        error:
          "Limite temporário de pedidos atingido. Tenta novamente mais tarde."
      },
      429
    );
  }

  const entitlement =
    await ensureEntitlement(
      env.DB,
      user.uid
    );

  const plan =
    entitlement?.plan ||
    "FREE";

  const accountEmail =
    typeof user.email === "string"
      ? user.email
          .trim()
          .slice(0, 320)
      : "";

  const insert =
    await env.DB.prepare(
      "INSERT INTO support_requests (" +
      "firebase_uid, account_email, display_name, " +
      "plan, request_type, subject, message, " +
      "app_version, created_at, status, updated_at) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
    )
      .bind(
        user.uid,
        accountEmail,
        name,
        plan,
        type,
        subject,
        message,
        appVersion,
        timestamp,
        "PENDING",
        timestamp
      )
      .run();

  const requestId =
    insert?.meta?.last_row_id ||
    (String(timestamp) + "-" +
      crypto.randomUUID());

  const apiKey =
    env.RESEND_API_KEY;

  const to =
    env.SUPPORT_TO_EMAIL;

  const from =
    env.RESEND_FROM_EMAIL;

  if (
    !apiKey ||
    !to ||
    !from
  ) {
    await env.DB.prepare(
      "UPDATE support_requests " +
      "SET status = 'ERROR', " +
      "last_error = ?, updated_at = ? " +
      "WHERE id = ?"
    )
      .bind(
        "Email de suporte não configurado.",
        timestamp,
        requestId
      )
      .run();

    return json(
      {
        error:
          "O email de suporte ainda não está configurado."
      },
      503
    );
  }

  const label =
    supportTypeLabel(type);

  const safeName =
    escapeHtml(
      name || "Não informado"
    );

  const safeEmail =
    escapeHtml(
      accountEmail ||
        "Não informado"
    );

  const safeUid =
    escapeHtml(
      user.uid
    );

  const safePlan =
    escapeHtml(
      plan
    );

  const safeVersion =
    escapeHtml(
      appVersion ||
        "Não informado"
    );

  const safeSubject =
    escapeHtml(
      subject
    );

  const safeMessage =
    escapeHtml(
      message
    );

  const emailPayload = {
    from,
    to: [to],
    subject:
      "[Audio Tools · " +
      label +
      "] " +
      subject,
    html:
      "<div style='font-family:Arial,sans-serif;" +
      "max-width:720px;margin:auto;color:#18212f'>" +
      "<h2>Audio Tools · " +
      escapeHtml(label) +
      "</h2>" +
      "<p><strong>Assunto:</strong> " +
      safeSubject +
      "</p>" +
      "<hr>" +
      "<p><strong>Nome:</strong> " +
      safeName +
      "</p>" +
      "<p><strong>Email:</strong> " +
      safeEmail +
      "</p>" +
      "<p><strong>Firebase UID:</strong> " +
      safeUid +
      "</p>" +
      "<p><strong>Plano:</strong> " +
      safePlan +
      "</p>" +
      "<p><strong>Versão:</strong> " +
      safeVersion +
      "</p>" +
      "<p><strong>Data:</strong> " +
      new Date(
        timestamp * 1000
      ).toISOString() +
      "</p>" +
      "<h3>Mensagem</h3>" +
      "<div style='white-space:pre-wrap;" +
      "padding:16px;border:1px solid #dfe5ee;" +
      "border-radius:12px;background:#f7f9fc'>" +
      safeMessage +
      "</div>" +
      "</div>"
  };

  if (
    /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(
      accountEmail
    )
  ) {
    emailPayload.reply_to =
      [accountEmail];
  }

  try {
    const response =
      await fetch(
        "https://api.resend.com/emails",
        {
          method: "POST",
          headers: {
            Authorization:
              "Bearer " +
              apiKey,
            "Content-Type":
              "application/json",
            "Idempotency-Key":
              "audio-tools-support/" +
              requestId
          },
          body:
            JSON.stringify(
              emailPayload
            )
        }
      );

    const result =
      await response
        .json()
        .catch(() => ({}));

    if (!response.ok) {
      const errorText =
        result?.message ||
        result?.error ||
        "Resend recusou o email.";

      await env.DB.prepare(
        "UPDATE support_requests " +
        "SET status = 'ERROR', " +
        "last_error = ?, updated_at = ? " +
        "WHERE id = ?"
      )
        .bind(
          String(errorText)
            .slice(0, 1000),
          nowSeconds(),
          requestId
        )
        .run();

      return json(
        {
          error:
            "Não foi possível enviar o email agora."
        },
        502
      );
    }

    const emailId =
      result?.id ||
      null;

    await env.DB.prepare(
      "UPDATE support_requests " +
      "SET status = 'SENT', " +
      "email_id = ?, " +
      "last_error = NULL, " +
      "updated_at = ? " +
      "WHERE id = ?"
    )
      .bind(
        emailId,
        nowSeconds(),
        requestId
      )
      .run();

    return json({
      ok: true,
      requestId:
        String(requestId),
      status:
        "SENT"
    });
  } catch (error) {
    await env.DB.prepare(
      "UPDATE support_requests " +
      "SET status = 'ERROR', " +
      "last_error = ?, updated_at = ? " +
      "WHERE id = ?"
    )
      .bind(
        String(
          error?.message ||
            "Email send failed."
        ).slice(0, 1000),
        nowSeconds(),
        requestId
      )
      .run();

    return json(
      {
        error:
          "Não foi possível enviar o email agora."
      },
      502
    );
  }
}

function returnPage(
  request
) {
  const url =
    new URL(request.url);

  const subscriptionId =
    url.searchParams.get(
      "subscription_id"
    ) ||
    url.searchParams.get(
      "token"
    ) ||
    "";

  const safeId =
    subscriptionId.replace(
      /[^a-zA-Z0-9_-]/g,
      ""
    );

  const deepLink =
    "audiotools://paypal/complete" +
    "?subscription_id=" +
    encodeURIComponent(
      safeId
    );

  const returnLink =
    safeId
      ? "<p><a href='" +
        deepLink +
        "' style='display:inline-block;" +
        "padding:14px 18px;border-radius:12px;" +
        "background:#2F6BFF;color:#fff;" +
        "text-decoration:none'>" +
        "Voltar ao Audio Tools</a></p>"
      : "<p>Assinatura não identificada.</p>";

  return html(
    "<!doctype html>" +
    "<html><head>" +
    "<meta charset='utf-8'>" +
    "<meta name='viewport' " +
    "content='width=device-width,initial-scale=1'>" +
    "<title>Audio Tools</title>" +
    "</head><body style='" +
    "font-family:system-ui;padding:32px;" +
    "max-width:560px;margin:auto'>" +
    "<h1>Pagamento recebido</h1>" +
    "<p>Volta ao Audio Tools para concluir " +
    "a ativação do teu plano.</p>" +
    returnLink +
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
    "<title>Audio Tools</title>" +
    "</head><body style='" +
    "font-family:system-ui;padding:32px;" +
    "max-width:560px;margin:auto'>" +
    "<h1>Assinatura cancelada</h1>" +
    "<p>Nenhuma nova assinatura foi ativada.</p>" +
    "</body></html>"
  );
}

export default {
  async fetch(request, env) {
    if (
      request.method ===
      "OPTIONS"
    ) {
      return new Response(
        null,
        {
          status: 204,
          headers:
            corsHeaders()
        }
      );
    }

    const url =
      new URL(request.url);

    try {
      await ensureDatabaseSchema(
        env.DB
      );

      if (
        request.method === "GET" &&
        url.pathname === "/"
      ) {
        return json({
          ok: true,
          service:
            "audio-tools",
          environment:
            env.PAYPAL_ENVIRONMENT ||
            "sandbox",
          version: "4",
          billing:
            "subscriptions"
        });
      }

      if (
        request.method === "GET" &&
        url.pathname ===
          "/api/plans"
      ) {
        return listPlans(env);
      }

      if (
        request.method === "GET" &&
        url.pathname ===
          "/api/paypal/return"
      ) {
        return returnPage(
          request
        );
      }

      if (
        request.method === "GET" &&
        url.pathname ===
          "/api/paypal/cancel"
      ) {
        return cancelPage();
      }

      const user =
        await requireUser(
          request,
          env
        );

      if (
        request.method === "GET" &&
        url.pathname ===
          "/api/entitlement"
      ) {
        return entitlement(
          env,
          user
        );
      }

      if (
        request.method === "POST" &&
        url.pathname ===
          "/api/paypal/create-subscription"
      ) {
        return createPayPalSubscription(
          request,
          env,
          user
        );
      }

      if (
        request.method === "POST" &&
        url.pathname ===
          "/api/paypal/activate-subscription"
      ) {
        return activateSubscription(
          request,
          env,
          user
        );
      }

      if (
        request.method === "POST" &&
        url.pathname ===
          "/api/support/contact"
      ) {
        return sendSupportContact(
          request,
          env,
          user
        );
      }

      return json(
        {
          error:
            "Not found."
        },
        404
      );
    } catch (error) {
      if (
        error instanceof Response
      ) {
        return error;
      }

      console.error(
        "Unhandled Worker error",
        error
      );

      return json(
        {
          error:
            error?.message ||
            "Internal server error."
        },
        500
      );
    }
  }
};
