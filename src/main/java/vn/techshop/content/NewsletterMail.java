package vn.techshop.content;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.transaction.event.TransactionalEventListener;
import vn.techshop.notification.EmailOutboxService;

@Component
public class NewsletterMail {
 private final ObjectProvider<JavaMailSender> sender;
 private final String origin,from;
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;
 private final EmailOutboxService outbox;

 public NewsletterMail(ObjectProvider<JavaMailSender> sender,@Value("${app.public-url:http://localhost:8080}") String origin,@Value("${app.mail.from:}") String from,org.springframework.jdbc.core.JdbcTemplate jdbc, EmailOutboxService outbox){
     this.sender=sender;this.origin=origin;this.from=from;this.jdbc=jdbc;this.outbox=outbox;
 }

 @org.springframework.scheduling.annotation.Async @TransactionalEventListener(phase=org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
 public void deliver(NewsletterService.OptInEmail event){
  String body="Xác nhận tự nguyện nhận tin, liên kết hết hạn sau 24 giờ:\n"+origin+"/newsletter/confirm#token="+event.confirmToken()+"\nHủy đăng ký:\n"+origin+"/newsletter/unsubscribe#token="+event.cancelToken();
  outbox.enqueue(event.email(), "Xác nhận nhận tin TechZone", body);
  try{
      var message=new SimpleMailMessage();message.setFrom(from);message.setTo(event.email());message.setSubject("Xác nhận nhận tin TechZone");message.setText(body);
      var mailSender = sender.getIfAvailable();
      if(mailSender != null) {
          mailSender.send(message);
      }
  }catch(Exception ex){
      org.slf4j.LoggerFactory.getLogger(NewsletterMail.class).warn("Newsletter delivery queued via outbox: {}",ex.getClass().getSimpleName());
  }
 }
}
