# OmnichannelIntegrationTest

> God node · 74 connections · `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`

**Community:** [Commerce Integration Tests](Commerce_Integration_Tests.md)

## Connections by Relation

### contains
- RacingAction `EXTRACTED`

### method
- .add() `EXTRACTED`
- .request() `EXTRACTED`
- .options() `EXTRACTED`
- .variantsRemainDistinctAndComparisonUsesUnionOfSpecifications() `EXTRACTED`
- .racingPaidPickupReplaysAndExpiryAwardOnlyOnce() `EXTRACTED`
- .fixture() `EXTRACTED`
- .expiredPickupRejectsQrAndCollectionBeforeExpiryJobWithoutMutation() `EXTRACTED`
- .tradeInDiscountRequiresInspectionAndCannotBeReused() `EXTRACTED`
- .warrantyExchangeConsumesReplacementWithoutDuplicatingSalesOrPoints() `EXTRACTED`
- .hundredCustomersCompeteForLastSerialOnlyOneWins() `EXTRACTED`
- .adminCanConfigureOffersWithoutChangingExistingQuotes() `EXTRACTED`
- .repairsEnforceSequentialStates() `EXTRACTED`
- .racingPaymentCallbacksAndCancelKeepOnePaidOrOneReconciliation() `EXTRACTED`
- .racingExpiredUnpaidPickupAndTwoExpiryJobsReleaseOnce() `EXTRACTED`
- .principal() `EXTRACTED`
- .instantDeliveryRoutesReservesSellsActivatesWarrantyAndAwardsMember() `EXTRACTED`
- .pickupExpiresAndReleasesOnce() `EXTRACTED`
- .paidPickupRemainsCollectableAfterHoldExpiry() `EXTRACTED`
- .verifiedVnpayIpnChecksSignatureAmountReplayAndNeverRevivesCancelled() `EXTRACTED`
- .terminalAndHomeDeliveryOrdersDoNotExposePickupQr() `EXTRACTED`
- *…and 23 more `method` connection(s) not listed (lowest-degree first to go)*

### references
- org.springframework.jdbc.core.JdbcTemplate `EXTRACTED`
- [OrderService](OrderService.md) `EXTRACTED`
- [CatalogService](CatalogService.md) `EXTRACTED`
- [IdentityService](IdentityService.md) `EXTRACTED`
- [BranchInventoryService](BranchInventoryService.md) `EXTRACTED`
- [CartService](CartService.md) `EXTRACTED`
- ProductVariantService `EXTRACTED`
- UserRepository `EXTRACTED`
- org.springframework.boot.test.context.SpringBootTest `EXTRACTED`
- org.springframework.test.context.ActiveProfiles `EXTRACTED`
- ProductRepository `EXTRACTED`
- org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc `EXTRACTED`
- org.springframework.test.web.servlet.MockMvc `EXTRACTED`
- PaymentService `EXTRACTED`
- OrderRepository `EXTRACTED`
- InstallmentService `EXTRACTED`
- WarrantyService `EXTRACTED`
- TradeInService `EXTRACTED`
- CouponService `EXTRACTED`
- LoyaltyService `EXTRACTED`
- *…and 7 more `references` connection(s) not listed (lowest-degree first to go)*

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*