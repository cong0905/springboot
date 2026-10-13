package vn.techshop.inventory;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    org.springframework.data.domain.Page<StockMovement> findByProductId(Long id, org.springframework.data.domain.Pageable page);
}
