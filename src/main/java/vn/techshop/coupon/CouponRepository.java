package vn.techshop.coupon;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    @Query("select new vn.techshop.coupon.CouponQuote(c.code,c.description,c.discountType,c.discountValue,c.minOrderAmount,c.maxDiscountAmount,c.maxUses,c.usedCount,c.expiresAt,c.status,c.assignedUserId) from Coupon c where lower(c.code)=lower(:code)")
    Optional<CouponQuote> quoteByCode(@Param("code") String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE LOWER(c.code) = LOWER(:code)")
    Optional<Coupon> lockByCodeIgnoreCase(@Param("code") String code);

    boolean existsByCode(String code);

    List<Coupon> findAllByOrderByCreatedAtDesc();
    List<Coupon> findByAssignedUserIdOrderByCreatedAtDesc(Long assignedUserId);
}
