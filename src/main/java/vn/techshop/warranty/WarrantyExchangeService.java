package vn.techshop.warranty;
import java.time.*;
import java.sql.Timestamp;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.order.OrderRepository;
import vn.techshop.catalog.ProductRepository;
import vn.techshop.inventory.InventoryService;
import vn.techshop.shared.BusinessException;
@Service
public class WarrantyExchangeService {
    public record Result(Long warrantyId,String previousSerial,String replacementSerial) {}
    private final JdbcTemplate jdbc;private final OrderRepository orders;private final ProductRepository products;private final InventoryService inventory;private final Clock clock;
    public WarrantyExchangeService(JdbcTemplate jdbc,OrderRepository orders,ProductRepository products,InventoryService inventory,Clock clock) {this.jdbc=jdbc;this.orders=orders;this.products=products;this.inventory=inventory;this.clock=clock;}
    @Transactional public Result exchange(Long warrantyId,String replacementSerial,Long actor) {
        var ids=jdbc.queryForList("select order_id from warranty_cards where id=?",Long.class,warrantyId);if(ids.isEmpty())throw BusinessException.missing();
        var order=orders.lockById(ids.getFirst()).orElseThrow(BusinessException::missing);
        var cards=jdbc.queryForList("select * from warranty_cards where id=? and revoked_at is null for update",warrantyId);if(cards.isEmpty())throw BusinessException.missing();var card=cards.getFirst();
        var day=LocalDate.now(clock.withZone(ZoneId.of("Asia/Ho_Chi_Minh")));
        if(!"DELIVERED".equals(order.getStatus())||day.isAfter(order.getDeliveredAt().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().plusDays(30)))throw BusinessException.invalid("Đổi máy áp dụng trong 30 ngày từ ngày giao.");
        String serial=card.get("imei_number").toString();var oldRows=jdbc.queryForList("select i.*,v.product_id from product_items_imei i join product_variants v on v.id=i.variant_id where i.imei_number=?",serial);
        if(oldRows.isEmpty())throw BusinessException.missing();var old=oldRows.getFirst();Long productId=((Number)old.get("product_id")).longValue();
        var product=products.lockById(productId).orElseThrow(BusinessException::missing);
        // Same order/product lock ordering as checkout and fulfillment. Lock physical units deterministically.
        String replacement=vn.techshop.shared.Rules.text(replacementSerial,5,40).toUpperCase(Locale.ROOT);
        var items=jdbc.queryForList("select * from product_items_imei where imei_number in (?,?) order by id for update",serial,replacement);
        var oldItem=items.stream().filter(i->serial.equals(i.get("imei_number"))).findFirst().orElseThrow(BusinessException::missing);
        var newItem=items.stream().filter(i->replacement.equals(i.get("imei_number"))).findFirst().orElseThrow(BusinessException::missing);
        if(!"SOLD".equals(oldItem.get("status"))||!"IN_STOCK".equals(newItem.get("status"))||!oldItem.get("variant_id").equals(newItem.get("variant_id"))||!oldItem.get("branch_id").equals(newItem.get("branch_id")))throw BusinessException.conflict("EXCHANGE_UNAVAILABLE","Máy thay thế phải cùng phiên bản, còn kho và cùng chi nhánh.");
        Long branchId=((Number)oldItem.get("branch_id")).longValue(),variantId=((Number)oldItem.get("variant_id")).longValue();
        jdbc.queryForList("select id from branch_inventories where branch_id=? and variant_id=? for update",branchId,variantId);
        jdbc.update("update product_items_imei set status='DEFECTIVE' where id=?",oldItem.get("id"));
        jdbc.update("update product_items_imei set status='SOLD',order_item_id=?,sold_at=? where id=?",oldItem.get("order_item_id"),Timestamp.from(clock.instant()),newItem.get("id"));
        jdbc.update("update branch_inventories set quantity=quantity-1,version=version+1 where branch_id=? and variant_id=?",branchId,variantId);
        inventory.apply(product,actor,null,"ADJUSTMENT",-1,"Đổi bảo hành 30 ngày: "+serial+" → "+replacement);
        for(var entry:List.of(oldItem,newItem))jdbc.update("insert into imei_history(imei_id,from_status,to_status,from_branch_id,to_branch_id,reason,occurred_at) values(?,?,?,?,?,?,?)",entry.get("id"),entry.get("status"),entry==oldItem?"DEFECTIVE":"SOLD",branchId,branchId,"Đổi bảo hành 30 ngày",Timestamp.from(clock.instant()));
        jdbc.update("update warranty_cards set imei_number=? where id=?",replacement,warrantyId);
        jdbc.update("insert into warranty_exchanges(warranty_id,old_imei,new_imei,actor_id,reason,occurred_at) values(?,?,?,?,?,?)",
            warrantyId,serial,replacement,actor,"Đổi bảo hành 30 ngày",Timestamp.from(clock.instant()));
        return new Result(warrantyId,serial,replacement);
    }
}
