package vn.techshop.config;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.techshop.catalog.*;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.inventory.*;
import vn.techshop.order.*;
import vn.techshop.reporting.*;
import vn.techshop.shared.PageView;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApi {
    private final CatalogService catalog;private final InventoryService inventory;private final OrderService orders;private final ReportingService reports;
    public AdminApi(CatalogService catalog,InventoryService inventory,OrderService orders,ReportingService reports) {this.catalog=catalog;this.inventory=inventory;this.orders=orders;this.reports=reports;}
    @GetMapping("/categories") List<CatalogService.CategoryView> categories() {return catalog.categories(true);}
    @PostMapping("/categories") ResponseEntity<CatalogService.CategoryView> createCategory(@Valid @RequestBody CatalogService.CategoryInput input) {return ResponseEntity.status(201).body(catalog.createCategory(input));}
    @PatchMapping("/categories/{id}") CatalogService.CategoryView updateCategory(@PathVariable Long id,@Valid @RequestBody CatalogService.CategoryUpdate input) {return catalog.updateCategory(id,input);}
    @GetMapping("/products") PageView<CatalogService.ProductView> products(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return catalog.search(keyword,categoryId,null,null,"newest",page,size,true,status);}
    @GetMapping("/products/{id}") CatalogService.ProductView product(@PathVariable Long id) {return catalog.detail(id,true);}
    @PostMapping("/products") ResponseEntity<CatalogService.ProductView> createProduct(@Valid @RequestBody CatalogService.ProductInput input) {return ResponseEntity.status(201).body(catalog.createProduct(input));}
    @PatchMapping("/products/{id}") CatalogService.ProductView updateProduct(@PathVariable Long id,@Valid @RequestBody CatalogService.ProductUpdate input) {return catalog.updateProduct(id,input);}
    @PostMapping("/products/{id}/stock-adjustments") InventoryService.Result adjustment(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody InventoryService.Adjustment input) {return inventory.adjust(id,user.id(),input);}
    @GetMapping("/products/{id}/stock-movements") PageView<InventoryService.MovementView> ledger(@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return inventory.ledger(id,page,size);}
    @GetMapping("/orders") PageView<OrderService.Summary> orders(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return orders.list(user.id(),true,status,page,size);}
    @GetMapping("/orders/{id}") OrderService.OrderView order(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id) {return orders.detail(id,user.id(),true);}
    @PostMapping("/orders/{id}/cancel") OrderService.OrderView cancel(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody OrderService.Cancel input) {return orders.cancel(id,user.id(),true,input);}
    @PostMapping("/orders/{id}/transitions") OrderService.OrderView transition(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody OrderService.Transition input) {return orders.transition(id,user.id(),input);}
    @GetMapping("/reports/summary") ReportingService.Summary summary(@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate) {return reports.summary(fromDate,toDate);}
    @GetMapping("/reports/top-products") List<ReportingService.TopProduct> top(@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(defaultValue="10") int limit) {return reports.top(fromDate,toDate,limit);}
}
