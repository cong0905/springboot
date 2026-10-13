package vn.techshop.identity;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.techshop.cart.Pricing;
import vn.techshop.notification.EmailOutboxService;

@Component
public class RecoveryMail {
 private final ObjectProvider<JavaMailSender> sender;
 private final JdbcTemplate jdbc;
 private final String origin,from;
 private final EmailOutboxService outbox;

 public RecoveryMail(ObjectProvider<JavaMailSender> sender,JdbcTemplate jdbc,@Value("${app.public-url:http://localhost:8080}") String origin,@Value("${app.mail.from:}") String from, EmailOutboxService outbox){
     this.sender=sender;this.jdbc=jdbc;this.origin=origin;this.from=from;this.outbox=outbox;
 }

 @org.springframework.scheduling.annotation.Async
 @TransactionalEventListener(phase=org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
 public void deliver(RecoveryService.ResetEmail event){
  String hash=Pricing.sha256(event.token());
  String body="Liên kết dùng một lần, hết hạn sau 15 phút:\n"+origin+"/reset-password#token="+event.token()+"\nNếu bạn không yêu cầu, bỏ qua email này.";
  outbox.enqueue(event.email(), "Đặt lại mật khẩu TechZone", body);
  try{
      var message=new SimpleMailMessage();message.setFrom(from);message.setTo(event.email());message.setSubject("Đặt lại mật khẩu TechZone");message.setText(body);
      var mailSender = sender.getIfAvailable();
      if(mailSender != null) {
          mailSender.send(message);
          jdbc.update("update password_reset_tokens set delivery_status='SENT' where token_hash=?",hash);
      }
  }catch(Exception ex){
      jdbc.update("update password_reset_tokens set delivery_status='QUEUED' where token_hash=?",hash);
      org.slf4j.LoggerFactory.getLogger(RecoveryMail.class).warn("Password recovery delivery queued via outbox: {}",ex.getClass().getSimpleName());
  }
 }
}
