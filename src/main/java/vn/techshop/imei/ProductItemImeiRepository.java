package vn.techshop.imei;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ProductItemImeiRepository extends JpaRepository<ProductItemImei,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from ProductItemImei e where e.id=:id")
 Optional<ProductItemImei> lockById(Long id);
}
