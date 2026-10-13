package vn.techshop.identity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
@RestController
public class GoogleLinkApi {
 private final UserRepository users;private final org.springframework.security.crypto.password.PasswordEncoder passwords;
 public GoogleLinkApi(UserRepository users,org.springframework.security.crypto.password.PasswordEncoder passwords){this.users=users;this.passwords=passwords;}
 public record Proof(@jakarta.validation.constraints.NotBlank String currentPassword){}
 @PostMapping("/api/v1/me/google-link") ResponseEntity<Void> begin(@AuthenticationPrincipal TechPrincipal principal,@Valid @RequestBody Proof proof,jakarta.servlet.http.HttpServletRequest request){
  var user=users.findById(principal.id()).orElseThrow(vn.techshop.shared.BusinessException::missing);
  if(!passwords.matches(proof.currentPassword(),user.getPasswordHash()))throw vn.techshop.shared.BusinessException.invalid("Mật khẩu hiện tại không đúng.");
  var session=request.getSession();session.setAttribute("googleLinkUser",user.getId());session.setAttribute("googleLinkUntil",System.currentTimeMillis()+120000L);return ResponseEntity.noContent().build();
 }
}
