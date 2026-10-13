# Customer and Admin Chat

> 17 nodes · cohesion 0.22

## Key Concepts

- **Query** (24 connections) — `src/main/java/vn/techshop/catalog/StorefrontCatalogService.java`
- **ChatMessageRepository** (19 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **ChatMessage** (12 connections) — `src/main/java/vn/techshop/chat/ChatMessage.java`
- **.history()** (9 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **org.springframework.data.domain.Pageable** (7 connections)
- **ChatMessageRepository.java** (5 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.context()** (5 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.findByConversationIdOrderByIdDesc()** (5 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.findByConversationIdAndIdGreaterThanOrderByIdAsc()** (4 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.findByConversationIdAndIdLessThanOrderByIdDesc()** (4 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.adminUnread()** (3 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.customerUnread()** (3 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.findByConversationIdAndSenderIdAndClientMessageId()** (3 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.latestId()** (3 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **.existsByInReplyToId()** (2 connections) — `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- **Entity** (1 connections)
- **Table** (1 connections)

## Relationships

- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (4 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (2 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/catalog/StorefrontCatalogService.java`
- `src/main/java/vn/techshop/chat/ChatMessage.java`
- `src/main/java/vn/techshop/chat/ChatMessageRepository.java`
- `src/main/java/vn/techshop/chat/ChatService.java`

## Audit Trail

- EXTRACTED: 35 (92%)
- INFERRED: 3 (8%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*