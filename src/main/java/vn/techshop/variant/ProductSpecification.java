package vn.techshop.variant;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="product_specifications")
public class ProductSpecification {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 public Long getId() { return id; }
 @Column(name="product_id", nullable=false) private Long productId;
 public Long getProductId() { return productId; }
 public void setProductId(Long v) { productId=v; }
 @Column(name="spec_group", length=80, nullable=false) private String specGroup;
 public String getSpecGroup() { return specGroup; }
 public void setSpecGroup(String v) { specGroup=v; }
 @Column(name="spec_name", length=80, nullable=false) private String specName;
 public String getSpecName() { return specName; }
 public void setSpecName(String v) { specName=v; }
 @Column(name="spec_value", length=500, nullable=false) private String specValue;
 public String getSpecValue() { return specValue; }
 public void setSpecValue(String v) { specValue=v; }
 @Column(name="display_order", nullable=false) private Integer displayOrder;
 public Integer getDisplayOrder() { return displayOrder; }
 public void setDisplayOrder(Integer v) { displayOrder=v; }
}
