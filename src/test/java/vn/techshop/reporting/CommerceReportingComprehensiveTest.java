package vn.techshop.reporting;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.techshop.branch.BranchInventoryService;
import vn.techshop.cart.CartService;
import vn.techshop.catalog.CatalogService;
import vn.techshop.identity.IdentityService;
import vn.techshop.order.OrderService;
import vn.techshop.payment.PaymentService;
import vn.techshop.payment.RefundService;
import vn.techshop.variant.ProductVariantService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.payment.vnpay.tmn-code=TESTSHOP",
    "app.payment.vnpay.hash-secret=test-signature-key-not-real",
    "app.payment.mock.enabled=true"
})
@ActiveProfiles("test")
class CommerceReportingComprehensiveTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired IdentityService identity;
    @Autowired CatalogService catalog;
    @Autowired ProductVariantService variants;
    @Autowired BranchInventoryService branches;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired PaymentService payments;
    @Autowired RefundService refunds;
    @Autowired ReportingService reports;
    @Autowired CommerceReportingService commerceReports;
    @Autowired ReportExportService exportService;

    Long admin, customer, product, variant, branch, category;

    @BeforeEach
    void setUp() {
        clean();
        var key = UUID.randomUUID().toString();
        admin = identity.bootstrapAdmin(new IdentityService.Registration("Admin", key + "a@test.local", "DemoAdmin2026!")).id();
        customer = identity.register(new IdentityService.Registration("Customer", key + "c@test.local", "DemoCustomer2026!")).id();
        category = catalog.createCategory(new CatalogService.CategoryInput("CAT-" + key, "Category", "ACTIVE")).id();
        product = catalog.createProduct(new CatalogService.ProductInput(category, "PROD-" + key, "Test Product", new BigDecimal("15990000"), "ACTIVE", "Desc")).id();
        variant = variants.create(product, new ProductVariantService.Input("VAR-" + key, "Titan", "256GB", new BigDecimal("15990000"), new BigDecimal("16990000"))).id();
        branch = branches.create(new BranchInventoryService.BranchInput("Branch HN", "BHN", "123 Hanoi Street, Hanoi", "HN", "0900000000", 21.0, 105.0)).id();
        branches.receive(admin, new BranchInventoryService.Import(variant, branch, List.of("IMEI-REP-001", "IMEI-REP-002", "IMEI-REP-003")));
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    private void clean() {
        for (String t : List.of("refund_reconciliations", "repair_tickets", "warranty_cards", "point_transactions",
                "member_profiles", "installment_applications", "trade_in_requests", "imei_history",
                "product_items_imei", "branch_inventories", "payment_transactions", "product_reviews",
                "stock_movements", "order_status_history", "order_items", "orders", "cart_items", "carts",
                "product_images", "product_specifications", "product_variants", "products", "categories",
                "users", "store_branches", "trade_in_devices", "trade_in_criterias", "installment_plans", "coupons")) {
            jdbc.update("delete from " + t);
        }
    }

    private OrderService.Checkout checkoutRequest(Long userId, String method) {
        var p = orders.preview(userId, new OrderService.PreviewRequest(null, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null)));
        return new OrderService.Checkout(
            UUID.randomUUID().toString(),
            p.cartVersion(),
            p.total(),
            p.pricingHash(),
            "Nguyen Van Test",
            "0901234567",
            "123 Hanoi Street",
            null,
            null,
            method,
            new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null)
        );
    }

    @Test
    void deliveredOrderFullyRefundedInSamePeriodShowsZeroNetRevenueAcrossReports() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, checkoutRequest(customer, "MOCK")).order();
        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid");

        var paidOrder = orders.detail(order.id(), admin, true);
        var shipped = orders.transition(order.id(), admin, new OrderService.Transition("SHIPPED", paidOrder.version(), false));
        var delivered = orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", shipped.version(), false));

        // Fixed date in range: 2026-10-04
        LocalDate testDay = LocalDate.of(2026, 10, 4);
        jdbc.update("update orders set created_at=?, delivered_at=? where id=?",
            LocalDateTime.parse("2026-10-04T08:00:00"), LocalDateTime.parse("2026-10-04T10:00:00"), order.id());

        // Process full refund on 2026-10-04
        var refundReq = refunds.requestRefund(admin, true, order.id(), new RefundService.RefundRequest(delivered.total(), "Khách trả hàng", "RETURN_GOODS", true));
        refunds.processRefund(admin, refundReq.id(), new RefundService.RefundProcess("COMPLETE", "REF-VNPAY-OP02-01", "Đã chuyển tiền"));
        jdbc.update("update refund_reconciliations set updated_at=? where id=?",
            LocalDateTime.parse("2026-10-04T14:00:00"), refundReq.id());

        // 1. Check ReportingService summary
        var summary = reports.summary(testDay, testDay);
        assertThat(summary.deliveredOrders()).isEqualTo(1);
        assertThat(summary.subtotal()).isEqualByComparingTo(new BigDecimal("15990000"));
        assertThat(summary.discountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(summary.merchandiseRevenue()).isEqualByComparingTo(new BigDecimal("15990000"));
        assertThat(summary.refundedAmount()).isEqualByComparingTo(delivered.total());
        assertThat(summary.netRevenue()).isEqualByComparingTo(BigDecimal.ZERO);

        // 2. Check CommerceReportingService channels
        var channels = commerceReports.channels(testDay, testDay);
        assertThat(channels).hasSize(1);
        var ch = channels.getFirst();
        assertThat(ch.deliveredOrders()).isEqualTo(1);
        assertThat(ch.goodsRevenue()).isEqualByComparingTo(new BigDecimal("15990000"));
        assertThat(ch.refundedAmount()).isEqualByComparingTo(delivered.total());
        assertThat(ch.netRevenue()).isEqualByComparingTo(BigDecimal.ZERO);

        // 3. Check CSV Export
        byte[] csvBytes = exportService.exportOrdersCsv(testDay, testDay);
        String csvContent = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csvContent).contains("Tiền hoàn,Thu ròng");
        // Row should contain total, refunded, and net=0
        assertThat(csvContent).contains(delivered.total().toString() + "," + delivered.total().toString() + ",0");
    }

    @Test
    void salesInPeriod1AndRefundInPeriod2PreservesHistoricalSalesAndRecordsRefundSeparately() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, checkoutRequest(customer, "MOCK")).order();
        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid");

        var paidOrder = orders.detail(order.id(), admin, true);
        var shipped = orders.transition(order.id(), admin, new OrderService.Transition("SHIPPED", paidOrder.version(), false));
        var delivered = orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", shipped.version(), false));

        LocalDate day1 = LocalDate.of(2026, 9, 15);
        LocalDate day2 = LocalDate.of(2026, 10, 4);

        // Delivered on day 1
        jdbc.update("update orders set created_at=?, delivered_at=? where id=?",
            LocalDateTime.parse("2026-09-15T08:00:00"), LocalDateTime.parse("2026-09-15T10:00:00"), order.id());

        // Refunded on day 2
        var refundReq = refunds.requestRefund(admin, true, order.id(), new RefundService.RefundRequest(delivered.total(), "Khách trả hàng tháng sau", "RETURN_GOODS", true));
        refunds.processRefund(admin, refundReq.id(), new RefundService.RefundProcess("COMPLETE", "REF-VNPAY-OP02-02", "Đã hoàn"));
        jdbc.update("update refund_reconciliations set updated_at=? where id=?",
            LocalDateTime.parse("2026-10-04T11:00:00"), refundReq.id());

        // Period 1 report: sales preserved, 0 refund
        var report1 = reports.summary(day1, day1);
        assertThat(report1.deliveredOrders()).isEqualTo(1);
        assertThat(report1.merchandiseRevenue()).isEqualByComparingTo(new BigDecimal("15990000"));
        assertThat(report1.refundedAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report1.netRevenue()).isEqualByComparingTo(delivered.total());

        // Period 2 report: 0 sales, refund recorded
        var report2 = reports.summary(day2, day2);
        assertThat(report2.deliveredOrders()).isZero();
        assertThat(report2.merchandiseRevenue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(report2.refundedAmount()).isEqualByComparingTo(delivered.total());
        assertThat(report2.netRevenue()).isEqualByComparingTo(delivered.total().negate());

        // Channel report on day 2 also reflects the refund
        var ch2 = commerceReports.channels(day2, day2);
        assertThat(ch2).isNotEmpty();
        assertThat(ch2.getFirst().refundedAmount()).isEqualByComparingTo(delivered.total());
        assertThat(ch2.getFirst().netRevenue()).isEqualByComparingTo(delivered.total().negate());
    }
}
