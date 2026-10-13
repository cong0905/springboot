package vn.techshop.guest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import vn.techshop.cart.CartService;
import vn.techshop.catalog.CatalogService;
import vn.techshop.order.OrderService;
@Controller
public class GuestWeb {
 private final GuestService guests;private final CartService carts;private final CatalogService catalog;private final OrderService orders;private final vn.techshop.variant.ProductVariantService variants;private final vn.techshop.review.ProductReviewService reviews;
 public GuestWeb(GuestService guests,CartService carts,CatalogService catalog,OrderService orders,vn.techshop.variant.ProductVariantService variants,vn.techshop.review.ProductReviewService reviews){this.guests=guests;this.carts=carts;this.catalog=catalog;this.orders=orders;this.variants=variants;this.reviews=reviews;}
 @GetMapping("/guest/products/{id}") String product(@PathVariable Long id,HttpServletRequest request,Model model){model.addAttribute("guestMode",true);model.addAttribute("product",catalog.detail(id,false));model.addAttribute("variants",variants.list(id));model.addAttribute("specifications",variants.specifications(id));model.addAttribute("reviewStats",reviews.getReviewStats(id));model.addAttribute("reviews",reviews.getApprovedReviews(id));Long owner=guests.current(request);model.addAttribute("cartVersion",owner==null?0:carts.view(owner).version());return "product";}
 @GetMapping("/guest/cart") String cart(HttpServletRequest request,Model model){model.addAttribute("guestMode",true);Long owner=guests.current(request);model.addAttribute("cart",owner==null?GuestApi.empty():carts.view(owner));return "cart";}
 @GetMapping("/guest/checkout") String checkout(HttpServletRequest request,@RequestParam(required=false) java.util.List<Long> selectedItemIds,Model model){Long owner=guests.require(request);model.addAttribute("guestMode",true);model.addAttribute("cart",carts.selected(owner,selectedItemIds));model.addAttribute("checkoutKey",java.util.UUID.randomUUID().toString());model.addAttribute("branches",java.util.List.of());model.addAttribute("plans",java.util.List.of());model.addAttribute("quotes",java.util.List.of());model.addAttribute("addresses",java.util.List.of());return "checkout";}
 @GetMapping("/guest/orders/{id}") String order(HttpServletRequest request,@PathVariable Long id,Model model){model.addAttribute("guestMode",true);model.addAttribute("order",orders.detail(id,guests.owner(request,id),false));return "order";}
 @GetMapping("/guest/orders") String orders(HttpServletRequest request,@RequestParam(defaultValue="0") int page,@RequestParam(required=false) String status,Model model){model.addAttribute("guestMode",true);model.addAttribute("status",status);model.addAttribute("orders",orders.list(guests.require(request),false,status,page,20));return "orders";}
}
