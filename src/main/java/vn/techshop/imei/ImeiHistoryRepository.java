package vn.techshop.imei;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ImeiHistoryRepository extends JpaRepository<ImeiHistory,Long> {
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from ImeiHistory e where e.id=:id")
 Optional<ImeiHistory> lockById(Long id);
}
