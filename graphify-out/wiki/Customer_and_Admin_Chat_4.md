# Customer and Admin Chat

> 32 nodes · cohesion 0.16

## Key Concepts

- **ChatIntegrationTest** (25 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **.customerSend()** (24 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.customerHistory()** (23 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.adminSend()** (12 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **Message** (10 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.twoWayConversationUnreadAndReadReceipts()** (10 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **.readWatermarkDoesNotHideLaterMessagesOrMoveBackwards()** (9 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **.send()** (9 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **Test** (9 connections)
- **PostMapping** (8 connections)
- **.findByCustomerId()** (8 connections) — `src/main/java/vn/techshop/chat/ChatConversationRepository.java`
- **.adminRead()** (8 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **Read** (8 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **Send** (8 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.conversation()** (8 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **.adminSend()** (7 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.customerSend()** (7 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.mark()** (7 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.cursorPagingHasNoGapsOrDuplicateMessages()** (7 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **.sendRateLimitRejectsExcessAndAllowsReplay()** (7 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **.adminRead()** (6 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.customerRead()** (6 connections) — `src/main/java/vn/techshop/chat/ChatApi.java`
- **.authenticationRolesCsrfAndForgeryAreRejected()** (6 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **.concurrentFirstMessagesCreateOneConversationAndConcurrentRetryCreatesOneMessage()** (6 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- **.retriesAreIdempotentAndSenderIdentityComesFromSession()** (5 connections) — `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`
- *... and 7 more nodes in this community*

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (21 shared connections)
- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (17 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (7 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (5 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (1 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (1 shared connections)
- [Commerce Reporting](Commerce_Reporting.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/chat/ChatApi.java`
- `src/main/java/vn/techshop/chat/ChatConversationRepository.java`
- `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- `src/main/java/vn/techshop/chat/ChatService.java`
- `src/test/java/vn/techshop/chat/AiChatIntegrationTest.java`
- `src/test/java/vn/techshop/chat/ChatIntegrationTest.java`

## Audit Trail

- EXTRACTED: 92 (68%)
- INFERRED: 43 (32%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*