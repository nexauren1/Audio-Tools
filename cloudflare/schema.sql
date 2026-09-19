PRAGMA foreign_keys = ON;

CREATE TABLE IF NOT EXISTS plans (
    plan_id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    price_usd TEXT NOT NULL DEFAULT '0.00',
    duration_days INTEGER,
    monthly_credits INTEGER NOT NULL DEFAULT 0,
    active INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

INSERT OR IGNORE INTO plans (
    plan_id, name, price_usd, duration_days,
    monthly_credits, active, created_at, updated_at
) VALUES
    (
        'FREE', 'Free', '0.00', NULL,
        0, 1, unixepoch(), unixepoch()
    ),
    (
        'PRO', 'Pro', '5.00', 30,
        1000, 1, unixepoch(), unixepoch()
    );

CREATE TABLE IF NOT EXISTS entitlements (
    firebase_uid TEXT PRIMARY KEY,
    plan TEXT NOT NULL DEFAULT 'FREE',
    starts_at INTEGER,
    expires_at INTEGER,
    status TEXT NOT NULL DEFAULT 'ACTIVE',
    source TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_at INTEGER NOT NULL,
    last_order_id TEXT
);

CREATE INDEX IF NOT EXISTS idx_entitlements_plan
    ON entitlements(plan);

CREATE INDEX IF NOT EXISTS idx_entitlements_status
    ON entitlements(status);

CREATE TABLE IF NOT EXISTS paypal_orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    firebase_uid TEXT NOT NULL,
    order_id TEXT NOT NULL UNIQUE,
    plan_id TEXT NOT NULL,
    amount TEXT NOT NULL,
    currency TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'CREATED',
    created_at INTEGER NOT NULL,
    captured_at INTEGER,
    payer_id TEXT,
    CHECK (amount <> '')
);

CREATE INDEX IF NOT EXISTS idx_paypal_orders_uid
    ON paypal_orders(firebase_uid);

CREATE INDEX IF NOT EXISTS idx_paypal_orders_status
    ON paypal_orders(status);

CREATE TABLE IF NOT EXISTS credit_accounts (
    firebase_uid TEXT PRIMARY KEY,
    purchased_credits INTEGER NOT NULL DEFAULT 0,
    plan_credits INTEGER NOT NULL DEFAULT 0,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS credit_transactions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    firebase_uid TEXT NOT NULL,
    bucket TEXT NOT NULL,
    amount INTEGER NOT NULL,
    type TEXT NOT NULL,
    source TEXT NOT NULL,
    reference_id TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_credit_transactions_uid
    ON credit_transactions(firebase_uid);

CREATE INDEX IF NOT EXISTS idx_credit_transactions_ref
    ON credit_transactions(reference_id);

CREATE TABLE IF NOT EXISTS tool_usage (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    firebase_uid TEXT NOT NULL,
    tool_id TEXT NOT NULL,
    credits_used INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tool_usage_uid
    ON tool_usage(firebase_uid);

CREATE INDEX IF NOT EXISTS idx_tool_usage_tool
    ON tool_usage(tool_id);
