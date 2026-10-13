package vn.techshop.identity;

import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class IdentityApi {
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.security.core.session.SessionRegistry sessions;
    private final IdentityService identity;
    public IdentityApi(IdentityService identity) {this.identity=identity;}
    public record TokenView(String token,String headerName,String parameterName) {}
    @GetMapping("/csrf") ResponseEntity<TokenView> csrf(CsrfToken token) {return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new TokenView(token.getToken(),token.getHeaderName(),token.getParameterName()));}
    @PostMapping("/auth/register") ResponseEntity<IdentityService.UserView> register(@Valid @RequestBody IdentityService.Registration request) {return ResponseEntity.status(201).body(identity.register(request));}
    @GetMapping("/me") IdentityService.UserView me(@AuthenticationPrincipal TechPrincipal user) {return new IdentityService.UserView(user.id(),user.fullName(),user.getUsername(),user.role());}
    @GetMapping("/me/profile") ResponseEntity<IdentityService.Profile> profile(@AuthenticationPrincipal TechPrincipal user){return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(identity.profile(user.id()));}
    @PatchMapping("/me/profile") IdentityService.Profile updateProfile(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody IdentityService.ProfileInput input,
        jakarta.servlet.http.HttpServletRequest request,jakarta.servlet.http.HttpServletResponse response) {
        var result=identity.updateProfile(user.id(),input);
        var fresh=(TechPrincipal)identity.loadUserByUsername(user.getUsername());fresh.eraseCredentials();
        var context=org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(fresh,null,fresh.getAuthorities()));
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        new org.springframework.security.web.context.HttpSessionSecurityContextRepository().saveContext(context,request,response);
        return result;
    }
    @PostMapping("/me/password") ResponseEntity<Void> password(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody IdentityService.PasswordInput input,jakarta.servlet.http.HttpServletRequest request){
        identity.changePassword(user.id(),input);
        sessions.getAllSessions(user,false).forEach(org.springframework.security.core.session.SessionInformation::expireNow);
        var session=request.getSession(false);if(session!=null)session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }
}
