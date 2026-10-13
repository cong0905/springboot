# Design and Delivery Guides

> 4 nodes · cohesion 0.50

## Key Concepts

- **Within30days,same variant/branch,IN_STOCK replacement; old DEFECTIVE,new SOLD; preserve original warranty; no revenue/points duplication** (3 connections) — `docs/13-omnichannel-guide.md`
- **POST /api/v1/admin/warranty/{id}/exchange** (3 connections) — `docs/openapi.json`
- **IMEI12-month warranty, sequential repairs and same-variant replacement within30days** (2 connections) — `docs/12-implementation-status.md`
- **Schema OmnichannelApi_Exchange** (2 connections) — `docs/openapi.json`

## Relationships

- No strong cross-community connections detected

## Source Files

- `docs/12-implementation-status.md`
- `docs/13-omnichannel-guide.md`
- `docs/openapi.json`

## Audit Trail

- EXTRACTED: 2 (67%)
- INFERRED: 1 (33%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*