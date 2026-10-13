package vn.techshop.cart;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface CartRepository extends JpaRepository<Cart, Long> {
    java.util.Optional<Cart> findByUserId(Long userId);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Cart c where c.userId = :userId")
    java.util.Optional<Cart> lockByUserId(@Param("userId") Long userId);
}
