package vn.techshop.guest;
import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import vn.techshop.cart.*;
import vn.techshop.identity.*;
import vn.techshop.order.*;
import vn.techshop.shared.*;
@Service
public class GuestService {
 private final IdentityService identity;private final UserRepository users;private final RequestLimits limits;private final vn.techshop.config.ClientIpResolver ips;private final JdbcTemplate jdbc;private final Clock clock;private final OrderService orders;
 public GuestService(IdentityService identity,UserRepository users,RequestLimits limits,vn.techshop.config.ClientIpResolver ips,JdbcTemplate jdbc,Clock clock,OrderService orders){this.identity=identity;this.users=users;this.limits=limits;this.ips=ips;this.jdbc=jdbc;this.clock=clock;this.orders=orders;}
 public Long current(HttpServletRequest request){var session=request.getSession(false);var id=session==null?null:(Long)session.getAttribute("guestOwner");return id!=null&&users.findById(id).filter(AppUser::isGuestAccount).isPresent()?id:null;}
 public Long require(HttpServletRequest request){var id=current(request);if(id==null)throw BusinessException.missing();return id;}
 public Long create(HttpServletRequest request){var id=current(request);if(id!=null)return id;limits.consume("guest-create:"+ips.resolve(request),5,Duration.ofHours(1));id=identity.guest();request.getSession().setAttribute("guestOwner",id);request.changeSessionId();return id;}
 @Transactional public String proof(Long orderId){
  if(jdbc.queryForObject("select count(*) from orders o join users u on u.id=o.user_id where o.id=? and u.guest_account=true",Integer.class,orderId)!=1)throw BusinessException.missing();
  limits.consume("guest-proof:"+orderId,20,Duration.ofHours(1));
  byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);String raw=HexFormat.of().formatHex(bytes);
  jdbc.update("delete from guest_order_proofs where expires_at<=?",java.sql.Timestamp.from(clock.instant()));
  jdbc.update("insert into guest_order_proofs(token_hash,order_id,expires_at) values(?,?,?)",Pricing.sha256(raw),orderId,java.sql.Timestamp.from(clock.instant().plus(Duration.ofDays(30))));return raw;
 }
 public Long owner(HttpServletRequest request,Long orderId){
  Long current=current(request);if(current!=null&&jdbc.queryForObject("select count(*) from orders where id=? and user_id=?",Integer.class,orderId,current)>0)return current;
  String token=Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new jakarta.servlet.http.Cookie[0])).filter(c->c.getName().equals("guest_order_"+orderId)).map(jakarta.servlet.http.Cookie::getValue).findFirst().orElse(null);
  if(token==null||!token.matches("[0-9a-f]{64}"))throw BusinessException.missing();
  var owners=jdbc.queryForList("select o.user_id from guest_order_proofs p join orders o on o.id=p.order_id join users u on u.id=o.user_id where p.token_hash=? and p.order_id=? and p.expires_at>? and u.guest_account=true",Long.class,Pricing.sha256(token),orderId,java.sql.Timestamp.from(clock.instant()));
  return owners.stream().findFirst().orElseThrow(BusinessException::missing);
 }
}
