package vn.techshop.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "product_images")
public class ProductImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "product_id", nullable = false)
    private Long productId;
    @Column(name = "asset_path", nullable = false, length = 255)
    private String assetPath;
    @Column(name = "alt_text", nullable = false, length = 200)
    private String altText;
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    public ProductImage() {}
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { this.productId = value; }
    public String getAssetPath() { return assetPath; }
    public void setAssetPath(String value) { this.assetPath = value; }
    public String getAltText() { return altText; }
    public void setAltText(String value) { this.altText = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { this.sortOrder = value; }
}
