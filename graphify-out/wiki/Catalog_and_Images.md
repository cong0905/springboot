# Catalog and Images

> 41 nodes · cohesion 0.15

## Key Concepts

- **.createProduct()** (33 connections) — `src/main/java/vn/techshop/catalog/CatalogService.java`
- **.createCategory()** (30 connections) — `src/main/java/vn/techshop/catalog/CatalogService.java`
- **org.junit.jupiter.api.BeforeEach** (24 connections)
- **ProductInput** (23 connections) — `src/main/java/vn/techshop/catalog/CatalogService.java`
- **.create()** (23 connections) — `src/main/java/vn/techshop/variant/ProductVariantService.java`
- **CategoryInput** (22 connections) — `src/main/java/vn/techshop/catalog/CatalogService.java`
- **.receive()** (21 connections) — `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- **.run()** (21 connections) — `src/main/java/vn/techshop/config/RetailDemoCatalog.java`
- **.subsetCheckoutKeepsOtherLinesAndRetryDoesNotConsumeThem()** (20 connections) — `src/test/java/vn/techshop/cart/SelectedCheckoutTest.java`
- **.create()** (17 connections) — `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- **.lockById()** (17 connections) — `src/main/java/vn/techshop/catalog/ProductRepository.java`
- **.run()** (15 connections) — `src/main/java/vn/techshop/config/OmnichannelDemoData.java`
- **Import** (14 connections) — `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- **.fixture()** (14 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.setUp()** (14 connections) — `src/test/java/vn/techshop/order/OrderHoldExpiryAndRejectionTest.java`
- **.setUp()** (14 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.setUp()** (14 connections) — `src/test/java/vn/techshop/reporting/CommerceReportingComprehensiveTest.java`
- **.setUp()** (14 connections) — `src/test/java/vn/techshop/warranty/WarrantyExchangeAndHistoryTest.java`
- **.setUp()** (13 connections) — `src/test/java/vn/techshop/CommerceComprehensiveTest.java`
- **.guestCartAndProofNeverGrantCustomerApiAccessOrOtherGuestOrder()** (13 connections) — `src/test/java/vn/techshop/guest/GuestCheckoutTest.java`
- **.productReviewRequiresModerationQueue()** (13 connections) — `src/test/java/vn/techshop/notification/EmailOutboxAndReviewModerationTest.java`
- **.routingUsesTwoQueriesInsteadOfRepeatedStoreReads()** (12 connections) — `src/test/java/vn/techshop/catalog/CatalogQueryBudgetTest.java`
- **.wishlistIsOwnerScopedIdempotentAndProtected()** (11 connections) — `src/test/java/vn/techshop/content/ContentAndWishlistTest.java`
- **.filtersMatchPriceStorageAndStockOnSameVariantWithoutGuessingBrand()** (10 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **.specification()** (9 connections) — `src/main/java/vn/techshop/variant/ProductVariantService.java`
- *... and 16 more nodes in this community*

## Relationships

- [Identity and Sessions](Identity_and_Sessions.md) (37 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (23 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (12 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (11 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (9 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (9 shared connections)
- [Branches and Inventory](Branches_and_Inventory.md) (8 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (7 shared connections)
- [Variants and Specifications](Variants_and_Specifications.md) (5 shared connections)
- [Product Reviews](Product_Reviews.md) (5 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (4 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (3 shared connections)

## Source Files

- `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- `src/main/java/vn/techshop/catalog/CatalogService.java`
- `src/main/java/vn/techshop/catalog/CategoryRepository.java`
- `src/main/java/vn/techshop/catalog/ProductRepository.java`
- `src/main/java/vn/techshop/config/AdminApi.java`
- `src/main/java/vn/techshop/config/OmnichannelDemoData.java`
- `src/main/java/vn/techshop/config/RetailDemoCatalog.java`
- `src/main/java/vn/techshop/variant/ProductVariantService.java`
- `src/test/java/vn/techshop/CommerceComprehensiveTest.java`
- `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- `src/test/java/vn/techshop/cart/SelectedCheckoutTest.java`
- `src/test/java/vn/techshop/catalog/CatalogQueryBudgetTest.java`
- `src/test/java/vn/techshop/content/ContentAndWishlistTest.java`
- `src/test/java/vn/techshop/guest/GuestCheckoutTest.java`
- `src/test/java/vn/techshop/notification/EmailOutboxAndReviewModerationTest.java`
- `src/test/java/vn/techshop/order/OrderHoldExpiryAndRejectionTest.java`
- `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- `src/test/java/vn/techshop/reporting/CommerceReportingComprehensiveTest.java`
- `src/test/java/vn/techshop/warranty/WarrantyExchangeAndHistoryTest.java`

## Audit Trail

- EXTRACTED: 88 (33%)
- INFERRED: 177 (67%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*