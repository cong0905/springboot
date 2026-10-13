package vn.techshop.identity;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.cart.Pricing;
import vn.techshop.shared.*;
@Service
public class RecoveryService {
 public record ResetEmail(Long userId,String email,String token){}
 private final JdbcTemplate jdbc;private final UserRepository users;private final PasswordEncoder encoder;private final Clock clock;private final ApplicationEventPublisher events;private final SessionRegistry sessions;
 private final boolean enabled;private final ObjectProvider<JavaMailSender> sender;
 public RecoveryService(JdbcTemplate jdbc,UserRepository users,PasswordEncoder encoder,Clock clock,ApplicationEventPublisher events,SessionRegistry sessions,@Value("${app.mail.enabled:false}") boolean enabled,ObjectProvider<JavaMailSender> sender){this.jdbc=jdbc;this.users=users;this.encoder=encoder;this.clock=clock;this.events=events;this.sessions=sessions;this.enabled=enabled;this.sender=sender;}
 public boolean enabled(){return enabled&&sender.getIfAvailable()!=null;}
 @Transactional public void request(String email){
  if(!enabled())throw new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"EMAIL_NOT_CONFIGURED","Chức năng email chưa được cấu hình.");
  var user=users.findByEmail(Rules.email(email)).orElse(null);if(user==null||!"CUSTOMER".equals(user.getRole()))return;
  users.lockById(user.getId()).orElseThrow();
  jdbc.update("update password_reset_tokens set used_at=? where user_id=? and used_at is null",Timestamp.from(clock.instant()),user.getId());
  byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);String raw=HexFormat.of().formatHex(bytes);
  jdbc.update("insert into password_reset_tokens(user_id,token_hash,expires_at) values(?,?,?)",user.getId(),Pricing.sha256(raw),Timestamp.from(clock.instant().plus(Duration.ofMinutes(15))));
  events.publishEvent(new ResetEmail(user.getId(),user.getEmail(),raw));
 }
 @Transactional public void reset(String token,String password){
  if(token==null||!token.matches("[0-9a-f]{64}"))throw BusinessException.invalid("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
  String hash=Pricing.sha256(token);var ids=jdbc.queryForList("select user_id from password_reset_tokens where token_hash=?",Long.class,hash);
  if(ids.isEmpty())throw BusinessException.invalid("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
  var user=users.lockById(ids.getFirst()).orElseThrow(BusinessException::missing);
  var rows=jdbc.queryForList("select expires_at,used_at from password_reset_tokens where token_hash=? for update",hash);var row=rows.getFirst();
  if(row.get("used_at")!=null||!((Timestamp)row.get("expires_at")).toInstant().isAfter(clock.instant()))throw BusinessException.invalid("Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
  if(password==null||password.length()<10||password.length()>64||password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72)throw BusinessException.invalid("Mật khẩu cần 10–64 ký tự và không quá 72 byte UTF-8.");
  user.setPasswordHash(encoder.encode(password));users.saveAndFlush(user);
  jdbc.update("update password_reset_tokens set used_at=? where user_id=? and used_at is null",Timestamp.from(clock.instant()),user.getId());
  sessions.getAllSessions(new TechPrincipal(user),false).forEach(org.springframework.security.core.session.SessionInformation::expireNow);
 }
}
