# Payment and VNPay

> 13 nodes · cohesion 0.21

## Key Concepts

- **.initiatePayment()** (21 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **PaymentServiceTest** (14 connections) — `src/test/java/vn/techshop/payment/PaymentServiceTest.java`
- **.mockInitiateCreatesTransactionAndMockUrl()** (5 connections) — `src/test/java/vn/techshop/payment/PaymentServiceTest.java`
- **.setTotalAmount()** (4 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.codInitiateReturnsNullPaymentUrl()** (4 connections) — `src/test/java/vn/techshop/payment/PaymentServiceTest.java`
- **.vnpayInitiateCreatesUrlWithHmacSha512()** (4 connections) — `src/test/java/vn/techshop/payment/PaymentServiceTest.java`
- **PaymentResult** (3 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **.setPaymentMethod()** (2 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.setAmount()** (2 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setClientIp()** (2 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setCreatedAt()** (2 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setUpdatedAt()** (2 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setUp()** (2 connections) — `src/test/java/vn/techshop/payment/PaymentServiceTest.java`

## Relationships

- [Payment and VNPay](Payment_and_VNPay.md) (10 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (9 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (1 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/order/PurchaseOrder.java`
- `src/main/java/vn/techshop/payment/PaymentService.java`
- `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- `src/test/java/vn/techshop/payment/PaymentServiceTest.java`

## Audit Trail

- EXTRACTED: 20 (53%)
- INFERRED: 18 (47%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*