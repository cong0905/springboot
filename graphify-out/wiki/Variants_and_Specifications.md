# Variants and Specifications

> 24 nodes · cohesion 0.12

## Key Concepts

- **ProductVariant** (25 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.view()** (15 connections) — `src/main/java/vn/techshop/variant/ProductVariantService.java`
- **.lockById()** (5 connections) — `src/main/java/vn/techshop/variant/ProductVariantRepository.java`
- **View** (5 connections) — `src/main/java/vn/techshop/variant/ProductVariantService.java`
- **.findByProductIdAndStatusOrderByIdAsc()** (3 connections) — `src/main/java/vn/techshop/variant/ProductVariantRepository.java`
- **.getColorName()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.getId()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.getOriginalPrice()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.getPrice()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.getProductId()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.getSkuCode()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.getStatus()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.getStorageCapacity()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.getVersion()** (2 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **Entity** (1 connections)
- **Table** (1 connections)
- **.setColorName()** (1 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.setOriginalPrice()** (1 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.setPrice()** (1 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.setProductId()** (1 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.setSkuCode()** (1 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.setStatus()** (1 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **.setStorageCapacity()** (1 connections) — `src/main/java/vn/techshop/variant/ProductVariant.java`
- **Lock** (1 connections)

## Relationships

- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (1 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/variant/ProductVariant.java`
- `src/main/java/vn/techshop/variant/ProductVariantRepository.java`
- `src/main/java/vn/techshop/variant/ProductVariantService.java`

## Audit Trail

- EXTRACTED: 25 (74%)
- INFERRED: 9 (26%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*