package vn.techshop.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_status_history")
public class OrderHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;
    @Column(name = "from_status", nullable = true, length = 16)
    private String fromStatus;
    @Column(name = "to_status", nullable = false, length = 16)
    private String toStatus;
    @Column(name = "reason", nullable = true, length = 500)
    private String reason;
    @Column(name = "order_version", nullable = false)
    private Long orderVersion;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public OrderHistory() {}
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId = value; }
    public Long getActorUserId() { return actorUserId; }
    public void setActorUserId(Long value) { this.actorUserId = value; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String value) { this.fromStatus = value; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String value) { this.toStatus = value; }
    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }
    public Long getOrderVersion() { return orderVersion; }
    public void setOrderVersion(Long value) { this.orderVersion = value; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant value) { this.occurredAt = value; }
}
