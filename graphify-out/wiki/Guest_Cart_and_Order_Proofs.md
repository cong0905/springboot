# Guest Cart and Order Proofs

> 33 nodes · cohesion 0.11

## Key Concepts

- **OrderView** (21 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **GuestApi** (20 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **.checkout()** (11 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **.require()** (11 connections) — `src/main/java/vn/techshop/guest/GuestService.java`
- **HttpServletRequest** (10 connections)
- **.owner()** (9 connections) — `src/main/java/vn/techshop/guest/GuestService.java`
- **.current()** (8 connections) — `src/main/java/vn/techshop/guest/GuestService.java`
- **.add()** (7 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **.cancel()** (7 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **.cart()** (7 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **.preview()** (7 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **.create()** (7 connections) — `src/main/java/vn/techshop/guest/GuestService.java`
- **.cart()** (7 connections) — `src/main/java/vn/techshop/guest/GuestWeb.java`
- **.detail()** (6 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **.remove()** (6 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **PostMapping** (6 connections)
- **.checkout()** (6 connections) — `src/main/java/vn/techshop/guest/GuestWeb.java`
- **.order()** (6 connections) — `src/main/java/vn/techshop/guest/GuestWeb.java`
- **.orders()** (6 connections) — `src/main/java/vn/techshop/guest/GuestWeb.java`
- **Add** (5 connections) — `src/main/java/vn/techshop/cart/CartService.java`
- **.empty()** (5 connections) — `src/main/java/vn/techshop/guest/GuestApi.java`
- **GetMapping** (5 connections)
- **HistoryView** (4 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **ItemView** (4 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **.OrderView()** (4 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- *... and 8 more nodes in this community*

## Relationships

- [Cart and Pricing](Cart_and_Pricing.md) (12 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (9 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (6 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (5 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (5 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (4 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (3 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (2 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (1 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/cart/CartService.java`
- `src/main/java/vn/techshop/guest/GuestApi.java`
- `src/main/java/vn/techshop/guest/GuestService.java`
- `src/main/java/vn/techshop/guest/GuestWeb.java`
- `src/main/java/vn/techshop/order/OrderService.java`

## Audit Trail

- EXTRACTED: 79 (75%)
- INFERRED: 27 (25%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*