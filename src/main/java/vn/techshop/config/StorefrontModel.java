package vn.techshop.config;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.ui.Model;
import vn.techshop.catalog.CatalogService;
import vn.techshop.cart.CartService;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.order.LookupOrderController;
import vn.techshop.payment.PaymentController;

/** Shared, database-backed navigation for every HTML storefront page. */
@ControllerAdvice(assignableTypes={vn.techshop.chat.ChatWeb.class,WebController.class,OmnichannelWeb.class,LookupOrderController.class,PaymentController.class,vn.techshop.identity.RecoveryController.class,vn.techshop.content.NewsController.class,vn.techshop.content.NewsletterController.class,vn.techshop.guest.GuestWeb.class})
public class StorefrontModel {
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.core.env.Environment environment;
    private final CatalogService catalog;
    private final CartService carts;
    public StorefrontModel(CatalogService catalog,CartService carts){this.catalog=catalog;this.carts=carts;}
    @ModelAttribute void navigation(@AuthenticationPrincipal TechPrincipal user,jakarta.servlet.http.HttpServletRequest request,Model model){
        model.addAttribute("storefrontCategories",catalog.categories(false));
        int cartCount = 0;
        if (user != null && "CUSTOMER".equals(user.role())) {
            try { cartCount = carts.view(user.id()).items().stream().mapToInt(CartService.ItemView::quantity).sum(); } catch (Exception ignored) {}
        } else if (user == null && request != null) {
            var session = request.getSession(false);
            Long guestId = session == null ? null : (Long) session.getAttribute("guestOwner");
            if (guestId != null) {
                try { cartCount = carts.view(guestId).items().stream().mapToInt(CartService.ItemView::quantity).sum(); } catch (Exception ignored) {}
            }
        }
        model.addAttribute("headerCartCount", cartCount);
        model.addAttribute("mockPaymentsEnabled",environment.getProperty("app.payment.mock.enabled",Boolean.class,false));
        model.addAttribute("googleEnabled",java.util.Arrays.asList(environment.getActiveProfiles()).contains("google"));model.addAttribute("viewer",user);
        model.addAttribute("isCustomer",user!=null && "CUSTOMER".equals(user.role()));
        model.addAttribute("isAdmin",user!=null && "ADMIN".equals(user.role()));
    }
}
