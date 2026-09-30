package vn.techshop.order;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    java.util.List<OrderHistory> findByOrderIdOrderByOrderVersionAsc(Long orderId);
}
