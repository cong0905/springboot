package vn.techshop.order;

import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.shared.PageView;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderApi {
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.config.ClientIpResolver clientIps;
    private final OrderService orders;
    private final vn.techshop.payment.RefundService refunds;
    public OrderApi(OrderService orders, vn.techshop.payment.RefundService refunds) {
        this.orders = orders;
        this.refunds = refunds;
    }
    @PostMapping ResponseEntity<OrderService.OrderView> checkout(@AuthenticationPrincipal TechPrincipal user,@Valid @RequestBody OrderService.Checkout input,jakarta.servlet.http.HttpServletRequest request) {
        var result=orders.checkout(user.id(),input,clientIps.resolve(request));
        if(result.replay())return ResponseEntity.ok().header("Idempotent-Replay","true").body(result.order());
        return ResponseEntity.created(URI.create("/api/v1/orders/"+result.order().id())).body(result.order());
    }
    @GetMapping PageView<OrderService.Summary> list(@AuthenticationPrincipal TechPrincipal user,@RequestParam(required=false) String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) {return orders.list(user.id(),false,status,page,size);}
    @GetMapping("/{id}") OrderService.OrderView detail(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id) {return orders.detail(id,user.id(),false);}
    @PostMapping("/{id}/reorder") vn.techshop.cart.CartService.CartView reorder(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id){return orders.reorder(user.id(),id);}
    @PostMapping("/{id}/cancel") OrderService.OrderView cancel(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody OrderService.Cancel input) {return orders.cancel(id,user.id(),false,input);}
    @PostMapping("/{id}/refund") vn.techshop.payment.RefundService.RefundView refund(@AuthenticationPrincipal TechPrincipal user,@PathVariable Long id,@Valid @RequestBody vn.techshop.payment.RefundService.RefundRequest input) {
        return refunds.requestRefund(user.id(), false, id, input);
    }
}
