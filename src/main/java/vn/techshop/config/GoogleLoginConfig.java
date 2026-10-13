package vn.techshop.config;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;
@Configuration @Profile("google")
public class GoogleLoginConfig {
 @Bean ClientRegistrationRepository googleRegistration(@Value("${GOOGLE_CLIENT_ID:}") String id,@Value("${GOOGLE_CLIENT_SECRET:}") String secret,@Value("${app.public-url:http://localhost:8080}") String origin){
  if(id.isBlank()||secret.isBlank())throw new IllegalStateException("Google profile cần client id và client secret.");
  return new InMemoryClientRegistrationRepository(CommonOAuth2Provider.GOOGLE.getBuilder("google").clientId(id).clientSecret(secret).scope("openid","profile","email").redirectUri(origin+"/login/oauth2/code/google").build());
 }
}
