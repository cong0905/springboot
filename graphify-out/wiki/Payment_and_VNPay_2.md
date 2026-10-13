# Payment and VNPay

> 22 nodes · cohesion 0.10

## Key Concepts

- **PaymentTransaction** (34 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.findByCode()** (9 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **.findByCodeIsExactAndDoesNotFallbackToLatestOrderTxn()** (5 connections) — `src/test/java/vn/techshop/payment/PaymentServiceTest.java`
- **.findByTransactionCode()** (4 connections) — `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`
- **Entity** (1 connections)
- **Table** (1 connections)
- **.getAmount()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getBankCode()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getClientIp()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getCreatedAt()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getId()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getOrderId()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getPaymentGateway()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getResponseMessage()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getStatus()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getTransactionCode()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getUpdatedAt()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.getVersion()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.PaymentTransaction()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setBankCode()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setResponseMessage()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setVersion()** (1 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`

## Relationships

- [Payment and VNPay](Payment_and_VNPay.md) (9 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/payment/PaymentService.java`
- `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`
- `src/test/java/vn/techshop/payment/PaymentServiceTest.java`

## Audit Trail

- EXTRACTED: 29 (88%)
- INFERRED: 4 (12%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*