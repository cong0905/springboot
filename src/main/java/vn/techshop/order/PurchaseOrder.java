package vn.techshop.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "orders")
public class PurchaseOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "checkout_key", nullable = false, length = 36, columnDefinition = "CHAR(36)")
    private String checkoutKey;
    @Column(name = "request_hash", nullable = false, length = 64, columnDefinition = "CHAR(64)")
    private String requestHash;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Column(name = "recipient_name", nullable = false, length = 100)
    private String recipientName;
    @Column(name = "phone", nullable = false, length = 10, columnDefinition = "CHAR(10)")
    private String phone;
    @Column(name = "address", nullable = false, length = 500)
    private String address;
    @Column(name = "note", nullable = true, length = 500)
    private String note;
    @Column(name = "currency", nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currency;
    @Column(name = "subtotal", nullable = false, precision = 15, scale = 0)
    private BigDecimal subtotal;
    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 0)
    private BigDecimal shippingFee;
    @Column(name = "total_amount", nullable = false, precision = 15, scale = 0)
    private BigDecimal totalAmount;
    @Column(name = "cod_collected", nullable = false)
    private Boolean codCollected;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "delivered_at", nullable = true)
    private Instant deliveredAt;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public PurchaseOrder() {}
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long value) { this.userId = value; }
    public String getCheckoutKey() { return checkoutKey; }
    public void setCheckoutKey(String value) { this.checkoutKey = value; }
    public String getRequestHash() { return requestHash; }
    public void setRequestHash(String value) { this.requestHash = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { this.recipientName = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { this.phone = value; }
    public String getAddress() { return address; }
    public void setAddress(String value) { this.address = value; }
    public String getNote() { return note; }
    public void setNote(String value) { this.note = value; }
    public String getCurrency() { return currency; }
    public void setCurrency(String value) { this.currency = value; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal value) { this.subtotal = value; }
    public BigDecimal getShippingFee() { return shippingFee; }
    public void setShippingFee(BigDecimal value) { this.shippingFee = value; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal value) { this.totalAmount = value; }
    public Boolean getCodCollected() { return codCollected; }
    public void setCodCollected(Boolean value) { this.codCollected = value; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant value) { this.deliveredAt = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { this.version = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
