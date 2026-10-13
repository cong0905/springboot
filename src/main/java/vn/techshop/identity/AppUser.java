package vn.techshop.identity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
public class AppUser {
    @Column(name="guest_account",nullable=false) private boolean guestAccount;
    public boolean isGuestAccount(){return guestAccount;}
    public void setGuestAccount(boolean value){guestAccount=value;}
    @Column(name="avatar_path",length=255) private String avatarPath;
    public String getAvatarPath(){return avatarPath;}
    public void setAvatarPath(String value){avatarPath=value;}
    @Column(name="default_address_id") private Long defaultAddressId;
    public Long getDefaultAddressId(){return defaultAddressId;}
    public void setDefaultAddressId(Long id){defaultAddressId=id;}
    @Column(name="phone",length=10) private String phone;
    @Column(name="birth_date") private java.time.LocalDate birthDate;
    @Column(name="gender",nullable=false,length=16) private String gender="UNSPECIFIED";
    @Version @Column(name="version",nullable=false) private Long version;
    public String getPhone(){return phone;}
    public void setPhone(String value){phone=value;}
    public java.time.LocalDate getBirthDate(){return birthDate;}
    public void setBirthDate(java.time.LocalDate value){birthDate=value;}
    public String getGender(){return gender;}
    public void setGender(String value){gender=value;}
    public Long getVersion(){return version;}
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
