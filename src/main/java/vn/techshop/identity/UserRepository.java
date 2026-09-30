package vn.techshop.identity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface UserRepository extends JpaRepository<AppUser, Long> {
    java.util.Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
