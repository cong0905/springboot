# Cart and Pricing

> 30 nodes · cohesion 0.12

## Key Concepts

- **.checkout()** (93 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.conflict()** (35 connections) — `src/main/java/vn/techshop/shared/BusinessException.java`
- **.preview()** (26 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **Checkout** (13 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.tradeInDiscountRequiresInspectionAndCannotBeReused()** (13 connections) — `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- **Options** (12 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.member()** (11 connections) — `src/main/java/vn/techshop/loyalty/LoyaltyService.java`
- **.selected()** (10 connections) — `src/main/java/vn/techshop/cart/CartService.java`
- **Pricing** (9 connections) — `src/main/java/vn/techshop/cart/Pricing.java`
- **.hash()** (9 connections) — `src/main/java/vn/techshop/cart/Pricing.java`
- **Line** (7 connections) — `src/main/java/vn/techshop/cart/Pricing.java`
- **.credit()** (7 connections) — `src/main/java/vn/techshop/tradein/TradeInService.java`
- **PreviewRequest** (6 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.checkoutRequest()** (6 connections) — `src/test/java/vn/techshop/reporting/CommerceReportingComprehensiveTest.java`
- **.approve()** (5 connections) — `src/main/java/vn/techshop/tradein/TradeInService.java`
- **.use()** (5 connections) — `src/main/java/vn/techshop/tradein/TradeInService.java`
- **.testAdminOrderFilteringByKeywordAndDate()** (5 connections) — `src/test/java/vn/techshop/CommerceComprehensiveTest.java`
- **.request()** (5 connections) — `src/test/java/vn/techshop/warranty/WarrantyExchangeAndHistoryTest.java`
- **.fee()** (4 connections) — `src/main/java/vn/techshop/cart/Pricing.java`
- **.discount()** (4 connections) — `src/main/java/vn/techshop/loyalty/LoyaltyService.java`
- **.defaults()** (4 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.equalTotalDoesNotHideDifferentLinePrices()** (4 connections) — `src/test/java/vn/techshop/cart/PricingTest.java`
- **.requireApproved()** (3 connections) — `src/main/java/vn/techshop/installment/InstallmentService.java`
- **.PreviewRequest()** (3 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **PricingTest** (3 connections) — `src/test/java/vn/techshop/cart/PricingTest.java`
- *... and 5 more nodes in this community*

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (9 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (7 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (6 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (5 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (5 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (5 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (4 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (3 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (3 shared connections)
- [Guest Cart and Order Proofs](Guest_Cart_and_Order_Proofs.md) (2 shared connections)
- [Coupons and Discounts](Coupons_and_Discounts.md) (2 shared connections)
- [Branches and Inventory](Branches_and_Inventory.md) (2 shared connections)

## Source Files

- `src/main/java/vn/techshop/cart/CartService.java`
- `src/main/java/vn/techshop/cart/Pricing.java`
- `src/main/java/vn/techshop/installment/InstallmentService.java`
- `src/main/java/vn/techshop/loyalty/LoyaltyService.java`
- `src/main/java/vn/techshop/order/OrderService.java`
- `src/main/java/vn/techshop/shared/BusinessException.java`
- `src/main/java/vn/techshop/tradein/TradeInService.java`
- `src/test/java/vn/techshop/CommerceComprehensiveTest.java`
- `src/test/java/vn/techshop/OmnichannelIntegrationTest.java`
- `src/test/java/vn/techshop/cart/PricingTest.java`
- `src/test/java/vn/techshop/reporting/CommerceReportingComprehensiveTest.java`
- `src/test/java/vn/techshop/warranty/WarrantyExchangeAndHistoryTest.java`

## Audit Trail

- EXTRACTED: 68 (62%)
- INFERRED: 42 (38%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*