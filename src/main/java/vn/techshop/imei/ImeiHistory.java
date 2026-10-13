package vn.techshop.imei;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="imei_history")
public class ImeiHistory {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 public Long getId() { return id; }
 @Column(name="imei_id", nullable=false) private Long imeiId;
 public Long getImeiId() { return imeiId; }
 public void setImeiId(Long v) { imeiId=v; }
 @Column(name="from_status", length=16) private String fromStatus;
 public String getFromStatus() { return fromStatus; }
 public void setFromStatus(String v) { fromStatus=v; }
 @Column(name="to_status", length=16, nullable=false) private String toStatus;
 public String getToStatus() { return toStatus; }
 public void setToStatus(String v) { toStatus=v; }
 @Column(name="from_branch_id") private Long fromBranchId;
 public Long getFromBranchId() { return fromBranchId; }
 public void setFromBranchId(Long v) { fromBranchId=v; }
 @Column(name="to_branch_id", nullable=false) private Long toBranchId;
 public Long getToBranchId() { return toBranchId; }
 public void setToBranchId(Long v) { toBranchId=v; }
 @Column(name="reason", length=500, nullable=false) private String reason;
 public String getReason() { return reason; }
 public void setReason(String v) { reason=v; }
 @JdbcTypeCode(SqlTypes.TIMESTAMP) @Column(name="occurred_at", nullable=false) private Instant occurredAt;
 public Instant getOccurredAt() { return occurredAt; }
 public void setOccurredAt(Instant v) { occurredAt=v; }
}
