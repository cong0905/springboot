package vn.techshop.branch;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface BranchInventoryRepository extends JpaRepository<BranchInventory,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from BranchInventory e where e.id=:id")
 Optional<BranchInventory> lockById(Long id);
}
