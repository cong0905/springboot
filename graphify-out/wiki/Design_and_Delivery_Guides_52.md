# Design and Delivery Guides

> 6 nodes · cohesion 0.53

## Key Concepts

- **Session** (6 connections) — `scripts/smoke_http.py`
- **.getjson()** (3 connections) — `scripts/smoke_http.py`
- **.mutate()** (3 connections) — `scripts/smoke_http.py`
- **.request()** (3 connections) — `scripts/smoke_http.py`
- **.__init__()** (2 connections) — `scripts/smoke_http.py`
- **.__init__()** (2 connections) — `scripts/smoke_http.py`

## Relationships

- No strong cross-community connections detected

## Source Files

- `scripts/smoke_http.py`

## Audit Trail

- EXTRACTED: 8 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*