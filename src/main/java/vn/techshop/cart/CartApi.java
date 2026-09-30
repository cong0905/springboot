package vn.techshop.cart;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.techshop.identity.TechPrincipal;

@RestController
@RequestMapping("/api/v1/cart")
public class CartApi {
    private final CartService cart;
    public CartApi(CartService cart) {this.cart=cart;}
    @GetMapping CartService.CartView view(@AuthenticationPrincipal TechPrincipal user) {return cart.view(user.id());}
    @PostMapping("/items") CartService.CartView add(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody CartService.Add input) {return cart.add(user.id(),input);}
    @PatchMapping("/items/{id}") CartService.CartView update(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody CartService.Update input) {return cart.update(user.id(),id,input);}
    @DeleteMapping("/items/{id}") ResponseEntity<Void> remove(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@RequestParam Long expectedVersion) {cart.remove(user.id(),id,expectedVersion);return ResponseEntity.noContent().build();}
}
