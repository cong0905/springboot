package vn.techshop.identity;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IdentityApi {
    private final IdentityService identity;
    public IdentityApi(IdentityService identity) {this.identity=identity;}
    public record TokenView(String token,String headerName,String parameterName) {}
    @GetMapping("/csrf") ResponseEntity<TokenView> csrf(CsrfToken token) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new TokenView(token.getToken(),token.getHeaderName(),token.getParameterName()));}
    @PostMapping("/auth/register") ResponseEntity<IdentityService.UserView> register(@Valid @RequestBody IdentityService.Registration request) {return ResponseEntity.status(201).body(identity.register(request));}
    @GetMapping("/me") IdentityService.UserView me(@AuthenticationPrincipal TechPrincipal user) {return new IdentityService.UserView(user.id(),user.fullName(),user.getUsername(),user.role());}
}
