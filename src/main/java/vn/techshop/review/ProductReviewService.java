package vn.techshop.review;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.catalog.ProductRepository;
import vn.techshop.shared.BusinessException;

@Service
@Transactional(readOnly = true)
public class ProductReviewService {
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate jdbc;
    public java.util.Map<Long,ReviewStats> stats(java.util.List<Long> ids){
        var result=new java.util.HashMap<Long,ReviewStats>();ids.forEach(id->result.put(id,new ReviewStats(0,0,0,0,0,0,0)));if(ids.isEmpty())return result;
        jdbc.query("select product_id,avg(rating) average_rating,count(*) total_reviews,sum(case when rating=5 then 1 else 0 end) five,sum(case when rating=4 then 1 else 0 end) four,sum(case when rating=3 then 1 else 0 end) three,sum(case when rating=2 then 1 else 0 end) two,sum(case when rating=1 then 1 else 0 end) one from product_reviews where status='APPROVED' and product_id in (:ids) group by product_id",java.util.Map.of("ids",ids),(org.springframework.jdbc.core.RowCallbackHandler)r->result.put(r.getLong("product_id"),new ReviewStats(Math.round(r.getDouble("average_rating")*10)/10.0,r.getLong("total_reviews"),r.getLong("five"),r.getLong("four"),r.getLong("three"),r.getLong("two"),r.getLong("one"))));return result;
    }
    public record ReviewView(
        Long id,
        Long productId,
        Long userId,
        String authorName,
        int rating,
        String comment,
        Instant createdAt,
        String status
    ) {}

    public record ReviewStats(
        double averageRating,
        long totalReviews,
        long fiveStarCount,
        long fourStarCount,
        long threeStarCount,
        long twoStarCount,
        long oneStarCount
    ) {}

    public record SubmitInput(
        int rating,
        String comment
    ) {}

    private final ProductReviewRepository reviews;
    private final ProductRepository products;
    private final Clock clock;

    public ProductReviewService(ProductReviewRepository reviews, ProductRepository products, Clock clock) {
        this.reviews = reviews;
        this.products = products;
        this.clock = clock;
    }

    public List<ReviewView> getApprovedReviews(Long productId) {
        return reviews.findByProductIdAndStatusOrderByCreatedAtDesc(productId, "APPROVED")
            .stream()
            .map(this::toView)
            .toList();
    }

    public ReviewStats getReviewStats(Long productId) {
        var list = reviews.findByProductIdAndStatusOrderByCreatedAtDesc(productId, "APPROVED");
        if (list.isEmpty()) {
            return new ReviewStats(0.0, 0, 0, 0, 0, 0, 0);
        }
        long five = 0, four = 0, three = 0, two = 0, one = 0;
        int sum = 0;
        for (var r : list) {
            sum += r.getRating();
            switch (r.getRating()) {
                case 5 -> five++;
                case 4 -> four++;
                case 3 -> three++;
                case 2 -> two++;
                case 1 -> one++;
            }
        }
        double avg = Math.round(((double) sum / list.size()) * 10.0) / 10.0;
        return new ReviewStats(avg, list.size(), five, four, three, two, one);
    }

    @Transactional
    public ReviewView submit(Long userId, String authorName, Long productId, SubmitInput input) {
        if (input.rating() < 1 || input.rating() > 5) {
            throw BusinessException.invalid("Số sao đánh giá phải từ 1 đến 5.");
        }
        if (input.comment() == null || input.comment().trim().isBlank()) {
            throw BusinessException.invalid("Vui lòng nhập nội dung đánh giá.");
        }
        if (input.comment().trim().length() > 1000) {
            throw BusinessException.invalid("Nội dung đánh giá không vượt quá 1000 ký tự.");
        }
        if (!products.existsById(productId)) {
            throw BusinessException.missing();
        }

        ProductReview review = new ProductReview();
        review.setProductId(productId);
        review.setUserId(userId);
        review.setAuthorName(authorName != null && !authorName.isBlank() ? authorName : "Khách hàng");
        review.setRating(input.rating());
        review.setComment(input.comment().trim());
        review.setStatus("PENDING");
        Instant now = clock.instant();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);

        return toView(reviews.save(review));
    }

    public List<ReviewView> getPendingReviews() {
        return reviews.findByStatusOrderByCreatedAtDesc("PENDING").stream().map(this::toView).toList();
    }

    @Transactional
    public void delete(Long reviewId, Long userId, boolean isAdmin) {
        var review = reviews.findById(reviewId)
            .orElseThrow(BusinessException::missing);

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw BusinessException.conflict("FORBIDDEN", "Bạn không có quyền xóa đánh giá này.");
        }

        reviews.delete(review);
    }

    @Transactional
    public ReviewView adminUpdateStatus(Long reviewId, String status) {
        if (!"APPROVED".equalsIgnoreCase(status) && !"REJECTED".equalsIgnoreCase(status) && !"PENDING".equalsIgnoreCase(status)) {
            throw BusinessException.invalid("Trạng thái đánh giá không hợp lệ.");
        }
        var review = reviews.findById(reviewId)
            .orElseThrow(BusinessException::missing);
        review.setStatus(status.toUpperCase());
        review.setUpdatedAt(clock.instant());
        return toView(reviews.save(review));
    }

    private ReviewView toView(ProductReview r) {
        return new ReviewView(
            r.getId(),
            r.getProductId(),
            r.getUserId(),
            r.getAuthorName(),
            r.getRating(),
            r.getComment(),
            r.getCreatedAt(),
            r.getStatus()
        );
    }
}
