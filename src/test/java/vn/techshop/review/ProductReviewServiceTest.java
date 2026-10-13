package vn.techshop.review;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import vn.techshop.catalog.ProductRepository;
import vn.techshop.shared.BusinessException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductReviewServiceTest {
    private ProductReviewRepository reviews;
    private ProductRepository products;
    private Clock clock;
    private ProductReviewService service;

    @BeforeEach
    void setUp() {
        reviews = Mockito.mock(ProductReviewRepository.class);
        products = Mockito.mock(ProductRepository.class);
        clock = Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneId.of("UTC"));
        service = new ProductReviewService(reviews, products, clock);
    }

    @Test
    void submitValidReviewSavesAndReturns() {
        when(products.existsById(1L)).thenReturn(true);
        when(reviews.save(any(ProductReview.class))).thenAnswer(inv -> {
            ProductReview r = inv.getArgument(0);
            return r;
        });

        var result = service.submit(10L, "Nguyễn Văn A", 1L, new ProductReviewService.SubmitInput(5, "Sản phẩm tuyệt vời!"));
        assertThat(result.rating()).isEqualTo(5);
        assertThat(result.comment()).isEqualTo("Sản phẩm tuyệt vời!");
        assertThat(result.authorName()).isEqualTo("Nguyễn Văn A");
        assertThat(result.status()).isEqualTo("PENDING");
    }

    @Test
    void reviewModerationWorkflowRequiresApprovalBeforeDisplay() {
        when(products.existsById(1L)).thenReturn(true);
        ProductReview created = new ProductReview();
        created.setId(99L);
        created.setProductId(1L);
        created.setUserId(10L);
        created.setAuthorName("Khách Hàng");
        created.setRating(5);
        created.setStatus("PENDING");
        created.setComment("Đánh giá chờ duyệt");
        created.setCreatedAt(Instant.now());
        when(reviews.findById(99L)).thenReturn(Optional.of(created));
        when(reviews.save(any(ProductReview.class))).thenAnswer(inv -> inv.getArgument(0));

        // Ban đầu review ở trạng thái PENDING
        when(reviews.findByStatusOrderByCreatedAtDesc("PENDING")).thenReturn(List.of(created));
        var pendingList = service.getPendingReviews();
        assertThat(pendingList).hasSize(1);
        assertThat(pendingList.get(0).status()).isEqualTo("PENDING");

        // Admin phê duyệt đánh giá
        var approved = service.adminUpdateStatus(99L, "APPROVED");
        assertThat(approved.status()).isEqualTo("APPROVED");

        // Khi duyệt xong, storefront mới hiển thị
        when(reviews.findByProductIdAndStatusOrderByCreatedAtDesc(1L, "APPROVED")).thenReturn(List.of(created));
        var publicReviews = service.getApprovedReviews(1L);
        assertThat(publicReviews).hasSize(1);
        assertThat(publicReviews.get(0).status()).isEqualTo("APPROVED");
    }

    @Test
    void submitInvalidRatingThrowsException() {
        assertThatThrownBy(() -> service.submit(10L, "A", 1L, new ProductReviewService.SubmitInput(0, "Tệ")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Số sao");

        assertThatThrownBy(() -> service.submit(10L, "A", 1L, new ProductReviewService.SubmitInput(6, "Tốt")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Số sao");
    }

    @Test
    void submitEmptyCommentThrowsException() {
        assertThatThrownBy(() -> service.submit(10L, "A", 1L, new ProductReviewService.SubmitInput(5, "   ")))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Vui lòng nhập");
    }

    @Test
    void calculateStatsCorrectly() {
        ProductReview r1 = new ProductReview();
        r1.setRating(5);
        ProductReview r2 = new ProductReview();
        r2.setRating(5);
        ProductReview r3 = new ProductReview();
        r3.setRating(4);

        when(reviews.findByProductIdAndStatusOrderByCreatedAtDesc(1L, "APPROVED"))
            .thenReturn(List.of(r1, r2, r3));

        var stats = service.getReviewStats(1L);
        assertThat(stats.totalReviews()).isEqualTo(3);
        assertThat(stats.averageRating()).isEqualTo(4.7);
        assertThat(stats.fiveStarCount()).isEqualTo(2);
        assertThat(stats.fourStarCount()).isEqualTo(1);
    }

    @Test
    void authorOrAdminCanDeleteReview() {
        ProductReview review = new ProductReview();
        review.setUserId(10L);
        when(reviews.findById(100L)).thenReturn(Optional.of(review));

        // Author can delete
        service.delete(100L, 10L, false);
        verify(reviews).delete(review);

        // Admin can delete any review
        service.delete(100L, 999L, true);
        verify(reviews, times(2)).delete(review);

        // Non-admin stranger cannot delete
        assertThatThrownBy(() -> service.delete(100L, 20L, false))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("không có quyền");
    }
}
