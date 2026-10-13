package vn.techshop.catalog;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.techshop.identity.TechPrincipal;
@RestController @RequestMapping("/api/v1/me/wishlist")
public class WishlistApi {
 private final WishlistService wishlists;public WishlistApi(WishlistService wishlists){this.wishlists=wishlists;}
 @GetMapping java.util.List<WishlistService.Entry> list(@AuthenticationPrincipal TechPrincipal user){return wishlists.list(user.id());}
 @PutMapping("/{id}") ResponseEntity<Void> add(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id){wishlists.add(user.id(),id);return ResponseEntity.noContent().build();}
 @DeleteMapping("/{id}") ResponseEntity<Void> remove(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id){wishlists.remove(user.id(),id);return ResponseEntity.noContent().build();}
}
