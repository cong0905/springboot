# Identity and Sessions

> 45 nodes · cohesion 0.10

## Key Concepts

- **Registration** (36 connections) — `src/main/java/vn/techshop/identity/IdentityService.java`
- **.csrf()** (31 connections) — `src/main/java/vn/techshop/identity/IdentityApi.java`
- **.register()** (31 connections) — `src/main/java/vn/techshop/identity/IdentityService.java`
- **.bootstrapAdmin()** (22 connections) — `src/main/java/vn/techshop/identity/IdentityService.java`
- **.customerAndGuestRefundRequestAndStatusTransparency()** (22 connections) — `src/test/java/vn/techshop/payment/OrderStatusAndRefundUxTest.java`
- **.adjust()** (20 connections) — `src/main/java/vn/techshop/inventory/InventoryService.java`
- **.run()** (15 connections) — `src/main/java/vn/techshop/config/DemoData.java`
- **.guestCartMergesIntoCustomerCartOnLoginAndRedirectsSafely()** (15 connections) — `src/test/java/vn/techshop/guest/GuestCartMergeAndExperienceTest.java`
- **.fixture()** (14 connections) — `src/test/java/vn/techshop/chat/AiChatIntegrationTest.java`
- **Adjustment** (13 connections) — `src/main/java/vn/techshop/inventory/InventoryService.java`
- **.resolve()** (11 connections) — `src/main/java/vn/techshop/identity/GoogleAccounts.java`
- **.fixture()** (11 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.request()** (10 connections) — `src/main/java/vn/techshop/identity/RecoveryService.java`
- **.loadUserByUsername()** (9 connections) — `src/main/java/vn/techshop/identity/IdentityService.java`
- **.findByEmail()** (8 connections) — `src/main/java/vn/techshop/identity/UserRepository.java`
- **.newsHidesDraftAndEscapesHtmlThenEnforcesVersionAndAdmin()** (8 connections) — `src/test/java/vn/techshop/content/ContentAndWishlistTest.java`
- **.personalVoucherCannotBeListedOrRedeemedByOtherCustomer()** (8 connections) — `src/test/java/vn/techshop/feature/FeatureCompletionTest.java`
- **.apiRequiresOwnerAndCsrfAndRendersAddressBook()** (8 connections) — `src/test/java/vn/techshop/identity/AddressServiceTest.java`
- **.passwordChangeRequiresCurrentPasswordAndExpiresAllLoggedInSessions()** (8 connections) — `src/test/java/vn/techshop/identity/CustomerProfileTest.java`
- **.recoveryStoresOnlyHashAndConsumesTokenOnceWithExpiryAndNoAccountEnumeration()** (8 connections) — `src/test/java/vn/techshop/identity/RecoveryAndGoogleTest.java`
- **.staleCartAndNegativeStockAreRejected()** (7 connections) — `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- **.avatarUploadUsesRealDecoderAndOwnerVersion()** (7 connections) — `src/test/java/vn/techshop/content/ContentAndWishlistTest.java`
- **.principal()** (7 connections) — `src/test/java/vn/techshop/content/ContentAndWishlistTest.java`
- **.updateOwnProfileRefreshesPrincipalAndRejectsStaleInvalidAndMissingCsrf()** (7 connections) — `src/test/java/vn/techshop/identity/CustomerProfileTest.java`
- **.googleRequiresVerifiedEmailAndPasswordProofForExistingAccount()** (7 connections) — `src/test/java/vn/techshop/identity/RecoveryAndGoogleTest.java`
- *... and 20 more nodes in this community*

## Relationships

- [Catalog and Images](Catalog_and_Images.md) (26 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (13 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (13 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (10 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (9 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (5 shared connections)
- [Coupons and Discounts](Coupons_and_Discounts.md) (3 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (3 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (3 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (3 shared connections)
- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (3 shared connections)
- [Product Reviews](Product_Reviews.md) (2 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/AdminBootstrap.java`
- `src/main/java/vn/techshop/config/DemoData.java`
- `src/main/java/vn/techshop/identity/GoogleAccounts.java`
- `src/main/java/vn/techshop/identity/IdentityApi.java`
- `src/main/java/vn/techshop/identity/IdentityService.java`
- `src/main/java/vn/techshop/identity/RecoveryService.java`
- `src/main/java/vn/techshop/identity/UserRepository.java`
- `src/main/java/vn/techshop/inventory/InventoryService.java`
- `src/test/java/vn/techshop/CommerceIntegrationTest.java`
- `src/test/java/vn/techshop/chat/AiChatIntegrationTest.java`
- `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- `src/test/java/vn/techshop/content/ContentAndWishlistTest.java`
- `src/test/java/vn/techshop/feature/FeatureCompletionTest.java`
- `src/test/java/vn/techshop/guest/GuestCartMergeAndExperienceTest.java`
- `src/test/java/vn/techshop/identity/AddressServiceTest.java`
- `src/test/java/vn/techshop/identity/CustomerProfileTest.java`
- `src/test/java/vn/techshop/identity/RecoveryAndGoogleTest.java`
- `src/test/java/vn/techshop/installment/InstallmentTransparencyAndSafetyTest.java`
- `src/test/java/vn/techshop/order/OrderPaginationAndFilterTest.java`
- `src/test/java/vn/techshop/payment/OrderStatusAndRefundUxTest.java`

## Audit Trail

- EXTRACTED: 67 (34%)
- INFERRED: 133 (66%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*