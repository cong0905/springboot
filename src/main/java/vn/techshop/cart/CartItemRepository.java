package vn.techshop.cart;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    java.util.List<CartItem> findByCartIdOrderByProductIdAsc(Long cartId);
    java.util.Optional<CartItem> findByIdAndCartId(Long id, Long cartId);
    java.util.Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    long countByCartId(Long cartId);
    void deleteByCartId(Long cartId);
}
