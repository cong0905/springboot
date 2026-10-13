package vn.techshop.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class OrderLookupTest {

    private OrderService orderService;
    private Clock clock;
    private LookupOrderController controller;

    @BeforeEach
    void setUp() {
        orderService = Mockito.mock(OrderService.class);
        clock = Clock.fixed(Instant.parse("2026-10-02T10:00:00Z"), ZoneId.of("UTC"));
        controller = new LookupOrderController(orderService, clock);
    }

    @Test
    @SuppressWarnings("unchecked")
    void fullLimiterCacheRejectsNewIpWithoutLookingUpCustomerData() {
        when(orderService.lookup(anyString(), anyString())).thenReturn(Optional.empty());
        var seed=new MockHttpServletRequest();seed.setRemoteAddr("10.1.1.1");
        controller.searchOrder("TS-00000001","0912345678",seed,new MockHttpServletResponse(),new ConcurrentModel());
        var entries=(java.util.Map<String,Object>)org.springframework.test.util.ReflectionTestUtils.getField(controller,"ipAttempts");
        Object counter=entries.values().iterator().next();
        for(int i=entries.size();i<10_000;i++)entries.put("test-ip-"+i,counter);
        var request=new MockHttpServletRequest();request.setRemoteAddr("10.2.2.2");var response=new MockHttpServletResponse();
        controller.searchOrder("TS-00000002","0912345678",request,response,new ConcurrentModel());
        assertThat(response.getStatus()).isEqualTo(429);
        Mockito.verify(orderService,Mockito.times(1)).lookup(anyString(),anyString());
    }

    @Test
    void testMaskingHelpers() {
        assertThat(OrderService.maskName("Nguyễn Văn An")).isEqualTo("Ng*** An");
        assertThat(OrderService.maskName("Lê Bình")).isEqualTo("Lê*** Bình");
        assertThat(OrderService.maskName("Nam")).isEqualTo("Na***");
        assertThat(OrderService.maskName("An")).isEqualTo("A***");
        assertThat(OrderService.maskName("")).isEqualTo("***");
        assertThat(OrderService.maskName(null)).isEqualTo("***");

        assertThat(OrderService.maskPhone("0987654321")).isEqualTo("09****4321");
        assertThat(OrderService.maskPhone("0312345678")).isEqualTo("03****5678");
        assertThat(OrderService.maskPhone("123")).isEqualTo("***");
        assertThat(OrderService.maskPhone(null)).isEqualTo("***");

        assertThat(OrderService.maskAddress("123 Đường Lê Lợi, Phường Bến Thành, Quận 1, TP. HCM"))
                .isEqualTo("***, Phường Bến Thành, Quận 1, TP. HCM");
        assertThat(OrderService.maskAddress("Không có dấu phẩy")).isEqualTo("***");
        assertThat(OrderService.maskAddress("")).isEqualTo("***");
        assertThat(OrderService.maskAddress(null)).isEqualTo("***");
    }

    @Test
    void testSuccessfulLookupReturnsMaskedData() {
        OrderService.PublicOrderView mockOrder = new OrderService.PublicOrderView(
                "TS-00000001",
                "CONFIRMED",
                BigDecimal.valueOf(1000000),
                BigDecimal.ZERO,
                null,
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(1030000),
                "VND",
                Instant.now(),
                null,
                "Ng*** An",
                "09****4321",
                "***, Quận 1, TP. HCM",
                "COD",
                "UNPAID",
                Collections.emptyList(),
                Collections.emptyList()
        );

        when(orderService.lookup("TS-00000001", "0987654321")).thenReturn(Optional.of(mockOrder));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();

        String view = controller.searchOrder("TS-00000001", "0987654321", request, response, model);

        assertThat(view).isEqualTo("order-lookup");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(model.getAttribute("order")).isEqualTo(mockOrder);
        assertThat(model.getAttribute("error")).isNull();
    }

    @Test
    void testFailedLookupReturnsUniformErrorMessage() {
        when(orderService.lookup(anyString(), anyString())).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.101");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Model model = new ConcurrentModel();

        // 1. Non-existent order
        controller.searchOrder("TS-99999999", "0987654321", request, response, model);
        assertThat(model.getAttribute("error")).isEqualTo("Thông tin tra cứu không chính xác hoặc không tìm thấy đơn hàng.");

        // 2. Empty code
        Model model2 = new ConcurrentModel();
        controller.searchOrder("", "0987654321", request, response, model2);
        assertThat(model2.getAttribute("error")).isEqualTo("Thông tin tra cứu không chính xác hoặc không tìm thấy đơn hàng.");

        // 3. Null phone
        Model model3 = new ConcurrentModel();
        controller.searchOrder("TS-00000001", null, request, response, model3);
        assertThat(model3.getAttribute("error")).isEqualTo("Thông tin tra cứu không chính xác hoặc không tìm thấy đơn hàng.");
    }

    @Test
    void testRateLimitByIpReturns429() {
        when(orderService.lookup(anyString(), anyString())).thenReturn(Optional.empty());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.200");

        // Execute 15 failed requests (up to limit)
        for (int i = 0; i < 15; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            Model model = new ConcurrentModel();
            controller.searchOrder("TS-0000000" + i, "098765432" + (i % 10), request, res, model);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 16th request from same IP should get 429
        MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
        Model blockedModel = new ConcurrentModel();
        controller.searchOrder("TS-00000099", "0987654321", request, blockedResponse, blockedModel);

        assertThat(blockedResponse.getStatus()).isEqualTo(429);
        assertThat(blockedResponse.getHeader("Retry-After")).isEqualTo("300");
        assertThat((String) blockedModel.getAttribute("error")).contains("quá nhiều lần");
    }

    @Test
    void testRateLimitByIdentifierReturns429() {
        when(orderService.lookup(anyString(), anyString())).thenReturn(Optional.empty());

        String orderCode = "TS-00000042";
        String phone = "0912345678";

        // Try 5 failed attempts from different IPs for the same target order/phone
        for (int i = 0; i < 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRemoteAddr("10.0.0." + i);
            MockHttpServletResponse res = new MockHttpServletResponse();
            Model model = new ConcurrentModel();
            controller.searchOrder(orderCode, phone, request, res, model);
            assertThat(res.getStatus()).isEqualTo(200);
        }

        // 6th attempt against the same target from another IP should be rate-limited (chống spray/brute-force)
        MockHttpServletRequest request6 = new MockHttpServletRequest();
        request6.setRemoteAddr("10.0.0.99");
        MockHttpServletResponse res6 = new MockHttpServletResponse();
        Model model6 = new ConcurrentModel();
        controller.searchOrder(orderCode, phone, request6, res6, model6);

        assertThat(res6.getStatus()).isEqualTo(429);
        assertThat(res6.getHeader("Retry-After")).isEqualTo("300");
        assertThat((String) model6.getAttribute("error")).contains("quá nhiều lần cho thông tin này");
    }
}
