# Design and Delivery Guides

> 5 nodes · cohesion 0.60

## Key Concepts

- **.github\workflows\ci.yml** (4 connections) — `.github/workflows/ci.yml`
- **Maven Wrapper -B verify** (3 connections) — `.github/workflows/ci.yml`
- **Fast-test job: Java 21, Maven verify, Surefire artifact** (2 connections) — `.github/workflows/ci.yml`
- **MySQL-test job: mysql:8.4 service, Maven verify, Surefire artifact** (2 connections) — `.github/workflows/ci.yml`
- **Surefire reports always uploaded** (1 connections) — `.github/workflows/ci.yml`

## Relationships

- No strong cross-community connections detected

## Source Files

- `.github/workflows/ci.yml`

## Audit Trail

- EXTRACTED: 6 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*