package vn.techshop.variant;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ProductSpecificationRepository extends JpaRepository<ProductSpecification,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from ProductSpecification e where e.id=:id")
 Optional<ProductSpecification> lockById(Long id);
}
