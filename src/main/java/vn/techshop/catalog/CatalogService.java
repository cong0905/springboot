package vn.techshop.catalog;

import jakarta.persistence.criteria.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.*;

@Service
@Transactional(readOnly=true)
public class CatalogService {
    public record CategoryInput(@NotBlank String code, @NotBlank String name, @NotBlank String status) {}
    public record CategoryUpdate(@NotBlank String name, @NotBlank String status, @NotNull Long expectedVersion) {}
    public record CategoryView(Long id,String code,String name,String status,Long version) {}
    public record ProductInput(@NotNull Long categoryId,@NotBlank String sku,@NotBlank String name,@NotNull BigDecimal price,@NotBlank String status,@Size(max=5000) String description) {}
    public record ProductUpdate(@NotNull Long categoryId,@NotBlank String name,@NotNull BigDecimal price,@NotBlank String status,@Size(max=5000) String description,@NotNull Long expectedVersion) {}
    public record ProductView(Long id,String sku,String name,String description,BigDecimal price,String currency,Integer stockQuantity,String status,Long version,Long categoryId,String categoryName,String thumbnailPath) {}
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final ProductImageRepository images;
    private final Clock clock;
    public CatalogService(ProductRepository products,CategoryRepository categories,ProductImageRepository images,Clock clock) { this.products=products;this.categories=categories;this.images=images;this.clock=clock; }
    public List<CategoryView> categories(boolean admin) { return (admin?categories.findAll(Sort.by("name")):categories.findByStatusOrderByNameAsc("ACTIVE")).stream().map(CatalogService::categoryView).toList(); }
    public PageView<ProductView> search(String keyword,Long categoryId,BigDecimal minPrice,BigDecimal maxPrice,String sort,int page,int size,boolean admin,String status) {
        if(keyword!=null && keyword.length()>100) throw BusinessException.invalid("Từ khóa quá dài.");
        if(minPrice!=null && minPrice.signum()<0 || maxPrice!=null && maxPrice.signum()<0 || minPrice!=null && maxPrice!=null && minPrice.compareTo(maxPrice)>0) throw BusinessException.invalid("Khoảng giá không hợp lệ.");
        Sort ordering=switch(sort) { case "newest" -> Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")); case "price_asc" -> Sort.by("price","id"); case "price_desc" -> Sort.by(Sort.Order.desc("price"),Sort.Order.desc("id")); default -> throw BusinessException.invalid("Kiểu sắp xếp không hợp lệ."); };
        Specification<Product> spec=(root,query,cb) -> {
            var checks=new ArrayList<Predicate>();
            if(!admin) { checks.add(cb.equal(root.get("status"),"ACTIVE")); var sub=query.subquery(Long.class);var category=sub.from(Category.class);sub.select(category.get("id")).where(cb.equal(category.get("id"),root.get("categoryId")),cb.equal(category.get("status"),"ACTIVE"));checks.add(cb.exists(sub)); }
            if(status!=null && admin) checks.add(cb.equal(root.get("status"),Rules.status(status)));
            if(categoryId!=null) checks.add(cb.equal(root.get("categoryId"),categoryId));
            if(minPrice!=null) checks.add(cb.greaterThanOrEqualTo(root.get("price"),minPrice));
            if(maxPrice!=null) checks.add(cb.lessThanOrEqualTo(root.get("price"),maxPrice));
            if(keyword!=null && !keyword.isBlank()) { String escaped=keyword.strip().toLowerCase(Locale.ROOT).replace("\\","\\\\").replace("%","\\%").replace("_","\\_");checks.add(cb.like(cb.lower(root.get("name")),"%"+escaped+"%",'\\')); }
            return cb.and(checks.toArray(Predicate[]::new));
        };
        var result=products.findAll(spec,PageView.request(page,size,ordering));
        var categoryMap=categories.findAllById(result.stream().map(Product::getCategoryId).distinct().toList()).stream().collect(Collectors.toMap(Category::getId,Category::getName));
        // Product thumbnails use a packaged asset in this first increment.
        return PageView.of(result.map(p -> productView(p,categoryMap.get(p.getCategoryId()))));
    }
    public ProductView detail(Long id,boolean admin) {
        var p=products.findById(id).orElseThrow(BusinessException::missing);
        var c=categories.findById(p.getCategoryId()).orElseThrow(BusinessException::missing);
        if(!admin && (!"ACTIVE".equals(p.getStatus()) || !"ACTIVE".equals(c.getStatus()))) throw BusinessException.missing();
        return productView(p,c.getName());
    }
    @Transactional public CategoryView createCategory(CategoryInput input) {
        String code=Rules.code(input.code());
        if(categories.existsByCode(code)) throw BusinessException.conflict("CODE_EXISTS","Mã danh mục đã tồn tại.");
        var c=new Category();c.setCode(code);c.setName(Rules.text(input.name(),2,100));c.setStatus(Rules.status(input.status()));c.setCreatedAt(clock.instant());c.setUpdatedAt(clock.instant());
        return categoryView(categories.saveAndFlush(c));
    }
    @Transactional public CategoryView updateCategory(Long id,CategoryUpdate input) {
        var c=categories.findById(id).orElseThrow(BusinessException::missing);Rules.version(c.getVersion(),input.expectedVersion(),"STALE_VERSION");
        c.setName(Rules.text(input.name(),2,100));c.setStatus(Rules.status(input.status()));c.setUpdatedAt(clock.instant());return categoryView(categories.saveAndFlush(c));
    }
    @Transactional public ProductView createProduct(ProductInput input) {
        var c=categories.findById(input.categoryId()).orElseThrow(BusinessException::missing);String sku=Rules.code(input.sku());
        if(products.existsBySku(sku)) throw BusinessException.conflict("SKU_EXISTS","SKU đã tồn tại.");
        var p=new Product();p.setCategoryId(c.getId());p.setSku(sku);p.setName(Rules.text(input.name(),2,200));p.setDescription(Rules.optional(input.description(),5000));p.setPrice(Rules.price(input.price()));p.setStockQuantity(0);p.setStatus(Rules.status(input.status()));p.setCreatedAt(clock.instant());p.setUpdatedAt(clock.instant());
        products.saveAndFlush(p);return productView(p,c.getName());
    }
    @Transactional public ProductView updateProduct(Long id,ProductUpdate input) {
        var p=products.findById(id).orElseThrow(BusinessException::missing);Rules.version(p.getVersion(),input.expectedVersion(),"STALE_VERSION");
        var c=categories.findById(input.categoryId()).orElseThrow(BusinessException::missing);
        p.setCategoryId(c.getId());p.setName(Rules.text(input.name(),2,200));p.setDescription(Rules.optional(input.description(),5000));p.setPrice(Rules.price(input.price()));p.setStatus(Rules.status(input.status()));p.setUpdatedAt(clock.instant());
        products.saveAndFlush(p);return productView(p,c.getName());
    }
    private static CategoryView categoryView(Category c) { return new CategoryView(c.getId(),c.getCode(),c.getName(),c.getStatus(),c.getVersion()); }
    private static ProductView productView(Product p,String categoryName) { return new ProductView(p.getId(),p.getSku(),p.getName(),p.getDescription(),p.getPrice(),"VND",p.getStockQuantity(),p.getStatus(),p.getVersion(),p.getCategoryId(),categoryName,"/assets/products/device.svg"); }
}
