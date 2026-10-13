# Design and Delivery Guides

> 11 nodes · cohesion 0.20

## Key Concepts

- **../../test/resources/application-test configuration** (5 connections) — `src/test/resources/application-test.yml`
- **../application-demo configuration** (4 connections) — `src/main/resources/application-demo.yml`
- **Flyway uses classpath:db/demo** (2 connections) — `src/main/resources/application-demo.yml`
- **H2 in-memory database in MySQL mode, DB_CLOSE_DELAY=-1 and 5000ms lock timeout** (2 connections) — `src/main/resources/application-demo.yml`
- **Test DB defaults to isolated H2 MySQL mode with 10000ms lock timeout and environment override** (2 connections) — `src/test/resources/application-test.yml`
- **Test migrations default to db/demo with environment override** (2 connections) — `src/test/resources/application-test.yml`
- **Demo application data enabled** (1 connections) — `src/main/resources/application-demo.yml`
- **Demo profile explicitly enables mock payment** (1 connections) — `src/main/resources/application-demo.yml`
- **Tests disable demo application data** (1 connections) — `src/test/resources/application-test.yml`
- **Test profile explicitly enables mock payment** (1 connections) — `src/test/resources/application-test.yml`
- **Tests use ./target/test-uploads** (1 connections) — `src/test/resources/application-test.yml`

## Relationships

- No strong cross-community connections detected

## Source Files

- `src/main/resources/application-demo.yml`
- `src/test/resources/application-test.yml`

## Audit Trail

- EXTRACTED: 9 (82%)
- INFERRED: 2 (18%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*