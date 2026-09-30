package vn.techshop.order;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface OrderRepository extends JpaRepository<PurchaseOrder, Long>, JpaSpecificationExecutor<PurchaseOrder> {
    java.util.Optional<PurchaseOrder> findByUserIdAndCheckoutKey(Long userId, String key);
    java.util.Optional<PurchaseOrder> findByIdAndUserId(Long id, Long userId);
    org.springframework.data.domain.Page<PurchaseOrder> findByUserId(Long userId, org.springframework.data.domain.Pageable page);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :id")
    java.util.Optional<PurchaseOrder> lockById(@Param("id") Long id);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from PurchaseOrder o where o.id = :id and o.userId = :userId")
    java.util.Optional<PurchaseOrder> lockOwned(@Param("id") Long id, @Param("userId") Long userId);
}
