# Checkout and Orders

> 11 nodes · cohesion 0.27

## Key Concepts

- **.getId()** (11 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.revoke()** (10 connections) — `src/main/java/vn/techshop/loyalty/LoyaltyService.java`
- **.delivered()** (9 connections) — `src/main/java/vn/techshop/loyalty/LoyaltyService.java`
- **.getTotalAmount()** (8 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.apply()** (7 connections) — `src/main/java/vn/techshop/installment/InstallmentService.java`
- **.activate()** (7 connections) — `src/main/java/vn/techshop/warranty/WarrantyService.java`
- **.resolvePaymentStatus()** (6 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.revoke()** (6 connections) — `src/main/java/vn/techshop/warranty/WarrantyService.java`
- **.getShippingFee()** (5 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getDeliveredAt()** (4 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getUserId()** (4 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (6 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (5 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (2 shared connections)
- [Installment Applications](Installment_Applications.md) (1 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/installment/InstallmentService.java`
- `src/main/java/vn/techshop/loyalty/LoyaltyService.java`
- `src/main/java/vn/techshop/order/OrderService.java`
- `src/main/java/vn/techshop/order/PurchaseOrder.java`
- `src/main/java/vn/techshop/warranty/WarrantyService.java`

## Audit Trail

- EXTRACTED: 14 (47%)
- INFERRED: 16 (53%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*