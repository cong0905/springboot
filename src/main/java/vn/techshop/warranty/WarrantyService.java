package vn.techshop.warranty;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.order.PurchaseOrder;
import vn.techshop.shared.*;
@Service @Transactional(readOnly=true)
public class WarrantyService {
    public record Card(Long id,String imeiNumber,LocalDate startDate,LocalDate endDate,String warrantyType) {}
    public record Ticket(Long id,Long warrantyId,String imeiNumber,String issue,String status) {}
    public record ExchangeEvent(Long id,Long warrantyId,String oldImei,String newImei,Long actorId,String reason,Instant occurredAt) {}
    private final JdbcTemplate jdbc;private final Clock clock;
    public WarrantyService(JdbcTemplate jdbc,Clock clock) {this.jdbc=jdbc;this.clock=clock;}
    @Transactional public void activate(PurchaseOrder order) {
        var date=order.getDeliveredAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        var imeis=jdbc.queryForList("select i.imei_number from product_items_imei i join order_items o on o.id=i.order_item_id where o.order_id=? and i.status='SOLD'",String.class,order.getId());
        for(String imei:imeis)if(jdbc.queryForObject("select count(*) from warranty_cards where order_id=? and imei_number=?",Integer.class,order.getId(),imei)==0)jdbc.update("insert into warranty_cards(imei_number,order_id,customer_id,start_date,end_date,warranty_type) values(?,?,?,?,?,?)",imei,order.getId(),order.getUserId(),date,date.plusMonths(12),"Chính hãng 12 tháng");
    }
    public List<Card> lookup(String query,String phone) {
        String q=Rules.text(query,5,40).toUpperCase(Locale.ROOT);
        // No customer identity or address is exposed. Phone lookup requires a matching serial as proof.
        if(phone!=null&&!phone.matches("0[0-9]{9}"))throw BusinessException.invalid("Số điện thoại không hợp lệ.");
        return jdbc.query("select w.* from warranty_cards w join orders o on o.id=w.order_id where w.imei_number=? and w.revoked_at is null and (? is null or o.phone=?)",(r,n)->new Card(r.getLong("id"),r.getString("imei_number"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),r.getString("warranty_type")),q,phone,phone);
    }
    public List<Card> owned(Long userId) {return jdbc.query("select * from warranty_cards where customer_id=? and revoked_at is null order by id desc",(r,n)->new Card(r.getLong("id"),r.getString("imei_number"),r.getDate("start_date").toLocalDate(),r.getDate("end_date").toLocalDate(),r.getString("warranty_type")),userId);}
    public List<Ticket> tickets() {
        return jdbc.query(
            "select t.id, t.warranty_id, t.imei_number as ticket_imei, w.imei_number as card_imei, t.issue, t.status " +
            "from repair_tickets t left join warranty_cards w on w.id=t.warranty_id order by t.id desc",
            (r,n)-> {
                String serial = r.getString("ticket_imei");
                if (serial == null || serial.isBlank()) {
                    serial = r.getString("card_imei");
                }
                if (serial == null || serial.isBlank()) {
                    serial = "Không xác định (dữ liệu cũ)";
                }
                return new Ticket(r.getLong("id"), r.getLong("warranty_id"), serial, r.getString("issue"), r.getString("status"));
            }
        );
    }
    @Transactional public void receive(Long warrantyId,String issue) {
        var cards = jdbc.queryForList("select imei_number from warranty_cards where id=? and revoked_at is null for update", String.class, warrantyId);
        if(cards.isEmpty())throw BusinessException.missing();
        String currentSerial = cards.getFirst();
        jdbc.update("insert into repair_tickets(warranty_id,imei_number,issue,created_at) values(?,?,?,?)",
            warrantyId, currentSerial, Rules.text(issue,5,500), Timestamp.from(clock.instant()));
    }
    public List<ExchangeEvent> exchanges(Long warrantyId) {
        return jdbc.query(
            "select * from warranty_exchanges where (? is null or warranty_id=?) order by id desc",
            (r,n)->new ExchangeEvent(r.getLong("id"),r.getLong("warranty_id"),r.getString("old_imei"),r.getString("new_imei"),r.getLong("actor_id"),r.getString("reason"),r.getTimestamp("occurred_at").toInstant()),
            warrantyId, warrantyId
        );
    }
    @Transactional public void transition(Long id,String status) {
        var rows=jdbc.queryForList("select status from repair_tickets where id=? for update",String.class,id);
        if(rows.isEmpty())throw BusinessException.missing();
        String current=rows.getFirst();
        String next=switch(current) {case "RECEIVED"->"REPAIRING";case "REPAIRING"->"COMPLETED";case "COMPLETED"->"HANDED_OVER";default->"";};
        if(!next.equals(status))throw BusinessException.invalid("Không được bỏ bước xử lý bảo hành.");jdbc.update("update repair_tickets set status=? where id=?",status,id);
    }
    @Transactional public void revoke(PurchaseOrder order) {
        jdbc.update("update warranty_cards set revoked_at=? where order_id=? and revoked_at is null",Timestamp.from(clock.instant()),order.getId());
    }
}
