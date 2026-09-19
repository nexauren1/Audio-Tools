CREATE TABLE IF NOT EXISTS paypal_orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    firebase_uid TEXT NOT NULL,
    order_id TEXT NOT NULL UNIQUE,
    plan_id TEXT NOT NULL,
    amount TEXT NOT NULL,
    currency TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'CREATED',
    created_at INTEGER NOT NULL,
    captured_at INTEGER
);

CREATE INDEX IF NOT EXISTS idx_paypal_orders_uid
    ON paypal_orders(firebase_uid);

CREATE TABLE IF NOT EXISTS entitlements (
    firebase_uid TEXT PRIMARY KEY,
    plan TEXT NOT NULL DEFAULT 'FREE',
    expires_at INTEGER,
    updated_at INTEGER NOT NULL,
    last_order_id TEXT
);

CREATE INDEX IF NOT EXISTS idx_entitlements_plan
    ON entitlements(plan);
