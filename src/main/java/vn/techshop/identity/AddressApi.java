package vn.techshop.identity;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/me/addresses")
public class AddressApi {
    private final AddressService addresses;
    public AddressApi(AddressService addresses){this.addresses=addresses;}
    @GetMapping java.util.List<AddressService.View> list(@AuthenticationPrincipal TechPrincipal user){return addresses.list(user.id());}
    @PostMapping ResponseEntity<AddressService.View> create(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody AddressService.Input input){return ResponseEntity.status(201).body(addresses.save(user.id(),null,input));}
    @PatchMapping("/{id}") AddressService.View update(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody AddressService.Input input){return addresses.save(user.id(),id,input);}
    @DeleteMapping("/{id}") ResponseEntity<Void> remove(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@RequestParam Long expectedVersion){addresses.remove(user.id(),id,expectedVersion);return ResponseEntity.noContent().build();}
}
