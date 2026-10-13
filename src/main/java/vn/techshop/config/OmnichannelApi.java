package vn.techshop.config;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.variant.*;
import vn.techshop.branch.BranchInventoryService;
import vn.techshop.order.*;
import vn.techshop.loyalty.LoyaltyService;
import vn.techshop.tradein.TradeInService;
import vn.techshop.installment.InstallmentService;
import vn.techshop.warranty.WarrantyService;
import vn.techshop.shared.BusinessException;

@RestController @RequestMapping("/api/v1")
public class OmnichannelApi {
    public record Decision(@NotBlank String status) {}
    public record Repair(@NotNull Long warrantyId,@NotBlank @Size(min=5,max=500) String issue) {}
    public record Pickup(@NotBlank String pickupCode) {}
    public record ImageAssignment(Long variantId) {}
    public record Exchange(@NotBlank @Size(min=5,max=40) String replacementSerial) {}
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.warranty.WarrantyExchangeService exchanges;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.reporting.CommerceReportingService reports;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.catalog.CatalogService catalog;
    @org.springframework.beans.factory.annotation.Autowired private CommerceSettingsService settings;
    public record VariantUpdate(@NotNull @Valid ProductVariantService.Input input,@NotNull Long expectedVersion) {}
    private final ProductVariantService variants;private final ProductComparisonService comparison;
    private final BranchInventoryService branches;private final OrderService orders;private final PickupService pickup;
    private final LoyaltyService loyalty;private final TradeInService tradein;private final InstallmentService installments;private final WarrantyService warranty;
    public OmnichannelApi(ProductVariantService variants,ProductComparisonService comparison,BranchInventoryService branches,OrderService orders,PickupService pickup,LoyaltyService loyalty,TradeInService tradein,InstallmentService installments,WarrantyService warranty) {this.variants=variants;this.comparison=comparison;this.branches=branches;this.orders=orders;this.pickup=pickup;this.loyalty=loyalty;this.tradein=tradein;this.installments=installments;this.warranty=warranty;}
    @GetMapping("/products/{id}/variants") List<ProductVariantService.View> variants(@PathVariable Long id) {return variants.list(id);}
    @GetMapping("/products/{id}/specifications") List<ProductVariantService.Spec> specs(@PathVariable Long id) {return variants.specifications(id);}
    @GetMapping("/products/compare") ProductComparisonService.Comparison compare(@RequestParam List<Long> ids) {return comparison.compare(ids);}
    @PostMapping("/admin/products/{id}/variants") ProductVariantService.View variant(@PathVariable Long id,@Valid @RequestBody ProductVariantService.Input in) {return variants.create(id,in);}
    @PatchMapping("/admin/variants/{id}") ProductVariantService.View variantUpdate(@PathVariable Long id,@Valid @RequestBody VariantUpdate in) {return variants.update(id,in.input(),in.expectedVersion());}
    @PostMapping("/admin/products/{id}/specifications") ResponseEntity<Void> spec(@PathVariable Long id,@Valid @RequestBody ProductVariantService.SpecInput in) {variants.specification(id,in);return ResponseEntity.noContent().build();}
    @PatchMapping("/admin/products/{id}/images/{imageId}/variant") ResponseEntity<Void> image(@PathVariable Long id,@PathVariable Long imageId,@RequestBody ImageAssignment input) {variants.assignImage(id,imageId,input.variantId());return ResponseEntity.noContent().build();}
    @GetMapping("/branches") List<BranchInventoryService.Branch> branches(@RequestParam(required=false) String provinceCode,@RequestParam(required=false) String districtCode) {return branches.publicBranches(provinceCode,districtCode);}
    @GetMapping("/variants/{id}/stores") List<BranchInventoryService.Stock> stores(@PathVariable Long id,@RequestParam(required=false) String provinceCode,@RequestParam(required=false) String districtCode,@RequestParam(defaultValue="false") boolean inStock) {return branches.findStoresWithStock(id,provinceCode,districtCode,inStock);}
    @PatchMapping("/admin/branches/{id}/details") BranchInventoryService.Branch branchDetails(@PathVariable Long id,@Valid @RequestBody BranchInventoryService.Details in){return branches.details(id,in);}
    @PostMapping("/admin/coupons") vn.techshop.coupon.Coupon coupon(@RequestBody vn.techshop.coupon.CouponService.CouponInput in){return couponService.create(in);}
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.coupon.CouponService couponService;
    @PostMapping("/admin/branches") BranchInventoryService.Branch branch(@Valid @RequestBody BranchInventoryService.BranchInput in) {return branches.create(in);}
    @GetMapping("/admin/imei") List<BranchInventoryService.Item> items(@RequestParam(required=false) Long branchId) {return branches.items(branchId);}
    @PostMapping("/admin/imei/import") ResponseEntity<Void> receive(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody BranchInventoryService.Import in) {branches.receive(user.id(),in);return ResponseEntity.noContent().build();}
    @PostMapping("/admin/imei/transfer") ResponseEntity<Void> transfer(@Valid @RequestBody BranchInventoryService.Transfer in) {branches.transfer(in);return ResponseEntity.noContent().build();}
    @PostMapping("/orders/preview") OrderService.Preview preview(@AuthenticationPrincipal TechPrincipal user,@RequestBody OrderService.PreviewRequest in) {return orders.preview(user.id(),in);}
    @PostMapping("/admin/orders/{id}/pickup") OrderService.OrderView pickup(@PathVariable Long id,@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody Pickup in) {return pickup.collect(id,user.id(),in.pickupCode());}
    @GetMapping("/me/member") LoyaltyService.Member member(@AuthenticationPrincipal TechPrincipal user) {return loyalty.member(user.id());}
    @GetMapping("/trade-in/devices") List<TradeInService.Device> devices() {return tradein.devices();}
    @PostMapping("/admin/trade-in/devices") ResponseEntity<Void> createDevice(@Valid @RequestBody CommerceSettingsService.DeviceInput in) {settings.device(in);return ResponseEntity.noContent().build();}
    @PostMapping("/admin/trade-in/criteria") ResponseEntity<Void> createCriteria(@Valid @RequestBody CommerceSettingsService.CriteriaInput in) {settings.criteria(in);return ResponseEntity.noContent().build();}
    @PostMapping("/admin/installment/plans") ResponseEntity<Void> createPlan(@Valid @RequestBody CommerceSettingsService.PlanInput in) {settings.plan(in);return ResponseEntity.noContent().build();}
    @PostMapping("/admin/trade-in/devices/{id}/archive") ResponseEntity<Void> archiveDevice(@PathVariable Long id) {settings.archiveDevice(id);return ResponseEntity.noContent().build();}
    @PostMapping("/admin/installment/plans/{id}/archive") ResponseEntity<Void> archivePlan(@PathVariable Long id) {settings.archivePlan(id);return ResponseEntity.noContent().build();}
    @GetMapping("/trade-in/criteria") List<TradeInService.Criteria> criteria() {return tradein.criteria();}
    @GetMapping("/me/trade-in") List<TradeInService.Quote> quotes(@AuthenticationPrincipal TechPrincipal user) {return tradein.quotes(user.id());}
    @PostMapping("/me/trade-in") TradeInService.Quote quote(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody TradeInService.Input in) {return tradein.quote(user.id(),in);}
    @PostMapping("/admin/trade-in/{id}/approve") ResponseEntity<Void> approve(@PathVariable Long id) {tradein.approve(id);return ResponseEntity.noContent().build();}
    @GetMapping("/installment/plans") List<InstallmentService.Plan> plans() {return installments.plans();}
    @GetMapping("/installment/quote") InstallmentService.Schedule schedule(@RequestParam Long planId,@RequestParam BigDecimal amount) {return installments.quote(planId,amount);}
    @PostMapping("/admin/orders/{id}/installment") ResponseEntity<Void> decision(@PathVariable Long id,@Valid @RequestBody Decision in) {installments.decision(id,in.status());return ResponseEntity.noContent().build();}
    @GetMapping("/warranty/lookup") List<WarrantyService.Card> lookup(@RequestParam String query,@RequestParam(required=false) String phone) {return warranty.lookup(query,phone);}
    @GetMapping("/me/warranty") List<WarrantyService.Card> owned(@AuthenticationPrincipal TechPrincipal user) {return warranty.owned(user.id());}
    @PostMapping("/admin/repairs") ResponseEntity<Void> receiveRepair(@Valid @RequestBody Repair in) {warranty.receive(in.warrantyId(),in.issue());return ResponseEntity.noContent().build();}
    @PostMapping("/admin/repairs/{id}/transitions") ResponseEntity<Void> repairStatus(@PathVariable Long id,@Valid @RequestBody Decision in) {warranty.transition(id,in.status());return ResponseEntity.noContent().build();}
    @PostMapping("/admin/warranty/{id}/exchange") vn.techshop.warranty.WarrantyExchangeService.Result exchange(@PathVariable Long id,@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody Exchange in) {return exchanges.exchange(id,in.replacementSerial(),user.id());}
    @GetMapping("/admin/reports/commerce") List<vn.techshop.reporting.CommerceReportingService.Channel> channels(@RequestParam(required=false) java.time.LocalDate fromDate,@RequestParam(required=false) java.time.LocalDate toDate) {return reports.channels(fromDate,toDate);}
    @GetMapping("/products/autocomplete") vn.techshop.shared.PageView<vn.techshop.catalog.CatalogService.ProductView> autocomplete(@RequestParam String keyword) {return catalog.search(keyword,null,null,null,"newest",0,5,false,null);}
}
