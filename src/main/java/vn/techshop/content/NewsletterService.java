package vn.techshop.content;
import java.util.*;
import java.time.*;
import java.sql.Timestamp;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.*;
import vn.techshop.cart.Pricing;
@Service
public class NewsletterService {
 public record OptInEmail(String email,String confirmToken,String cancelToken){}
 private final JdbcTemplate jdbc;private final Clock clock;private final ApplicationEventPublisher events;private final vn.techshop.identity.RecoveryService mail;
 public NewsletterService(JdbcTemplate jdbc,Clock clock,ApplicationEventPublisher events,vn.techshop.identity.RecoveryService mail){this.jdbc=jdbc;this.clock=clock;this.events=events;this.mail=mail;}
 @Transactional public void subscribe(String raw){
  if(!mail.enabled())throw new BusinessException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,"EMAIL_NOT_CONFIGURED","Email chưa được cấu hình.");
  String email=Rules.email(raw);var rows=jdbc.queryForList("select status from newsletter_subscriptions where email=? for update",String.class,email);if(!rows.isEmpty()&&"SUBSCRIBED".equals(rows.getFirst()))return;
  String confirm=token(),cancel=token();var now=Timestamp.from(clock.instant());var expiry=Timestamp.from(clock.instant().plus(Duration.ofHours(24)));
  if(rows.isEmpty())jdbc.update("insert into newsletter_subscriptions(email,confirm_hash,cancel_hash,expires_at,updated_at) values(?,?,?,?,?)",email,Pricing.sha256(confirm),Pricing.sha256(cancel),expiry,now);
  else jdbc.update("update newsletter_subscriptions set status='PENDING',confirm_hash=?,cancel_hash=?,expires_at=?,updated_at=? where email=?",Pricing.sha256(confirm),Pricing.sha256(cancel),expiry,now,email);
  events.publishEvent(new OptInEmail(email,confirm,cancel));
 }
 @Transactional public void confirm(String token){
  valid(token);String hash=Pricing.sha256(token);var rows=jdbc.queryForList("select id,expires_at from newsletter_subscriptions where confirm_hash=? and status='PENDING' for update",hash);
  if(rows.isEmpty()||!((Timestamp)rows.getFirst().get("expires_at")).toInstant().isAfter(clock.instant()))throw BusinessException.invalid("Liên kết không hợp lệ hoặc đã hết hạn.");
  jdbc.update("update newsletter_subscriptions set status='SUBSCRIBED',confirm_hash=null,confirmed_at=?,updated_at=? where id=?",Timestamp.from(clock.instant()),Timestamp.from(clock.instant()),rows.getFirst().get("id"));
 }
 @Transactional public void unsubscribe(String token){valid(token);jdbc.update("update newsletter_subscriptions set status='UNSUBSCRIBED',confirm_hash=null,cancel_hash=null,updated_at=? where cancel_hash=?",Timestamp.from(clock.instant()),Pricing.sha256(token));}
 private static String token(){byte[] bytes=new byte[32];new java.security.SecureRandom().nextBytes(bytes);return HexFormat.of().formatHex(bytes);}
 private static void valid(String token){if(token==null||!token.matches("[0-9a-f]{64}"))throw BusinessException.invalid("Liên kết không hợp lệ.");}
}
