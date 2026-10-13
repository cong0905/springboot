# Security and Web Controllers

> 43 nodes · cohesion 0.09

## Key Concepts

- **.searchOrder()** (14 connections) — `src/main/java/vn/techshop/order/LookupOrderController.java`
- **LookupOrderController** (13 connections) — `src/main/java/vn/techshop/order/LookupOrderController.java`
- **.lookup()** (13 connections) — `src/main/java/vn/techshop/order/OrderService.java`
- **LookupOrderController.java** (12 connections) — `src/main/java/vn/techshop/order/LookupOrderController.java`
- **LoginAttempts** (11 connections) — `src/main/java/vn/techshop/config/LoginAttempts.java`
- **OrderLookupTest** (11 connections) — `src/test/java/vn/techshop/order/OrderLookupTest.java`
- **LoginAttemptsTest** (9 connections) — `src/test/java/vn/techshop/config/LoginAttemptsTest.java`
- **.failed()** (8 connections) — `src/main/java/vn/techshop/config/LoginAttempts.java`
- **.blocked()** (7 connections) — `src/main/java/vn/techshop/config/LoginAttempts.java`
- **LoginAttempts.java** (6 connections) — `src/main/java/vn/techshop/config/LoginAttempts.java`
- **.testFullCacheEvictionDoesNotAllowUnmeteredFailures()** (6 connections) — `src/test/java/vn/techshop/config/LoginAttemptsTest.java`
- **locale** (5 connections) — `src/main/java/vn/techshop/config/LoginAttempts.java`
- **Counter** (5 connections) — `src/main/java/vn/techshop/config/LoginAttempts.java`
- **.testSuccessfulLoginClearsFailures()** (5 connections) — `src/test/java/vn/techshop/config/LoginAttemptsTest.java`
- **.fullLimiterCacheRejectsNewIpWithoutLookingUpCustomerData()** (5 connections) — `src/test/java/vn/techshop/order/OrderLookupTest.java`
- **.normalizeUser()** (4 connections) — `src/main/java/vn/techshop/config/LoginAttempts.java`
- **.recordAttempt()** (4 connections) — `src/main/java/vn/techshop/order/LookupOrderController.java`
- **.testAccountRecoversAfterExpiry()** (4 connections) — `src/test/java/vn/techshop/config/LoginAttemptsTest.java`
- **.testMultipleAccountsFromSameIpTriggersIpLimit()** (4 connections) — `src/test/java/vn/techshop/config/LoginAttemptsTest.java`
- **.testMultipleIpsAgainstSameAccountTriggersLimit()** (4 connections) — `src/test/java/vn/techshop/config/LoginAttemptsTest.java`
- **.testFailedLookupReturnsUniformErrorMessage()** (4 connections) — `src/test/java/vn/techshop/order/OrderLookupTest.java`
- **.testRateLimitByIdentifierReturns429()** (4 connections) — `src/test/java/vn/techshop/order/OrderLookupTest.java`
- **.testRateLimitByIpReturns429()** (4 connections) — `src/test/java/vn/techshop/order/OrderLookupTest.java`
- **.testSuccessfulLookupReturnsMaskedData()** (4 connections) — `src/test/java/vn/techshop/order/OrderLookupTest.java`
- **.getAccountAttempts()** (3 connections) — `src/main/java/vn/techshop/config/LoginAttempts.java`
- *... and 18 more nodes in this community*

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (16 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (4 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (4 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (3 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (2 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (2 shared connections)
- [Branches and Inventory](Branches_and_Inventory.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/LoginAttempts.java`
- `src/main/java/vn/techshop/order/LookupOrderController.java`
- `src/main/java/vn/techshop/order/OrderRepository.java`
- `src/main/java/vn/techshop/order/OrderService.java`
- `src/main/java/vn/techshop/shared/Rules.java`
- `src/test/java/vn/techshop/config/LoginAttemptsTest.java`
- `src/test/java/vn/techshop/order/OrderLookupTest.java`

## Audit Trail

- EXTRACTED: 86 (77%)
- INFERRED: 26 (23%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*