package vn.techshop.content;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import java.time.Duration;
@Controller
public class NewsletterController {
 private final NewsletterService newsletters;private final vn.techshop.identity.RecoveryService mail;private final vn.techshop.identity.RequestLimits limits;private final vn.techshop.config.ClientIpResolver ips;
 public NewsletterController(NewsletterService newsletters,vn.techshop.identity.RecoveryService mail,vn.techshop.identity.RequestLimits limits,vn.techshop.config.ClientIpResolver ips){this.newsletters=newsletters;this.mail=mail;this.limits=limits;this.ips=ips;}
 public record Subscribe(@jakarta.validation.constraints.Email @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=254) String email,@jakarta.validation.constraints.AssertTrue boolean consent){}
 public record Token(@jakarta.validation.constraints.NotBlank String token){}
 @GetMapping("/newsletter") String page(Model model){model.addAttribute("mailEnabled",mail.enabled());return "newsletter";}
 @GetMapping({"/newsletter/confirm","/newsletter/unsubscribe"}) String proof(jakarta.servlet.http.HttpServletRequest request,Model model){model.addAttribute("unsubscribe",request.getRequestURI().endsWith("unsubscribe"));return "newsletter-proof";}
 @PostMapping("/api/v1/newsletter") @ResponseBody ResponseEntity<java.util.Map<String,String>> subscribe(@Valid @RequestBody Subscribe input,jakarta.servlet.http.HttpServletRequest request){limits.consume("newsletter-ip:"+ips.resolve(request),10,Duration.ofHours(1));limits.consume("newsletter-mail:"+vn.techshop.cart.Pricing.sha256(input.email().strip().toLowerCase(java.util.Locale.ROOT)),3,Duration.ofHours(1));newsletters.subscribe(input.email());return ResponseEntity.accepted().body(java.util.Map.of("message","Nếu email phù hợp, hãy kiểm tra thư xác nhận đăng ký."));}
 @PostMapping("/api/v1/newsletter/confirm") @ResponseBody ResponseEntity<Void> confirm(@Valid @RequestBody Token input){newsletters.confirm(input.token());return ResponseEntity.noContent().build();}
 @PostMapping("/api/v1/newsletter/unsubscribe") @ResponseBody ResponseEntity<Void> cancel(@Valid @RequestBody Token input){newsletters.unsubscribe(input.token());return ResponseEntity.noContent().build();}
}
