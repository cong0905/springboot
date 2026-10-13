# Design and Delivery Guides

> 12 nodes · cohesion 0.17

## Key Concepts

- **../application configuration** (9 connections) — `src/main/resources/application.yml`
- **Production profile enforces secure session cookies** (2 connections) — `src/main/resources/application-prod.yml`
- **30-minute sessions; HttpOnly and SameSite lax cookies; configurable secure flag** (2 connections) — `src/main/resources/application.yml`
- **Environment-controlled admin bootstrap disabled by default** (1 connections) — `src/main/resources/application.yml`
- **Suppress server error messages and stack traces** (1 connections) — `src/main/resources/application.yml`
- **MySQL datasource configured by DB_URL, DB_USERNAME and DB_PASSWORD** (1 connections) — `src/main/resources/application.yml`
- **application-prod profile configuration** (1 connections) — `src/main/resources/application-prod.yml`
- **Hibernate validates schema; Flyway applies classpath:db/migration** (1 connections) — `src/main/resources/application.yml`
- **Reject unknown JSON properties** (1 connections) — `src/main/resources/application.yml`
- **Configurable APP_UPLOAD_DIR defaults to ./data/uploads** (1 connections) — `src/main/resources/application.yml`
- **JDBC and Hibernate use UTC** (1 connections) — `src/main/resources/application.yml`
- **VNPay merchant and signing configuration from environment; sandbox URL and return URL defaults** (1 connections) — `src/main/resources/application.yml`

## Relationships

- No strong cross-community connections detected

## Source Files

- `src/main/resources/application-prod.yml`
- `src/main/resources/application.yml`

## Audit Trail

- EXTRACTED: 10 (91%)
- INFERRED: 1 (9%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*