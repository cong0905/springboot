# Design and Delivery Guides

> 4 nodes · cohesion 0.50

## Key Concepts

- **POST /api/v1/orders/preview** (4 connections) — `docs/openapi.json`
- **POST /api/v1/orders/preview returns cartVersion,total,pricingHash for checkout with same options** (2 connections) — `docs/13-omnichannel-guide.md`
- **Schema OrderService_Preview** (2 connections) — `docs/openapi.json`
- **Schema OrderService_PreviewRequest** (2 connections) — `docs/openapi.json`

## Relationships

- No strong cross-community connections detected

## Source Files

- `docs/13-omnichannel-guide.md`
- `docs/openapi.json`

## Audit Trail

- EXTRACTED: 3 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*