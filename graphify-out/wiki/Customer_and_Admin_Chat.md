# Customer and Admin Chat

> 41 nodes · cohesion 0.11

## Key Concepts

- **ChatService** (53 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **ChatApi** (21 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.startAi()** (14 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **Support** (12 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.unread()** (11 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.accept()** (10 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.adminConversation()** (10 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.adminHistory()** (9 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.closeSupport()** (9 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.handoff()** (9 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.inbox()** (9 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.support()** (9 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **History** (8 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.start()** (6 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **GetMapping** (6 connections)
- **.accept()** (5 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.adminMessages()** (5 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.adminUnread()** (5 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.closeSupport()** (5 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.customerMessages()** (5 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.customerUnread()** (5 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.handoff()** (5 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.inbox()** (5 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **Responsive customer/admin chat thread and inbox** (5 connections) — `src/main/resources/templates/chat.html`
- **AiStart** (4 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- *... and 16 more nodes in this community*

## Relationships

- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (36 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (30 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (7 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (4 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (3 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (2 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (1 shared connections)

## Source Files

- `docs/19-customer-admin-chat.md`
- `src/main/java/vn/techshop/chat/ChatApi.java`
- `src/main/java/vn/techshop/chat/ChatService.java`
- `src/main/resources/db/migration/V25__customer_admin_chat.sql`
- `src/main/resources/static/js/chat.js`
- `src/main/resources/templates/chat.html`

## Audit Trail

- EXTRACTED: 148 (87%)
- INFERRED: 22 (13%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*