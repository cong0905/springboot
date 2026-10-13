# Design and Delivery Guides

> 4 nodes · cohesion 0.50

## Key Concepts

- **Schema OmnichannelApi_Decision** (3 connections) — `docs/openapi.json`
- **POST /api/v1/admin/repairs/{id}/transitions** (3 connections) — `docs/openapi.json`
- **RECEIVED->REPAIRING->COMPLETED->HANDED_OVER; no skipping** (2 connections) — `docs/13-omnichannel-guide.md`
- **POST /api/v1/admin/orders/{id}/installment** (2 connections) — `docs/openapi.json`

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