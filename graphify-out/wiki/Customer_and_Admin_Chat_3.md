# Customer and Admin Chat

> 35 nodes · cohesion 0.13

## Key Concepts

- **AiChatWorker** (24 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **ChatConversation** (17 connections) — `src/main/java/vn/techshop/chat/ChatConversation.java`
- **.ready()** (14 connections) — `src/main/java/vn/techshop/ai/AiSettings.java`
- **AiChatLifecycle** (14 connections) — `src/main/java/vn/techshop/chat/AiChatLifecycle.java`
- **ChatConversationRepository** (14 connections) — `src/main/java/vn/techshop/chat/ChatConversationRepository.java`
- **AiJobRepository** (12 connections) — `src/main/java/vn/techshop/chat/AiJobRepository.java`
- **.lockById()** (12 connections) — `src/main/java/vn/techshop/chat/ChatConversationRepository.java`
- **.claim()** (11 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **.complete()** (10 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **.runOnce()** (10 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **.enqueue()** (9 connections) — `src/main/java/vn/techshop/chat/AiChatLifecycle.java`
- **.handoff()** (9 connections) — `src/main/java/vn/techshop/chat/AiChatLifecycle.java`
- **AiChatJob** (7 connections) — `src/main/java/vn/techshop/chat/AiChatJob.java`
- **.invalidate()** (7 connections) — `src/main/java/vn/techshop/chat/AiChatLifecycle.java`
- **.cancelUnavailable()** (7 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **.valid()** (7 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **.fail()** (6 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **.tick()** (6 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **Work** (6 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **.accept()** (5 connections) — `src/main/java/vn/techshop/chat/AiChatLifecycle.java`
- **.eligible()** (5 connections) — `src/main/java/vn/techshop/chat/AiJobRepository.java`
- **.AiChatLifecycle()** (4 connections) — `src/main/java/vn/techshop/chat/AiChatLifecycle.java`
- **.current()** (4 connections) — `src/main/java/vn/techshop/chat/AiChatWorker.java`
- **.supersede()** (4 connections) — `src/main/java/vn/techshop/chat/AiJobRepository.java`
- **.aiConversations()** (4 connections) — `src/main/java/vn/techshop/chat/ChatConversationRepository.java`
- *... and 10 more nodes in this community*

## Relationships

- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (20 shared connections)
- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (16 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (2 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (1 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/ai/AiAdvisor.java`
- `src/main/java/vn/techshop/ai/AiSettings.java`
- `src/main/java/vn/techshop/chat/AiChatJob.java`
- `src/main/java/vn/techshop/chat/AiChatLifecycle.java`
- `src/main/java/vn/techshop/chat/AiChatWorker.java`
- `src/main/java/vn/techshop/chat/AiJobRepository.java`
- `src/main/java/vn/techshop/chat/ChatConversation.java`
- `src/main/java/vn/techshop/chat/ChatConversationRepository.java`

## Audit Trail

- EXTRACTED: 92 (77%)
- INFERRED: 28 (23%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*