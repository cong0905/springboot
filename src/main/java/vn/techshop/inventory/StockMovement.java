package vn.techshop.inventory;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;
    @Column(name = "order_item_id", nullable = true)
    private Long orderItemId;
    @Column(name = "movement_type", nullable = false, length = 16)
    private String movementType;
    @Column(name = "delta", nullable = false)
    private Integer delta;
    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public StockMovement() {}
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { this.productId = value; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long value) { this.actorUserId = value; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long value) { this.orderItemId = value; }
    public String getMovementType() { return movementType; }
    public void setMovementType(String value) { this.movementType = value; }
    public Integer getDelta() { return delta; }
    public void setDelta(Integer value) { this.delta = value; }
    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer value) { this.balanceAfter = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant value) { this.occurredAt = value; }
}
