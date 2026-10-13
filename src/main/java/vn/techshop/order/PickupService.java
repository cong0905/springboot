package vn.techshop.order;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.BusinessException;
@Service
public class PickupService {
    private final OrderRepository repo;private final OrderService orders;private final JdbcTemplate jdbc;private final Clock clock;
    public PickupService(OrderRepository repo,OrderService orders,JdbcTemplate jdbc,Clock clock) {this.repo=repo;this.orders=orders;this.jdbc=jdbc;this.clock=clock;}
    @Transactional public OrderService.OrderView collect(Long id,Long actorId,String code) {
        var o=repo.lockById(id).orElseThrow(BusinessException::missing);
        if(!"CLICK_AND_COLLECT".equals(o.getFulfillmentType())||code==null||o.getPickupCode()==null||!MessageDigest.isEqual(code.getBytes(StandardCharsets.UTF_8),o.getPickupCode().getBytes(StandardCharsets.UTF_8)))throw BusinessException.invalid("Mã nhận máy không khớp.");
        if("DELIVERED".equals(o.getStatus()))return orders.detail(id,actorId,true);
        if(!"PAID".equals(o.getPaymentStatus())&&o.getHoldExpiresAt()!=null&&!clock.instant().isBefore(o.getHoldExpiresAt())
            &&List.of("PENDING","CONFIRMED").contains(o.getStatus()))
            throw BusinessException.conflict("PICKUP_EXPIRED","Đã hết hạn giữ máy. Vui lòng đặt đơn mới.");
        if("PENDING".equals(o.getStatus()))orders.transition(id,actorId,new OrderService.Transition("CONFIRMED",o.getVersion(),false));
        if("CONFIRMED".equals(o.getStatus()))orders.transition(id,actorId,new OrderService.Transition("SHIPPED",o.getVersion(),false));
        boolean needCod = "COD".equals(o.getPaymentMethod()) && !"PAID".equals(o.getPaymentStatus());
        return orders.transition(id,actorId,new OrderService.Transition("DELIVERED",o.getVersion(),needCod));
    }
    @Scheduled(fixedDelayString="${app.pickup.expiry-interval-ms:60000}")
    public void expire() {
        // DATETIME is stored in UTC by Hibernate; do not bind a Timestamp using the host's local zone.
        var cutoff=java.time.LocalDateTime.ofInstant(clock.instant(),java.time.ZoneOffset.UTC);
        var ids=jdbc.queryForList("select id from orders where status in ('PENDING','CONFIRMED') and payment_status<>'PAID' and hold_expires_at is not null and hold_expires_at<=?",Long.class,cutoff);
        for(Long id:ids) {
            var order=repo.findById(id).orElse(null);if(order==null)continue;
            if("COD".equals(order.getPaymentMethod()) && !"CLICK_AND_COLLECT".equals(order.getFulfillmentType()) && !"PENDING".equals(order.getStatus())) {
                continue;
            }
            try {
                String reason = "INSTALLMENT".equals(order.getPaymentMethod())
                    ? "Hết hạn chờ duyệt trả góp"
                    : ("CLICK_AND_COLLECT".equals(order.getFulfillmentType()) ? "Hết hạn giữ máy sau 24 giờ" : ("COD".equals(order.getPaymentMethod()) ? "Hết hạn xác nhận đơn COD sau 24 giờ" : "Hết hạn thanh toán online"));
                orders.cancel(id,order.getUserId(),true,new OrderService.Cancel(reason,order.getVersion()));
            } catch(BusinessException ex) {
                org.slf4j.LoggerFactory.getLogger(getClass()).debug("Order expiry skipped order {}: {}",id,ex.getMessage());
            }
        }
    }
}
