package vn.techshop.notification;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;
import vn.techshop.catalog.*;
import vn.techshop.identity.*;
import vn.techshop.review.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class EmailOutboxAndReviewModerationTest {
    @Autowired EmailOutboxService outboxService;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductReviewService reviewService;
    @Autowired CatalogService catalog;
    @Autowired IdentityService identity;

    @Test
    void emailOutboxEnqueuesAndRetainsThroughRestarts() {
        String testEmail = "customer-" + UUID.randomUUID() + "@test.local";
        String subject = "Khôi phục mật khẩu";
        String body = "Mã xác thực là SECRET-123456";

        Long outboxId = outboxService.enqueue(testEmail, subject, body);
        assertThat(outboxId).isNotNull();

        // Kiểm tra dữ liệu được lưu bền vững trong bảng email_outbox
        var row = jdbc.queryForMap("select * from email_outbox where id = ?", outboxId);
        assertThat(row.get("recipient")).isEqualTo(testEmail);
        assertThat(row.get("subject")).isEqualTo(subject);
        assertThat(row.get("body")).isEqualTo(body);
        assertThat(row.get("status")).isEqualTo("PENDING");
        assertThat((Integer) row.get("retry_count")).isEqualTo(0);
        assertThat((Integer) row.get("max_retries")).isEqualTo(3);
        assertThat(row.get("next_retry_at")).isNotNull();

        // Giả lập xử lý thất bại 1 lần (ví dụ mạng gián đoạn trước restart)
        jdbc.update("update email_outbox set retry_count = 1, next_retry_at = CURRENT_TIMESTAMP where id = ?", outboxId);

        // Bản ghi vẫn tồn tại với trạng thái PENDING chờ retry
        var pendingList = outboxService.getPending();
        assertThat(pendingList).anyMatch(item -> item.id().equals(outboxId));

        // Giả lập vượt quá max_retries
        jdbc.update("update email_outbox set retry_count = 3, status = 'FAILED', error_message = 'Connection refused' where id = ?", outboxId);
        var failedRow = jdbc.queryForMap("select status, error_message from email_outbox where id = ?", outboxId);
        assertThat(failedRow.get("status")).isEqualTo("FAILED");
        assertThat((String) failedRow.get("error_message")).contains("Connection refused");
        // Đảm bảo không log lộ token/bí mật
        assertThat((String) failedRow.get("error_message")).doesNotContain("SECRET-123456");
    }

    @Test
    void productReviewRequiresModerationQueue() {
        String key = UUID.randomUUID().toString();
        var cat = catalog.createCategory(new CatalogService.CategoryInput(key, "Category " + key, "ACTIVE"));
        var prod = catalog.createProduct(new CatalogService.ProductInput(cat.id(), key + "-p", "Product " + key, new java.math.BigDecimal("100000"), "ACTIVE", null));
        var user = identity.register(new IdentityService.Registration("Reviewer " + key, "rev-" + key + "@test.local", "StrongPass1234!"));

        // Khách hàng gửi đánh giá
        var submitted = reviewService.submit(user.id(), "Khách Hàng " + key, prod.id(), new ProductReviewService.SubmitInput(5, "Chất lượng rất tốt!"));
        
        // 1. Phải ở trạng thái PENDING chờ kiểm duyệt, KHÔNG được tự động APPROVED
        assertThat(submitted.status()).isEqualTo("PENDING");

        // 2. Trang sản phẩm công khai CHƯA ĐƯỢC hiển thị đánh giá này
        var publicReviewsBefore = reviewService.getApprovedReviews(prod.id());
        assertThat(publicReviewsBefore).noneMatch(r -> r.id().equals(submitted.id()));

        // 3. Đánh giá nằm trong hàng chờ duyệt của Admin
        var pendingQueue = reviewService.getPendingReviews();
        assertThat(pendingQueue).anyMatch(r -> r.id().equals(submitted.id()));

        // 4. Admin phê duyệt đánh giá
        var approved = reviewService.adminUpdateStatus(submitted.id(), "APPROVED");
        assertThat(approved.status()).isEqualTo("APPROVED");

        // 5. Sau khi duyệt, storefront hiển thị đánh giá này
        var publicReviewsAfter = reviewService.getApprovedReviews(prod.id());
        assertThat(publicReviewsAfter).anyMatch(r -> r.id().equals(submitted.id()));
    }
}
