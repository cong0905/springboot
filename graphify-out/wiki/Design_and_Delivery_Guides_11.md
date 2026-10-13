# Design and Delivery Guides

> 21 nodes · cohesion 0.10

## Key Concepts

- **docs\13-omnichannel-guide.md** (25 connections) — `docs/13-omnichannel-guide.md`
- **POST /api/v1/admin/orders/{id}/pickup** (4 connections) — `docs/openapi.json`
- **POST /api/v1/admin/imei/transfer** (3 connections) — `docs/openapi.json`
- **Demo3/6/9/12-month schedule with integer VND and final-period remainder; approve before delivery** (2 connections) — `docs/12-implementation-status.md`
- **Server calculates fee and installments; last period absorbs remainder; total periods equals amount** (2 connections) — `docs/13-omnichannel-guide.md`
- **Pickup UUID drives state transaction; repeat delivered pickup does not resell or award twice** (2 connections) — `docs/13-omnichannel-guide.md`
- **Transfer only IN_STOCK serials; records source/destination and preserves total stock** (2 connections) — `docs/13-omnichannel-guide.md`
- **Schema BranchInventoryService_Transfer** (2 connections) — `docs/openapi.json`
- **Schema OmnichannelApi_Pickup** (2 connections) — `docs/openapi.json`
- **Schema OrderService_OrderView** (2 connections) — `docs/openapi.json`
- **Images assign variantId; null variantId is shared album** (1 connections) — `docs/13-omnichannel-guide.md`
- **Session cookie plus X-CSRF-TOKEN; admin ROLE_ADMIN; customer principal from session** (1 connections) — `docs/13-omnichannel-guide.md`
- **Preview hashes/version/options must match; same UUID/content replays; changed content rejected** (1 connections) — `docs/13-omnichannel-guide.md`
- **60-second job cancels unpaid expired PENDING/CONFIRMED pickup and restores stock** (1 connections) — `docs/13-omnichannel-guide.md`
- **Standard product stock must be zero before first variant; serial inventory must use commerce import rather than manual total adjustments** (1 connections) — `docs/13-omnichannel-guide.md`
- **2h nearest complete-stock branch within20km; fee20000+5000/km rounded up** (1 connections) — `docs/13-omnichannel-guide.md`
- **Paid pickup not auto-expired; cancel yields REFUND_REQUIRED; gateway refund absent** (1 connections) — `docs/13-omnichannel-guide.md`
- **One IMEI order, one branch; ordinary/serial carts checked out separately** (1 connections) — `docs/13-omnichannel-guide.md`
- **Global unique serial,5-40 alphanumeric/hyphen chars; batch1-500; one per line** (1 connections) — `docs/13-omnichannel-guide.md`
- **Standard fee30000 below1m subtotal, free from1m** (1 connections) — `docs/13-omnichannel-guide.md`
- **VNPay HMAC-SHA512,merchant,amountx100,status; real reachable merchant/IPN still required** (1 connections) — `docs/13-omnichannel-guide.md`

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (8 shared connections)

## Source Files

- `docs/12-implementation-status.md`
- `docs/13-omnichannel-guide.md`
- `docs/openapi.json`

## Audit Trail

- EXTRACTED: 27 (96%)
- INFERRED: 1 (4%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*