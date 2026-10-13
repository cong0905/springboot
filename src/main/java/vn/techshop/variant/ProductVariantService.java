package vn.techshop.variant;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.catalog.*;
import vn.techshop.shared.*;

@Service
@Transactional(readOnly=true)
public class ProductVariantService {
    public record Input(@NotBlank @Size(max=50) String skuCode,@NotBlank @Size(max=80) String colorName,
            @NotBlank @Size(max=80) String storageCapacity,@NotNull @DecimalMin("0") BigDecimal price,
            @NotNull @DecimalMin("0") BigDecimal originalPrice) {}
    public record View(Long id,Long productId,String skuCode,String colorName,String storageCapacity,
            BigDecimal price,BigDecimal originalPrice,String status,Long version,int available,List<String> images) {}
    public record SpecInput(@NotBlank @Size(max=80) String specGroup,@NotBlank @Size(max=80) String specName,
            @NotBlank @Size(max=500) String specValue,@Min(0) int displayOrder) {}
    public record Spec(String group,String name,String value,int displayOrder) {}
    private final ProductVariantRepository variants;
    private final ProductSpecificationRepository specs;
    private final ProductRepository products;
    private final CatalogService catalog;
    private final JdbcTemplate jdbc;
    public ProductVariantService(ProductVariantRepository variants,ProductSpecificationRepository specs,
            ProductRepository products,CatalogService catalog,JdbcTemplate jdbc) {
        this.variants=variants;this.specs=specs;this.products=products;this.catalog=catalog;this.jdbc=jdbc;
    }
    public List<View> list(Long productId) {
        return list(productId,false);
    }
    public List<View> list(Long productId,boolean admin) {
        catalog.detail(productId,admin);
        return variants.findByProductIdAndStatusOrderByIdAsc(productId,"ACTIVE").stream().map(this::view).toList();
    }
    public ProductVariant require(Long id,Long productId) {
        var v=variants.findById(id).orElseThrow(BusinessException::missing);
        if(!v.getProductId().equals(productId)||!"ACTIVE".equals(v.getStatus()))throw BusinessException.invalid("Biến thể không thuộc sản phẩm hoặc đã ngừng bán.");
        return v;
    }
    public boolean hasVariants(Long productId) {return jdbc.queryForObject("select count(*) from product_variants where product_id=?",Integer.class,productId)>0;}
    public ProductVariant forCart(Long id,Long productId){return variants.findById(id).filter(v->v.getProductId().equals(productId)).orElse(null);}
    public int available(Long variantId) {
        return jdbc.queryForObject("select coalesce(sum(i.quantity-i.reserved_quantity),0) from branch_inventories i join store_branches b on b.id=i.branch_id where i.variant_id=? and b.is_active=true",Integer.class,variantId);
    }
    public View view(ProductVariant v) {
        var images=jdbc.queryForList("select asset_path from product_images where product_id=? and (variant_id=? or variant_id is null) order by sort_order,id",String.class,v.getProductId(),v.getId());
        return new View(v.getId(),v.getProductId(),v.getSkuCode(),v.getColorName(),v.getStorageCapacity(),v.getPrice(),v.getOriginalPrice(),v.getStatus(),v.getVersion(),available(v.getId()),images);
    }
    @Transactional public View create(Long productId,Input input) {
        var product=products.lockById(productId).orElseThrow(BusinessException::missing);
        if(product.getStockQuantity()>0&&!hasVariants(productId))throw BusinessException.conflict("LEGACY_STOCK_REMAINING","Điều chỉnh kho cũ về 0 trước khi chuyển sang quản lý serial.");
        Rules.price(input.price());Rules.price(input.originalPrice());
        if(input.originalPrice().compareTo(input.price())<0)throw BusinessException.invalid("Giá gốc phải từ giá bán trở lên.");
        if(jdbc.queryForObject("select count(*) from product_variants where sku_code=?",Integer.class,input.skuCode())>0)throw BusinessException.conflict("SKU_EXISTS","Mã SKU đã tồn tại.");
        var v=new ProductVariant();v.setProductId(productId);v.setSkuCode(Rules.text(input.skuCode(),1,50));v.setColorName(Rules.text(input.colorName(),1,80));v.setStorageCapacity(Rules.text(input.storageCapacity(),1,80));v.setPrice(input.price());v.setOriginalPrice(input.originalPrice());v.setStatus("ACTIVE");
        return view(variants.saveAndFlush(v));
    }
    @Transactional public View update(Long id,Input input,Long expectedVersion) {
        var initial=variants.findById(id).orElseThrow(BusinessException::missing);
        products.lockById(initial.getProductId()).orElseThrow(BusinessException::missing);
        var v=variants.lockById(id).orElseThrow(BusinessException::missing);Rules.version(v.getVersion(),expectedVersion,"STALE_VERSION");
        if(!v.getSkuCode().equals(input.skuCode()))throw BusinessException.invalid("SKU bất biến.");
        Rules.price(input.price());Rules.price(input.originalPrice());if(input.originalPrice().compareTo(input.price())<0)throw BusinessException.invalid("Giá gốc không hợp lệ.");
        v.setColorName(Rules.text(input.colorName(),1,80));v.setStorageCapacity(Rules.text(input.storageCapacity(),1,80));v.setPrice(input.price());v.setOriginalPrice(input.originalPrice());return view(variants.saveAndFlush(v));
    }
    public List<Spec> specifications(Long productId) {
        catalog.detail(productId,false);
        return jdbc.query("select * from product_specifications where product_id=? order by display_order,id",(rs,n)->new Spec(rs.getString("spec_group"),rs.getString("spec_name"),rs.getString("spec_value"),rs.getInt("display_order")),productId);
    }
    public record Image(Long id,String assetPath,Long variantId) {}
    public List<Image> images(Long productId) {
        return jdbc.query("select id,asset_path,variant_id from product_images where product_id=? order by sort_order,id",(r,n)->new Image(r.getLong("id"),r.getString("asset_path"),r.getObject("variant_id",Long.class)),productId);
    }
    @Transactional public void assignImage(Long productId,Long imageId,Long variantId) {
        products.lockById(productId).orElseThrow(BusinessException::missing);
        if(variantId!=null)require(variantId,productId);
        if(jdbc.update("update product_images set variant_id=? where id=? and product_id=?",variantId,imageId,productId)!=1)throw BusinessException.missing();
    }
    @Transactional public void specification(Long productId,SpecInput input) {
        products.lockById(productId).orElseThrow(BusinessException::missing);
        String group=Rules.text(input.specGroup(),1,80),name=Rules.text(input.specName(),1,80),value=Rules.text(input.specValue(),1,500);
        jdbc.update("delete from product_specifications where product_id=? and spec_group=? and spec_name=?",productId,group,name);
        jdbc.update("insert into product_specifications(product_id,spec_group,spec_name,spec_value,display_order) values(?,?,?,?,?)",productId,group,name,value,input.displayOrder());
    }
}
