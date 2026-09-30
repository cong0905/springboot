package vn.techshop.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import vn.techshop.shared.PageView;

@RestController
@RequestMapping("/api/v1")
public class CatalogApi {
    private final CatalogService catalog;
    public CatalogApi(CatalogService catalog) {this.catalog=catalog;}
    @GetMapping("/categories") List<CatalogService.CategoryView> categories() {return catalog.categories(false);}
    @GetMapping("/products") PageView<CatalogService.ProductView> products(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="newest") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size) {return catalog.search(keyword,categoryId,minPrice,maxPrice,sort,page,size,false,null);}
    @GetMapping("/products/{id}") CatalogService.ProductView detail(@PathVariable Long id) {return catalog.detail(id,false);}
}
