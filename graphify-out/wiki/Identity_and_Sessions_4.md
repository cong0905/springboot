# Identity and Sessions

> 21 nodes · cohesion 0.16

## Key Concepts

- **ClientIpResolver** (16 connections) — `src/main/java/vn/techshop/config/ClientIpResolver.java`
- **RecoveryController** (13 connections) — `src/main/java/vn/techshop/identity/RecoveryController.java`
- **RequestLimits** (13 connections) — `src/main/java/vn/techshop/identity/RequestLimits.java`
- **.resolve()** (11 connections) — `src/main/java/vn/techshop/config/ClientIpResolver.java`
- **.request()** (9 connections) — `src/main/java/vn/techshop/identity/RecoveryController.java`
- **.reset()** (9 connections) — `src/main/java/vn/techshop/identity/RecoveryController.java`
- **.ChatService()** (8 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.consume()** (8 connections) — `src/main/java/vn/techshop/identity/RequestLimits.java`
- **.forgot()** (4 connections) — `src/main/java/vn/techshop/identity/RecoveryController.java`
- **.RecoveryController()** (4 connections) — `src/main/java/vn/techshop/identity/RecoveryController.java`
- **.literal()** (2 connections) — `src/main/java/vn/techshop/config/ClientIpResolver.java`
- **GetMapping** (2 connections)
- **HttpServletRequest** (2 connections)
- **PostMapping** (2 connections)
- **ResponseBody** (2 connections)
- **.resetPage()** (2 connections) — `src/main/java/vn/techshop/identity/RecoveryController.java`
- **Request** (2 connections) — `src/main/java/vn/techshop/identity/RecoveryController.java`
- **Reset** (2 connections) — `src/main/java/vn/techshop/identity/RecoveryController.java`
- **Window** (2 connections) — `src/main/java/vn/techshop/identity/RequestLimits.java`
- **.ClientIpResolver()** (1 connections) — `src/main/java/vn/techshop/config/ClientIpResolver.java`
- **.RequestLimits()** (1 connections) — `src/main/java/vn/techshop/identity/RequestLimits.java`

## Relationships

- [Security and Web Controllers](Security_and_Web_Controllers.md) (4 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (4 shared connections)
- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (3 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (3 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (2 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (2 shared connections)

## Source Files

- `src/main/java/vn/techshop/chat/ChatService.java`
- `src/main/java/vn/techshop/config/ClientIpResolver.java`
- `src/main/java/vn/techshop/identity/RecoveryController.java`
- `src/main/java/vn/techshop/identity/RequestLimits.java`

## Audit Trail

- EXTRACTED: 45 (87%)
- INFERRED: 7 (13%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*