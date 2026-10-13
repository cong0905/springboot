# Design and Delivery Guides

> 24 nodes · cohesion 0.08

## Key Concepts

- **docs\12-implementation-status.md** (32 connections) — `docs/12-implementation-status.md`
- **Standard delivery, local20km Haversine2h, 24h pickup expiry and internal receipt** (2 connections) — `docs/12-implementation-status.md`
- **DELIVERED unique order ledger:1% merchandise points, >10m/>30m ranks and1%/3% discounts** (2 connections) — `docs/12-implementation-status.md`
- **Owner-bound approved trade-in quote valid7days and used once** (2 connections) — `docs/12-implementation-status.md`
- **DELIVERED1% post-discount merchandise points floor;1point1000VND; S_MEM>10m,S_VIP>30m; discounts1%/3%,no redemption** (2 connections) — `docs/13-omnichannel-guide.md`
- **Pickup requires one branch covering entire cart, free fee and24h reservation** (2 connections) — `docs/13-omnichannel-guide.md`
- **APPROVED trade-in owner-bound,7-day validity,once,cap merchandise amount; unpaid cancel restores approval; no duplicate old serial** (2 connections) — `docs/13-omnichannel-guide.md`
- **Browser QA checks guest regions, canceled obsolete preview, 409 retry guard and authenticated pickup QR** (1 connections) — `docs/12-implementation-status.md`
- **Pickup expiry uses UTC DATETIME cutoff consistently with JPA; unpaid live holds remain reserved** (1 connections) — `docs/12-implementation-status.md`
- **Branch stock/reservation, import1-500 serials, IN_STOCK transfers and IMEI ledger** (1 connections) — `docs/12-implementation-status.md`
- **Reported Chrome/Playwright demo E2E: 1440/390/320px, HTTP200, no horizontal overflow/JS errors; qa/browser-results.json** (1 connections) — `docs/12-implementation-status.md`
- **Graphify graph/report/HTML/wiki are indexes; code/migrations/QA are implementation evidence** (1 connections) — `docs/12-implementation-status.md`
- **Camera/USB scanner hardware untested; BarcodeDetector browser-dependent** (1 connections) — `docs/12-implementation-status.md`
- **One serial order uses one branch; ordinary and serial items ordered separately; no split warehouse, redeem, other-model exchange or VIP warranty** (1 connections) — `docs/12-implementation-status.md`
- **Documented local implementation completed; production Definition of Done not accepted** (1 connections) — `docs/12-implementation-status.md`
- **Flyway V1-V9 execution schemas; V1-V4 unchanged; database/schema.sql remains baseline** (1 connections) — `docs/12-implementation-status.md`
- **Reported MySQL8.0.43 Maven verify: techshop_test on loopback33317; 58 tests, 0 failures/errors/skipped; qa/mysql-tests.json** (1 connections) — `docs/12-implementation-status.md`
- **SUCCESS immutable; callbacks cannot revive CANCELLED; PAID needs refund, no automatic gateway refund** (1 connections) — `docs/12-implementation-status.md`
- **COD, demo/test MOCK, VNPay HMAC-SHA512 merchant/amount/status/replay IPN; return does not mark paid** (1 connections) — `docs/12-implementation-status.md`
- **Reported browser flow: variant/cart/preview/pickup/admin code/DELIVERED/warranty/points/rank** (1 connections) — `docs/12-implementation-status.md`
- **Reported 100-worker last-IMEI race: one success, one RESERVED serial** (1 connections) — `docs/12-implementation-status.md`
- **Reported failure after order/history/stock/IMEI writes: complete transaction rollback preserves cart and stock** (1 connections) — `docs/12-implementation-status.md`
- **Reported real multipart PNG byte/type verification; fake PNG rejected** (1 connections) — `docs/12-implementation-status.md`
- **Variant SKU/color/storage/price/version and albums/specs/comparison** (1 connections) — `docs/12-implementation-status.md`

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (10 shared connections)

## Source Files

- `docs/12-implementation-status.md`
- `docs/13-omnichannel-guide.md`

## Audit Trail

- EXTRACTED: 30 (91%)
- INFERRED: 3 (9%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*