package vn.techshop.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.branch.BranchInventoryService;
import vn.techshop.catalog.ProductRepository;
import vn.techshop.inventory.InventoryService;
import vn.techshop.loyalty.LoyaltyService;
import vn.techshop.order.OrderHistory;
import vn.techshop.order.OrderHistoryRepository;
import vn.techshop.order.OrderItemRepository;
import vn.techshop.order.OrderRepository;
import vn.techshop.order.PurchaseOrder;
import vn.techshop.shared.BusinessException;
import vn.techshop.shared.Rules;
import vn.techshop.tradein.TradeInService;
import vn.techshop.warranty.WarrantyService;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RefundService {

    public record RefundRequest(
        @NotNull BigDecimal amount,
        @NotBlank String reason,
        String refundType,
        boolean restockItems
    ) {}

    public record RefundProcess(
        @NotBlank String action,
        String referenceCode,
        String notes,
        Boolean restockItems
    ) {
        public RefundProcess(String action, String referenceCode, String notes) {
            this(action, referenceCode, notes, null);
        }
    }

    public record RefundView(
        Long id,
        String refundCode,
        Long orderId,
        Long paymentTransactionId,
        BigDecimal amount,
        String status,
        String refundType,
        String reason,
        Long actorId,
        Long processedBy,
        String referenceCode,
        String gateway,
        boolean restockItems,
        String notes,
        Instant createdAt,
        Instant updatedAt
    ) {}

    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderHistoryRepository histories;
    private final ProductRepository products;
    private final InventoryService inventory;
    private final BranchInventoryService branches;
    private final LoyaltyService loyalty;
    private final WarrantyService warranty;
    private final TradeInService tradein;

    public RefundService(
        JdbcTemplate jdbc,
        Clock clock,
        OrderRepository orders,
        OrderItemRepository orderItems,
        OrderHistoryRepository histories,
        ProductRepository products,
        InventoryService inventory,
        BranchInventoryService branches,
        LoyaltyService loyalty,
        WarrantyService warranty,
        TradeInService tradein
    ) {
        this.jdbc = jdbc;
        this.clock = clock;
        this.orders = orders;
        this.orderItems = orderItems;
        this.histories = histories;
        this.products = products;
        this.inventory = inventory;
        this.branches = branches;
        this.loyalty = loyalty;
        this.warranty = warranty;
        this.tradein = tradein;
    }

    @Transactional
    public RefundView recordLatePaymentReconciliation(Long orderId, Long paymentTransactionId, BigDecimal amount, String gateway) {
        if (paymentTransactionId != null) {
            var existing = jdbc.query(
                "select * from refund_reconciliations where order_id=? and payment_transaction_id=?",
                this::mapRow, orderId, paymentTransactionId
            );
            if (!existing.isEmpty()) {
                return existing.getFirst();
            }
        }
        Instant now = clock.instant();
        String code = "REC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String cleanGateway = gateway != null ? gateway.trim().toUpperCase() : "MANUAL";
        jdbc.update(
            "insert into refund_reconciliations (refund_code, order_id, payment_transaction_id, amount, status, refund_type, reason, actor_id, gateway, restock_items, created_at, updated_at) " +
            "values (?, ?, ?, ?, 'REQUESTED', 'LATE_PAYMENT_RECONCILIATION', 'Thanh toán thành công đến muộn trên đơn đã hủy', 0, ?, false, ?, ?)",
            code, orderId, paymentTransactionId, amount, cleanGateway, Timestamp.from(now), Timestamp.from(now)
        );
        Long id = jdbc.queryForObject("select id from refund_reconciliations where refund_code=?", Long.class, code);
        return detail(id);
    }

    @Transactional
    public RefundView requestRefund(Long actorId, boolean admin, Long orderId, RefundRequest input) {
        var order = (admin ? orders.lockById(orderId) : orders.lockOwned(orderId, actorId))
            .orElseThrow(BusinessException::missing);

        if (input.amount() == null || input.amount().compareTo(BigDecimal.ZERO) <= 0 || input.amount().stripTrailingZeros().scale()>0) {
            throw BusinessException.invalid("Số tiền hoàn phải lớn hơn 0.");
        }
        if (!"PAID".equals(order.getPaymentStatus())) {
            throw BusinessException.conflict("REFUND_NOT_ELIGIBLE", "Chỉ hoàn tiền cho đơn đã thanh toán.");
        }

        BigDecimal activeTotal = jdbc.queryForObject(
            "select coalesce(sum(amount), 0) from refund_reconciliations where order_id=? and status in ('REQUESTED', 'PROCESSING', 'SUCCEEDED')",
            BigDecimal.class, orderId
        );
        if (activeTotal.add(input.amount()).compareTo(order.getTotalAmount()) > 0) {
            throw BusinessException.conflict("REFUND_AMOUNT_EXCEEDED", "Tổng số tiền hoàn vượt quá giá trị đơn hàng.");
        }

        Instant now = clock.instant();
        String code = "REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String reason = Rules.text(input.reason(), 5, 500);
        String type = input.refundType() != null && !input.refundType().isBlank() ? input.refundType() : "ORDER_CANCELLATION";
        if(!java.util.Set.of("ORDER_CANCELLATION","RETURN_GOODS","PRICE_ADJUSTMENT").contains(type))throw BusinessException.invalid("Loại hoàn tiền không hợp lệ.");
        // Returning inventory is a separate physical event; only staff may attest a full return.
        if (input.restockItems() && (!admin || (!"DELIVERED".equals(order.getStatus()) && !"SHIPPED".equals(order.getStatus())) || input.amount().compareTo(order.getTotalAmount()) != 0))
            throw BusinessException.invalid("Chỉ quản trị được xác nhận đã nhận lại toàn bộ hàng của đơn đã giao hoặc đang giao.");
        if(("PENDING".equals(order.getStatus())||"CONFIRMED".equals(order.getStatus())) && input.amount().compareTo(order.getTotalAmount())!=0)
            throw BusinessException.invalid("Hủy đơn chưa giao cần hoàn đủ giá trị đã thanh toán.");
        String status = admin ? "PROCESSING" : "REQUESTED";

        jdbc.update(
            "insert into refund_reconciliations (refund_code, order_id, amount, status, refund_type, reason, actor_id, gateway, restock_items, created_at, updated_at) " +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            code, orderId, input.amount(), status, type, reason, actorId,
            order.getPaymentMethod() != null ? order.getPaymentMethod() : "MANUAL",
            input.restockItems(), Timestamp.from(now), Timestamp.from(now)
        );

        Long id = jdbc.queryForObject("select id from refund_reconciliations where refund_code=?", Long.class, code);
        return detail(id);
    }

    @Transactional
    public RefundView processRefund(Long adminId, Long refundId, RefundProcess input) {
        // Follow the shared order -> transaction/refund lock order used by checkout/callback.
        var original=detail(refundId);
        var order = orders.lockById(original.orderId()).orElseThrow(BusinessException::missing);
        var existing = jdbc.query("select * from refund_reconciliations where id=? for update", this::mapRow, refundId);
        if (existing.isEmpty()) throw BusinessException.missing();
        var refund = existing.getFirst();

        if ("SUCCEEDED".equals(refund.status())) {
            // Idempotent retry: do not refund twice
            return refund;
        }
        if ("FAILED".equals(refund.status())) {
            throw BusinessException.conflict("REFUND_ALREADY_FAILED", "Yêu cầu hoàn tiền đã bị từ chối trước đó.");
        }

        Instant now = clock.instant();
        String action = input.action() != null ? input.action().trim().toUpperCase() : "";

        if ("REJECT".equals(action)) {
            jdbc.update(
                "update refund_reconciliations set status='FAILED', processed_by=?, notes=?, updated_at=? where id=?",
                adminId, Rules.optional(input.notes(), 1000), Timestamp.from(now), refundId
            );
            return detail(refundId);
        }

        if("PROCESS".equals(action)) {
            jdbc.update("update refund_reconciliations set status='PROCESSING',processed_by=?,notes=?,updated_at=? where id=?",adminId,Rules.optional(input.notes(),1000),Timestamp.from(now),refundId);
            return detail(refundId);
        }
        if ("APPROVE".equals(action) || "COMPLETE".equals(action)) {
            boolean restockRequested = Boolean.TRUE.equals(input.restockItems()) || refund.restockItems();

            BigDecimal currentSucceeded = jdbc.queryForObject(
                "select coalesce(sum(amount), 0) from refund_reconciliations where order_id=? and status='SUCCEEDED' and id != ?",
                BigDecimal.class, order.getId(), refundId
            );
            BigDecimal totalSucceeded = (currentSucceeded != null ? currentSucceeded : BigDecimal.ZERO).add(refund.amount());
            boolean isFullRefund = totalSucceeded.compareTo(order.getTotalAmount()) >= 0;
            String previousStatus = order.getStatus();

            // Rule OP-01: For SHIPPED order, full refund requires confirmed retrieval of goods
            if ("SHIPPED".equals(previousStatus) && isFullRefund && !restockRequested) {
                throw BusinessException.conflict("RETURN_GOODS_REQUIRED", "Đơn đang giao cần xác nhận thu hồi hàng về kho trước khi hoàn tất hoàn tiền.");
            }

            String refCode = Rules.text(input.referenceCode(), 3, 100);
            jdbc.update(
                "update refund_reconciliations set status='SUCCEEDED', processed_by=?, reference_code=?, notes=?, restock_items=?, updated_at=? where id=?",
                adminId, refCode, Rules.optional(input.notes(), 1000), restockRequested, Timestamp.from(now), refundId
            );

            if (isFullRefund) {
                order.setPaymentStatus("REFUNDED");
            }

            // Case 1: Order is PENDING or CONFIRMED -> Cancel order and release stock/serial
            if ("PENDING".equals(previousStatus) || "CONFIRMED".equals(previousStatus)) {
                order.setStatus("CANCELLED");
                order.setUpdatedAt(now);
                orders.saveAndFlush(order);

                for (var item : orderItems.findByOrderIdOrderByProductIdAsc(order.getId())) {
                    var product = products.lockById(item.getProductId()).orElseThrow(BusinessException::missing);
                    inventory.apply(product, adminId, item.getId(), "CANCEL", item.getQuantity(), "Hoàn tiền hủy đơn");
                    branches.release(item.getId());
                }
                tradein.cancel(order.getId());
            }
            // Case 2: Order is SHIPPED and full refund with confirmed return of goods -> Cancel order and release stock/serial
            else if ("SHIPPED".equals(previousStatus) && isFullRefund && restockRequested) {
                order.setStatus("CANCELLED");
                order.setUpdatedAt(now);
                orders.saveAndFlush(order);

                for (var item : orderItems.findByOrderIdOrderByProductIdAsc(order.getId())) {
                    var product = products.lockById(item.getProductId()).orElseThrow(BusinessException::missing);
                    inventory.apply(product, adminId, item.getId(), "CANCEL", item.getQuantity(), "Hoàn tiền thu hồi đơn đang giao");
                    branches.release(item.getId());
                }
                tradein.cancel(order.getId());
            }
            // Case 3: Order was DELIVERED and restockItems is requested
            else if ("DELIVERED".equals(previousStatus) && restockRequested) {
                for (var item : orderItems.findByOrderIdOrderByProductIdAsc(order.getId())) {
                    var product = products.lockById(item.getProductId()).orElseThrow(BusinessException::missing);
                    inventory.apply(product, adminId, item.getId(), "CANCEL", item.getQuantity(), "Khách trả hàng hoàn tiền");
                    branches.returnSoldItem(item.getId());
                }
                loyalty.revoke(order);
                warranty.revoke(order);
            }

            order.setUpdatedAt(now);
            orders.saveAndFlush(order);

            long nextVersion = jdbc.queryForObject(
                "select coalesce(max(order_version), 0) + 1 from order_status_history where order_id=?",
                Long.class, order.getId()
            );
            OrderHistory h = new OrderHistory();
            h.setOrderId(order.getId());
            h.setActorUserId(adminId);
            h.setFromStatus(previousStatus);
            h.setToStatus(order.getStatus());
            h.setReason("Hoàn tiền thành công " + refund.amount() + "đ (Mã đối soát: " + refCode + ")");
            h.setOrderVersion(nextVersion);
            h.setOccurredAt(now);
            histories.saveAndFlush(h);

            return detail(refundId);
        }

        throw BusinessException.invalid("Hành động xử lý không hợp lệ: " + action);
    }

    public RefundView detail(Long refundId) {
        var list = jdbc.query("select * from refund_reconciliations where id=?", this::mapRow, refundId);
        if (list.isEmpty()) throw BusinessException.missing();
        return list.getFirst();
    }

    public List<RefundView> listRefunds(String status, Long orderId) {
        String s = Rules.optional(status, 20);
        return jdbc.query(
            "select * from refund_reconciliations where (? is null or status=?) and (? is null or order_id=?) order by id desc",
            this::mapRow, s, s, orderId, orderId
        );
    }

    private RefundView mapRow(ResultSet r, int rowNum) throws SQLException {
        Timestamp cat = r.getTimestamp("created_at");
        Timestamp uat = r.getTimestamp("updated_at");
        Long pid = r.getObject("payment_transaction_id", Long.class);
        Long pby = r.getObject("processed_by", Long.class);
        return new RefundView(
            r.getLong("id"),
            r.getString("refund_code"),
            r.getLong("order_id"),
            pid,
            r.getBigDecimal("amount"),
            r.getString("status"),
            r.getString("refund_type"),
            r.getString("reason"),
            r.getLong("actor_id"),
            pby,
            r.getString("reference_code"),
            r.getString("gateway"),
            r.getBoolean("restock_items"),
            r.getString("notes"),
            cat != null ? cat.toInstant() : null,
            uat != null ? uat.toInstant() : null
        );
    }
}
