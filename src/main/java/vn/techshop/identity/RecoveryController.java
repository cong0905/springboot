package vn.techshop.identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.Duration;
@Controller
public class RecoveryController {
 private final RecoveryService recovery;private final RequestLimits limits;private final vn.techshop.config.ClientIpResolver ips;
 public RecoveryController(RecoveryService recovery,RequestLimits limits,vn.techshop.config.ClientIpResolver ips){this.recovery=recovery;this.limits=limits;this.ips=ips;}
 public record Request(@NotBlank @Email @Size(max=254) String email){}
 public record Reset(@NotBlank String token,@NotBlank @Size(min=10,max=64) String newPassword){}
 @GetMapping("/forgot-password") String forgot(Model model){model.addAttribute("mailEnabled",recovery.enabled());return "forgot-password";}
 @GetMapping("/reset-password") String resetPage(){return "reset-password";}
 @PostMapping("/api/v1/auth/forgot-password") @ResponseBody ResponseEntity<java.util.Map<String,String>> request(@Valid @RequestBody Request input,jakarta.servlet.http.HttpServletRequest request){
  limits.consume("reset-ip:"+ips.resolve(request),20,Duration.ofHours(1));limits.consume("reset-mail:"+vn.techshop.cart.Pricing.sha256(input.email().strip().toLowerCase(java.util.Locale.ROOT)),3,Duration.ofHours(1));recovery.request(input.email());return ResponseEntity.accepted().body(java.util.Map.of("message","Nếu tài khoản phù hợp tồn tại, liên kết sẽ được gửi qua email."));
 }
 @PostMapping("/api/v1/auth/reset-password") @ResponseBody ResponseEntity<Void> reset(@Valid @RequestBody Reset input,jakarta.servlet.http.HttpServletRequest request){limits.consume("reset-use:"+ips.resolve(request),15,Duration.ofMinutes(15));recovery.reset(input.token(),input.newPassword());return ResponseEntity.noContent().build();}
}
