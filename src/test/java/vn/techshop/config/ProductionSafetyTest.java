package vn.techshop.config;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import static org.assertj.core.api.Assertions.*;
class ProductionSafetyTest {
    MockEnvironment safe(){return new MockEnvironment().withProperty("server.servlet.session.cookie.secure","true");}
    @Test void rejectsDemoMockInsecureCookiesAndBootstrap() {
        for(String key:new String[]{"app.demo","app.payment.mock.enabled","app.bootstrap-admin.enabled"})
            assertThatThrownBy(()->new ProductionSafety(safe().withProperty(key,"true")).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->new ProductionSafety(new MockEnvironment()).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
        var mixed=safe();mixed.setActiveProfiles("production","demo");
        assertThatThrownBy(()->new ProductionSafety(mixed).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
    }
    @Test void rejectsLocalhostOrIncompleteGatewayButAllowsUnconfiguredCod() {
        assertThatCode(()->new ProductionSafety(safe()).afterPropertiesSet()).doesNotThrowAnyException();
        var env=safe().withProperty("app.payment.vnpay.tmn-code","shop");
        assertThatThrownBy(()->new ProductionSafety(env).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
        env.withProperty("app.payment.vnpay.hash-secret","test-only").withProperty("app.payment.vnpay.url","https://sandbox.vnpayment.vn/paymentv2/vpcpay.html").withProperty("app.payment.vnpay.return-url","http://localhost:8080/payment/vnpay/return");
        assertThatThrownBy(()->new ProductionSafety(env).afterPropertiesSet()).isInstanceOf(IllegalStateException.class);
        env.withProperty("app.payment.vnpay.return-url","https://shop.example/payment/vnpay/return");
        assertThatCode(()->new ProductionSafety(env).afterPropertiesSet()).doesNotThrowAnyException();
    }
    @Test void forwardedHeaderIgnoredUnlessImmediatePeerTrustedAndInvalidHopFailsClosed() {
        var request=new MockHttpServletRequest();request.setRemoteAddr("192.0.2.2");request.addHeader("X-Forwarded-For","198.51.100.9");
        assertThat(new ClientIpResolver("").resolve(request)).isEqualTo("192.0.2.2");
        assertThat(new ClientIpResolver("192.0.2.2").resolve(request)).isEqualTo("198.51.100.9");
        request.removeHeader("X-Forwarded-For");request.addHeader("X-Forwarded-For","evil.example");
        assertThat(new ClientIpResolver("192.0.2.2").resolve(request)).isEqualTo("192.0.2.2");
    }
}
