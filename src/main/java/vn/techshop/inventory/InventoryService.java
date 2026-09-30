package vn.techshop.inventory;

import jakarta.validation.constraints.*;
import java.time.Clock;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import vn.techshop.catalog.*;
import vn.techshop.shared.*;

@Service
public class InventoryService {
    public record Adjustment(@NotNull Integer delta,@NotBlank @Size(min=5,max=500) String reason,@NotNull Long expectedVersion) {}
    public record Result(Long productId,int stockQuantity,Long version,Long movementId) {}
    public record MovementView(Long id,String type,int delta,int balanceAfter,String reason,Long actorId,java.time.Instant occurredAt) {}
    private final ProductRepository products;private final StockMovementRepository movements;private final Clock clock;
    public InventoryService(ProductRepository products,StockMovementRepository movements,Clock clock) {this.products=products;this.movements=movements;this.clock=clock;}
    @Transactional public Result adjust(Long productId,Long actorId,Adjustment request) {
        if(request.delta()==null || request.delta()==0) throw BusinessException.invalid("Số lượng điều chỉnh phải khác 0.");
        var p=products.lockById(productId).orElseThrow(BusinessException::missing);Rules.version(p.getVersion(),request.expectedVersion(),"STALE_VERSION");
        var movement=apply(p,actorId,null,"ADJUSTMENT",request.delta(),Rules.text(request.reason(),5,500));products.flush();return new Result(p.getId(),p.getStockQuantity(),p.getVersion(),movement.getId());
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public StockMovement apply(Product product,Long actorId,Long orderItemId,String type,int delta,String reason) {
        long balance=(long)product.getStockQuantity()+delta;
        if(balance<0 || balance>Integer.MAX_VALUE) throw BusinessException.conflict("INSUFFICIENT_STOCK","Tồn kho không đủ hoặc vượt giới hạn.");
        product.setStockQuantity((int)balance);product.setUpdatedAt(clock.instant());products.save(product);
        var movement=new StockMovement();movement.setProductId(product.getId());movement.setActorUserId(actorId);movement.setOrderItemId(orderItemId);movement.setMovementType(type);movement.setDelta(delta);movement.setBalanceAfter((int)balance);movement.setReason(reason);movement.setOccurredAt(clock.instant());return movements.save(movement);
    }
    @Transactional(readOnly=true) public PageView<MovementView> ledger(Long productId,int page,int size) {
        if(!products.existsById(productId))throw BusinessException.missing();
        return PageView.of(movements.findByProductId(productId,PageView.request(page,size,Sort.by(Sort.Order.desc("id")))).map(m -> new MovementView(m.getId(),m.getMovementType(),m.getDelta(),m.getBalanceAfter(),m.getReason(),m.getActorUserId(),m.getOccurredAt())));
    }
}
