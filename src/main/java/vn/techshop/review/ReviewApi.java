package vn.techshop.review;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.techshop.identity.TechPrincipal;

@RestController
@RequestMapping("/api/v1")
public class ReviewApi {
    public record ReviewSummaryResponse(
        ProductReviewService.ReviewStats stats,
        List<ProductReviewService.ReviewView> reviews
    ) {}

    private final ProductReviewService reviews;

    public ReviewApi(ProductReviewService reviews) {
        this.reviews = reviews;
    }

    @GetMapping("/products/{id}/reviews")
    public ReviewSummaryResponse list(@PathVariable Long id) {
        return new ReviewSummaryResponse(
            reviews.getReviewStats(id),
            reviews.getApprovedReviews(id)
        );
    }

    @PostMapping("/products/{id}/reviews")
    public ProductReviewService.ReviewView submit(
        @PathVariable Long id,
        @AuthenticationPrincipal TechPrincipal user,
        @Valid @RequestBody ProductReviewService.SubmitInput input
    ) {
        return reviews.submit(user.id(), user.fullName(), id, input);
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id,
        @AuthenticationPrincipal TechPrincipal user
    ) {
        boolean isAdmin = "ADMIN".equals(user.role());
        reviews.delete(id, user.id(), isAdmin);
        return ResponseEntity.noContent().build();
    }

    public record ReviewStatusInput(String status) {}

    @GetMapping("/admin/reviews/pending")
    public List<ProductReviewService.ReviewView> pendingReviews() {
        return reviews.getPendingReviews();
    }

    @PatchMapping("/admin/reviews/{id}/status")
    public ProductReviewService.ReviewView updateReviewStatus(
        @PathVariable Long id,
        @RequestBody ReviewStatusInput in
    ) {
        return reviews.adminUpdateStatus(id, in.status());
    }
}
