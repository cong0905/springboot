# Design and Delivery Guides

> 13 nodes · cohesion 0.15

## Key Concepts

- **docs\01-spec.md** (17 connections) — `docs/01-spec.md`
- **FR-08 and BR-08/09: cart then products ascending ID; atomic order/items/history/ledger/cart** (2 connections) — `docs/01-spec.md`
- **Public service transaction; inventory joins existing transaction** (2 connections) — `docs/02-architecture.md`
- **FR-03: only ACTIVE products and categories public** (1 connections) — `docs/01-spec.md`
- **FR-11/BR-13: locked cancel restores stock once** (1 connections) — `docs/01-spec.md`
- **FR-12/BR-14/15: PENDING->CONFIRMED->SHIPPED->DELIVERED; pre-shipping cancellation only** (1 connections) — `docs/01-spec.md`
- **FR-10/BR-11/16: immutable purchased money, names and delivery information** (1 connections) — `docs/01-spec.md`
- **BR-05: integral VND, BigDecimal and DECIMAL(15,0)** (1 connections) — `docs/01-spec.md`
- **NFR-03: p95 catalog <=500 ms, checkout <=1000 ms at 20 concurrent users** (1 connections) — `docs/01-spec.md`
- **FR-07: account cart, at most 20 lines and quantities 1-99** (1 connections) — `docs/01-spec.md`
- **BR-07: changed cart/price/fee requires new preview confirmation** (1 connections) — `docs/01-spec.md`
- **NFR-04: keyboard/labels/errors at 360px and 1440px** (1 connections) — `docs/01-spec.md`
- **FR-02: role and ownership checks on server** (1 connections) — `docs/01-spec.md`

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (4 shared connections)

## Source Files

- `docs/01-spec.md`
- `docs/02-architecture.md`

## Audit Trail

- EXTRACTED: 15 (94%)
- INFERRED: 1 (6%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*