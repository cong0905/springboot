# Commerce Integration Tests

> 18 nodes · cohesion 0.31

## Key Concepts

- **CommerceIntegrationTest** (33 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.checkoutReplaySnapshotsCancelAndLedgerAreConsistent()** (12 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.add()** (11 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.request()** (11 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **Test** (11 connections)
- **.priceChangeAndKeyReuseDoNotMutateStock()** (9 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.lastUnitConcurrentCheckoutHasOneWinner()** (8 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.simultaneousSameKeyAndCancellationDoNotDuplicateMovements()** (8 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.transitionAndRevenueUseDeliveredTime()** (8 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.missingCsrfWrongRoleOwnershipAndUnknownFieldsAreRejected()** (7 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **ProductUpdate** (6 connections) — `src/main/java/vn/techshop/catalog/CatalogService.java`
- **.checkoutWritesRollBackTogetherWhenTransactionFails()** (6 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.coreTemplatesRenderForCustomerAndAdmin()** (6 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.inactiveCatalogInvalidFiltersAndEmptyStatusAreHandled()** (6 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.principal()** (5 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.compete()** (4 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.loginCreatesSessionAndLogoutInvalidatesIt()** (3 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **PlatformTransactionManager** (1 connections)

## Relationships

- [Payment and VNPay](Payment_and_VNPay.md) (11 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (10 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (7 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (7 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (6 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (4 shared connections)
- [Variants and Specifications](Variants_and_Specifications.md) (3 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (2 shared connections)
- [Commerce Reporting](Commerce_Reporting.md) (2 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (1 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/catalog/CatalogService.java`
- `src/test/java/vn/techshop/CommerceIntegrationTest.java`

## Audit Trail

- EXTRACTED: 64 (63%)
- INFERRED: 37 (37%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*