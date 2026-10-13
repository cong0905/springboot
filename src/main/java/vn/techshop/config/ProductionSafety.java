package vn.techshop.config;

import java.net.URI;
import java.util.Arrays;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component @Profile("production")
public class ProductionSafety implements InitializingBean {
    private final Environment env;
    public ProductionSafety(Environment env){this.env=env;}
    public void afterPropertiesSet() {
        if(Arrays.stream(env.getActiveProfiles()).anyMatch(p->p.equals("demo")||p.equals("test"))
            || bool("app.demo") || bool("app.payment.mock.enabled")) fail("Demo/mock không được bật trong production.");
        if(!bool("server.servlet.session.cookie.secure"))fail("Production yêu cầu cookie Secure và HTTPS.");
        if(!"NONE".equalsIgnoreCase(env.getProperty("server.forward-headers-strategy","NONE")))
            fail("Production chỉ chấp nhận proxy đã cấu hình qua app.security.trusted-proxies.");
        if(bool("app.bootstrap-admin.enabled"))fail("Tắt bootstrap-admin trong production; provisioning thực hiện riêng.");
        if(bool("app.installment.allow-demo"))fail("Cấm bật app.installment.allow-demo trong production.");
        if(bool("app.mail.enabled")||Arrays.asList(env.getActiveProfiles()).contains("google"))validateHttps(env.getProperty("app.public-url",""));
        if(bool("app.mail.enabled")&&(!env.getProperty("spring.mail.properties.mail.smtp.starttls.required",Boolean.class,false)||env.getProperty("app.mail.from","").isBlank()))fail("Email production cần STARTTLS bắt buộc và địa chỉ gửi.");
        String code=env.getProperty("app.payment.vnpay.tmn-code",""),secret=env.getProperty("app.payment.vnpay.hash-secret","");
        if(code.isBlank()!=secret.isBlank())fail("VNPay cần đủ merchant và secret.");
        if(!code.isBlank()) {
            validateHttps(env.getProperty("app.payment.vnpay.url",""));
            validateHttps(env.getProperty("app.payment.vnpay.return-url",""));
        }
    }
    private boolean bool(String key){return env.getProperty(key,Boolean.class,false);}
    private static void validateHttps(String raw) {
        try { var uri=URI.create(raw);String host=uri.getHost();
            if(!"https".equals(uri.getScheme()) || host==null || host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1") || uri.getUserInfo()!=null)
                fail("Production yêu cầu URL thanh toán HTTPS public.");
        } catch(IllegalArgumentException ex){fail("URL thanh toán không hợp lệ.");}
    }
    private static void fail(String message){throw new IllegalStateException(message);}
}
