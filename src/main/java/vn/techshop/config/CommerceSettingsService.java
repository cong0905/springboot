package vn.techshop.config;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.*;

/** Append terms and archive offers so existing quotes/applications retain their terms. */
@Service @Transactional(readOnly=true)
public class CommerceSettingsService {
    public record DeviceInput(@NotBlank @Size(max=100) String model,@NotNull BigDecimal basePrice,@NotNull BigDecimal subsidyAmount) {}
    public record CriteriaInput(@NotBlank @Size(max=100) String name,@Min(0) @Max(100) int deductionPercent) {}
    public record PlanInput(@NotBlank @Size(max=100) String provider,int months,@NotNull BigDecimal feePercent) {}
    public record Device(Long id,String model,BigDecimal basePrice,BigDecimal subsidyAmount,boolean active) {}
    public record Plan(Long id,String provider,int months,BigDecimal feePercent,boolean active) {}
    private final JdbcTemplate jdbc;
    public CommerceSettingsService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Device> devices() {return jdbc.query("select * from trade_in_devices order by id",(r,n)->new Device(r.getLong("id"),r.getString("model"),r.getBigDecimal("base_price"),r.getBigDecimal("subsidy_amount"),r.getBoolean("is_active")));}
    public List<Plan> plans() {return jdbc.query("select * from installment_plans order by id",(r,n)->new Plan(r.getLong("id"),r.getString("provider"),r.getInt("months"),r.getBigDecimal("fee_percent"),r.getBoolean("is_active")));}
    @Transactional public void device(DeviceInput in) {
        jdbc.update("insert into trade_in_devices(model,base_price,subsidy_amount) values(?,?,?)",Rules.text(in.model(),1,100),Rules.price(in.basePrice()),Rules.price(in.subsidyAmount()));
    }
    @Transactional public void criteria(CriteriaInput in) {
        if(in.deductionPercent()<0||in.deductionPercent()>100)throw BusinessException.invalid("Mức khấu trừ từ 0 đến 100%.");
        jdbc.update("insert into trade_in_criterias(name,deduction_percent) values(?,?)",Rules.text(in.name(),1,100),in.deductionPercent());
    }
    @Transactional public void plan(PlanInput in) {
        if(!Set.of(3,6,9,12).contains(in.months())||in.feePercent()==null||in.feePercent().signum()<0||in.feePercent().compareTo(BigDecimal.valueOf(100))>0||in.feePercent().stripTrailingZeros().scale()>2)throw BusinessException.invalid("Kỳ hạn 3/6/9/12 tháng; phí từ 0 đến 100%, tối đa 2 số thập phân.");
        jdbc.update("insert into installment_plans(provider,months,fee_percent) values(?,?,?)",Rules.text(in.provider(),1,100),in.months(),in.feePercent());
    }
    @Transactional public void archiveDevice(Long id) {if(jdbc.update("update trade_in_devices set is_active=false where id=?",id)!=1)throw BusinessException.missing();}
    @Transactional public void archivePlan(Long id) {if(jdbc.update("update installment_plans set is_active=false where id=?",id)!=1)throw BusinessException.missing();}
}
