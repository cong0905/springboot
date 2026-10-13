package vn.techshop.reporting;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ReportExportService {
    private final JdbcTemplate jdbc;
    private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss").withZone(VIETNAM);

    public ReportExportService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public byte[] exportOrdersCsv(LocalDate fromDate, LocalDate toDate) {
        LocalDate end = toDate == null ? LocalDate.now(VIETNAM) : toDate;
        LocalDate start = fromDate == null ? end.minusDays(30) : fromDate;
        var range = ReportingService.range(start, end);

        StringBuilder csv = new StringBuilder();
        // UTF-8 BOM so Microsoft Excel correctly displays Vietnamese characters
        csv.append("\uFEFF");
        csv.append("Mã đơn,Thời gian đặt,Thời gian giao,Khách hàng,Số điện thoại,Địa chỉ,Tạm tính,Giảm giá,Mã giảm giá,Phí giao,Tổng thanh toán,Tiền hoàn,Thu ròng,Phương thức thanh toán,Trạng thái thanh toán,Trạng thái đơn\n");

        String sql = "SELECT o.id, o.created_at, o.delivered_at, o.recipient_name, o.phone, o.address, " +
                     "o.subtotal, o.discount_amount, o.coupon_code, o.shipping_fee, o.total_amount, " +
                     "coalesce((select sum(r.amount) from refund_reconciliations r where r.order_id=o.id and r.status='SUCCEEDED'), 0) as refunded_amount, " +
                     "o.payment_method, o.payment_status, o.status " +
                     "FROM orders o WHERE o.created_at >= ? AND o.created_at < ? ORDER BY o.id DESC";

        jdbc.query(sql, rs -> {
            long id = rs.getLong("id");
            String code = "TS-" + String.format(Locale.ROOT, "%08d", id);
            var createdAt = rs.getTimestamp("created_at") != null ? DATE_TIME_FORMATTER.format(rs.getTimestamp("created_at").toInstant()) : "";
            var deliveredAt = rs.getTimestamp("delivered_at") != null ? DATE_TIME_FORMATTER.format(rs.getTimestamp("delivered_at").toInstant()) : "";
            String name = escapeCsv(rs.getString("recipient_name"));
            String phone = escapeCsvText(rs.getString("phone"));
            String address = escapeCsv(rs.getString("address"));
            long subtotal = rs.getBigDecimal("subtotal") != null ? rs.getBigDecimal("subtotal").longValue() : 0;
            long discount = rs.getBigDecimal("discount_amount") != null ? rs.getBigDecimal("discount_amount").longValue() : 0;
            String coupon = rs.getString("coupon_code") != null ? rs.getString("coupon_code") : "";
            long fee = rs.getBigDecimal("shipping_fee") != null ? rs.getBigDecimal("shipping_fee").longValue() : 0;
            long total = rs.getBigDecimal("total_amount") != null ? rs.getBigDecimal("total_amount").longValue() : 0;
            long refunded = rs.getBigDecimal("refunded_amount") != null ? rs.getBigDecimal("refunded_amount").longValue() : 0;
            long net = total - refunded;
            String payMethod = rs.getString("payment_method") != null ? rs.getString("payment_method") : "COD";
            String payStatus = rs.getString("payment_status") != null ? rs.getString("payment_status") : "UNPAID";
            String status = rs.getString("status");

            csv.append(code).append(",")
               .append(createdAt).append(",")
               .append(deliveredAt).append(",")
               .append(name).append(",")
               .append(phone).append(",")
               .append(address).append(",")
               .append(subtotal).append(",")
               .append(discount).append(",")
               .append(escapeCsv(coupon)).append(",")
               .append(fee).append(",")
               .append(total).append(",")
               .append(refunded).append(",")
               .append(net).append(",")
               .append(escapeCsv(payMethod)).append(",")
               .append(escapeCsv(payStatus)).append(",")
               .append(escapeCsv(status)).append("\n");
        }, range[0], range[1]);

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Excel CSV has no type schema: an explicit text guard preserves identifiers such as 090... . */
    public static String escapeCsvText(String value) {
        if(value==null || value.isEmpty()) return "";
        return escapeCsv("'"+value);
    }

    public static String escapeCsv(String value) {
        if (value == null) return "";
        String sanitized = value.replace("\0", "");
        String stripped = sanitized.stripLeading();
        boolean isFormula = false;
        if (!stripped.isEmpty()) {
            char first = stripped.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r') {
                isFormula = true;
            }
        }
        if (isFormula) {
            sanitized = "'" + sanitized;
        }
        if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")
                || sanitized.contains("\r") || sanitized.contains("\t") || isFormula) {
            return "\"" + sanitized.replace("\"", "\"\"") + "\"";
        }
        return sanitized;
    }
}
