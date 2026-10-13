package vn.techshop.warranty;

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
import vn.techshop.variant.ProductVariantService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "app.payment.vnpay.tmn-code=TESTSHOP",
    "app.payment.vnpay.hash-secret=test-signature-key-not-real",
    "app.payment.mock.enabled=true"
})
@ActiveProfiles("test")
class WarrantyExchangeAndHistoryTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired IdentityService identity;
    @Autowired CatalogService catalog;
    @Autowired ProductVariantService variants;
    @Autowired BranchInventoryService branches;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired PaymentService payments;
    @Autowired WarrantyService warranty;
    @Autowired WarrantyExchangeService exchangeService;

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
        branches.receive(admin, new BranchInventoryService.Import(variant, branch, List.of("IMEI-ORIG-001", "IMEI-SWAP-002")));
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    private void clean() {
        for (String t : List.of("warranty_exchanges", "refund_reconciliations", "repair_tickets", "warranty_cards", "point_transactions",
                "member_profiles", "installment_applications", "trade_in_requests", "imei_history",
                "product_items_imei", "branch_inventories", "payment_transactions", "product_reviews",
                "stock_movements", "order_status_history", "order_items", "orders", "cart_items", "carts",
                "product_images", "product_specifications", "product_variants", "products", "categories",
                "users", "store_branches", "trade_in_devices", "trade_in_criterias", "installment_plans", "coupons")) {
            jdbc.update("delete from " + t);
        }
    }

    private OrderService.Checkout request(Long userId) {
        var p = orders.preview(userId, new OrderService.PreviewRequest(null, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null)));
        return new OrderService.Checkout(
            UUID.randomUUID().toString(),
            p.cartVersion(),
            p.total(),
            p.pricingHash(),
            "Customer Test",
            "0900000000",
            "123 Hanoi Street",
            null,
            null,
            "MOCK",
            new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null)
        );
    }

    @Test
    void exchangeRetainsOriginalSerialOnOldRepairTicketAndRecordsExchangeEvent() {
        // 1. Customer buys device with IMEI-ORIG-001
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, request(customer)).order();
        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid");

        var paid = orders.detail(order.id(), admin, true);
        var shipped = orders.transition(order.id(), admin, new OrderService.Transition("SHIPPED", paid.version(), false));
        orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", shipped.version(), false));

        // Warranty card activated
        var cards = warranty.owned(customer);
        assertThat(cards).hasSize(1);
        var card = cards.getFirst();
        assertThat(card.imeiNumber()).isEqualTo("IMEI-ORIG-001");

        // 2. Customer brings original device in for repair
        warranty.receive(card.id(), "Lỗi micro cuộc gọi");
        var ticketsBefore = warranty.tickets();
        assertThat(ticketsBefore).hasSize(1);
        assertThat(ticketsBefore.getFirst().imeiNumber()).isEqualTo("IMEI-ORIG-001");

        // 3. Perform device exchange to replacement serial IMEI-SWAP-002
        var result = exchangeService.exchange(card.id(), "IMEI-SWAP-002", admin);
        assertThat(result.previousSerial()).isEqualTo("IMEI-ORIG-001");
        assertThat(result.replacementSerial()).isEqualTo("IMEI-SWAP-002");

        // 4. Verification: Old repair ticket MUST still show IMEI-ORIG-001! (OP-03)
        var ticketsAfter = warranty.tickets();
        assertThat(ticketsAfter).hasSize(1);
        assertThat(ticketsAfter.getFirst().imeiNumber()).isEqualTo("IMEI-ORIG-001");

        // 5. Verification: Warranty exchange event is cleanly recorded
        var exchanges = warranty.exchanges(card.id());
        assertThat(exchanges).hasSize(1);
        var ex = exchanges.getFirst();
        assertThat(ex.oldImei()).isEqualTo("IMEI-ORIG-001");
        assertThat(ex.newImei()).isEqualTo("IMEI-SWAP-002");
        assertThat(ex.actorId()).isEqualTo(admin);
        assertThat(ex.occurredAt()).isNotNull();

        // 6. New repair ticket created on the warranty card now uses IMEI-SWAP-002
        warranty.receive(card.id(), "Dán lại màn hình");
        var updatedTickets = warranty.tickets();
        assertThat(updatedTickets).hasSize(2);
        // Latest ticket has new IMEI
        assertThat(updatedTickets.getFirst().imeiNumber()).isEqualTo("IMEI-SWAP-002");
        // Old ticket still retains old IMEI
        assertThat(updatedTickets.get(1).imeiNumber()).isEqualTo("IMEI-ORIG-001");
    }
}
