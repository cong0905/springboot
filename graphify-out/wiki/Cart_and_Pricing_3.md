# Cart and Pricing

> 19 nodes · cohesion 0.13

## Key Concepts

- **Cart** (19 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.touch()** (10 connections) — `src/main/java/vn/techshop/cart/CartService.java`
- **.lock()** (8 connections) — `src/main/java/vn/techshop/cart/CartService.java`
- **.remove()** (7 connections) — `src/main/java/vn/techshop/cart/CartService.java`
- **.lockByUserId()** (6 connections) — `src/main/java/vn/techshop/cart/CartRepository.java`
- **.findByIdAndCartId()** (4 connections) — `src/main/java/vn/techshop/cart/CartItemRepository.java`
- **.getId()** (2 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.getUpdatedAt()** (2 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.getVersion()** (2 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.setUpdatedAt()** (2 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.Cart()** (1 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.getCreatedAt()** (1 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.getUserId()** (1 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.setCreatedAt()** (1 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.setUserId()** (1 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **.setVersion()** (1 connections) — `src/main/java/vn/techshop/cart/Cart.java`
- **Entity** (1 connections)
- **Table** (1 connections)
- **Lock** (1 connections)

## Relationships

- [Cart and Pricing](Cart_and_Pricing.md) (1 shared connections)
- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (1 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (1 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/cart/Cart.java`
- `src/main/java/vn/techshop/cart/CartItemRepository.java`
- `src/main/java/vn/techshop/cart/CartRepository.java`
- `src/main/java/vn/techshop/cart/CartService.java`

## Audit Trail

- EXTRACTED: 22 (85%)
- INFERRED: 4 (15%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*