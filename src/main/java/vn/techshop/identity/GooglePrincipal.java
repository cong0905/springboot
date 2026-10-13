package vn.techshop.identity;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.*;
import java.util.Map;
public class GooglePrincipal extends TechPrincipal implements OidcUser {
 private final OidcUser source;
 public GooglePrincipal(AppUser user,OidcUser source){super(user);eraseCredentials();this.source=source;}
 public Map<String,Object> getAttributes(){return source.getAttributes();}
 public Map<String,Object> getClaims(){return source.getClaims();}
 public OidcUserInfo getUserInfo(){return source.getUserInfo();}
 public OidcIdToken getIdToken(){return source.getIdToken();}
 public String getName(){return source.getSubject();}
}
