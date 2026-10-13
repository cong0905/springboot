# Commerce Web Pages

> 18 nodes · cohesion 0.12

## Key Concepts

- **checkout template** (13 connections) — `src/main/resources/templates/checkout.html`
- **order template** (12 connections) — `src/main/resources/templates/order.html`
- **Coupon code entered into unified checkout pricing preview** (3 connections) — `src/main/resources/templates/checkout.html`
- **Sandbox transaction code, gateway and amount display** (3 connections) — `src/main/resources/templates/mock-payment.html`
- **Order item totals, coupon discount, contact and status timeline** (3 connections) — `src/main/resources/templates/order.html`
- **Payment choice COD, demo MOCK, configured VNPay or internal demo INSTALLMENT** (2 connections) — `src/main/resources/templates/checkout.html`
- **Choose pickup branch; click-and-collect advertises 24-hour hold** (2 connections) — `src/main/resources/templates/checkout.html`
- **Unpaid active MOCK/VNPay orders resume using persisted server paymentUrl; paid or terminal orders hide link** (2 connections) — `src/main/resources/templates/order.html`
- **Display fulfillment type, assigned branch, pickup code, hold expiry and order-item serials** (2 connections) — `src/main/resources/templates/order.html`
- **Pickup store, instant coordinates and installment plan enabled only for their matching choices** (1 connections) — `src/main/resources/templates/checkout.html`
- **Owner/admin-only active pickup QR PNG, 256x256, pickupCode payload, no-store, expiry check** (1 connections) — `src/main/resources/templates/order.html`
- **Admin pickup form supports QR camera scanning with manual/USB fallback** (1 connections) — `src/main/resources/templates/order.html`
- **Choose standard home delivery, click-and-collect or demo two-hour delivery** (1 connections) — `src/main/resources/templates/checkout.html`
- **Demo two-hour delivery: maximum 20km, 20000 VND plus 5000 VND per rounded-up km; nearest branch with all units** (1 connections) — `src/main/resources/templates/checkout.html`
- **Price inputs cancel obsolete previews; latest matching preview enables checkout; 409 requires fresh pricing** (1 connections) — `src/main/resources/templates/checkout.html`
- **Versioned cancellation with mandatory reason and stock restoration** (1 connections) — `src/main/resources/templates/order.html`
- **Print internal order slip; electronic invoice issuance not implemented** (1 connections) — `src/main/resources/templates/order.html`
- **Admin PENDING to CONFIRMED to SHIPPED to DELIVERED; delivery requires codCollected** (1 connections) — `src/main/resources/templates/order.html`

## Relationships

- [Commerce Web Pages](Commerce_Web_Pages.md) (11 shared connections)

## Source Files

- `src/main/resources/templates/checkout.html`
- `src/main/resources/templates/mock-payment.html`
- `src/main/resources/templates/order.html`

## Audit Trail

- EXTRACTED: 26 (87%)
- INFERRED: 4 (13%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*