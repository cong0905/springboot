package vn.techshop.payment;

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
import vn.techshop.loyalty.LoyaltyService;
import vn.techshop.order.OrderService;
import vn.techshop.shared.BusinessException;
import vn.techshop.variant.ProductVariantService;
import vn.techshop.warranty.WarrantyService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "app.payment.vnpay.tmn-code=TESTSHOP",
    "app.payment.vnpay.hash-secret=test-signature-key-not-real",
    "app.payment.mock.enabled=true"
})
@ActiveProfiles("test")
class RefundAndReconciliationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired IdentityService identity;
    @Autowired CatalogService catalog;
    @Autowired ProductRepository products;
    @Autowired ProductVariantService variants;
    @Autowired BranchInventoryService branches;
    @Autowired CartService carts;
    @Autowired OrderService orders;
    @Autowired PaymentService payments;
    @Autowired RefundService refunds;
    @Autowired LoyaltyService loyalty;
    @Autowired WarrantyService warranty;

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
        branches.receive(admin, new BranchInventoryService.Import(variant, branch, List.of("IMEI-REF-001", "IMEI-REF-002")));
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

    private OrderService.Checkout request(Long userId, OrderService.Options opts, String method) {
        var p = orders.preview(userId, new OrderService.PreviewRequest(null, opts));
        return new OrderService.Checkout(
            UUID.randomUUID().toString(),
            p.cartVersion(),
            p.total(),
            p.pricingHash(),
            "Recipient",
            "0900000000",
            "123 Hanoi Street, Hanoi",
            null,
            null,
            method,
            opts
        );
    }

    @Test
    void requestAndApproveRefundForPaidOrderCancelsAndRestocks() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, request(customer, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null), "MOCK")).order();

        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid");

        var paidOrder = orders.detail(order.id(), customer, false);
        assertThat(paidOrder.paymentStatus()).isEqualTo("PAID");
        assertThat(paidOrder.status()).isEqualTo("CONFIRMED");
        long historyBefore=jdbc.queryForObject("select count(*) from order_status_history where order_id=?",Long.class,order.id());
        payments.completePayment(code,"00","MOCK","Retry success");
        payments.completePayment(code,"99","MOCK","Stale failure");
        assertThat(orders.detail(order.id(),customer,false).paymentStatus()).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("select count(*) from order_status_history where order_id=?",Long.class,order.id())).isEqualTo(historyBefore);
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).hasSize(1);

        // Admin requests refund
        var refund = refunds.requestRefund(admin, true, order.id(), new RefundService.RefundRequest(paidOrder.total(), "Khách hủy đơn", "ORDER_CANCELLATION", false));
        assertThat(refund.status()).isEqualTo("PROCESSING");
        assertThat(refund.amount()).isEqualByComparingTo(paidOrder.total());

        // Process refund completion with bank reference
        var completed = refunds.processRefund(admin, refund.id(), new RefundService.RefundProcess("COMPLETE", "REF-VNPAY-998877", "Đã chuyển trả"));
        assertThat(completed.status()).isEqualTo("SUCCEEDED");
        assertThat(completed.referenceCode()).isEqualTo("REF-VNPAY-998877");

        // Verify order is now CANCELLED, paymentStatus is REFUNDED, and stock is restored
        var cancelledOrder = orders.detail(order.id(), customer, false);
        assertThat(cancelledOrder.status()).isEqualTo("CANCELLED");
        assertThat(cancelledOrder.paymentStatus()).isEqualTo("REFUNDED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("IN_STOCK"))).hasSize(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).isEmpty();

        // Idempotent: processing again does not re-apply stock changes or fail
        var second = refunds.processRefund(admin, refund.id(), new RefundService.RefundProcess("COMPLETE", "REF-VNPAY-998877", "Thử lại"));
        assertThat(second.status()).isEqualTo("SUCCEEDED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
    }

    @Test
    void cannotRefundExceedingTotalAmount() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, request(customer, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null), "MOCK")).order();

        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid");

        var paidOrder = orders.detail(order.id(), customer, false);
        BigDecimal excessive = paidOrder.total().add(BigDecimal.ONE);

        assertThatThrownBy(() -> refunds.requestRefund(admin, true, order.id(), new RefundService.RefundRequest(excessive, "Quá tiền", "ORDER_CANCELLATION", false)))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void latePaymentOnCancelledOrderCreatesReconciliationLedger() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, request(customer, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null), "MOCK")).order();

        // Order cancelled while unpaid
        orders.cancel(order.id(), customer, false, new OrderService.Cancel("Đổi ý", order.version()));
        assertThat(orders.detail(order.id(), customer, false).status()).isEqualTo("CANCELLED");

        // Late payment arrives
        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        assertThatThrownBy(() -> payments.completePayment(code, "00", "MOCK", "Late money arrived"))
            .isInstanceOf(BusinessException.class);

        // Check that reconciliation ledger recorded the incident
        var recs = refunds.listRefunds(null, order.id());
        assertThat(recs).hasSize(1);
        var rec = recs.getFirst();
        assertThat(rec.status()).isEqualTo("REQUESTED");
        assertThat(rec.refundType()).isEqualTo("LATE_PAYMENT_RECONCILIATION");

        // Admin approves and reconciles it
        var processed = refunds.processRefund(admin, rec.id(), new RefundService.RefundProcess("APPROVE", "BANK-TRANSFER-999", "Đã hoàn trả khách"));
        assertThat(processed.status()).isEqualTo("SUCCEEDED");
        assertThat(processed.referenceCode()).isEqualTo("BANK-TRANSFER-999");
    }

    @Test
    void refundDeliveredOrderWithRestockRevokesPointsAndWarranty() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, request(customer, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null), "MOCK")).order();

        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid");

        // Transition to SHIPPED then DELIVERED
        var o = orders.detail(order.id(), admin, true);
        o = orders.transition(order.id(), admin, new OrderService.Transition("SHIPPED", o.version(), false));
        o = orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", o.version(), false));

        // Check loyalty points and warranty cards exist
        long pointsBefore = loyalty.member(customer).pointsBalance();
        assertThat(pointsBefore).isGreaterThan(0);
        String imei = o.items().getFirst().imeiNumbers().getFirst();
        assertThat(warranty.lookup(imei, "0900000000")).isNotEmpty();
        Long oldCard=warranty.owned(customer).getFirst().id();
        warranty.receive(oldCard,"Màn hình đã được kiểm tra trước trả hàng");
        Long repairTicket=warranty.tickets().getFirst().id();

        // Customer returns goods and admin processes refund with restockItems=true
        var refundReq = refunds.requestRefund(admin, true, order.id(), new RefundService.RefundRequest(o.total(), "Khách đổi trả 7 ngày", "RETURN_GOODS", true));
        var completed = refunds.processRefund(admin, refundReq.id(), new RefundService.RefundProcess("COMPLETE", "REF-CASH-001", "Hoàn tiền mặt"));

        assertThat(completed.status()).isEqualTo("SUCCEEDED");
        // Points revoked
        assertThat(loyalty.member(customer).pointsBalance()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from point_transactions where order_id=?",Integer.class,order.id())).isEqualTo(1);
        assertThat(jdbc.queryForObject("select points_delta from point_reversals where order_id=?",Long.class,order.id())).isEqualTo(-pointsBefore);
        loyalty.revoke(ordersToEntity(order.id()));
        assertThat(jdbc.queryForObject("select count(*) from point_reversals where order_id=?",Integer.class,order.id())).isEqualTo(1);
        // Warranty revoked
        assertThat(warranty.lookup(imei, "0900000000")).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from warranty_cards where id=?",Integer.class,oldCard)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from repair_tickets where id=?",Integer.class,repairTicket)).isEqualTo(1);
        // Item back in stock
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("IN_STOCK"))).hasSize(2);
    }
    @Test void returnedSerialCanBeSoldAgainWithANewWarrantyWithoutRewritingOldRepairHistory(){
        carts.add(customer,new CartService.Add(product,1,carts.view(customer).version(),variant));
        var first=orders.checkout(customer,request(customer,new OrderService.Options("HOME_DELIVERY_STANDARD",null,null,null,null,null,null),"COD")).order();
        var confirmed=orders.transition(first.id(),admin,new OrderService.Transition("CONFIRMED",first.version(),false));
        var shipped=orders.transition(first.id(),admin,new OrderService.Transition("SHIPPED",confirmed.version(),false));
        var delivered=orders.transition(first.id(),admin,new OrderService.Transition("DELIVERED",shipped.version(),true));
        String serial=delivered.items().getFirst().imeiNumbers().getFirst();
        var old=warranty.owned(customer).getFirst();warranty.receive(old.id(),"Lịch sử sửa chữa của lần mua đầu");
        var refund=refunds.requestRefund(admin,true,first.id(),new RefundService.RefundRequest(first.total(),"Trả hàng đầy đủ","RETURN_GOODS",true));
        refunds.processRefund(admin,refund.id(),new RefundService.RefundProcess("COMPLETE","LOCAL-RETURN-001","Chứng từ thử nghiệm"));
        assertThatThrownBy(()->warranty.receive(old.id(),"Không được dùng thẻ đã thu hồi")).isInstanceOf(BusinessException.class);
        var revoked=jdbc.queryForObject("select revoked_at from warranty_cards where id=?",java.sql.Timestamp.class,old.id());
        warranty.revoke(ordersToEntity(first.id()));
        assertThat(jdbc.queryForObject("select revoked_at from warranty_cards where id=?",java.sql.Timestamp.class,old.id())).isEqualTo(revoked);
        carts.add(customer,new CartService.Add(product,1,carts.view(customer).version(),variant));
        var second=orders.checkout(customer,request(customer,new OrderService.Options("HOME_DELIVERY_STANDARD",null,null,null,null,null,null),"COD")).order();
        confirmed=orders.transition(second.id(),admin,new OrderService.Transition("CONFIRMED",second.version(),false));
        shipped=orders.transition(second.id(),admin,new OrderService.Transition("SHIPPED",confirmed.version(),false));
        delivered=orders.transition(second.id(),admin,new OrderService.Transition("DELIVERED",shipped.version(),true));
        assertThat(delivered.items().getFirst().imeiNumbers()).contains(serial);
        var current=warranty.lookup(serial,null);assertThat(current).hasSize(1);assertThat(current.getFirst().id()).isNotEqualTo(old.id());
        assertThat(jdbc.queryForObject("select count(*) from warranty_cards where imei_number=?",Integer.class,serial)).isEqualTo(2);
        assertThat(warranty.tickets()).hasSize(1);assertThat(warranty.tickets().getFirst().warrantyId()).isEqualTo(old.id());
    }
    @Autowired vn.techshop.order.OrderRepository orderRepository;
    vn.techshop.order.PurchaseOrder ordersToEntity(Long id){return orderRepository.findById(id).orElseThrow();}
    @Test void failedLateCallbackAndUnpaidCancellationCannotCreateRefundMoney(){
        carts.add(customer,new CartService.Add(product,1,carts.view(customer).version(),variant));
        var order=orders.checkout(customer,request(customer,new OrderService.Options("HOME_DELIVERY_STANDARD",null,null,null,null,null,null),"MOCK")).order();
        orders.cancel(order.id(),customer,false,new OrderService.Cancel("Không cần nữa",order.version()));
        String code=jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,order.id());
        assertThatThrownBy(()->payments.completePayment(code,"99","MOCK","Declined")).isInstanceOf(BusinessException.class);
        assertThat(refunds.listRefunds(null,order.id())).isEmpty();
        assertThatThrownBy(()->refunds.requestRefund(admin,true,order.id(),new RefundService.RefundRequest(order.total(),"Không có tiền đã nhận","ORDER_CANCELLATION",false))).isInstanceOf(BusinessException.class);
    }

    @Test
    void fullRefundOnShippedOrderRequiresGoodsReturnAndReleasesStockExactlyOnce() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, request(customer, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null), "MOCK")).order();
        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid");

        var paidOrder = orders.detail(order.id(), admin, true);
        assertThat(paidOrder.status()).isEqualTo("CONFIRMED");
        var shipped = orders.transition(order.id(), admin, new OrderService.Transition("SHIPPED", paidOrder.version(), false));
        BigDecimal orderTotal = shipped.total();

        // Customer cannot self-attest goods return
        assertThatThrownBy(() -> refunds.requestRefund(customer, false, order.id(), new RefundService.RefundRequest(orderTotal, "Khách tự xác nhận", "RETURN_GOODS", true)))
            .isInstanceOf(BusinessException.class);

        // Customer requests refund without restock
        var req = refunds.requestRefund(customer, false, order.id(), new RefundService.RefundRequest(orderTotal, "Khách đổi ý khi đang giao", "RETURN_GOODS", false));
        assertThat(req.status()).isEqualTo("REQUESTED");

        // Admin tries to complete without confirming goods returned -> rejected
        assertThatThrownBy(() -> refunds.processRefund(admin, req.id(), new RefundService.RefundProcess("COMPLETE", "REF-SHIPPED-001", "Chưa nhận hàng đã duyệt", false)))
            .isInstanceOf(BusinessException.class);

        // Stock still reserved
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).hasSize(1);
        assertThat(orders.detail(order.id(), admin, true).status()).isEqualTo("SHIPPED");

        // Admin confirms goods retrieval during completion
        var completed = refunds.processRefund(admin, req.id(), new RefundService.RefundProcess("COMPLETE", "REF-SHIPPED-001", "Đã nhận lại hàng từ shipper", true));
        assertThat(completed.status()).isEqualTo("SUCCEEDED");
        assertThat(completed.restockItems()).isTrue();

        // Order cancelled and refunded
        var cancelledOrder = orders.detail(order.id(), admin, true);
        assertThat(cancelledOrder.status()).isEqualTo("CANCELLED");
        assertThat(cancelledOrder.paymentStatus()).isEqualTo("REFUNDED");

        // Stock restored, branch reservation cleared
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("IN_STOCK"))).hasSize(2);
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("RESERVED"))).isEmpty();

        // Attempting to deliver cancelled order is rejected
        assertThatThrownBy(() -> orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", cancelledOrder.version(), false)))
            .isInstanceOf(BusinessException.class);

        // Idempotent retry: calling processRefund again does not double-restore stock
        var retried = refunds.processRefund(admin, req.id(), new RefundService.RefundProcess("COMPLETE", "REF-SHIPPED-001", "Thử lại", true));
        assertThat(retried.status()).isEqualTo("SUCCEEDED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
    }

    @Test
    void partialRefundOnShippedOrderKeepsOrderShippedAndDeliversNormally() {
        carts.add(customer, new CartService.Add(product, 1, carts.view(customer).version(), variant));
        var order = orders.checkout(customer, request(customer, new OrderService.Options("HOME_DELIVERY_STANDARD", null, null, null, null, null, null), "MOCK")).order();
        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid");

        var paidOrder = orders.detail(order.id(), admin, true);
        assertThat(paidOrder.status()).isEqualTo("CONFIRMED");
        var shipped = orders.transition(order.id(), admin, new OrderService.Transition("SHIPPED", paidOrder.version(), false));

        // Partial refund (e.g. 500,000 price adjustment or compensation)
        BigDecimal partial = new BigDecimal("500000");
        var req = refunds.requestRefund(admin, true, order.id(), new RefundService.RefundRequest(partial, "Bồi thường giao chậm", "PRICE_ADJUSTMENT", false));
        var completed = refunds.processRefund(admin, req.id(), new RefundService.RefundProcess("COMPLETE", "REF-PARTIAL-001", "Đã chuyển"));
        assertThat(completed.status()).isEqualTo("SUCCEEDED");

        // Order remains SHIPPED and PAID
        var current = orders.detail(order.id(), admin, true);
        assertThat(current.status()).isEqualTo("SHIPPED");
        assertThat(current.paymentStatus()).isEqualTo("PAID");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);

        // Can transition to DELIVERED
        var delivered = orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", current.version(), false));
        assertThat(delivered.status()).isEqualTo("DELIVERED");
        assertThat(delivered.paymentStatus()).isEqualTo("PAID");
        assertThat(branches.items(branch).stream().filter(i -> i.status().equals("SOLD"))).hasSize(1);
    }
}
