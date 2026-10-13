package vn.techshop.coupon;
import java.math.BigDecimal;
import java.time.Instant;
/** Preview data is deliberately not a managed JPA entity before redemption locks. */
public record CouponQuote(String code,String description,String discountType,BigDecimal discountValue,
        BigDecimal minOrderAmount,BigDecimal maxDiscountAmount,Integer maxUses,Integer usedCount,
        Instant expiresAt,String status,Long assignedUserId) {
    public static CouponQuote from(Coupon c){return new CouponQuote(c.getCode(),c.getDescription(),c.getDiscountType(),c.getDiscountValue(),c.getMinOrderAmount(),c.getMaxDiscountAmount(),c.getMaxUses(),c.getUsedCount(),c.getExpiresAt(),c.getStatus(),c.getAssignedUserId());}
}
