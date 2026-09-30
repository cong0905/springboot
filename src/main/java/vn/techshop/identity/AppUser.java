package vn.techshop.identity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(name = "email", nullable = false, length = 254)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(name = "role", nullable = false, length = 16)
    private String role;
    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AppUser() {}
    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public void setFullName(String value) { this.fullName = value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { this.email = value; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { this.passwordHash = value; }
    public String getRole() { return role; }
    public void setRole(String value) { this.role = value; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }
}
