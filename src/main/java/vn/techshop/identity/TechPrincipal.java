package vn.techshop.identity;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class TechPrincipal extends User {
    private final Long id;
    private final String fullName;
    private final String role;
    public TechPrincipal(AppUser user) {
        super(user.getEmail(), user.getPasswordHash(), List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
        id = user.getId(); fullName = user.getFullName(); role = user.getRole();
    }
    public Long id() { return id; }
    public String fullName() { return fullName; }
    public String role() { return role; }
}
