package vn.techshop.branch;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="branch_inventories")
public class BranchInventory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 public Long getId() { return id; }
 @Column(name="branch_id", nullable=false) private Long branchId;
 public Long getBranchId() { return branchId; }
 public void setBranchId(Long v) { branchId=v; }
 @Column(name="variant_id", nullable=false) private Long variantId;
 public Long getVariantId() { return variantId; }
 public void setVariantId(Long v) { variantId=v; }
 @Column(name="quantity", nullable=false) private Integer quantity;
 public Integer getQuantity() { return quantity; }
 public void setQuantity(Integer v) { quantity=v; }
 @Column(name="reserved_quantity", nullable=false) private Integer reservedQuantity;
 public Integer getReservedQuantity() { return reservedQuantity; }
 public void setReservedQuantity(Integer v) { reservedQuantity=v; }
 @Version private Long version;
 public Long getVersion() { return version; }
}
