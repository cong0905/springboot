package vn.techshop.identity;

import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.cart.*;
import vn.techshop.shared.*;

@Service
public class IdentityService implements UserDetailsService {
    public record Registration(@NotBlank @Size(min=2,max=100) String fullName, @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=10,max=64) String password) {}
    public record UserView(Long id, String fullName, String email, String role) {}
    private final UserRepository users;
    private final CartRepository carts;
    private final PasswordEncoder encoder;
    private final Clock clock;
    public IdentityService(UserRepository users, CartRepository carts, PasswordEncoder encoder, Clock clock) {
        this.users=users; this.carts=carts; this.encoder=encoder; this.clock=clock;
    }
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT)).map(TechPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ."));
    }
    @Transactional
    public UserView register(Registration request) { return create(request, "CUSTOMER"); }
    @Transactional
    public UserView bootstrapAdmin(Registration request) { return create(request, "ADMIN"); }
    private UserView create(Registration request, String role) {
        String email=Rules.email(request.email());
        if (users.existsByEmail(email)) throw BusinessException.conflict("EMAIL_EXISTS", "Email đã được sử dụng.");
        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) throw BusinessException.invalid("Email không hợp lệ.");
        if (request.password()==null || request.password().length()<10 || request.password().length()>64 || request.password().getBytes(StandardCharsets.UTF_8).length>72)
            throw BusinessException.invalid("Mật khẩu cần 10–64 ký tự và không quá 72 byte UTF-8.");
        var user=new AppUser(); user.setFullName(Rules.text(request.fullName(),2,100)); user.setEmail(email);
        user.setPasswordHash(encoder.encode(request.password())); user.setRole(role); user.setCreatedAt(clock.instant());
        users.saveAndFlush(user);
        if ("CUSTOMER".equals(role)) { var cart=new Cart(); cart.setUserId(user.getId()); cart.setCreatedAt(clock.instant()); cart.setUpdatedAt(clock.instant()); carts.save(cart); }
        return view(user);
    }
    public static UserView view(AppUser u) { return new UserView(u.getId(),u.getFullName(),u.getEmail(),u.getRole()); }
}
