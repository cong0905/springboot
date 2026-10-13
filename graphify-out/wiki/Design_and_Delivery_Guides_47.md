# Design and Delivery Guides

> 8 nodes · cohesion 0.25

## Key Concepts

- **org.springframework.boot.test.context.TestConfiguration** (4 connections)
- **CounterConfig** (3 connections) — `src/test/java/vn/techshop/catalog/CatalogQueryBudgetTest.java`
- **.countedSource()** (3 connections) — `src/test/java/vn/techshop/catalog/CatalogQueryBudgetTest.java`
- **Mailbox** (3 connections) — `src/test/java/vn/techshop/identity/RecoveryAndGoogleTest.java`
- **.sender()** (3 connections) — `src/test/java/vn/techshop/identity/RecoveryAndGoogleTest.java`
- **org.springframework.beans.factory.config.BeanPostProcessor** (2 connections)
- **Bean** (1 connections)
- **JavaMailSender** (1 connections)

## Relationships

- [Commerce Integration Tests](Commerce_Integration_Tests.md) (1 shared connections)

## Source Files

- `src/test/java/vn/techshop/catalog/CatalogQueryBudgetTest.java`
- `src/test/java/vn/techshop/identity/RecoveryAndGoogleTest.java`

## Audit Trail

- EXTRACTED: 8 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*