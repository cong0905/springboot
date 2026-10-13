# Checkout and Orders

> 27 nodes · cohesion 0.12

## Key Concepts

- **.view()** (37 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.publicView()** (27 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.findByOrderIdOrderByProductIdAsc()** (7 connections) — `src/main/java/vn/techshop/order/OrderItemRepository.java`
- **PublicOrderView** (6 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.testMaskingHelpers()** (5 connections) — `src/test/java/vn/techshop/order/OrderLookupTest.java`
- **.findByOrderIdOrderByOrderVersionAsc()** (4 connections) — `src/main/java/vn/techshop/order/OrderHistoryRepository.java`
- **.code()** (4 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **PublicHistoryView** (4 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **PublicItemView** (4 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.PublicOrderView()** (4 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.maskAddress()** (3 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.maskName()** (3 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.maskPhone()** (3 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.getAddress()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getCouponCode()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getCreatedAt()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getCurrency()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getDiscountAmount()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getPhone()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getRecipientName()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getSubtotal()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getVersion()** (3 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getAssignedBranchId()** (2 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getFulfillmentType()** (2 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- **.getHoldExpiresAt()** (2 connections) — `src/main/java/vn/techshop/order/PurchaseOrder.java`
- *... and 2 more nodes in this community*

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (15 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (5 shared connections)
- [Guest Cart and Order Proofs](Guest_Cart_and_Order_Proofs.md) (4 shared connections)

## Source Files

- `src/main/java/vn/techshop/order/OrderHistoryRepository.java`
- `src/main/java/vn/techshop/order/OrderItemRepository.java`
- `src/main/java/vn/techshop/order/OrderService.java`
- `src/main/java/vn/techshop/order/PurchaseOrder.java`
- `src/test/java/vn/techshop/order/OrderLookupTest.java`

## Audit Trail

- EXTRACTED: 29 (43%)
- INFERRED: 39 (57%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*