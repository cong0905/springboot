# Payment and VNPay

> 7 nodes · cohesion 0.29

## Key Concepts

- **.resumeUrl()** (9 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **.vnpayReturn()** (6 connections) — `src/main/java/vn/techshop/payment/PaymentController.java`
- **.getPaymentMethod()** (5 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.buildVNPayUrl()** (4 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **.hmacSHA512()** (4 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **.verifyVNPay()** (4 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **.findByOrderIdOrderByCreatedAtDesc()** (4 connections) — `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`

## Relationships

- [Payment and VNPay](Payment_and_VNPay.md) (4 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (2 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (2 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/order/PurchaseOrder.java`
- `src/main/java/vn/techshop/payment/PaymentController.java`
- `src/main/java/vn/techshop/payment/PaymentService.java`
- `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`

## Audit Trail

- EXTRACTED: 8 (53%)
- INFERRED: 7 (47%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*