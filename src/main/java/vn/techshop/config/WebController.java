package vn.techshop.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.techshop.catalog.CatalogService;
import vn.techshop.cart.CartService;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.inventory.InventoryService;
import vn.techshop.order.OrderService;
import vn.techshop.reporting.ReportingService;
import vn.techshop.shared.BusinessException;

@Controller
public class WebController {
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.catalog.WishlistService wishlists;
    @GetMapping("/wishlist") String wishlist(@AuthenticationPrincipal TechPrincipal user,Model model){model.addAttribute("wishlist",wishlists.list(user.id()));return "wishlist";}
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.catalog.ProductRepository productRepository;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.catalog.StorefrontCatalogService storefront;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.payment.RefundService refunds;
    @GetMapping("/admin/refunds") String refunds(@RequestParam(required=false) String status,@RequestParam(required=false) Long orderId,Model model){model.addAttribute("refunds",refunds.listRefunds(status,orderId));model.addAttribute("status",status);model.addAttribute("orderId",orderId);return "admin/refunds";}
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.identity.AddressService addresses;
    @GetMapping("/addresses") String addresses(@AuthenticationPrincipal TechPrincipal user,Model model){model.addAttribute("addresses",addresses.list(user.id()));return "addresses";}
    @org.springframework.beans.factory.annotation.Value("${app.demo:false}") private boolean demoCatalog;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.variant.ProductVariantService variants;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.branch.BranchInventoryService branches;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.installment.InstallmentService installments;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.tradein.TradeInService tradein;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.reporting.CommerceReportingService commerceReports;
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.identity.IdentityService identity;
    private final CatalogService catalog;private final CartService cart;private final OrderService orders;private final ReportingService reports;private final InventoryService inventory;private final vn.techshop.review.ProductReviewService reviews;
    public WebController(CatalogService catalog,CartService cart,OrderService orders,ReportingService reports,InventoryService inventory,vn.techshop.review.ProductReviewService reviews) {this.catalog=catalog;this.cart=cart;this.orders=orders;this.reports=reports;this.inventory=inventory;this.reviews=reviews;}
    @ModelAttribute void common(@AuthenticationPrincipal TechPrincipal user,Model model) {model.addAttribute("demoCatalog",demoCatalog);model.addAttribute("viewer",user);model.addAttribute("isAdmin",user!=null && "ADMIN".equals(user.role()));model.addAttribute("isCustomer",user!=null && "CUSTOMER".equals(user.role()));}
    @GetMapping({"/","/products"}) String products(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="newest") String sort,@RequestParam(defaultValue="0") int page,@RequestParam(required=false) String brand,@RequestParam(required=false) String storage,@RequestParam(defaultValue="false") boolean inStock,Model model) {
        var pageView=storefront.search(keyword,categoryId,minPrice,maxPrice,sort,page,12,brand,storage,inStock);model.addAttribute("brand",brand);model.addAttribute("storage",storage);model.addAttribute("inStock",inStock);model.addAttribute("facets",storefront.facets(keyword,categoryId,minPrice,maxPrice,brand,storage,inStock));model.addAttribute("catalog",pageView);model.addAttribute("categories",catalog.categories(false));model.addAttribute("keyword",keyword);model.addAttribute("categoryId",categoryId);model.addAttribute("minPrice",minPrice);model.addAttribute("maxPrice",maxPrice);model.addAttribute("sort",sort);
        model.addAttribute("cardRatings",reviews.stats(pageView.content().stream().map(vn.techshop.catalog.CatalogService.ProductView::id).toList()));model.addAttribute("cardOriginals",storefront.originals(pageView.content()));return "products";
    }
    @GetMapping("/products/{id}") String product(@PathVariable Long id,@AuthenticationPrincipal TechPrincipal user,Model model) {model.addAttribute("product",catalog.detail(id,false));model.addAttribute("variants",variants.list(id));model.addAttribute("specifications",variants.specifications(id));model.addAttribute("reviewStats",reviews.getReviewStats(id));model.addAttribute("reviews",reviews.getApprovedReviews(id));if(user!=null && "CUSTOMER".equals(user.role()))model.addAttribute("cartVersion",cart.view(user.id()).version());return "product";}
    @GetMapping("/login") String login() {return "login";}
    @GetMapping("/register") String register() {return "register";}
    @GetMapping("/cart") String cart(@AuthenticationPrincipal TechPrincipal user,Model model) {model.addAttribute("cart",cart.view(user.id()));return "cart";}
    @GetMapping("/checkout") String checkout(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) java.util.List<Long> selectedItemIds,Model model) {model.addAttribute("cart",cart.selected(user.id(),selectedItemIds));model.addAttribute("checkoutKey",UUID.randomUUID().toString());model.addAttribute("addresses",addresses.list(user.id()));model.addAttribute("branches",branches.publicBranches(null,null));model.addAttribute("plans",installments.plans());model.addAttribute("quotes",tradein.quotes(user.id()).stream().filter(q->"APPROVED".equals(q.status())).toList());return "checkout";}
    @GetMapping("/orders") String orders(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,Model model) {String cleanStatus=status!=null&&!status.isBlank()?status.strip():null;model.addAttribute("status",cleanStatus);model.addAttribute("orders",orders.list(user.id(),false,cleanStatus,page,20));return "orders";}
    @GetMapping("/orders/{id}") String order(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,Model model) {model.addAttribute("order",orders.detail(id,user.id(),false));return "order";}
    @GetMapping({"/admin","/admin/reports"}) String dashboard(@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,Model model) {model.addAttribute("channels",commerceReports.channels(fromDate,toDate));model.addAttribute("report",reports.summary(fromDate,toDate));model.addAttribute("top",reports.top(fromDate,toDate,10));return "admin/dashboard";}
    @GetMapping("/admin/categories") String categories(Model model) {model.addAttribute("categories",catalog.categories(true));return "admin/categories";}
    @GetMapping("/admin/products") String adminProducts(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) String keyword,Model model) {model.addAttribute("keyword",keyword);model.addAttribute("catalog",catalog.search(keyword,null,null,null,"newest",page,20,true,null));return "admin/products";}
    @GetMapping("/admin/products/new") String newProduct(Model model) {model.addAttribute("categories",catalog.categories(true));model.addAttribute("product",null);return "admin/product-form";}
    @GetMapping("/admin/products/{id}/edit") String editProduct(@PathVariable Long id,Model model) {model.addAttribute("categories",catalog.categories(true));model.addAttribute("product",catalog.detail(id,true));model.addAttribute("brand",productRepository.findById(id).orElseThrow().getBrand());model.addAttribute("ledger",inventory.ledger(id,0,20));model.addAttribute("variants",variants.list(id,true));model.addAttribute("variantImages",variants.images(id));return "admin/product-form";}
    @GetMapping("/admin/orders") String adminOrders(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) String status,@RequestParam(required=false) java.time.LocalDate fromDate,@RequestParam(required=false) java.time.LocalDate toDate,@RequestParam(required=false) String keyword,@RequestParam(defaultValue="0") int page,Model model) {
        if(fromDate!=null && toDate!=null && fromDate.isAfter(toDate)) throw BusinessException.invalid("Từ ngày không được lớn hơn đến ngày.");
        String cleanStatus=status!=null&&!status.isBlank()?status.strip():null;
        String cleanKeyword=keyword!=null&&!keyword.isBlank()?keyword.strip():null;
        model.addAttribute("status",cleanStatus);
        model.addAttribute("fromDate",fromDate);
        model.addAttribute("toDate",toDate);
        model.addAttribute("keyword",cleanKeyword);
        model.addAttribute("orders",orders.list(user.id(),true,cleanStatus,fromDate,toDate,cleanKeyword,page,20));
        return "orders";
    }
    @GetMapping("/admin/orders/{id}") String adminOrder(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,Model model) {model.addAttribute("order",orders.detail(id,user.id(),true));return "order";}
    @GetMapping("/admin/users") String adminUsers(@RequestParam(required=false) String keyword,@RequestParam(defaultValue="0") int page,Model model) {model.addAttribute("keyword",keyword);model.addAttribute("users",identity.listUsers(keyword,page,20));return "admin/users";}
    @GetMapping("/profile") String profile(@AuthenticationPrincipal TechPrincipal user,Model model) {model.addAttribute("profile",identity.profile(user.id()));return "profile";}
}

