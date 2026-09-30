package vn.techshop.reporting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.BusinessException;

@Service
@Transactional(readOnly=true)
public class ReportingService {
    public record Summary(LocalDate fromDate,LocalDate toDate,String timeZone,long placedOrders,long deliveredOrders,BigDecimal merchandiseRevenue,BigDecimal shippingCollected,BigDecimal codCollectedTotal,BigDecimal aov,Map<String,Long> statusCountsAsOf,Instant asOf) {}
    public record TopProduct(Long productId,String sku,String name,long unitsSold,BigDecimal merchandiseRevenue) {}
    private final JdbcTemplate jdbc;private final Clock clock;
    private static final ZoneId VIETNAM=ZoneId.of("Asia/Ho_Chi_Minh");
    public ReportingService(JdbcTemplate jdbc,Clock clock) {this.jdbc=jdbc;this.clock=clock;}
    public Summary summary(LocalDate from,LocalDate to) {
        LocalDate end=to==null?LocalDate.now(clock.withZone(VIETNAM)):to;LocalDate start=from==null?end.minusDays(6):from;var range=range(start,end);
        var revenue=jdbc.queryForMap("select count(*) as qty,coalesce(sum(subtotal),0) as merchandise,coalesce(sum(shipping_fee),0) as fees,coalesce(sum(total_amount),0) as total from orders where status='DELIVERED' and delivered_at>=? and delivered_at<?",range[0],range[1]);
        long count=((Number)revenue.get("qty")).longValue();BigDecimal goods=new BigDecimal(revenue.get("merchandise").toString()),fees=new BigDecimal(revenue.get("fees").toString()),total=new BigDecimal(revenue.get("total").toString());
        Long placed=jdbc.queryForObject("select count(*) from orders where created_at>=? and created_at<?",Long.class,range[0],range[1]);var states=new LinkedHashMap<String,Long>();for(var status:vn.techshop.order.OrderState.values())states.put(status.name(),0L);
        jdbc.query("select status,count(*) as qty from orders group by status",rs -> {states.put(rs.getString("status"),rs.getLong("qty"));});
        return new Summary(start,end,VIETNAM.getId(),placed==null?0:placed,count,goods,fees,total,count==0?null:goods.divide(BigDecimal.valueOf(count),0,RoundingMode.HALF_UP),states,clock.instant());
    }
    public List<TopProduct> top(LocalDate from,LocalDate to,int limit) {
        if(limit<1 || limit>50)throw BusinessException.invalid("Giới hạn phải từ 1 đến 50.");LocalDate end=to==null?LocalDate.now(clock.withZone(VIETNAM)):to;LocalDate start=from==null?end.minusDays(6):from;var range=range(start,end);
        return jdbc.query("select p.id,p.sku,p.name,sum(i.quantity) as units,sum(i.line_total) as revenue from order_items i join orders o on o.id=i.order_id join products p on p.id=i.product_id where o.status='DELIVERED' and o.delivered_at>=? and o.delivered_at<? group by p.id,p.sku,p.name order by units desc,revenue desc,p.id asc limit ?",(rs,n) -> new TopProduct(rs.getLong("id"),rs.getString("sku"),rs.getString("name"),rs.getLong("units"),rs.getBigDecimal("revenue")),range[0],range[1],limit);
    }
    static java.sql.Timestamp[] range(LocalDate start,LocalDate end) {
        if(end.isBefore(start) || ChronoUnit.DAYS.between(start,end)>365)throw BusinessException.invalid("Khoảng ngày không hợp lệ hoặc vượt 366 ngày.");
        return new java.sql.Timestamp[]{java.sql.Timestamp.from(start.atStartOfDay(VIETNAM).toInstant()),java.sql.Timestamp.from(end.plusDays(1).atStartOfDay(VIETNAM).toInstant())};
    }
}
