package vn.techshop.coupon;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.BusinessException;

@Service
@Transactional(readOnly = true)
public class CouponService {
    public record CouponResult(
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        String description
    ) {}

    public record CouponInput(
        String code,
        String description,
        String discountType,
        BigDecimal discountValue,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer maxUses,
        Instant expiresAt,
        String status,
        Long assignedUserId
    ) {
        public CouponInput(String code,String description,String discountType,BigDecimal discountValue,BigDecimal minOrderAmount,BigDecimal maxDiscountAmount,Integer maxUses,Instant expiresAt,String status){this(code,description,discountType,discountValue,minOrderAmount,maxDiscountAmount,maxUses,expiresAt,status,null);}
    }
    public record PersonalCoupon(String code,String description,String discountType,BigDecimal discountValue,BigDecimal minOrderAmount,Instant expiresAt,boolean usable) {}
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    public List<PersonalCoupon> assigned(Long userId){return coupons.findByAssignedUserIdOrderByCreatedAtDesc(userId).stream().map(c->new PersonalCoupon(c.getCode(),c.getDescription(),c.getDiscountType(),c.getDiscountValue(),c.getMinOrderAmount(),c.getExpiresAt(),"ACTIVE".equals(c.getStatus())&&(c.getExpiresAt()==null||c.getExpiresAt().isAfter(clock.instant()))&&c.getUsedCount()<c.getMaxUses())).toList();}
    private void owner(CouponQuote coupon,Long userId){if(coupon.assignedUserId()!=null&&!coupon.assignedUserId().equals(userId))throw BusinessException.conflict("COUPON_NOT_FOUND","Mã giảm giá không tồn tại.");}


    private final CouponRepository coupons;
    private final Clock clock;

    public CouponService(CouponRepository coupons, Clock clock) {
        this.coupons = coupons;
        this.clock = clock;
    }

    public CouponResult validateAndCalculate(String code, BigDecimal subtotal) {return validateAndCalculate(code,subtotal,null);}
    public CouponResult validateAndCalculate(String code, BigDecimal subtotal,Long userId) {
        if (code == null || code.isBlank()) {
            throw BusinessException.invalid("Vui lòng nhập mã giảm giá.");
        }
        var coupon = coupons.quoteByCode(code.trim())
            .orElseThrow(() -> BusinessException.conflict("COUPON_NOT_FOUND", "Mã giảm giá không tồn tại."));

        owner(coupon,userId);
        validateCoupon(coupon, subtotal);
        BigDecimal discount = calculateDiscount(coupon, subtotal);

        return new CouponResult(
            coupon.code(),
            coupon.discountType(),
            coupon.discountValue(),
            discount,
            coupon.description()
        );
    }

    @Transactional
    public BigDecimal redeem(String code, BigDecimal subtotal) {return redeem(code,subtotal,null);}
    @Transactional public BigDecimal redeem(String code, BigDecimal subtotal,Long userId) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }
        var coupon = coupons.lockByCodeIgnoreCase(code.trim())
            .orElseThrow(() -> BusinessException.conflict("COUPON_NOT_FOUND", "Mã giảm giá không tồn tại."));

        var quote=CouponQuote.from(coupon);
        owner(quote,userId);
        validateCoupon(quote, subtotal);
        BigDecimal discount = calculateDiscount(quote, subtotal);

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        coupon.setUpdatedAt(clock.instant());
        coupons.saveAndFlush(coupon);

        return discount;
    }

    private void validateCoupon(CouponQuote coupon, BigDecimal subtotal) {
        if (!"ACTIVE".equalsIgnoreCase(coupon.status())) {
            throw BusinessException.conflict("COUPON_INACTIVE", "Mã giảm giá hiện đang bị khóa.");
        }
        Instant now = clock.instant();
        if (coupon.expiresAt() != null && !coupon.expiresAt().isAfter(now)) {
            throw BusinessException.conflict("COUPON_EXPIRED", "Mã giảm giá đã hết hạn sử dụng.");
        }
        if (coupon.usedCount() != null && coupon.maxUses() != null && coupon.usedCount() >= coupon.maxUses()) {
            throw BusinessException.conflict("COUPON_EXHAUSTED", "Mã giảm giá đã hết lượt sử dụng.");
        }
        if (coupon.minOrderAmount() != null && subtotal.compareTo(coupon.minOrderAmount()) < 0) {
            throw BusinessException.conflict("COUPON_MIN_ORDER", "Đơn hàng tối thiểu " + coupon.minOrderAmount().setScale(0) + " ₫ để áp dụng mã.");
        }
    }

    private BigDecimal calculateDiscount(CouponQuote coupon, BigDecimal subtotal) {
        BigDecimal discount;
        if ("PERCENT".equalsIgnoreCase(coupon.discountType())) {
            discount = subtotal.multiply(coupon.discountValue())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
            if (coupon.maxDiscountAmount() != null && coupon.maxDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                discount = discount.min(coupon.maxDiscountAmount());
            }
        } else {
            // FIXED discount
            discount = coupon.discountValue().min(subtotal);
        }
        return discount.max(BigDecimal.ZERO).setScale(0, RoundingMode.HALF_UP);
    }

    public List<Coupon> listAll() {
        return coupons.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Coupon create(CouponInput input) {
        String code=vn.techshop.shared.Rules.text(input.code(),1,50).toUpperCase(java.util.Locale.ROOT);
        if(!code.matches("[A-Z0-9_-]{1,50}") || input.discountType()==null || !java.util.Set.of("PERCENT","FIXED").contains(input.discountType().toUpperCase(java.util.Locale.ROOT)) || input.discountValue()==null || input.discountValue().signum()<=0 || input.discountValue().stripTrailingZeros().scale()>0 || ("PERCENT".equalsIgnoreCase(input.discountType())&&input.discountValue().compareTo(new BigDecimal("100"))>0) || input.minOrderAmount()!=null&&input.minOrderAmount().signum()<0 || input.maxDiscountAmount()!=null&&input.maxDiscountAmount().signum()<0 || input.maxUses()!=null&&input.maxUses()<1 || input.expiresAt()!=null&&!input.expiresAt().isAfter(clock.instant()) || input.status()!=null&&!java.util.Set.of("ACTIVE","INACTIVE").contains(input.status().toUpperCase(java.util.Locale.ROOT)))throw BusinessException.invalid("Thông tin mã giảm giá không hợp lệ.");
        if(input.assignedUserId()!=null && jdbc.queryForObject("select count(*) from users where id=? and role='CUSTOMER' and guest_account=false",Integer.class,input.assignedUserId())!=1)throw BusinessException.missing();
        if (coupons.findByCodeIgnoreCase(code).isPresent()) {
            throw BusinessException.conflict("COUPON_EXISTS", "Mã giảm giá đã tồn tại.");
        }
        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setAssignedUserId(input.assignedUserId());
        coupon.setDescription(input.description());
        coupon.setDiscountType(input.discountType().toUpperCase());
        coupon.setDiscountValue(input.discountValue());
        coupon.setMinOrderAmount(input.minOrderAmount() != null ? input.minOrderAmount() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(input.maxDiscountAmount());
        coupon.setMaxUses(input.maxUses() != null ? input.maxUses() : 100);
        coupon.setUsedCount(0);
        coupon.setExpiresAt(input.expiresAt());
        coupon.setStatus(input.status() != null ? input.status().toUpperCase() : "ACTIVE");
        Instant now = clock.instant();
        coupon.setCreatedAt(now);
        coupon.setUpdatedAt(now);
        return coupons.save(coupon);
    }
}
