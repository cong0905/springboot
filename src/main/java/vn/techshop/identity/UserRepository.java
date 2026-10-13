package vn.techshop.identity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface UserRepository extends JpaRepository<AppUser, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from AppUser u where u.id=:id")
    java.util.Optional<AppUser> lockById(@Param("id") Long id);
    java.util.Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    org.springframework.data.domain.Page<AppUser> findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase(String email, String fullName, org.springframework.data.domain.Pageable pageable);
}
