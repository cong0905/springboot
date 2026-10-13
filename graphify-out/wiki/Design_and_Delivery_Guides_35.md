# Design and Delivery Guides

> 11 nodes · cohesion 0.22

## Key Concepts

- **docs\04-api-contract.md** (11 connections) — `docs/04-api-contract.md`
- **CheckoutRequest: key, version, expected total/pricing hash and delivery information** (3 connections) — `docs/04-api-contract.md`
- **Canonical SHA-256 pricingHash and requestHash** (2 connections) — `docs/04-api-contract.md`
- **POST /api/v1/orders** (2 connections) — `docs/04-api-contract.md`
- **POST /api/v1/admin/orders/{id}/transitions** (1 connections) — `docs/04-api-contract.md`
- **Contract describes target design; runtime differences documented separately** (1 connections) — `docs/04-api-contract.md`
- **POST /api/v1/orders/{id}/cancel** (1 connections) — `docs/04-api-contract.md`
- **GET /api/v1/csrf** (1 connections) — `docs/04-api-contract.md`
- **400 validation, 401 auth, 403 access/CSRF, 404 resource, 409 conflicts, 429 rate limit** (1 connections) — `docs/04-api-contract.md`
- **GET /api/v1/admin/reports/summary** (1 connections) — `docs/04-api-contract.md`
- **Thymeleaf and JSON API share application services** (1 connections) — `docs/04-api-contract.md`

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (1 shared connections)

## Source Files

- `docs/04-api-contract.md`

## Audit Trail

- EXTRACTED: 13 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*