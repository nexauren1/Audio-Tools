# Audio Tools — PayPal Sandbox backend

This Worker is isolated from the existing Android/Firebase code.

Firebase Authentication remains the source of truth for accounts.
The Worker accepts a Firebase ID token in the Authorization header
and verifies it before using D1.

Firebase project:
audio-tools-e725e

PayPal:
- Sandbox only
- PAYPAL_CLIENT_ID
- PAYPAL_CLIENT_SECRET

Pro:
- One-time purchase
- $5.00 USD by default
- 30 days by default

D1:
- audio-tools-db
- binding: DB

Endpoints:
- GET /
- GET /api/entitlement
- POST /api/paypal/create-order
- POST /api/paypal/capture-order
- GET /api/paypal/return
- GET /api/paypal/cancel

Do not deploy this source over the existing audio-tools Worker until
that Worker's current source is inspected and the handlers are merged.
