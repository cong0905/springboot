# Coupons and Discounts

> 75 nodes · cohesion 0.07

## Key Concepts

- **Coupon** (42 connections) — `src/main/java/vn/techshop/coupon/Coupon.java`
- **CouponService** (28 connections) — `src/main/java/vn/techshop/coupon/CouponService.java`
- **.create()** (25 connections) — `src/main/java/vn/techshop/coupon/CouponService.java`
- **.from()** (20 connections) — `src/main/java/vn/techshop/coupon/CouponQuote.java`
- **.validateAndCalculate()** (17 connections) — `src/main/java/vn/techshop/coupon/CouponService.java`
- **.redeem()** (13 connections) — `src/main/java/vn/techshop/coupon/CouponService.java`
- **.percentDiscountCalculatedCorrectlyWithCap()** (13 connections) — `src/test/java/vn/techshop/coupon/CouponServiceTest.java`
- **.throwsWhenCouponExpired()** (13 connections) — `src/test/java/vn/techshop/coupon/CouponServiceTest.java`
- **CouponRepository** (12 connections) — `src/main/java/vn/techshop/coupon/CouponRepository.java`
- **.fixedDiscountCalculatedCorrectly()** (12 connections) — `src/test/java/vn/techshop/coupon/CouponServiceTest.java`
- **.redeemIncrementsUsedCount()** (12 connections) — `src/test/java/vn/techshop/coupon/CouponServiceTest.java`
- **.throwsWhenMaxUsesReached()** (12 connections) — `src/test/java/vn/techshop/coupon/CouponServiceTest.java`
- **.throwsWhenOrderAmountBelowMinimum()** (12 connections) — `src/test/java/vn/techshop/coupon/CouponServiceTest.java`
- **CouponServiceTest** (11 connections) — `src/test/java/vn/techshop/coupon/CouponServiceTest.java`
- **CouponApi** (10 connections) — `src/main/java/vn/techshop/coupon/CouponApi.java`
- **.quoteByCode()** (9 connections) — `src/main/java/vn/techshop/coupon/CouponRepository.java`
- **.setCode()** (8 connections) — `src/main/java/vn/techshop/coupon/Coupon.java`
- **.setDiscountType()** (8 connections) — `src/main/java/vn/techshop/coupon/Coupon.java`
- **.setDiscountValue()** (8 connections) — `src/main/java/vn/techshop/coupon/Coupon.java`
- **.setMaxUses()** (8 connections) — `src/main/java/vn/techshop/coupon/Coupon.java`
- **.setMinOrderAmount()** (8 connections) — `src/main/java/vn/techshop/coupon/Coupon.java`
- **.setStatus()** (8 connections) — `src/main/java/vn/techshop/coupon/Coupon.java`
- **.setUsedCount()** (8 connections) — `src/main/java/vn/techshop/coupon/Coupon.java`
- **.validate()** (8 connections) — `src/main/java/vn/techshop/coupon/CouponApi.java`
- **CouponQuote** (7 connections) — `src/main/java/vn/techshop/coupon/CouponQuote.java`
- *... and 50 more nodes in this community*

## Relationships

- [Checkout and Orders](Checkout_and_Orders.md) (8 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (5 shared connections)
- [Payment and VNPay](Payment_and_VNPay.md) (5 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (5 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (4 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (2 shared connections)
- [Catalog and Images](Catalog_and_Images.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/coupon/Coupon.java`
- `src/main/java/vn/techshop/coupon/CouponApi.java`
- `src/main/java/vn/techshop/coupon/CouponQuote.java`
- `src/main/java/vn/techshop/coupon/CouponRepository.java`
- `src/main/java/vn/techshop/coupon/CouponService.java`
- `src/test/java/vn/techshop/coupon/CouponServiceTest.java`

## Audit Trail

- EXTRACTED: 135 (60%)
- INFERRED: 91 (40%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*