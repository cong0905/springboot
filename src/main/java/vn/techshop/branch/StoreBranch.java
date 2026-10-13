package vn.techshop.branch;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="store_branches")
public class StoreBranch {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 public Long getId() { return id; }
 @Column(name="name", length=100, nullable=false) private String name;
 public String getName() { return name; }
 public void setName(String v) { name=v; }
 @Column(name="code", length=50, nullable=false) private String code;
 public String getCode() { return code; }
 public void setCode(String v) { code=v; }
 @Column(name="address", length=500, nullable=false) private String address;
 public String getAddress() { return address; }
 public void setAddress(String v) { address=v; }
 @Column(name="province_code", length=20, nullable=false) private String provinceCode;
 public String getProvinceCode() { return provinceCode; }
 public void setProvinceCode(String v) { provinceCode=v; }
 @Column(name="district_code", length=20) private String districtCode;
 public String getDistrictCode() { return districtCode; }
 public void setDistrictCode(String v) { districtCode=v; }
 @Column(name="ward_code", length=20) private String wardCode;
 public String getWardCode() { return wardCode; }
 public void setWardCode(String v) { wardCode=v; }
 @Column(name="phone", length=20, nullable=false) private String phone;
 public String getPhone() { return phone; }
 public void setPhone(String v) { phone=v; }
 @Column(name="latitude", nullable=false) private Double latitude;
 public Double getLatitude() { return latitude; }
 public void setLatitude(Double v) { latitude=v; }
 @Column(name="longitude", nullable=false) private Double longitude;
 public Double getLongitude() { return longitude; }
 public void setLongitude(Double v) { longitude=v; }
 @Column(name="is_active", nullable=false) private Boolean active;
 public Boolean getActive() { return active; }
 public void setActive(Boolean v) { active=v; }
}
