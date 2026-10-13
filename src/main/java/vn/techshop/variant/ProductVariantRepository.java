package vn.techshop.variant;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ProductVariantRepository extends JpaRepository<ProductVariant,Long> {
 java.util.List<ProductVariant> findByProductIdAndStatusOrderByIdAsc(Long productId,String status);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from ProductVariant e where e.id=:id")
 Optional<ProductVariant> lockById(Long id);
}
