# Payment and VNPay

> 18 nodes · cohesion 0.20

## Key Concepts

- **.completePaymentFailureKeepsOrderUnpaid()** (14 connections) — `src/test/java/vn/techshop/payment/PaymentServiceTest.java`
- **.completePaymentSuccessUpdatesOrderToConfirmedAndPaid()** (14 connections) — `src/test/java/vn/techshop/payment/PaymentServiceTest.java`
- **PaymentTransactionRepository** (10 connections) — `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`
- **CouponRepository.java** (8 connections) — `src/main/java/vn/techshop/coupon/CouponRepository.java`
- **.lockByCode()** (8 connections) — `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`
- **.getStatus()** (7 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **org.springframework.data.jpa.repository.JpaRepository** (6 connections)
- **.orderIdByCode()** (6 connections) — `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`
- **org.springframework.data.jpa.repository.Query** (5 connections)
- **.getPaymentStatus()** (5 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.setTransactionCode()** (5 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setOrderId()** (4 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setPaymentGateway()** (4 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **.setStatus()** (4 connections) — `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- **PaymentTransactionRepository.java** (4 connections) — `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`
- **org.springframework.data.jpa.repository.Lock** (3 connections)
- **.setPaymentStatus()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.setStatus()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (10 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (4 shared connections)
- [Branches and Inventory](Branches_and_Inventory.md) (1 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (1 shared connections)
- [Coupons and Discounts](Coupons_and_Discounts.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/coupon/CouponRepository.java`
- `src/main/java/vn/techshop/order/PurchaseOrder.java`
- `src/main/java/vn/techshop/payment/PaymentTransaction.java`
- `src/main/java/vn/techshop/payment/PaymentTransactionRepository.java`
- `src/test/java/vn/techshop/payment/PaymentServiceTest.java`

## Audit Trail

- EXTRACTED: 24 (50%)
- INFERRED: 24 (50%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*