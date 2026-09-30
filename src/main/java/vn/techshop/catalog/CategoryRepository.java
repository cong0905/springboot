package vn.techshop.catalog;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface CategoryRepository extends JpaRepository<Category, Long> {
    java.util.List<Category> findByStatusOrderByNameAsc(String status);
    boolean existsByCode(String code);
}
