# Design and Delivery Guides

> 10 nodes · cohesion 0.22

## Key Concepts

- **smoke_http.py** (10 connections) — `scripts/smoke_http.py`
- **Inputs** (5 connections) — `scripts/smoke_http.py`
- **login()** (3 connections) — `scripts/smoke_http.py`
- **request** (2 connections) — `scripts/install_ci_scanners.py`
- **parser** (1 connections) — `scripts/smoke_http.py`
- **HTMLParser** (1 connections)
- **cookiejar** (1 connections) — `scripts/smoke_http.py`
- **.handle_starttag()** (1 connections) — `scripts/smoke_http.py`
- **Manual smoke test: run a fresh demo profile at 127.0.0.1:8080 first. Uses only…** (1 connections) — `scripts/smoke_http.py`
- **error** (1 connections) — `scripts/smoke_http.py`

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (5 shared connections)

## Source Files

- `scripts/install_ci_scanners.py`
- `scripts/smoke_http.py`

## Audit Trail

- EXTRACTED: 15 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*