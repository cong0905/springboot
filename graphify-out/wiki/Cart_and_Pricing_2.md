# Cart and Pricing

> 22 nodes · cohesion 0.10

## Key Concepts

- **CartItem** (22 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **CartItemRepository** (13 connections) — `src/main/java/vn/techshop/cart/CartItemRepository.java`
- **.findByCartIdAndProductIdAndVariantId()** (4 connections) — `src/main/java/vn/techshop/cart/CartItemRepository.java`
- **.countByCartId()** (3 connections) — `src/main/java/vn/techshop/cart/CartItemRepository.java`
- **.findByCartIdAndProductId()** (2 connections) — `src/main/java/vn/techshop/cart/CartItemRepository.java`
- **.CartItem()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.getCartId()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.getCreatedAt()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.getId()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.getProductId()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.getQuantity()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.getUpdatedAt()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.getVariantId()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.setCartId()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.setCreatedAt()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.setProductId()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.setQuantity()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.setUpdatedAt()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **.setVariantId()** (1 connections) — `src/main/java/vn/techshop/cart/CartItem.java`
- **Entity** (1 connections)
- **Table** (1 connections)
- **.deleteByCartId()** (1 connections) — `src/main/java/vn/techshop/cart/CartItemRepository.java`

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (1 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (1 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/cart/CartItem.java`
- `src/main/java/vn/techshop/cart/CartItemRepository.java`

## Audit Trail

- EXTRACTED: 26 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*