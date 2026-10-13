package vn.techshop.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import vn.techshop.identity.*;

@Component
@ConditionalOnProperty(name="app.bootstrap-admin.enabled",havingValue="true")
public class AdminBootstrap implements CommandLineRunner {
    private final IdentityService identity;private final UserRepository users;private final String email;private final String password;
    public AdminBootstrap(IdentityService identity,UserRepository users,@Value("${app.bootstrap-admin.email}") String email,@Value("${app.bootstrap-admin.password}") String password) {this.identity=identity;this.users=users;this.email=email;this.password=password;}
    public void run(String... args) {
        var existing=users.findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT));
        if(existing.isPresent()) {if(!"ADMIN".equals(existing.get().getRole()))throw new IllegalStateException("Bootstrap email belongs to a non-admin account");return;}
        identity.bootstrapAdmin(new IdentityService.Registration("Quản trị viên",email,password));
    }
}
