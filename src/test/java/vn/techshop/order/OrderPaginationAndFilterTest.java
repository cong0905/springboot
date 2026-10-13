package vn.techshop.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.techshop.identity.IdentityService;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.identity.UserRepository;

import java.util.Objects;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderPaginationAndFilterTest {

    @Autowired MockMvc mvc;
    @Autowired IdentityService identity;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;

    private Long adminId;
    private Long customerId;

    @BeforeEach
    void setUp() throws Exception {
        try (var connection = Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            String url = connection.getMetaData().getURL();
            if (!url.startsWith("jdbc:h2:mem:techshop-test") && !url.matches("jdbc:mysql://[^/]+/techshop_test(?:\\?.*)?")) {
                throw new IllegalStateException("Unsafe test datasource: " + url);
            }
        }
        String key = UUID.randomUUID().toString().substring(0, 8);
        adminId = identity.bootstrapAdmin(new IdentityService.Registration("Admin Test", "admin" + key + "@test.local", "TestAdmin2026!")).id();
        customerId = identity.register(new IdentityService.Registration("Customer Test", "user" + key + "@test.local", "TestUser2026!")).id();
    }

    private TechPrincipal principal(Long userId) {
        return new TechPrincipal(users.findById(userId).orElseThrow());
    }

    @Test
    void testDateRangeValidationRejectsFromDateAfterToDateOnAdminWeb() throws Exception {
        mvc.perform(get("/admin/orders")
                        .param("fromDate", "2026-10-15")
                        .param("toDate", "2026-10-01")
                        .with(user(principal(adminId))))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Từ ngày không được lớn hơn đến ngày")));
    }

    @Test
    void testDateRangeValidationRejectsFromDateAfterToDateOnAdminApi() throws Exception {
        mvc.perform(get("/api/v1/admin/orders")
                        .param("fromDate", "2026-10-15")
                        .param("toDate", "2026-10-01")
                        .with(user(principal(adminId))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("Từ ngày không được lớn hơn đến ngày")));
    }

    @Test
    void testAdminOrdersFiltersPreservedInHtmlAndResetAvailable() throws Exception {
        mvc.perform(get("/admin/orders")
                        .param("keyword", "TS-999")
                        .param("status", "PENDING")
                        .param("fromDate", "2026-10-01")
                        .param("toDate", "2026-10-15")
                        .param("page", "0")
                        .with(user(principal(adminId))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("TS-999")))
                .andExpect(content().string(containsString("2026-10-01")))
                .andExpect(content().string(containsString("2026-10-15")))
                .andExpect(content().string(containsString("href=\"/admin/orders\" class=\"button secondary\"")))
                .andExpect(content().string(containsString("Xóa lọc")));
    }

    @Test
    void testCustomerOrdersShowsResetFilterWhenStatusProvided() throws Exception {
        mvc.perform(get("/orders")
                        .param("status", "CONFIRMED")
                        .with(user(principal(customerId))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/orders\" class=\"button secondary\"")))
                .andExpect(content().string(containsString("Xóa lọc")));
    }

    @Test
    void testValidDateRangeAccepted() throws Exception {
        mvc.perform(get("/admin/orders")
                        .param("fromDate", "2026-10-01")
                        .param("toDate", "2026-10-15")
                        .with(user(principal(adminId))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/admin/orders")
                        .param("fromDate", "2026-10-01")
                        .param("toDate", "2026-10-15")
                        .with(user(principal(adminId))))
                .andExpect(status().isOk());
    }

    @Test void paginationLinksActuallyPreserveAllFiltersAcrossPages() throws Exception {
        String keyword="Filter-"+UUID.randomUUID();
        for(int i=0;i<21;i++) jdbc.update("insert into orders(user_id,checkout_key,request_hash,status,recipient_name,phone,address,subtotal,shipping_fee,total_amount,created_at,updated_at) values(?,?,?,'PENDING',?,'0900000000','123 Hanoi street',100,0,100,?,?)",
            customerId,UUID.randomUUID().toString(),"a".repeat(64),keyword,
            java.sql.Timestamp.valueOf("2026-10-03 03:00:00"),java.sql.Timestamp.valueOf("2026-10-03 03:00:00"));
        for(int page=0;page<2;page++) {
            String html=mvc.perform(get("/admin/orders").param("keyword",keyword).param("status","PENDING")
                .param("fromDate","2026-10-01").param("toDate","2026-10-15").param("page",String.valueOf(page))
                .with(user(principal(adminId)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            var links=java.util.regex.Pattern.compile("href=\"([^\"]+)\"[^>]*>(?:← Trang trước|Trang sau →)").matcher(html);
            org.assertj.core.api.Assertions.assertThat(links.find()).isTrue();
            org.assertj.core.api.Assertions.assertThat(links.group(1)).contains("keyword="+keyword,"status=PENDING","fromDate=2026-10-01","toDate=2026-10-15");
        }
    }
}
