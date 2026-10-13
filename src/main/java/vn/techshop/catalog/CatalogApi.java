package vn.techshop.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import vn.techshop.shared.PageView;

@RestController
@RequestMapping("/api/v1")
public class CatalogApi {
    @org.springframework.beans.factory.annotation.Autowired private StorefrontCatalogService storefront;
    private final CatalogService catalog;
    public CatalogApi(CatalogService catalog) {this.catalog=catalog;}
    @GetMapping("/categories") List<CatalogService.CategoryView> categories() {return catalog.categories(false);}
    @GetMapping("/products") PageView<CatalogService.ProductView> products(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="newest") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size,@RequestParam(required=false) String brand,@RequestParam(required=false) String storage,@RequestParam(defaultValue="false") boolean inStock) {return storefront.search(keyword,categoryId,minPrice,maxPrice,sort,page,size,brand,storage,inStock);}
    @GetMapping("/products/facets") StorefrontCatalogService.Facets facets(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,@RequestParam(required=false) String brand,@RequestParam(required=false) String storage,@RequestParam(defaultValue="false") boolean inStock){return storefront.facets(keyword,categoryId,minPrice,maxPrice,brand,storage,inStock);}
    @GetMapping("/products/{id}") CatalogService.ProductView detail(@PathVariable Long id) {return catalog.detail(id,false);}
}
