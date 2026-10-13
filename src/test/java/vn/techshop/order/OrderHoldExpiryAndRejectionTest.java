package vn.techshop.order;

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
import vn.techshop.catalog.ProductRepository;
import vn.techshop.identity.IdentityService;
import vn.techshop.installment.InstallmentService;
import vn.techshop.payment.PaymentService;
import vn.techshop.shared.BusinessException;
import vn.techshop.variant.ProductVariantService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "app.payment.vnpay.tmn-code=TESTSHOP",
    "app.payment.vnpay.hash-secret=test-signature-key-not-real",
    "app.payment.mock.enabled=true",
    "app.pickup.expiry-interval-ms=3600000"
})
@ActiveProfiles("test")
class OrderHoldExpiryAndRejectionTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired IdentityService identity;
    @Autowired CatalogService catalog;
    @Autowired ProductRepository products;
    @Autowired ProductVariantService variants;
    @Autowired BranchInventoryService branches;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired PickupService pickup;
    @Autowired InstallmentService installments;
    @Autowired PaymentService payments;

    Long admin, customer, product, variant, branch, category;

    @BeforeEach
    void setUp() {
        clean();
        var key = UUID.randomUUID().toString();
        admin = identity.bootstrapAdmin(new IdentityService.Registration("Admin", key + "a@test.local", "DemoAdmin2026!")).id();
        customer = identity.register(new IdentityService.Registration("Customer", key + "c@test.local", "DemoCustomer2026!")).id();
        category = catalog.createCategory(new CatalogService.CategoryInput("CAT-" + key, "Category", "ACTIVE")).id();
        product = catalog.createProduct(new CatalogService.ProductInput(category, "PROD-" + key, "Test Product", new BigDecimal("10000000"), "ACTIVE", "Desc")).id();
        variant = variants.create(product, new ProductVariantService.Input("VAR-" + key, "Black", "128GB", new BigDecimal("10000000"), new BigDecimal("11000000"))).id();
        branch = branches.create(new BranchInventoryService.BranchInput("Branch 1", "B1", "123 Hanoi Street, Hanoi", "HN", "0900000000", 21.0, 105.0)).id();
        branches.receive(admin, new BranchInventoryService.Import(variant, branch, List.of("IMEI-EXP-001", "IMEI-EXP-002")));
        jdbc.update("insert into installment_plans(provider,months,fee_percent) values('BankTest',3,0)");
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    private void clean() {
        for (String t : List.of("refund_reconciliations", "repair_tickets", "warranty_cards", "point_transactions", "member_profiles",
                "installment_applications", "trade_in_requests", "imei_history", "product_items_imei",
                "branch_inventories", "payment_transactions", "product_reviews", "stock_movements",
                "order_status_history", "order_items", "orders", "cart_items", "carts", "product_images",
                "product_specifications", "product_variants", "products", "categories", "users",
                "store_branches", "trade_in_devices", "trade_in_criterias", "installment_plans", "coupons")) {
            jdbc.update("delete from " + t);
        }
    }

    private OrderService.Checkout request(Long userId, OrderService.Options opts, String method) {
        var p = orders.preview(userId, new OrderService.PreviewRequest(null, opts));
        return new OrderService.Checkout(
            UUID.randomUUID().toString(),
            p.cartVersion(),
            p.total(),
            p.pricingHash(),
            "Recipient",
            "0900000000",
            "Address 123",
            null,
            null,
            method,
            opts
        );
    }

    @Test
    void onlineDeliveryUnpaidOrderExpiresAndReleasesInventoryAndSerial() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, request(customer, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null), "MOCK")).order();

        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).hasSize(1);

        // Fast-forward hold expiry to the past
        jdbc.update("update orders set hold_expires_at=? where id=?", java.time.LocalDateTime.ofInstant(Instant.now().minusSeconds(10), java.time.ZoneOffset.UTC), order.id());

        // Run expiry job
        pickup.expire();

        // Order is cancelled and stock/serial restored
        var cancelled = orders.detail(order.id(), customer, false);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("IN_STOCK"))).hasSize(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).isEmpty();

        // Late payment callback on cancelled order throws conflict
        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        assertThatThrownBy(() -> payments.completePayment(code, "00", "MOCK", "Late payment arrived"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void installmentRejectionReleasesInventoryAndSerial() {
        Long planId = installments.plans().getFirst().id();
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var options = new OrderService.Options("CLICK_AND_COLLECT", branch, null, null, null, null, planId);
        var order = orders.checkout(customer, request(customer, options, "INSTALLMENT")).order();

        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).hasSize(1);

        // Decision: REJECTED
        installments.decision(order.id(), "REJECTED");

        // Order should be automatically CANCELLED and reservation released
        var cancelled = orders.detail(order.id(), customer, false);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("IN_STOCK"))).hasSize(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).isEmpty();
        assertThat(cancelled.history().getLast().reason()).contains("Hồ sơ trả góp bị từ chối");
    }

    @Test
    void installmentOrderExpiresAfterHoldDeadline() {
        Long planId = installments.plans().getFirst().id();
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var options = new OrderService.Options("CLICK_AND_COLLECT", branch, null, null, null, null, planId);
        var order = orders.checkout(customer, request(customer, options, "INSTALLMENT")).order();

        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).hasSize(1);

        // Fast-forward hold expiry
        jdbc.update("update orders set hold_expires_at=? where id=?", java.time.LocalDateTime.ofInstant(Instant.now().minusSeconds(10), java.time.ZoneOffset.UTC), order.id());

        // Run expiry job
        pickup.expire();

        var cancelled = orders.detail(order.id(), customer, false);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("IN_STOCK"))).hasSize(2);
        assertThat(cancelled.history().getLast().reason()).contains("Hết hạn chờ duyệt trả góp");
    }

    @Test
    void codDeliveryOrderExpiresWhenPendingAndReleasesInventoryAndSerial() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var options = new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null);
        var order = orders.checkout(customer, request(customer, options, "COD")).order();

        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).hasSize(1);

        // Fast-forward hold expiry to past
        jdbc.update("update orders set hold_expires_at=? where id=?", java.time.LocalDateTime.ofInstant(Instant.now().minusSeconds(10), java.time.ZoneOffset.UTC), order.id());

        // Run expiry job
        pickup.expire();

        var cancelled = orders.detail(order.id(), customer, false);
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("IN_STOCK"))).hasSize(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).isEmpty();
        assertThat(cancelled.history().getLast().reason()).contains("Hết hạn xác nhận đơn COD sau 24 giờ");
    }

    @Test
    void codDeliveryOrderConfirmedClearsHoldExpiryAndDoesNotExpire() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var options = new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null);
        var order = orders.checkout(customer, request(customer, options, "COD")).order();

        // Admin confirms COD order
        orders.transition(order.id(), admin, new OrderService.Transition("CONFIRMED", order.version(), false));
        var confirmed = orders.detail(order.id(), customer, false);
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");

        // Database hold_expires_at should be null now
        var holdExpiresAt = jdbc.queryForList("select hold_expires_at from orders where id=?", java.sql.Timestamp.class, order.id());
        assertThat(holdExpiresAt.getFirst()).isNull();

        // Run expiry job
        pickup.expire();

        // Order remains CONFIRMED, inventory remains reserved
        var stillConfirmed = orders.detail(order.id(), customer, false);
        assertThat(stillConfirmed.status()).isEqualTo("CONFIRMED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).hasSize(1);
    }
}
