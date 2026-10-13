package vn.techshop.identity;
import java.util.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.core.oidc.*;
import org.springframework.security.oauth2.core.oidc.user.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest(properties={"app.mail.enabled=true","app.mail.from=test@example.invalid"}) @AutoConfigureMockMvc @ActiveProfiles("test") @Import(RecoveryAndGoogleTest.Mailbox.class)
class RecoveryAndGoogleTest {
 static java.util.concurrent.BlockingQueue<SimpleMailMessage> messages=new java.util.concurrent.LinkedBlockingQueue<>();
 @TestConfiguration static class Mailbox{@Bean JavaMailSender sender(){return new JavaMailSenderImpl(){@Override public void send(SimpleMailMessage message){messages.add(message);}};}}
 @Autowired IdentityService identity;@Autowired RecoveryService recovery;@Autowired GoogleAccounts google;@Autowired UserRepository users;@Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;
 @Autowired vn.techshop.content.NewsletterService newsletter;
 @Test void newsletterRequiresConfirmationAndSupportsIndependentUnsubscribe() throws Exception {
  messages.clear();String email=email();newsletter.subscribe(email);var message=messages.poll(5,java.util.concurrent.TimeUnit.SECONDS);assertThat(message).isNotNull();
  String[] tokens=message.getText().split("#token=");String confirm=tokens[1].split("\\s")[0],cancel=tokens[2].split("\\s")[0];
  assertThat(jdbc.queryForObject("select status from newsletter_subscriptions where email=?",String.class,email)).isEqualTo("PENDING");
  newsletter.confirm(confirm);assertThatThrownBy(()->newsletter.confirm(confirm)).isInstanceOf(vn.techshop.shared.BusinessException.class);
  newsletter.unsubscribe(cancel);newsletter.unsubscribe(cancel);
  assertThat(jdbc.queryForObject("select status from newsletter_subscriptions where email=?",String.class,email)).isEqualTo("UNSUBSCRIBED");
  mvc.perform(post("/api/v1/newsletter").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\",\"consent\":false}")).andExpect(status().isBadRequest());
 }
 String email(){return UUID.randomUUID()+"@example.invalid";}
 @Test void recoveryStoresOnlyHashAndConsumesTokenOnceWithExpiryAndNoAccountEnumeration() throws Exception {
  messages.clear();String email=email();Long id=identity.register(new IdentityService.Registration("Recovery Owner",email,"OriginalPass2026!")).id();
  mvc.perform(post("/api/v1/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\""+email+"\"}")).andExpect(status().isAccepted()).andExpect(jsonPath("$.token").doesNotExist());
  var mail=messages.poll(5,java.util.concurrent.TimeUnit.SECONDS);assertThat(mail).isNotNull();String token=mail.getText().split("#token=")[1].split("\\s")[0];
  assertThat(jdbc.queryForObject("select token_hash from password_reset_tokens where user_id=?",String.class,id)).isEqualTo(vn.techshop.cart.Pricing.sha256(token)).isNotEqualTo(token);
  recovery.reset(token,"ChangedPass2026!");assertThatThrownBy(()->recovery.reset(token,"AnotherPass2026!")).isInstanceOf(vn.techshop.shared.BusinessException.class);
  mvc.perform(post("/api/v1/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\""+email()+"\"}")).andExpect(status().isAccepted()).andExpect(jsonPath("$.token").doesNotExist());
  messages.clear();recovery.request(email);var expired=messages.poll(5,java.util.concurrent.TimeUnit.SECONDS);String expiredToken=expired.getText().split("#token=")[1].split("\\s")[0];
  jdbc.update("update password_reset_tokens set expires_at=? where token_hash=?",java.sql.Timestamp.from(Instant.now().minusSeconds(60)),vn.techshop.cart.Pricing.sha256(expiredToken));
  assertThatThrownBy(()->recovery.reset(expiredToken,"AnotherPass2026!")).isInstanceOf(vn.techshop.shared.BusinessException.class);
  mvc.perform(post("/api/v1/auth/reset-password").contentType("application/json").content("{}")).andExpect(status().isForbidden());
 }
 OidcUser claims(String subject,String email,boolean verified){var data=new HashMap<String,Object>();data.put("sub",subject);data.put("email",email);data.put("email_verified",verified);data.put("name","Google Owner");return new DefaultOidcUser(List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("OIDC_USER")),new OidcIdToken("test-not-a-real-token",Instant.now(),Instant.now().plusSeconds(300),data));}
 @Test void googleRequiresVerifiedEmailAndPasswordProofForExistingAccount(){
  String mail=email();Long id=identity.register(new IdentityService.Registration("Password Owner",mail,"OriginalPass2026!")).id();String subject=UUID.randomUUID().toString();
  assertThatThrownBy(()->google.resolve(claims(subject,mail,false),null)).isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class);
  assertThatThrownBy(()->google.resolve(claims(subject,mail,true),null)).isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class);
  assertThat(google.resolve(claims(subject,mail,true),id).id()).isEqualTo(id);assertThat(google.resolve(claims(subject,mail,true),null).id()).isEqualTo(id);
  assertThat(google.resolve(claims(UUID.randomUUID().toString(),email(),true),null).role()).isEqualTo("CUSTOMER");
 }
}
