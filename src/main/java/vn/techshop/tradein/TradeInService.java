package vn.techshop.tradein;
import jakarta.validation.constraints.*;
import java.math.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.*;
@Service @Transactional(readOnly=true)
public class TradeInService {
    public record Device(Long id,String model,BigDecimal basePrice,BigDecimal subsidyAmount) {}
    public record Criteria(Long id,String name,int deductionPercent) {}
    public record Input(@NotNull Long deviceId,@NotNull Long criteriaId,@NotBlank @Pattern(regexp="[A-Za-z0-9-]{5,40}") String oldImei) {}
    public record Quote(Long id,Long userId,String model,String condition,String oldImei,BigDecimal quotedAmount,String status,Long orderId) {}
    private final JdbcTemplate jdbc;private final Clock clock;
    public TradeInService(JdbcTemplate jdbc,Clock clock) {this.jdbc=jdbc;this.clock=clock;}
    public List<Device> devices() {return jdbc.query("select * from trade_in_devices where is_active=true order by id",(r,n)->new Device(r.getLong("id"),r.getString("model"),r.getBigDecimal("base_price"),r.getBigDecimal("subsidy_amount")));}
    public List<Criteria> criteria() {return jdbc.query("select * from trade_in_criterias order by id",(r,n)->new Criteria(r.getLong("id"),r.getString("name"),r.getInt("deduction_percent")));}
    public List<Quote> quotes(Long userId) {return jdbc.query("select q.*,d.model,c.name from trade_in_requests q join trade_in_devices d on d.id=q.device_id join trade_in_criterias c on c.id=q.criteria_id where (? is null or q.user_id=?) order by q.id desc",(r,n)->new Quote(r.getLong("id"),r.getLong("user_id"),r.getString("model"),r.getString("name"),r.getString("old_imei"),r.getBigDecimal("quoted_amount"),r.getString("status"),r.getObject("order_id",Long.class)),userId,userId);}
    @Transactional public Quote quote(Long userId,Input in) {
        var d=devices().stream().filter(x->x.id().equals(in.deviceId())).findFirst().orElseThrow(BusinessException::missing);
        var c=criteria().stream().filter(x->x.id().equals(in.criteriaId())).findFirst().orElseThrow(BusinessException::missing);
        String imei=Rules.text(in.oldImei(),5,40).toUpperCase(Locale.ROOT);if(!imei.matches("[A-Z0-9-]{5,40}"))throw BusinessException.invalid("Serial không hợp lệ.");
        BigDecimal amount=d.basePrice().multiply(BigDecimal.valueOf(100-c.deductionPercent())).divide(BigDecimal.valueOf(100),0,RoundingMode.DOWN).add(d.subsidyAmount());
        jdbc.update("insert into trade_in_requests(user_id,device_id,criteria_id,old_imei,quoted_amount,created_at) values(?,?,?,?,?,?)",userId,in.deviceId(),in.criteriaId(),imei,amount,Timestamp.from(clock.instant()));return quotes(userId).getFirst();
    }
    @Transactional public void approve(Long id) {
        jdbc.queryForList("select id from trade_in_requests where id=? for update",id);
        if(jdbc.update("update trade_in_requests set status='APPROVED' where id=? and status='QUOTED'",id)!=1)throw BusinessException.conflict("TRADE_IN_STATE","Hồ sơ đã được xử lý.");
    }
    public BigDecimal credit(Long userId,Long id,BigDecimal max) {
        if(id==null)return BigDecimal.ZERO;
        var q=quotes(userId).stream().filter(x->x.id().equals(id)&&"APPROVED".equals(x.status())&&x.orderId()==null).findFirst().orElseThrow(()->BusinessException.invalid("Phiếu thu cũ phải được cửa hàng thẩm định trước khi dùng."));
        var created=jdbc.queryForObject("select created_at from trade_in_requests where id=?",Timestamp.class,id);
        if(created.toInstant().plus(Duration.ofDays(7)).isBefore(clock.instant()))throw BusinessException.invalid("Phiếu thu cũ hết hạn sau 7 ngày.");
        return q.quotedAmount().min(max);
    }
    @Transactional public void use(Long userId,Long id,Long orderId) {
        if(id==null)return;jdbc.queryForList("select id from trade_in_requests where id=? for update",id);
        if(jdbc.queryForObject("select count(*) from trade_in_requests where old_imei=(select old_imei from trade_in_requests where id=?) and status='USED'",Integer.class,id)>0)throw BusinessException.conflict("TRADE_IN_USED","Máy cũ đã được thu.");
        credit(userId,id,new BigDecimal("999999999999999"));
        if(jdbc.update("update trade_in_requests set status='USED',order_id=? where id=? and user_id=? and status='APPROVED' and order_id is null",orderId,id,userId)!=1)throw BusinessException.conflict("TRADE_IN_USED","Phiếu đã được dùng.");
    }
    @Transactional public void cancel(Long orderId) {jdbc.update("update trade_in_requests set status='APPROVED',order_id=null where order_id=?",orderId);}
}
