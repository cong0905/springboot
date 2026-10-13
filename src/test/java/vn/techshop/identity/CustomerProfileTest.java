package vn.techshop.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CustomerProfileTest {
    @Autowired MockMvc mvc;
    @Autowired IdentityService identity;
    @Autowired UserRepository users;
    @Autowired org.springframework.security.core.session.SessionRegistry sessions;
    String email(){return "profile-"+java.util.UUID.randomUUID()+"@test.local";}
    MockHttpSession login(String email,String password) throws Exception {
        return (MockHttpSession)mvc.perform(post("/login").servletPath("/login").with(csrf()).param("username",email).param("password",password))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","/products")).andReturn().getRequest().getSession(false);
    }
    @Test void updateOwnProfileRefreshesPrincipalAndRejectsStaleInvalidAndMissingCsrf() throws Exception {
        String mail=email();Long id=identity.register(new IdentityService.Registration("Profile Owner",mail,"OriginalPass2026!")).id();
        var session=login(mail,"OriginalPass2026!");
        mvc.perform(get("/profile").session(session)).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Hồ sơ cá nhân")));
        String body="{\"fullName\":\"Updated Owner\",\"phone\":\"0912345678\",\"birthDate\":\"2000-01-02\",\"gender\":\"OTHER\",\"expectedVersion\":0}";
        mvc.perform(patch("/api/v1/me/profile").session(session).contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(patch("/api/v1/me/profile").session(session).with(csrf()).contentType("application/json").content(body))
            .andExpect(status().isOk()).andExpect(jsonPath("$.email").value(mail)).andExpect(jsonPath("$.version").value(1));
        mvc.perform(get("/api/v1/me").session(session)).andExpect(jsonPath("$.fullName").value("Updated Owner"));
        mvc.perform(get("/api/v1/me/profile").session(session)).andExpect(jsonPath("$.passwordHash").doesNotExist());
        mvc.perform(patch("/api/v1/me/profile").session(session).with(csrf()).contentType("application/json").content(body)).andExpect(status().isConflict());
        mvc.perform(patch("/api/v1/me/profile").session(session).with(csrf()).contentType("application/json").content(body.replace("0912345678","not-a-phone").replace("Version\":0","Version\":1"))).andExpect(status().isBadRequest());
        assertThat(users.findById(id).orElseThrow().getRole()).isEqualTo("CUSTOMER");
        mvc.perform(get("/api/v1/me/profile")).andExpect(status().isUnauthorized());
    }
    @Test void passwordChangeRequiresCurrentPasswordAndExpiresAllLoggedInSessions() throws Exception {
        String mail=email();identity.register(new IdentityService.Registration("Password Owner",mail,"OriginalPass2026!"));
        var first=login(mail,"OriginalPass2026!");var second=login(mail,"OriginalPass2026!");
        var principal=(TechPrincipal)identity.loadUserByUsername(mail);
        assertThat(sessions.getAllSessions(principal,false)).hasSize(2);
        mvc.perform(post("/api/v1/me/password").session(first).with(csrf()).contentType("application/json").content("{\"currentPassword\":\"incorrect\",\"newPassword\":\"ChangedPass2026!\"}")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/me/password").session(first).with(csrf()).contentType("application/json").content("{\"currentPassword\":\"OriginalPass2026!\",\"newPassword\":\"ChangedPass2026!\"}")).andExpect(status().isNoContent());
        assertThat(first.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/me/profile").session(second)).andExpect(status().isUnauthorized());
        mvc.perform(post("/login").servletPath("/login").with(csrf()).param("username",mail).param("password","OriginalPass2026!"))
            .andExpect(header().string("Location","/login?error"));
        mvc.perform(get("/api/v1/me/profile").session(login(mail,"ChangedPass2026!"))).andExpect(status().isOk());
    }
}
