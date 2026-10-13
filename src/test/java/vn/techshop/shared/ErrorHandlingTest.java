package vn.techshop.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.techshop.identity.IdentityService;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.identity.UserRepository;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorHandlingTest {

    @Autowired MockMvc mvc;
    @Autowired IdentityService identity;
    @Autowired UserRepository users;

    TechPrincipal adminPrincipal;

    @BeforeEach
    void setUp() {
        var key = UUID.randomUUID().toString();
        var adminId = identity.bootstrapAdmin(new IdentityService.Registration("Admin", key + "admin@test.local", "DemoAdmin2026!")).id();
        adminPrincipal = new TechPrincipal(users.findById(adminId).orElseThrow());
    }

    @Test
    void missingRepairTicketReturns404NotFoundNot500() throws Exception {
        mvc.perform(post("/api/v1/admin/repairs/999999/transitions")
                .with(csrf())
                .with(user(adminPrincipal))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"status": "REPAIRING"}
                """))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void missingRequiredParameterReturns400BadRequest() throws Exception {
        // /api/v1/products/compare requires parameter 'ids'
        mvc.perform(get("/api/v1/products/compare"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidPathVariableTypeReturns400BadRequest() throws Exception {
        mvc.perform(get("/api/v1/admin/products/not-a-number")
                .with(user(adminPrincipal)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }
}
