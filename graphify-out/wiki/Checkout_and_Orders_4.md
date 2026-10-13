# Checkout and Orders

> 31 nodes · cohesion 0.11

## Key Concepts

- **.completePayment()** (37 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **.processRefund()** (37 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **OrderHistory** (26 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **RefundView** (12 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **.latePaymentOnCancelledOrderCreatesReconciliationLedger()** (12 connections) — `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`
- **.listRefunds()** (7 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **.detail()** (6 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **.refunds()** (4 connections) — `src/main/java/vn/techshop/config/AdminApi.java`
- **RefundProcess** (4 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **.recordLatePaymentReconciliation()** (4 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **.setActorUserId()** (3 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.setFromStatus()** (3 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.setOccurredAt()** (3 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.setOrderId()** (3 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.setOrderVersion()** (3 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.setReason()** (3 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.setToStatus()** (3 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.mapRow()** (3 connections) — `src/main/java/vn/techshop/payment/RefundService.java`
- **java.sql.ResultSet** (2 connections)
- **Entity** (1 connections)
- **Table** (1 connections)
- **.getActorUserId()** (1 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.getFromStatus()** (1 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.getId()** (1 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- **.getOccurredAt()** (1 connections) — `src/main/java/vn/techshop/order/OrderHistory.java`
- *... and 6 more nodes in this community*

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (10 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (7 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (4 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (4 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (3 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (2 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (2 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (1 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/AdminApi.java`
- `src/main/java/vn/techshop/order/OrderHistory.java`
- `src/main/java/vn/techshop/payment/PaymentService.java`
- `src/main/java/vn/techshop/payment/RefundService.java`
- `src/test/java/vn/techshop/payment/RefundAndReconciliationTest.java`

## Audit Trail

- EXTRACTED: 47 (57%)
- INFERRED: 36 (43%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*