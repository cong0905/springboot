package vn.techshop.coupon;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.techshop.shared.BusinessException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CouponServiceTest {
    private CouponRepository repository;
    private Clock clock;
    private CouponService service;
    private Instant now;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(CouponRepository.class);
        now = Instant.parse("2026-10-01T10:00:00Z");
        clock = Clock.fixed(now, ZoneId.of("UTC"));
        service = new CouponService(repository, clock);
    }

    @Test
    void percentDiscountCalculatedCorrectlyWithCap() {
        Coupon coupon = new Coupon();
        coupon.setCode("TECH10");
        coupon.setDiscountType("PERCENT");
        coupon.setDiscountValue(new BigDecimal("10"));
        coupon.setMinOrderAmount(new BigDecimal("200000"));
        coupon.setMaxDiscountAmount(new BigDecimal("50000"));
        coupon.setMaxUses(100);
        coupon.setUsedCount(0);
        coupon.setStatus("ACTIVE");

        when(repository.quoteByCode("TECH10")).thenReturn(Optional.of(CouponQuote.from(coupon)));

        // Subtotal = 1.000.000 ₫ -> 10% is 100.000 ₫, but cap is 50.000 ₫
        var result = service.validateAndCalculate("TECH10", new BigDecimal("1000000"));
        assertThat(result.discountAmount()).isEqualByComparingTo("50000");

        // Subtotal = 300.000 ₫ -> 10% is 30.000 ₫, which is < cap 50.000 ₫
        var result2 = service.validateAndCalculate("TECH10", new BigDecimal("300000"));
        assertThat(result2.discountAmount()).isEqualByComparingTo("30000");
    }

    @Test
    void fixedDiscountCalculatedCorrectly() {
        Coupon coupon = new Coupon();
        coupon.setCode("SALE50K");
        coupon.setDiscountType("FIXED");
        coupon.setDiscountValue(new BigDecimal("50000"));
        coupon.setMinOrderAmount(new BigDecimal("100000"));
        coupon.setMaxUses(100);
        coupon.setUsedCount(0);
        coupon.setStatus("ACTIVE");

        when(repository.quoteByCode("SALE50K")).thenReturn(Optional.of(CouponQuote.from(coupon)));

        var result = service.validateAndCalculate("SALE50K", new BigDecimal("500000"));
        assertThat(result.discountAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void throwsWhenOrderAmountBelowMinimum() {
        Coupon coupon = new Coupon();
        coupon.setCode("MIN200");
        coupon.setDiscountType("FIXED");
        coupon.setDiscountValue(new BigDecimal("20000"));
        coupon.setMinOrderAmount(new BigDecimal("200000"));
        coupon.setMaxUses(100);
        coupon.setUsedCount(0);
        coupon.setStatus("ACTIVE");

        when(repository.quoteByCode("MIN200")).thenReturn(Optional.of(CouponQuote.from(coupon)));

        assertThatThrownBy(() -> service.validateAndCalculate("MIN200", new BigDecimal("150000")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Đơn hàng tối thiểu");
    }

    @Test
    void throwsWhenCouponExpired() {
        Coupon coupon = new Coupon();
        coupon.setCode("OLD");
        coupon.setDiscountType("FIXED");
        coupon.setDiscountValue(new BigDecimal("10000"));
        coupon.setMinOrderAmount(BigDecimal.ZERO);
        coupon.setMaxUses(100);
        coupon.setUsedCount(0);
        coupon.setStatus("ACTIVE");
        coupon.setExpiresAt(now.minusSeconds(3600));

        when(repository.quoteByCode("OLD")).thenReturn(Optional.of(CouponQuote.from(coupon)));

        assertThatThrownBy(() -> service.validateAndCalculate("OLD", new BigDecimal("200000")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("hết hạn");
    }

    @Test
    void throwsWhenMaxUsesReached() {
        Coupon coupon = new Coupon();
        coupon.setCode("FULL");
        coupon.setDiscountType("FIXED");
        coupon.setDiscountValue(new BigDecimal("10000"));
        coupon.setMinOrderAmount(BigDecimal.ZERO);
        coupon.setMaxUses(5);
        coupon.setUsedCount(5);
        coupon.setStatus("ACTIVE");

        when(repository.quoteByCode("FULL")).thenReturn(Optional.of(CouponQuote.from(coupon)));

        assertThatThrownBy(() -> service.validateAndCalculate("FULL", new BigDecimal("200000")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("hết lượt sử dụng");
    }

    @Test
    void redeemIncrementsUsedCount() {
        Coupon coupon = new Coupon();
        coupon.setCode("TECH10");
        coupon.setDiscountType("PERCENT");
        coupon.setDiscountValue(new BigDecimal("10"));
        coupon.setMinOrderAmount(BigDecimal.ZERO);
        coupon.setMaxUses(10);
        coupon.setUsedCount(2);
        coupon.setStatus("ACTIVE");

        when(repository.lockByCodeIgnoreCase("TECH10")).thenReturn(Optional.of(coupon));

        BigDecimal discount = service.redeem("TECH10", new BigDecimal("1000000"));
        assertThat(discount).isEqualByComparingTo("100000");
        assertThat(coupon.getUsedCount()).isEqualTo(3);
        verify(repository).saveAndFlush(coupon);
    }
}
