# Design and Delivery Guides

> 10 nodes · cohesion 0.29

## Key Concepts

- **StorefrontModel** (16 connections) — `src/main/java/vn/techshop/config/StorefrontModel.java`
- **PaymentController** (12 connections) — `src/main/java/vn/techshop/payment/PaymentController.java`
- **StorefrontModel.java** (10 connections) — `src/main/java/vn/techshop/config/StorefrontModel.java`
- **.mockPayPage()** (10 connections) — `src/main/java/vn/techshop/payment/PaymentController.java`
- **.navigation()** (9 connections) — `src/main/java/vn/techshop/config/StorefrontModel.java`
- **org.springframework.web.bind.annotation.ControllerAdvice** (2 connections)
- **org.springframework.web.bind.annotation.ModelAttribute** (2 connections)
- **PaymentView** (2 connections) — `src/main/java/vn/techshop/payment/PaymentController.java`
- **Environment** (1 connections)
- **HttpServletRequest** (1 connections)

## Relationships

- [Security and Web Controllers](Security_and_Web_Controllers.md) (13 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (10 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (5 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (4 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (3 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (2 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/StorefrontModel.java`
- `src/main/java/vn/techshop/payment/PaymentController.java`

## Audit Trail

- EXTRACTED: 44 (86%)
- INFERRED: 7 (14%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*