# Design and Delivery Guides

> 53 nodes · cohesion 0.07

## Key Concepts

- **TechPrincipal** (127 connections) — `src/main/java/vn/techshop/identity/TechPrincipal.java`
- **.id()** (77 connections) — `src/main/java/vn/techshop/identity/TechPrincipal.java`
- **.missing()** (27 connections) — `src/main/java/vn/techshop/shared/BusinessException.java`
- **.actor()** (14 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.list()** (14 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **OrderApi** (13 connections) — `src/main/java/vn/techshop/order/OrderApi.java`
- **.ownLocked()** (11 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.role()** (10 connections) — `src/main/java/vn/techshop/identity/TechPrincipal.java`
- **.checkout()** (10 connections) — `src/main/java/vn/techshop/order/OrderApi.java`
- **.customerRead()** (9 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.qr()** (9 connections) — `src/main/java/vn/techshop/order/PickupQrController.java`
- **.mockPayConfirm()** (9 connections) — `src/main/java/vn/techshop/payment/PaymentController.java`
- **PostMapping** (8 connections)
- **.adjustment()** (7 connections) — `src/main/java/vn/techshop/config/AdminApi.java`
- **.cancel()** (7 connections) — `src/main/java/vn/techshop/config/AdminApi.java`
- **.orders()** (7 connections) — `src/main/java/vn/techshop/config/AdminApi.java`
- **.processRefund()** (7 connections) — `src/main/java/vn/techshop/config/AdminApi.java`
- **.requestAdminRefund()** (7 connections) — `src/main/java/vn/techshop/config/AdminApi.java`
- **.transition()** (7 connections) — `src/main/java/vn/techshop/config/AdminApi.java`
- **.me()** (7 connections) — `src/main/java/vn/techshop/identity/IdentityApi.java`
- **.cancel()** (7 connections) — `src/main/java/vn/techshop/order/OrderApi.java`
- **.list()** (7 connections) — `src/main/java/vn/techshop/order/OrderApi.java`
- **.refund()** (7 connections) — `src/main/java/vn/techshop/order/OrderApi.java`
- **.delete()** (7 connections) — `src/main/java/vn/techshop/review/ReviewApi.java`
- **.remove()** (6 connections) — `src/main/java/vn/techshop/cart/CartApi.java`
- *... and 28 more nodes in this community*

## Relationships

- [Payment and VNPay](Payment_and_VNPay.md) (13 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (13 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (11 shared connections)
- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (8 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (6 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (6 shared connections)
- [Guest Cart and Order Proofs](Guest_Cart_and_Order_Proofs.md) (6 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (6 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (5 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (3 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (2 shared connections)

## Source Files

- `src/main/java/vn/techshop/cart/CartApi.java`
- `src/main/java/vn/techshop/chat/ChatApi.java`
- `src/main/java/vn/techshop/chat/ChatService.java`
- `src/main/java/vn/techshop/config/AdminApi.java`
- `src/main/java/vn/techshop/config/CommerceSettingsService.java`
- `src/main/java/vn/techshop/config/OmnichannelWeb.java`
- `src/main/java/vn/techshop/identity/IdentityApi.java`
- `src/main/java/vn/techshop/identity/TechPrincipal.java`
- `src/main/java/vn/techshop/order/OrderApi.java`
- `src/main/java/vn/techshop/order/OrderService.java`
- `src/main/java/vn/techshop/order/PickupQrController.java`
- `src/main/java/vn/techshop/payment/PaymentController.java`
- `src/main/java/vn/techshop/review/ReviewApi.java`
- `src/main/java/vn/techshop/shared/BusinessException.java`
- `src/test/java/vn/techshop/payment/OrderStatusAndRefundUxTest.java`

## Audit Trail

- EXTRACTED: 126 (69%)
- INFERRED: 56 (31%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*