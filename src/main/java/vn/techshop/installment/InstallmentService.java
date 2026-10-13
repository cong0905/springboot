package vn.techshop.installment;
import java.math.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.order.*;
import vn.techshop.shared.BusinessException;

@Service @Transactional(readOnly=true)
public class InstallmentService {
    public record Plan(Long id,String provider,int months,BigDecimal feePercent) {}
    public record Schedule(BigDecimal principal,BigDecimal fee,BigDecimal total,List<BigDecimal> monthlyPayments) {}
    public record Application(Long id,Long orderId,String provider,String status,BigDecimal financedAmount) {}
    private final JdbcTemplate jdbc;
    private final OrderRepository orders;
    private final ObjectProvider<OrderService> orderService;
    private final org.springframework.core.env.Environment env;

    public InstallmentService(JdbcTemplate jdbc, OrderRepository orders, ObjectProvider<OrderService> orderService, org.springframework.core.env.Environment env) {
        this.jdbc = jdbc;
        this.orders = orders;
        this.orderService = orderService;
        this.env = env;
    }

    public boolean isProduction() {
        return env != null && Arrays.asList(env.getActiveProfiles()).contains("production");
    }

    public List<Plan> plans() {
        var all = jdbc.query("select * from installment_plans where is_active=true order by months,id",(r,n)->new Plan(r.getLong("id"),r.getString("provider"),r.getInt("months"),r.getBigDecimal("fee_percent")));
        if (isProduction()) {
            boolean allowDemo = env.getProperty("app.installment.allow-demo", Boolean.class, false);
            if (!allowDemo) {
                return all.stream()
                    .filter(p -> !p.provider().toLowerCase(Locale.ROOT).contains("demo"))
                    .toList();
            }
        }
        return all;
    }

    public Schedule quote(Long id,BigDecimal amount) {
        if(amount==null||amount.signum()<0||amount.stripTrailingZeros().scale()>0||amount.compareTo(new BigDecimal("999999999999"))>0)throw BusinessException.invalid("Số tiền trả góp không hợp lệ.");
        var p=plans().stream().filter(x->x.id().equals(id)).findFirst().orElseThrow(BusinessException::missing);
        var fee=amount.multiply(p.feePercent()).divide(BigDecimal.valueOf(100),0,RoundingMode.CEILING);var total=amount.add(fee);
        var base=total.divide(BigDecimal.valueOf(p.months()),0,RoundingMode.DOWN);var payments=new ArrayList<BigDecimal>();
        for(int i=0;i<p.months();i++)payments.add(i==p.months()-1?total.subtract(base.multiply(BigDecimal.valueOf(p.months()-1))):base);
        return new Schedule(amount,fee,total,List.copyOf(payments));
    }

    @Transactional public void apply(PurchaseOrder order,Long planId) {
        quote(planId,order.getTotalAmount());
        jdbc.update("insert into installment_applications(order_id,plan_id,financed_amount) values(?,?,?)",order.getId(),planId,order.getTotalAmount());
    }

    public List<Application> applications() {
        return jdbc.query("select a.*,p.provider from installment_applications a join installment_plans p on p.id=a.plan_id order by a.id desc",(r,n)->new Application(r.getLong("id"),r.getLong("order_id"),r.getString("provider"),r.getString("status"),r.getBigDecimal("financed_amount")));
    }

    @Transactional public void decision(Long orderId,String status) {
        if (isProduction()) {
            boolean allowDemo = env.getProperty("app.installment.allow-demo", Boolean.class, false);
            if (!allowDemo) {
                var app = jdbc.queryForList("select p.provider from installment_applications a join installment_plans p on p.id=a.plan_id where a.order_id=?", String.class, orderId);
                if (!app.isEmpty() && app.get(0).toLowerCase(Locale.ROOT).contains("demo")) {
                    throw BusinessException.invalid("Hồ sơ trả góp demo không được phép phê duyệt trong môi trường production.");
                }
            }
        }
        var order = orders.lockById(orderId).orElseThrow(BusinessException::missing);
        if(!Set.of("APPROVED","REJECTED").contains(status)||jdbc.update("update installment_applications set status=? where order_id=? and status='PENDING'",status,orderId)!=1)throw BusinessException.invalid("Hồ sơ không hợp lệ hoặc đã xử lý.");
        if("REJECTED".equals(status)) {
            var svc = orderService.getIfAvailable();
            if(svc != null && !"CANCELLED".equals(order.getStatus())) {
                svc.cancel(orderId, order.getUserId(), true, new OrderService.Cancel("Hồ sơ trả góp bị từ chối", order.getVersion()));
            }
        }
    }

    public void requireApproved(Long orderId) {
        if(jdbc.queryForObject("select count(*) from installment_applications where order_id=? and status='APPROVED'",Integer.class,orderId)!=1)throw BusinessException.conflict("INSTALLMENT_PENDING","Hồ sơ trả góp chưa được duyệt.");
    }
}
