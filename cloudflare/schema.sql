PRAGMA foreign_keys = ON;

DROP TABLE IF EXISTS plans;

CREATE TABLE plans (
    plan_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    price_usd TEXT NOT NULL DEFAULT '0.00',
    duration_days INTEGER,
    billing_interval TEXT NOT NULL DEFAULT 'NONE',
    description TEXT NOT NULL DEFAULT '',
    includes TEXT NOT NULL DEFAULT '[]',
    paypal_product_id TEXT,
    paypal_plan_id TEXT,
    active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

INSERT INTO plans (
    plan_id,
    name,
    price_usd,
    duration_days,
    billing_interval,
    description,
    includes,
    active,
    created_at,
    updated_at
) VALUES
(
    'FREE',
    'Free',
    '0.00',
    NULL,
    'NONE',
    'Acesso gratuito às ferramentas disponíveis no plano Free.',
    '["Ferramentas Free","Processamento local disponível"]',
    1,
    unixepoch(),
    unixepoch()
),
(
    'PRO',
    'Pro',
    '5.00',
    30,
    'MONTH',
    'Mais ferramentas e recursos para quem trabalha com áudio com mais frequência.',
    '["Tudo do Free","Ferramentas Pro","Novos recursos Pro"]',
    1,
    unixepoch(),
    unixepoch()
),
(
    'PREMIUM',
    'Premium',
    '10.00',
    30,
    'MONTH',
    'Acesso completo ao Pro e aos recursos Premium do Audio Tools.',
    '["Tudo do Free","Tudo do Pro","Ferramentas Premium","Novos recursos Premium"]',
    1,
    unixepoch(),
    unixepoch()
);

DROP TABLE IF EXISTS entitlements;

CREATE TABLE entitlements (
    firebase_uid TEXT PRIMARY KEY,
    plan TEXT NOT NULL DEFAULT 'FREE',
    starts_at INTEGER,
    expires_at INTEGER,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    source TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_at INTEGER NOT NULL,
    last_subscription_id TEXT
);

CREATE INDEX idx_entitlements_plan
    ON entitlements(plan);

CREATE INDEX idx_entitlements_status
    ON entitlements(status);

CREATE TABLE paypal_subscriptions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    firebase_uid TEXT NOT NULL,
    subscription_id TEXT NOT NULL UNIQUE,
    plan_id TEXT NOT NULL,
    paypal_plan_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'APPROVAL_PENDING',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    approved_at INTEGER,
    current_period_end INTEGER,
    next_billing_time INTEGER,
    payer_id TEXT
);

CREATE INDEX idx_paypal_subscriptions_uid
    ON paypal_subscriptions(firebase_uid);

CREATE INDEX idx_paypal_subscriptions_status
    ON paypal_subscriptions(status);

CREATE TABLE IF NOT EXISTS support_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    firebase_uid TEXT NOT NULL,
    account_email TEXT NOT NULL DEFAULT '',
    display_name TEXT NOT NULL DEFAULT '',
    plan TEXT NOT NULL DEFAULT 'FREE',
    request_type TEXT NOT NULL,
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    app_version TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'PENDING',
    email_id TEXT,
    last_error TEXT,
    updated_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_support_requests_uid
    ON support_requests(firebase_uid, created_at);
