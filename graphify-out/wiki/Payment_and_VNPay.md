# Payment and VNPay

> 44 nodes · cohesion 0.15

## Key Concepts

- **.view()** (63 connections) — `src/main/java/vn/techshop/cart/CartService.java`
- **.detail()** (43 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.add()** (41 connections) — `src/main/java/vn/techshop/cart/CartService.java`
- **.transition()** (31 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.requestRefund()** (23 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **.items()** (22 connections) — `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- **.refundDeliveredOrderWithRestockRevokesPointsAndWarranty()** (21 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.returnedSerialCanBeSoldAgainWithANewWarrantyWithoutRewritingOldRepairHistory()** (17 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.deliveredOrderFullyRefundedInSamePeriodShowsZeroNetRevenueAcrossReports()** (16 connections) — `src/test/java/vn/techshop/reporting/CommerceReportingComprehensiveTest.java`
- **Transition** (15 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **RefundRequest** (15 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **.salesInPeriod1AndRefundInPeriod2PreservesHistoricalSalesAndRecordsRefundSeparately()** (15 connections) — `src/test/java/vn/techshop/reporting/CommerceReportingComprehensiveTest.java`
- **.exchangeRetainsOriginalSerialOnOldRepairTicketAndRecordsExchangeEvent()** (15 connections) — `src/test/java/vn/techshop/warranty/WarrantyExchangeAndHistoryTest.java`
- **.fullRefundOnShippedOrderRequiresGoodsReturnAndReleasesStockExactlyOnce()** (14 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.partialRefundOnShippedOrderKeepsOrderShippedAndDeliversNormally()** (14 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.mergeGuestCart()** (12 connections) — `src/main/java/vn/techshop/cart/CartService.java`
- **.failedLateCallbackAndUnpaidCancellationCannotCreateRefundMoney()** (12 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.request()** (12 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.requestAndApproveRefundForPaidOrderCancelsAndRestocks()** (12 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.lookup()** (11 connections) — `src/main/java/vn/techshop/warranty/WarrantyService.java`
- **.codDeliveryOrderConfirmedClearsHoldExpiryAndDoesNotExpire()** (11 connections) — `src/test/java/vn/techshop/order/OrderHoldExpiryAndRejectionTest.java`
- **.installmentOrderExpiresAfterHoldDeadline()** (10 connections) — `src/test/java/vn/techshop/order/OrderHoldExpiryAndRejectionTest.java`
- **.installmentRejectionReleasesInventoryAndSerial()** (10 connections) — `src/test/java/vn/techshop/order/OrderHoldExpiryAndRejectionTest.java`
- **.onlineDeliveryUnpaidOrderExpiresAndReleasesInventoryAndSerial()** (10 connections) — `src/test/java/vn/techshop/order/OrderHoldExpiryAndRejectionTest.java`
- **.cannotRefundExceedingTotalAmount()** (10 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- *... and 19 more nodes in this community*

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (55 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (48 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (9 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (6 shared connections)
- [Guest Cart and Order Proofs](Guest_Cart_and_Order_Proofs.md) (6 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (5 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (3 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (2 shared connections)
- [Installment Applications](Installment_Applications.md) (2 shared connections)
- [Commerce Reporting](Commerce_Reporting.md) (2 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (1 shared connections)
- [Variants and Specifications](Variants_and_Specifications.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/branch/BranchInventoryService.java`
- `src/main/java/vn/techshop/cart/CartItemRepository.java`
- `src/main/java/vn/techshop/cart/CartRepository.java`
- `src/main/java/vn/techshop/cart/CartService.java`
- `src/main/java/vn/techshop/config/OmnichannelApi.java`
- `src/main/java/vn/techshop/guest/GuestApi.java`
- `src/main/java/vn/techshop/order/OrderService.java`
- `src/main/java/vn/techshop/payment/RefundService.java`
- `src/main/java/vn/techshop/reporting/ReportingService.java`
- `src/main/java/vn/techshop/variant/ProductVariantService.java`
- `src/main/java/vn/techshop/warranty/WarrantyService.java`
- `src/test/java/vn/techshop/order/OrderHoldExpiryAndRejectionTest.java`
- `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- `src/test/java/vn/techshop/reporting/CommerceReportingComprehensiveTest.java`
- `src/test/java/vn/techshop/warranty/WarrantyExchangeAndHistoryTest.java`

## Audit Trail

- EXTRACTED: 94 (34%)
- INFERRED: 186 (66%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*