package vn.techshop.reporting;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class CommerceReportingService {
    public record Channel(
        String fulfillmentType,
        String branch,
        long deliveredOrders,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal goodsRevenue,
        BigDecimal shippingRevenue,
        BigDecimal collectedTotal,
        BigDecimal refundedAmount,
        BigDecimal netRevenue
    ) {
        public Channel(String fulfillmentType, String branch, long deliveredOrders, BigDecimal goodsRevenue, BigDecimal shippingRevenue, BigDecimal collectedTotal) {
            this(fulfillmentType, branch, deliveredOrders, goodsRevenue, BigDecimal.ZERO, goodsRevenue, shippingRevenue, collectedTotal, BigDecimal.ZERO, collectedTotal);
        }
    }
    private final JdbcTemplate jdbc;private final Clock clock;
    public CommerceReportingService(JdbcTemplate jdbc,Clock clock) {this.jdbc=jdbc;this.clock=clock;}
    public List<Channel> channels(LocalDate from,LocalDate to) {
        var end=to==null?LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh"))):to;
        var start=from==null?end.minusDays(6):from;var range=ReportingService.range(start,end);

        var salesRows = jdbc.query(
            "select coalesce(o.fulfillment_type, 'HOME_DELIVERY_STANDARD') as fulfillment_type, " +
            "coalesce(b.name, 'Kho mặc định') as branch, " +
            "count(*) as qty, " +
            "coalesce(sum(o.subtotal), 0) as subtotal, " +
            "coalesce(sum(o.discount_amount), 0) as discount, " +
            "coalesce(sum(o.subtotal - o.discount_amount), 0) as goods, " +
            "coalesce(sum(o.shipping_fee), 0) as shipping, " +
            "coalesce(sum(o.total_amount), 0) as total " +
            "from orders o " +
            "left join store_branches b on b.id = o.assigned_branch_id " +
            "where o.status = 'DELIVERED' and o.delivered_at >= ? and o.delivered_at < ? " +
            "group by o.fulfillment_type, b.name order by total desc",
            (r, n) -> new SalesRow(
                r.getString("fulfillment_type"),
                r.getString("branch"),
                r.getLong("qty"),
                r.getBigDecimal("subtotal"),
                r.getBigDecimal("discount"),
                r.getBigDecimal("goods"),
                r.getBigDecimal("shipping"),
                r.getBigDecimal("total")
            ),
            range[0], range[1]
        );

        var refundRows = jdbc.query(
            "select coalesce(o.fulfillment_type, 'HOME_DELIVERY_STANDARD') as fulfillment_type, " +
            "coalesce(b.name, 'Kho mặc định') as branch, " +
            "coalesce(sum(r.amount), 0) as refunded " +
            "from refund_reconciliations r " +
            "join orders o on o.id = r.order_id " +
            "left join store_branches b on b.id = o.assigned_branch_id " +
            "where r.status = 'SUCCEEDED' and r.updated_at >= ? and r.updated_at < ? " +
            "group by o.fulfillment_type, b.name",
            (r, n) -> Map.entry(
                r.getString("fulfillment_type") + "::" + r.getString("branch"),
                r.getBigDecimal("refunded")
            ),
            range[0], range[1]
        );
        Map<String, BigDecimal> refundMap = new LinkedHashMap<>();
        for (var e : refundRows) {
            refundMap.put(e.getKey(), e.getValue());
        }

        Set<String> processedKeys = new HashSet<>();
        List<Channel> result = new ArrayList<>();

        for (var s : salesRows) {
            String key = s.fulfillmentType + "::" + s.branch;
            processedKeys.add(key);
            BigDecimal ref = refundMap.getOrDefault(key, BigDecimal.ZERO);
            BigDecimal net = s.total.subtract(ref);
            result.add(new Channel(
                s.fulfillmentType,
                s.branch,
                s.qty,
                s.subtotal,
                s.discount,
                s.goods,
                s.shipping,
                s.total,
                ref,
                net
            ));
        }

        for (var e : refundMap.entrySet()) {
            if (!processedKeys.contains(e.getKey())) {
                String[] parts = e.getKey().split("::", 2);
                BigDecimal ref = e.getValue();
                BigDecimal net = BigDecimal.ZERO.subtract(ref);
                result.add(new Channel(
                    parts[0],
                    parts.length > 1 ? parts[1] : "Kho mặc định",
                    0L,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    ref,
                    net
                ));
            }
        }

        return result;
    }

    private record SalesRow(
        String fulfillmentType,
        String branch,
        long qty,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal goods,
        BigDecimal shipping,
        BigDecimal total
    ) {}
}
