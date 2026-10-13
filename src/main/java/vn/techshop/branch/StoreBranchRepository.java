package vn.techshop.branch;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface StoreBranchRepository extends JpaRepository<StoreBranch,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from StoreBranch e where e.id=:id")
 Optional<StoreBranch> lockById(Long id);
}
