package vn.techshop.installment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.UUID;
import vn.techshop.order.OrderRepository;
import vn.techshop.order.PurchaseOrder;
import vn.techshop.shared.BusinessException;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InstallmentTransparencyAndSafetyTest {
    @Autowired MockMvc mvc;
    @Autowired InstallmentService installmentService;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired OrderRepository orders;

    @Autowired vn.techshop.identity.IdentityService identityService;

    @Test
    void installmentCalculationIsTransparentAndAccurate() throws Exception {
        // Đảm bảo có gói trả góp test với phí 2.5%
        jdbc.update("delete from installment_plans where provider='Test Partner 6M'");
        jdbc.update("insert into installment_plans(provider, months, fee_percent, is_active) values('Test Partner 6M', 6, 2.50, true)");
        Long planId = jdbc.queryForObject("select id from installment_plans where provider='Test Partner 6M'", Long.class);

        BigDecimal principal = new BigDecimal("12000000"); // 12 triệu
        // Fee = 12,000,000 * 2.5% = 300,000
        // Total = 12,300,000
        // Base monthly = 12,300,000 / 6 = 2,050,000
        var schedule = installmentService.quote(planId, principal);
        assertThat(schedule.principal()).isEqualByComparingTo(principal);
        assertThat(schedule.fee()).isEqualByComparingTo(new BigDecimal("300000"));
        assertThat(schedule.total()).isEqualByComparingTo(new BigDecimal("12300000"));
        assertThat(schedule.monthlyPayments()).hasSize(6);
        for (var p : schedule.monthlyPayments()) {
            assertThat(p).isEqualByComparingTo(new BigDecimal("2050000"));
        }

        // Gọi API storefront để kiểm tra endpoint công khai trả về đúng
        mvc.perform(get("/api/v1/installment/quote")
                .param("planId", planId.toString())
                .param("amount", "12000000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principal").value(12000000))
                .andExpect(jsonPath("$.fee").value(300000))
                .andExpect(jsonPath("$.total").value(12300000))
                .andExpect(jsonPath("$.monthlyPayments").isArray());
    }

    @Test
    void productionRejectsDemoInstallmentApprovalAndFiltersDemoPlans() {
        // Tạo môi trường mock production
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("production");

        InstallmentService prodService = new InstallmentService(
            jdbc,
            orders,
            null,
            prodEnv
        );

        // Đảm bảo có cả gói demo và gói đối tác thật
        jdbc.update("delete from installment_plans where provider in ('Demo Partner 3M', 'Real Bank 12M')");
        jdbc.update("insert into installment_plans(provider, months, fee_percent, is_active) values('Demo Partner 3M', 3, 0.00, true)");
        jdbc.update("insert into installment_plans(provider, months, fee_percent, is_active) values('Real Bank 12M', 12, 1.50, true)");

        // Ở production, plans() chỉ trả về gói thật, loại bỏ gói demo
        var activePlans = prodService.plans();
        assertThat(activePlans).noneMatch(p -> p.provider().toLowerCase().contains("demo"));
        assertThat(activePlans).anyMatch(p -> p.provider().equals("Real Bank 12M"));

        // Tạo đơn hàng giả lập hồ sơ demo
        Long demoPlanId = jdbc.queryForObject("select id from installment_plans where provider='Demo Partner 3M'", Long.class);
        var user = identityService.register(new vn.techshop.identity.IdentityService.Registration("Test Buyer", "buyer-" + UUID.randomUUID() + "@test.local", "StrongPass123!"));
        var order = new PurchaseOrder();
        order.setUserId(user.id());
        order.setCheckoutKey(UUID.randomUUID().toString());
        order.setRequestHash("a".repeat(64));
        order.setStatus("PENDING");
        order.setPaymentStatus("UNPAID");
        order.setPaymentMethod("INSTALLMENT");
        order.setSubtotal(new BigDecimal("5000000"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setShippingFee(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("5000000"));
        order.setCurrency("VND");
        order.setRecipientName("Test Buyer");
        order.setPhone("0901234567");
        order.setAddress("Test Address");
        order.setCodCollected(false);
        order.setCreatedAt(java.time.Instant.now());
        order.setUpdatedAt(java.time.Instant.now());
        var savedOrder = orders.saveAndFlush(order);

        jdbc.update("insert into installment_applications(order_id, plan_id, status, financed_amount) values(?,?,?,?)",
            savedOrder.getId(), demoPlanId, "PENDING", new BigDecimal("5000000"));

        // Ở production, cố tình duyệt hồ sơ demo phải ném BusinessException
        assertThatThrownBy(() -> prodService.decision(savedOrder.getId(), "APPROVED"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("Hồ sơ trả góp demo không được phép phê duyệt trong môi trường production.");
    }
}
