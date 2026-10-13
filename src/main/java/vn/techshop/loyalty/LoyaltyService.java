package vn.techshop.loyalty;
import java.math.*;
import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.order.PurchaseOrder;
@Service @Transactional(readOnly=true)
public class LoyaltyService {
    public record Member(Long userId,String rank,BigDecimal accumulatedSpending,long pointsBalance,int discountPercent) {}
    private final JdbcTemplate jdbc;private final Clock clock;
    public LoyaltyService(JdbcTemplate jdbc,Clock clock) {this.jdbc=jdbc;this.clock=clock;}
    public Member member(Long id) {
        var rows=jdbc.query("select * from member_profiles where user_id=?",(r,n)->new Member(id,r.getString("member_rank"),r.getBigDecimal("accumulated_spending"),r.getLong("points_balance"),percent(r.getString("member_rank"))),id);
        return rows.isEmpty()?new Member(id,"S_NEW",BigDecimal.ZERO,0,0):rows.getFirst();
    }
    private static int percent(String rank) {return "S_VIP".equals(rank)?3:"S_MEM".equals(rank)?1:0;}
    public BigDecimal discount(Long id,BigDecimal subtotal) {return subtotal.multiply(BigDecimal.valueOf(member(id).discountPercent())).divide(BigDecimal.valueOf(100),0,RoundingMode.DOWN);}
    @Transactional public void delivered(PurchaseOrder order) {
        jdbc.queryForList("select id from users where id=? for update",order.getUserId());
        if(jdbc.queryForObject("select count(*) from point_transactions where order_id=?",Integer.class,order.getId())>0)return;
        if(jdbc.queryForObject("select count(*) from member_profiles where user_id=?",Integer.class,order.getUserId())==0)jdbc.update("insert into member_profiles(user_id) values(?)",order.getUserId());
        var m=member(order.getUserId());BigDecimal spending=m.accumulatedSpending().add(order.getTotalAmount().subtract(order.getShippingFee()));
        long points=order.getTotalAmount().subtract(order.getShippingFee()).divide(BigDecimal.valueOf(100000),0,RoundingMode.DOWN).longValueExact();
        String rank=spending.compareTo(new BigDecimal("30000000"))>0?"S_VIP":spending.compareTo(new BigDecimal("10000000"))>0?"S_MEM":"S_NEW";
        jdbc.update("update member_profiles set accumulated_spending=?,points_balance=points_balance+?,member_rank=? where user_id=?",spending,points,rank,order.getUserId());
        jdbc.update("insert into point_transactions(user_id,order_id,points_delta,occurred_at) values(?,?,?,?)",order.getUserId(),order.getId(),points,Timestamp.from(clock.instant()));
    }
    @Transactional public void revoke(PurchaseOrder order) {
        jdbc.queryForList("select id from users where id=? for update",order.getUserId());
        if(jdbc.queryForObject("select count(*) from point_reversals where order_id=?",Integer.class,order.getId())>0)return;
        var pointsAwarded = jdbc.queryForList("select points_delta from point_transactions where order_id=? and points_delta > 0", Long.class, order.getId());
        if (pointsAwarded.isEmpty()) return;
        long totalAwarded = pointsAwarded.stream().mapToLong(Long::longValue).sum();
        var m=member(order.getUserId());
        BigDecimal netSpending = order.getTotalAmount().subtract(order.getShippingFee());
        BigDecimal spending = m.accumulatedSpending().subtract(netSpending).max(BigDecimal.ZERO);
        long newBalance = Math.max(0, m.pointsBalance() - totalAwarded);
        String rank = spending.compareTo(new BigDecimal("30000000")) > 0 ? "S_VIP" : (spending.compareTo(new BigDecimal("10000000")) > 0 ? "S_MEM" : "S_NEW");
        jdbc.update("update member_profiles set accumulated_spending=?,points_balance=?,member_rank=? where user_id=?", spending, newBalance, rank, order.getUserId());
        jdbc.update("insert into point_reversals(order_id,user_id,points_delta,spending_delta,occurred_at) values(?,?,?,?,?)",order.getId(),order.getUserId(),-totalAwarded,netSpending.negate(),Timestamp.from(clock.instant()));
    }
}
