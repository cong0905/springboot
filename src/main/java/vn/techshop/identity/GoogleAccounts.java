package vn.techshop.identity;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class GoogleAccounts {
 private final JdbcTemplate jdbc;private final UserRepository users;private final IdentityService identity;
 public GoogleAccounts(JdbcTemplate jdbc,UserRepository users,IdentityService identity){this.jdbc=jdbc;this.users=users;this.identity=identity;}
 @Transactional public GooglePrincipal resolve(OidcUser claims,Long linkUserId){
  if(!Boolean.TRUE.equals(claims.getEmailVerified())||claims.getSubject()==null||claims.getSubject().length()>255||claims.getEmail()==null)throw failure("GOOGLE_IDENTITY_INVALID");
  String email=claims.getEmail().strip().toLowerCase(Locale.ROOT);
  var known=jdbc.queryForList("select user_id from google_identities where subject_id=?",Long.class,claims.getSubject());
  if(!known.isEmpty()){
   if(linkUserId!=null&&!known.getFirst().equals(linkUserId))throw failure("GOOGLE_ALREADY_LINKED");
   var user=users.findById(known.getFirst()).orElseThrow(()->failure("GOOGLE_IDENTITY_INVALID"));
   if(!"CUSTOMER".equals(user.getRole()))throw failure("GOOGLE_ROLE_INVALID");return new GooglePrincipal(user,claims);
  }
  AppUser user;
  if(linkUserId!=null){user=users.lockById(linkUserId).orElseThrow(()->failure("GOOGLE_LINK_REQUIRED"));if(!user.getEmail().equals(email)||!"CUSTOMER".equals(user.getRole()))throw failure("GOOGLE_LINK_REQUIRED");}
  else{
   // Email collisions never silently link a password/admin account.
   if(users.existsByEmail(email))throw failure("GOOGLE_LINK_REQUIRED");
   String name=claims.getFullName();if(name==null||name.strip().length()<2)name="Google Customer";if(name.length()>100)name=name.substring(0,100);
   Long id=identity.register(new IdentityService.Registration(name,email,UUID.randomUUID()+"Strong!" )).id();user=users.findById(id).orElseThrow();
  }
  if(jdbc.queryForObject("select count(*) from google_identities where user_id=?",Integer.class,user.getId())>0)throw failure("GOOGLE_ALREADY_LINKED");
  jdbc.update("insert into google_identities(subject_id,user_id) values(?,?)",claims.getSubject(),user.getId());return new GooglePrincipal(user,claims);
 }
 private static OAuth2AuthenticationException failure(String code){return new OAuth2AuthenticationException(new OAuth2Error(code));}
}
