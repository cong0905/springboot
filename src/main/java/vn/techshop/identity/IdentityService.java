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
    public record Profile(Long id,String fullName,String email,String phone,java.time.LocalDate birthDate,String gender,Long version,String avatarPath) {}
    public record ProfileInput(@NotBlank @Size(min=2,max=100) String fullName,@Pattern(regexp="0[0-9]{9}|^$") String phone,
        @PastOrPresent java.time.LocalDate birthDate,String gender,@NotNull Long expectedVersion) {}
    public record PasswordInput(@NotBlank String currentPassword,@NotBlank @Size(min=10,max=64) String newPassword) {}
    public record Registration(@NotBlank @Size(min=2,max=100) String fullName, @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(min=10,max=64) String password) {}
    public record UserView(Long id, String fullName, String email, String role, java.time.Instant createdAt) {
        public UserView(Long id, String fullName, String email, String role) {
            this(id, fullName, email, role, null);
        }
    }
    private final UserRepository users;
    private final CartRepository carts;
    private final PasswordEncoder encoder;
    private final Clock clock;
    public IdentityService(UserRepository users, CartRepository carts, PasswordEncoder encoder, Clock clock) {
        this.users=users; this.carts=carts; this.encoder=encoder; this.clock=clock;
    }
    public UserDetails loadUserByUsername(String email) {
        return users.findByEmail(email.strip().toLowerCase(java.util.Locale.ROOT)).filter(u->!u.isGuestAccount()).map(TechPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Thông tin đăng nhập không hợp lệ."));
    }
    @Transactional(readOnly=true) public Profile profile(Long id) {return profileView(users.findById(id).orElseThrow(BusinessException::missing));}
    @Transactional public Profile updateProfile(Long id,ProfileInput input) {
        var user=users.lockById(id).orElseThrow(BusinessException::missing);
        Rules.version(user.getVersion(),input.expectedVersion(),"PROFILE_CHANGED");
        String phone=Rules.optional(input.phone(),10);
        if(phone!=null && !phone.matches("0[0-9]{9}"))throw BusinessException.invalid("Số điện thoại không hợp lệ.");
        var date=input.birthDate();
        if(date!=null && (date.isAfter(java.time.LocalDate.now(clock))||date.isBefore(java.time.LocalDate.of(1900,1,1))))
            throw BusinessException.invalid("Ngày sinh không hợp lệ.");
        String gender=input.gender()==null?"UNSPECIFIED":input.gender();
        if(!java.util.Set.of("UNSPECIFIED","MALE","FEMALE","OTHER").contains(gender))throw BusinessException.invalid("Giới tính không hợp lệ.");
        user.setFullName(Rules.text(input.fullName(),2,100));user.setPhone(phone);user.setBirthDate(date);user.setGender(gender);
        return profileView(users.saveAndFlush(user));
    }
    @Transactional public void changePassword(Long id,PasswordInput input) {
        var user=users.lockById(id).orElseThrow(BusinessException::missing);
        if(input.currentPassword()==null || !encoder.matches(input.currentPassword(),user.getPasswordHash()))
            throw BusinessException.invalid("Mật khẩu hiện tại không đúng.");
        String password=input.newPassword();
        if(password==null || password.length()<10 || password.length()>64 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw BusinessException.invalid("Mật khẩu cần 10–64 ký tự và không quá 72 byte UTF-8.");
        if(encoder.matches(password,user.getPasswordHash()))throw BusinessException.invalid("Mật khẩu mới phải khác mật khẩu hiện tại.");
        user.setPasswordHash(encoder.encode(password));users.saveAndFlush(user);
    }
    private static Profile profileView(AppUser u){return new Profile(u.getId(),u.getFullName(),u.getEmail(),u.getPhone(),u.getBirthDate(),u.getGender(),u.getVersion(),u.getAvatarPath());}
    @Transactional
    public UserView register(Registration request) { return create(request, "CUSTOMER"); }
    @Transactional public Long guest(){
        String nonce=java.util.UUID.randomUUID().toString();var view=create(new Registration("Khách mua hàng",nonce+"@guest-session.invalid",java.util.UUID.randomUUID()+"Guest!"),"CUSTOMER");
        var user=users.findById(view.id()).orElseThrow();user.setGuestAccount(true);users.saveAndFlush(user);return user.getId();
    }
    @Transactional
    public UserView bootstrapAdmin(Registration request) { return create(request, "ADMIN"); }
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserView> listUsers(String query, int page, int size) {
        var pageable = org.springframework.data.domain.PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        if (query != null && !query.isBlank()) {
            String q = query.trim();
            return users.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(q, q, pageable).map(IdentityService::view);
        }
        return users.findAll(pageable).map(IdentityService::view);
    }
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
    public static UserView view(AppUser u) { return new UserView(u.getId(),u.getFullName(),u.getEmail(),u.getRole(),u.getCreatedAt()); }
}
