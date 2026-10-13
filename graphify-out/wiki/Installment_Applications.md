# Installment Applications

> 10 nodes · cohesion 0.24

## Key Concepts

- **.adminCanConfigureOffersWithoutChangingExistingQuotes()** (12 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.plans()** (11 connections) — `src/main/java/vn/techshop/installment/InstallmentService.java`
- **.quote()** (9 connections) — `src/main/java/vn/techshop/installment/InstallmentService.java`
- **.devices()** (7 connections) — `src/main/java/vn/techshop/tradein/TradeInService.java`
- **.schedule()** (4 connections) — `src/main/java/vn/techshop/config/OmnichannelApi.java`
- **.isProduction()** (3 connections) — `src/main/java/vn/techshop/installment/InstallmentService.java`
- **Schedule** (3 connections) — `src/main/java/vn/techshop/installment/InstallmentService.java`
- **.installmentCalculationIsTransparentAndAccurate()** (3 connections) — `src/test/java/vn/techshop/installment/InstallmentTransparencyAndSafetyTest.java`
- **Plan** (2 connections) — `src/main/java/vn/techshop/installment/InstallmentService.java`
- **Device** (2 connections) — `src/main/java/vn/techshop/tradein/TradeInService.java`

## Relationships

- [Security and Web Controllers](Security_and_Web_Controllers.md) (6 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (2 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (1 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (1 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/OmnichannelApi.java`
- `src/main/java/vn/techshop/installment/InstallmentService.java`
- `src/main/java/vn/techshop/tradein/TradeInService.java`
- `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- `src/test/java/vn/techshop/installment/InstallmentTransparencyAndSafetyTest.java`

## Audit Trail

- EXTRACTED: 11 (50%)
- INFERRED: 11 (50%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*