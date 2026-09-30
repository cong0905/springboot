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

@Controller
public class WebController {
    private final CatalogService catalog;private final CartService cart;private final OrderService orders;private final ReportingService reports;private final InventoryService inventory;
    public WebController(CatalogService catalog,CartService cart,OrderService orders,ReportingService reports,InventoryService inventory) {this.catalog=catalog;this.cart=cart;this.orders=orders;this.reports=reports;this.inventory=inventory;}
    @ModelAttribute void common(@AuthenticationPrincipal TechPrincipal user,Model model) {model.addAttribute("viewer",user);model.addAttribute("isAdmin",user!=null && "ADMIN".equals(user.role()));model.addAttribute("isCustomer",user!=null && "CUSTOMER".equals(user.role()));}
    @GetMapping({"/","/products"}) String products(@RequestParam(required=false) String keyword,@RequestParam(required=false) Long categoryId,@RequestParam(required=false) BigDecimal minPrice,@RequestParam(required=false) BigDecimal maxPrice,@RequestParam(defaultValue="newest") String sort,@RequestParam(defaultValue="0") int page,Model model) {
        model.addAttribute("catalog",catalog.search(keyword,categoryId,minPrice,maxPrice,sort,page,12,false,null));model.addAttribute("categories",catalog.categories(false));model.addAttribute("keyword",keyword);model.addAttribute("categoryId",categoryId);model.addAttribute("minPrice",minPrice);model.addAttribute("maxPrice",maxPrice);model.addAttribute("sort",sort);return "products";
    }
    @GetMapping("/products/{id}") String product(@PathVariable Long id,@AuthenticationPrincipal TechPrincipal user,Model model) {model.addAttribute("product",catalog.detail(id,false));if(user!=null && "CUSTOMER".equals(user.role()))model.addAttribute("cartVersion",cart.view(user.id()).version());return "product";}
    @GetMapping("/login") String login() {return "login";}
    @GetMapping("/register") String register() {return "register";}
    @GetMapping("/cart") String cart(@AuthenticationPrincipal TechPrincipal user,Model model) {model.addAttribute("cart",cart.view(user.id()));return "cart";}
    @GetMapping("/checkout") String checkout(@AuthenticationPrincipal TechPrincipal user,Model model) {model.addAttribute("cart",cart.view(user.id()));model.addAttribute("checkoutKey",UUID.randomUUID().toString());return "checkout";}
    @GetMapping("/orders") String orders(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,Model model) {model.addAttribute("status",status);model.addAttribute("orders",orders.list(user.id(),false,status,page,20));return "orders";}
    @GetMapping("/orders/{id}") String order(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,Model model) {model.addAttribute("order",orders.detail(id,user.id(),false));return "order";}
    @GetMapping({"/admin","/admin/reports"}) String dashboard(@RequestParam(required=false) LocalDate fromDate,@RequestParam(required=false) LocalDate toDate,Model model) {model.addAttribute("report",reports.summary(fromDate,toDate));model.addAttribute("top",reports.top(fromDate,toDate,10));return "admin/dashboard";}
    @GetMapping("/admin/categories") String categories(Model model) {model.addAttribute("categories",catalog.categories(true));return "admin/categories";}
    @GetMapping("/admin/products") String adminProducts(@RequestParam(defaultValue="0") int page,@RequestParam(required=false) String keyword,Model model) {model.addAttribute("keyword",keyword);model.addAttribute("catalog",catalog.search(keyword,null,null,null,"newest",page,20,true,null));return "admin/products";}
    @GetMapping("/admin/products/new") String newProduct(Model model) {model.addAttribute("categories",catalog.categories(true));model.addAttribute("product",null);return "admin/product-form";}
    @GetMapping("/admin/products/{id}/edit") String editProduct(@PathVariable Long id,Model model) {model.addAttribute("categories",catalog.categories(true));model.addAttribute("product",catalog.detail(id,true));model.addAttribute("ledger",inventory.ledger(id,0,20));return "admin/product-form";}
    @GetMapping("/admin/orders") String adminOrders(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,Model model) {model.addAttribute("status",status);model.addAttribute("orders",orders.list(user.id(),true,status,page,20));return "orders";}
    @GetMapping("/admin/orders/{id}") String adminOrder(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,Model model) {model.addAttribute("order",orders.detail(id,user.id(),true));return "order";}
}
