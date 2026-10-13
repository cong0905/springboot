# Checkout and Orders

> 13 nodes · cohesion 0.23

## Key Concepts

- **.cancel()** (30 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.collect()** (17 connections) — `src/main/java/vn/techshop/order/PickupService.java`
- **.lockById()** (15 connections) — `src/main/java/vn/techshop/order/OrderRepository.java`
- **.decision()** (12 connections) — `src/main/java/vn/techshop/installment/InstallmentService.java`
- **.exchange()** (12 connections) — `src/main/java/vn/techshop/warranty/WarrantyExchangeService.java`
- **2. Backlog sửa chữa có căn cứ** (11 connections) — `docs/17-repair-plan.md`
- **2. Backlog sửa chữa có căn cứ** (11 connections) — `kehoanh.md`
- **.vnPayIpn()** (10 connections) — `src/main/java/vn/techshop/payment/PaymentService.java`
- **.transition()** (9 connections) — `src/main/java/vn/techshop/warranty/WarrantyService.java`
- **.history()** (8 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.lockOwned()** (6 connections) — `src/main/java/vn/techshop/order/OrderRepository.java`
- **Lock** (2 connections)
- **Result** (2 connections) — `src/main/java/vn/techshop/warranty/WarrantyExchangeService.java`

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (12 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (11 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (8 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (6 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (4 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (4 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (3 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (2 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (2 shared connections)
- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (2 shared connections)
- [Guest Cart and Order Proofs](Guest_Cart_and_Order_Proofs.md) (2 shared connections)
- [Installment Applications](Installment_Applications.md) (1 shared connections)

## Source Files

- `docs/17-repair-plan.md`
- `kehoanh.md`
- `src/main/java/vn/techshop/installment/InstallmentService.java`
- `src/main/java/vn/techshop/order/OrderRepository.java`
- `src/main/java/vn/techshop/order/OrderService.java`
- `src/main/java/vn/techshop/order/PickupService.java`
- `src/main/java/vn/techshop/payment/PaymentService.java`
- `src/main/java/vn/techshop/warranty/WarrantyExchangeService.java`
- `src/main/java/vn/techshop/warranty/WarrantyService.java`

## Audit Trail

- EXTRACTED: 35 (47%)
- INFERRED: 40 (53%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*