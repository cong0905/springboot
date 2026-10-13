# Design and Delivery Guides

> 15 nodes · cohesion 0.21

## Key Concepts

- **.security()** (16 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **SecurityConfig** (11 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **.resolveSafeRedirect()** (5 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **Bean** (4 connections)
- **.failure()** (4 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **HttpServletRequest** (3 connections)
- **.passwordEncoder()** (3 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **.sessionEvents()** (3 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **.sessionRegistry()** (3 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **SessionRegistry** (2 connections)
- **HttpSessionEventPublisher** (1 connections)
- **ClientRegistrationRepository** (1 connections)
- **Configuration** (1 connections)
- **HttpServletResponse** (1 connections)
- **ObjectProvider** (1 connections)

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (4 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (3 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (1 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (1 shared connections)
- [Commerce Reporting](Commerce_Reporting.md) (1 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/SecurityConfig.java`

## Audit Trail

- EXTRACTED: 29 (88%)
- INFERRED: 4 (12%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*