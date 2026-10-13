# Design and Delivery Guides

> 9 nodes · cohesion 0.31

## Key Concepts

- **compose.yml** (8 connections) — `compose.yml`
- **MySQL 8.4 database service** (4 connections) — `compose.yml`
- **App service built locally** (2 connections) — `compose.yml`
- **App starts after DB health check** (2 connections) — `compose.yml`
- **mysql_data persistent volume** (2 connections) — `compose.yml`
- **DB and bootstrap admin credentials supplied through environment** (1 connections) — `compose.yml`
- **Loopback-only ports 3306 and 8080** (1 connections) — `compose.yml`
- **app_uploads volume mounted at /app/data/uploads** (1 connections) — `compose.yml`
- **VNPay merchant/hash/return configured through environment** (1 connections) — `compose.yml`

## Relationships

- No strong cross-community connections detected

## Source Files

- `compose.yml`

## Audit Trail

- EXTRACTED: 11 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*