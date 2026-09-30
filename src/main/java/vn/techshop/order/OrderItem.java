package vn.techshop.order;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "sku_snapshot", nullable = false, length = 50)
    private String skuSnapshot;
    @Column(name = "name_snapshot", nullable = false, length = 200)
    private String nameSnapshot;
    @Column(name = "unit_price", nullable = false, precision = 15, scale = 0)
    private BigDecimal unitPrice;
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    @Column(name = "line_total", nullable = false, precision = 15, scale = 0)
    private BigDecimal lineTotal;

    public OrderItem() {}
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long value) { this.orderId = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { this.productId = value; }
    public String getSkuSnapshot() { return skuSnapshot; }
    public void setSkuSnapshot(String value) { this.skuSnapshot = value; }
    public String getNameSnapshot() { return nameSnapshot; }
    public void setNameSnapshot(String value) { this.nameSnapshot = value; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal value) { this.unitPrice = value; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer value) { this.quantity = value; }
    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal value) { this.lineTotal = value; }
}
