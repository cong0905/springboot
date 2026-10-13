package vn.techshop.variant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="product_variants")
public class ProductVariant {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 public Long getId() { return id; }
 @Column(name="product_id", nullable=false) private Long productId;
 public Long getProductId() { return productId; }
 public void setProductId(Long v) { productId=v; }
 @Column(name="sku_code", length=50, nullable=false) private String skuCode;
 public String getSkuCode() { return skuCode; }
 public void setSkuCode(String v) { skuCode=v; }
 @Column(name="color_name", length=80, nullable=false) private String colorName;
 public String getColorName() { return colorName; }
 public void setColorName(String v) { colorName=v; }
 @Column(name="storage_capacity", length=80, nullable=false) private String storageCapacity;
 public String getStorageCapacity() { return storageCapacity; }
 public void setStorageCapacity(String v) { storageCapacity=v; }
 @Column(name="price", precision=15, scale=0, nullable=false) private BigDecimal price;
 public BigDecimal getPrice() { return price; }
 public void setPrice(BigDecimal v) { price=v; }
 @Column(name="original_price", precision=15, scale=0, nullable=false) private BigDecimal originalPrice;
 public BigDecimal getOriginalPrice() { return originalPrice; }
 public void setOriginalPrice(BigDecimal v) { originalPrice=v; }
 @Column(name="status", length=16, nullable=false) private String status;
 public String getStatus() { return status; }
 public void setStatus(String v) { status=v; }
 @Version private Long version;
 public Long getVersion() { return version; }
}
