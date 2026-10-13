package vn.techshop.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.UUID;
import vn.techshop.catalog.*;
import vn.techshop.cart.*;
import vn.techshop.identity.*;
import vn.techshop.inventory.InventoryService;
import vn.techshop.order.*;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderStatusAndRefundUxTest {
    @Autowired MockMvc mvc;
    @Autowired IdentityService identity;
    @Autowired CatalogService catalog;
    @Autowired InventoryService inventory;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired RefundService refunds;
    @Autowired UserRepository users;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

    private TechPrincipal principal(Long userId) {
        return new TechPrincipal(users.findById(userId).orElseThrow());
    }

    @Test
    void customerAndGuestRefundRequestAndStatusTransparency() throws Exception {
        String key = UUID.randomUUID().toString();
        var admin = identity.bootstrapAdmin(new IdentityService.Registration("Admin " + key, key + "@test.local", "AdminPass123!"));
        var customer = identity.register(new IdentityService.Registration("Customer " + key, "c-" + key + "@test.local", "CustPass123!"));
        var otherCust = identity.register(new IdentityService.Registration("Other " + key, "other-" + key + "@test.local", "OtherPass123!"));

        var cat = catalog.createCategory(new CatalogService.CategoryInput(key, "Category " + key, "ACTIVE"));
        var prod = catalog.createProduct(new CatalogService.ProductInput(cat.id(), key + "-p", "Smart Watch " + key, new BigDecimal("2000000"), "ACTIVE", null));
        inventory.adjust(prod.id(), admin.id(), new InventoryService.Adjustment(20, "Initial stock", prod.version()));

        // ==========================================
        // SCENARIO 1: ĐƠN CHƯA GIAO (PENDING) HOÀN ĐỦ TIỀN VÀ HỦY ĐƠN
        // ==========================================
        var cartView = carts.view(customer.id());
        carts.add(customer.id(), new CartService.Add(prod.id(), 1, cartView.version()));
        var preview = orders.preview(customer.id(), new OrderService.PreviewRequest(null, null));
        var checkout = new OrderService.Checkout(UUID.randomUUID().toString(), preview.cartVersion(), preview.total(), preview.pricingHash(), "Nguyen Van A", "0901234567", "123 Le Loi, Q1, HCMC", null, null, "COD", null);
        var orderResult = orders.checkout(customer.id(), checkout, "127.0.0.1");
        Long orderId = orderResult.order().id();

        // Giả lập thanh toán online thành công
        jdbc.update("update orders set payment_status='PAID', payment_method='VNPAY' where id=?", orderId);

        // Kiểm tra detail trước khi hoàn tiền: paymentStatus là PAID, refundedAmount là 0
        var detailBefore = orders.detail(orderId, customer.id(), false);
        assertThat(detailBefore.paymentStatus()).isEqualTo("PAID");
        assertThat(detailBefore.refundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // 1. Khách khác cố tình gửi yêu cầu hoàn tiền cho đơn này -> Phải bị từ chối (404 missing)
        mvc.perform(post("/api/v1/orders/" + orderId + "/refund")
                .with(user(principal(otherCust.id())))
                .with(csrf())
                .contentType("application/json")
                .content("{\"amount\":2000000,\"reason\":\"Khách khác yêu cầu hoàn tiền\",\"refundType\":\"ORDER_CANCELLATION\",\"restockItems\":false}"))
                .andExpect(status().isNotFound());

        // 2. Chính chủ gửi yêu cầu hoàn tiền toàn bộ (2,000,000đ)
        mvc.perform(post("/api/v1/orders/" + orderId + "/refund")
                .with(user(principal(customer.id())))
                .with(csrf())
                .contentType("application/json")
                .content("{\"amount\":2000000,\"reason\":\"Khách hủy đơn chưa giao\",\"refundType\":\"ORDER_CANCELLATION\",\"restockItems\":false}"))
                .andExpect(status().isOk());

        // Admin phê duyệt hoàn tiền
        Long refund1Id = jdbc.queryForObject("select id from refund_reconciliations where order_id=? and amount=2000000", Long.class, orderId);
        refunds.processRefund(admin.id(), refund1Id, new RefundService.RefundProcess("APPROVE", "REF-TX-001", "Duyet hoan toan bo", false));

        // Kiểm tra detail sau khi hoàn đủ tiền: status REFUNDED, refundedAmount = 2000000
        var detailFull = orders.detail(orderId, customer.id(), false);
        assertThat(detailFull.paymentStatus()).isEqualTo("REFUNDED");
        assertThat(detailFull.refundedAmount()).isEqualByComparingTo(new BigDecimal("2000000"));

        // Tra cứu đơn (lookup) cũng hiển thị rõ trạng thái REFUNDED
        var lookupFull = orders.lookup("TS-" + String.format(java.util.Locale.ROOT, "%08d", orderId), "0901234567").orElseThrow();
        assertThat(lookupFull.paymentStatus()).isEqualTo("REFUNDED");
        assertThat(lookupFull.refundedAmount()).isEqualByComparingTo(new BigDecimal("2000000"));

        // ==========================================
        // SCENARIO 2: ĐƠN ĐÃ GIAO (DELIVERED) HOÀN TIỀN MỘT PHẦN (PRICE_ADJUSTMENT)
        // ==========================================
        var cartView2 = carts.view(customer.id());
        carts.add(customer.id(), new CartService.Add(prod.id(), 1, cartView2.version()));
        var preview2 = orders.preview(customer.id(), new OrderService.PreviewRequest(null, null));
        var checkout2 = new OrderService.Checkout(UUID.randomUUID().toString(), preview2.cartVersion(), preview2.total(), preview2.pricingHash(), "Nguyen Van B", "0909999888", "456 Tran Hung Dao, HCMC", null, null, "COD", null);
        var orderResult2 = orders.checkout(customer.id(), checkout2, "127.0.0.1");
        Long order2Id = orderResult2.order().id();

        // Đơn đã thanh toán và đã giao hàng thành công
        jdbc.update("update orders set payment_status='PAID', payment_method='VNPAY', status='DELIVERED', delivered_at=?, cod_collected=false where id=?", java.sql.Timestamp.from(java.time.Instant.now()), order2Id);

        // Admin hoàn tiền một phần (500,000đ do giảm giá bù hoặc điều chỉnh giá)
        var refundPartialReq = refunds.requestRefund(admin.id(), true, order2Id, new RefundService.RefundRequest(new BigDecimal("500000"), "Giam gia bu tru sau mua", "PRICE_ADJUSTMENT", false));
        refunds.processRefund(admin.id(), refundPartialReq.id(), new RefundService.RefundProcess("APPROVE", "REF-TX-002", "Duyet hoan 500k", false));

        // Kiểm tra detail đơn 2: status PAID, refundedAmount = 500000
        var detailPartial = orders.detail(order2Id, customer.id(), false);
        assertThat(detailPartial.paymentStatus()).isEqualTo("PAID");
        assertThat(detailPartial.refundedAmount()).isEqualByComparingTo(new BigDecimal("500000"));

        // Tra cứu đơn 2: status PAID, refundedAmount = 500000
        var lookupPartial = orders.lookup("TS-" + String.format(java.util.Locale.ROOT, "%08d", order2Id), "0909999888").orElseThrow();
        assertThat(lookupPartial.paymentStatus()).isEqualTo("PAID");
        assertThat(lookupPartial.refundedAmount()).isEqualByComparingTo(new BigDecimal("500000"));
    }
}
