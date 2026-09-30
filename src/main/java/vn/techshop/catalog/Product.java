package vn.techshop.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "category_id", nullable = false)
    private Long categoryId;
    @Column(name = "sku", nullable = false, length = 50)
    private String sku;
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    @Column(name = "description", nullable = true, columnDefinition = "TEXT")
    private String description;
    @Column(name = "price", nullable = false, precision = 15, scale = 0)
    private BigDecimal price;
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;
    @Column(name = "status", nullable = false, length = 16)
    private String status;
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Product() {}
    public Long getId() { return id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long value) { this.categoryId = value; }
    public String getSku() { return sku; }
    public void setSku(String value) { this.sku = value; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal value) { this.price = value; }
    public Integer getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(Integer value) { this.stockQuantity = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public Long getVersion() { return version; }
    public void setVersion(Long value) { this.version = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
