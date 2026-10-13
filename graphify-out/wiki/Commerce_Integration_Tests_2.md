# Commerce Integration Tests

> 46 nodes · cohesion 0.18

## Key Concepts

- **OmnichannelIntegrationTest** (74 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **Test** (32 connections)
- **.add()** (28 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.request()** (28 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.options()** (25 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.findStoresWithStock()** (18 connections) — `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- **.variantsRemainDistinctAndComparisonUsesUnionOfSpecifications()** (16 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **Cancel** (15 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.transfer()** (14 connections) — `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- **.expire()** (14 connections) — `src/main/java/vn/techshop/order/PickupService.java`
- **.racingPaidPickupReplaysAndExpiryAwardOnlyOnce()** (14 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.expiredPickupRejectsQrAndCollectionBeforeExpiryJobWithoutMutation()** (13 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.warrantyExchangeConsumesReplacementWithoutDuplicatingSalesOrPoints()** (13 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.hundredCustomersCompeteForLastSerialOnlyOneWins()** (12 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.instantDeliveryRoutesReservesSellsActivatesWarrantyAndAwardsMember()** (12 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.principal()** (12 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.racingExpiredUnpaidPickupAndTwoExpiryJobsReleaseOnce()** (12 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.racingPaymentCallbacksAndCancelKeepOnePaidOrOneReconciliation()** (12 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.repairsEnforceSequentialStates()** (12 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.paidPickupRemainsCollectableAfterHoldExpiry()** (11 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.pickupExpiresAndReleasesOnce()** (11 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.verifiedVnpayIpnChecksSignatureAmountReplayAndNeverRevivesCancelled()** (11 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.expiryJobKeepsUnpaidPickupUntilUtcDeadline()** (10 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.racingCouponLastUseHasOneOrderAndRollsBackLosingCartAndSerial()** (10 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.racingSignedVnpayIpnReturnsSuccessAndReplayWithoutDuplicateHistory()** (10 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- *... and 21 more nodes in this community*

## Relationships

- [Payment and VNPay](Payment_and_VNPay.md) (38 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (34 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (34 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (13 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (8 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (6 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (6 shared connections)
- [Commerce Reporting](Commerce_Reporting.md) (3 shared connections)
- [Installment Applications](Installment_Applications.md) (3 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (2 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (2 shared connections)
- [Branches and Inventory](Branches_and_Inventory.md) (2 shared connections)

## Source Files

- `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- `src/main/java/vn/techshop/config/OmnichannelApi.java`
- `src/main/java/vn/techshop/order/OrderService.java`
- `src/main/java/vn/techshop/order/PickupService.java`
- `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`

## Audit Trail

- EXTRACTED: 217 (62%)
- INFERRED: 132 (38%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*