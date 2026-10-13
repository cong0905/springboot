package vn.techshop.imei;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="product_items_imei")
public class ProductItemImei {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 public Long getId() { return id; }
 @Column(name="variant_id", nullable=false) private Long variantId;
 public Long getVariantId() { return variantId; }
 public void setVariantId(Long v) { variantId=v; }
 @Column(name="branch_id", nullable=false) private Long branchId;
 public Long getBranchId() { return branchId; }
 public void setBranchId(Long v) { branchId=v; }
 @Column(name="imei_number", length=40, nullable=false) private String imeiNumber;
 public String getImeiNumber() { return imeiNumber; }
 public void setImeiNumber(String v) { imeiNumber=v; }
 @Column(name="status", length=16, nullable=false) private String status;
 public String getStatus() { return status; }
 public void setStatus(String v) { status=v; }
 @Column(name="order_item_id") private Long orderItemId;
 public Long getOrderItemId() { return orderItemId; }
 public void setOrderItemId(Long v) { orderItemId=v; }
 @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="imported_at", nullable=false) private Instant importedAt;
 public Instant getImportedAt() { return importedAt; }
 public void setImportedAt(Instant v) { importedAt=v; }
 @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="sold_at") private Instant soldAt;
 public Instant getSoldAt() { return soldAt; }
 public void setSoldAt(Instant v) { soldAt=v; }
}
