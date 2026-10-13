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
    public record BrandInput(@jakarta.validation.constraints.Size(max=80) String brand,@jakarta.validation.constraints.NotNull Long expectedVersion){}
    @PatchMapping("/products/{id}/brand") ResponseEntity<Void> brand(@PathVariable Long id,@Valid @RequestBody BrandInput input){catalog.brand(id,input.brand(),input.expectedVersion());return ResponseEntity.noContent().build();}
    public record ImageView(Long id,Long productId,String assetPath,String altText,Integer sortOrder,Long variantId) {}
    private static ImageView imageView(ProductImage i) {return new ImageView(i.getId(),i.getProductId(),i.getAssetPath(),i.getAltText(),i.getSortOrder(),i.getVariantId());}
    private final CatalogService catalog;private final InventoryService inventory;private final OrderService orders;private final ReportingService reports;private final vn.techshop.shared.FileStorageService storage;private final ReportExportService exportService;private final vn.techshop.identity.IdentityService identity;private final vn.techshop.payment.RefundService refunds;
    public AdminApi(CatalogService catalog,InventoryService inventory,OrderService orders,ReportingService reports,vn.techshop.shared.FileStorageService storage,ReportExportService exportService,vn.techshop.identity.IdentityService identity,vn.techshop.payment.RefundService refunds) {this.catalog=catalog;this.inventory=inventory;this.orders=orders;this.reports=reports;this.storage=storage;this.exportService=exportService;this.identity=identity;this.refunds=refunds;}
    @GetMapping("/users") public org.springframework.data.domain.Page<vn.techshop.identity.IdentityService.UserView> users(@RequestParam(required=false) String query,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return identity.listUsers(query,page,size);}
    @GetMapping("/categories") List<CatalogService.CategoryView> categories() {return catalog.categories(true);}
    @PostMapping("/categories") ResponseEntity<CatalogService.CategoryView> createCategory(@Valid @RequestBody CatalogService.CategoryInput input) {return ResponseEntity.status(201).body(catalog.createCategory(input));}
    @PatchMapping("/categories/{id}") CatalogService.CategoryView updateCategory(@PathVariable Long id,@Valid @RequestBody CatalogService.CategoryUpdate input) {return catalog.updateCategory(id,input);}
    @GetMapping("/products") PageView<CatalogService.ProductView> products(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return catalog.search(keyword,categoryId,null,null,"newest",page,size,true,status);}
    @GetMapping("/products/{id}") CatalogService.ProductView product(@PathVariable Long id) {return catalog.detail(id,true);}
    @PostMapping("/products") ResponseEntity<CatalogService.ProductView> createProduct(@Valid @RequestBody CatalogService.ProductInput input) {return ResponseEntity.status(201).body(catalog.createProduct(input));}
    @PatchMapping("/products/{id}") CatalogService.ProductView updateProduct(@PathVariable Long id,@Valid @RequestBody CatalogService.ProductUpdate input) {return catalog.updateProduct(id,input);}
    @PostMapping("/products/{id}/images") public ResponseEntity<ImageView> uploadImage(@PathVariable Long id,@RequestParam("file") org.springframework.web.multipart.MultipartFile file,@RequestParam(defaultValue="0") int sortOrder) {catalog.detail(id,true);String assetPath=storage.store(file);try{return ResponseEntity.status(201).body(imageView(catalog.addImage(id,assetPath,file.getOriginalFilename(),sortOrder)));}catch(RuntimeException ex){storage.delete(assetPath);throw ex;}}
    @DeleteMapping("/products/{id}/images/{imageId}") public ResponseEntity<Void> deleteImage(@PathVariable Long id,@PathVariable Long imageId) {var img=catalog.listImages(id).stream().filter(i->i.getId().equals(imageId)).findFirst().orElseThrow(vn.techshop.shared.BusinessException::missing);catalog.deleteImage(imageId);storage.delete(img.getAssetPath());return ResponseEntity.noContent().build();}
    @GetMapping("/products/{id}/images") public List<ImageView> listImages(@PathVariable Long id) {return catalog.listImages(id).stream().map(AdminApi::imageView).toList();}
    @PostMapping("/products/{id}/stock-adjustments") InventoryService.Result adjustment(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody InventoryService.Adjustment input) {return inventory.adjust(id,user.id(),input);}
    @GetMapping("/products/{id}/stock-movements") PageView<InventoryService.MovementView> ledger(@PathVariable Long id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return inventory.ledger(id,page,size);}
    @GetMapping("/orders") PageView<OrderService.Summary> orders(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) String status,@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(required=false) String keyword,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return orders.list(user.id(),true,status,fromDate,toDate,keyword,page,size);}
    @GetMapping("/orders/{id}") OrderService.OrderView order(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id) {return orders.detail(id,user.id(),true);}
    @PostMapping("/orders/{id}/cancel") OrderService.OrderView cancel(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody OrderService.Cancel input) {return orders.cancel(id,user.id(),true,input);}
    @PostMapping("/orders/{id}/transitions") OrderService.OrderView transition(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody OrderService.Transition input) {return orders.transition(id,user.id(),input);}
    @GetMapping("/refunds") public List<vn.techshop.payment.RefundService.RefundView> refunds(@RequestParam(required=false) String status,@RequestParam(required=false) Long orderId) {return refunds.listRefunds(status,orderId);}
    @PostMapping("/orders/{id}/refund") public vn.techshop.payment.RefundService.RefundView requestAdminRefund(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody vn.techshop.payment.RefundService.RefundRequest input) {return refunds.requestRefund(user.id(),true,id,input);}
    @PostMapping("/refunds/{id}/process") public vn.techshop.payment.RefundService.RefundView processRefund(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody vn.techshop.payment.RefundService.RefundProcess input) {return refunds.processRefund(user.id(),id,input);}
    @GetMapping("/reports/summary") ReportingService.Summary summary(@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate) {return reports.summary(fromDate,toDate);}
    @GetMapping("/reports/top-products") List<ReportingService.TopProduct> top(@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,@RequestParam(defaultValue="10") int limit) {return reports.top(fromDate,toDate,limit);}
    @GetMapping(value="/reports/export/orders",produces="text/csv") public ResponseEntity<byte[]> exportOrders(@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate) {byte[] csv=exportService.exportOrdersCsv(fromDate,toDate);return ResponseEntity.ok().header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"techshop-orders-"+LocalDate.now()+".csv\"").header(org.springframework.http.HttpHeaders.CONTENT_TYPE,"text/csv; charset=UTF-8").body(csv);}

}
