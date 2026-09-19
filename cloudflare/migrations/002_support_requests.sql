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
