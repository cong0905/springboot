package vn.techshop.catalog;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    java.util.List<ProductImage> findByProductIdOrderBySortOrderAscIdAsc(Long productId);
    void deleteByProductId(Long productId);
}
