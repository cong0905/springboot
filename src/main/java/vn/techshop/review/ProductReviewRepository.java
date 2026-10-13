package vn.techshop.review;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, String status);

    List<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId);

    Optional<ProductReview> findByIdAndUserId(Long id, Long userId);

    List<ProductReview> findByStatusOrderByCreatedAtDesc(String status);

    List<ProductReview> findAllByOrderByCreatedAtDesc();

    long countByProductIdAndStatus(Long productId, String status);
}
