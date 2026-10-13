package vn.techshop.guest;
import jakarta.validation.Valid;
import jakarta.servlet.http.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import vn.techshop.cart.*;
import vn.techshop.order.*;
import vn.techshop.shared.BusinessException;
@RestController @RequestMapping("/api/v1/guest")
public class GuestApi {
 private final GuestService guests;private final CartService carts;private final OrderService orders;private final vn.techshop.payment.RefundService refunds;private final boolean secure;private final vn.techshop.config.ClientIpResolver ips;
 public GuestApi(GuestService guests,CartService carts,OrderService orders,vn.techshop.payment.RefundService refunds,@Value("${server.servlet.session.cookie.secure:false}") boolean secure,vn.techshop.config.ClientIpResolver ips){this.guests=guests;this.carts=carts;this.orders=orders;this.refunds=refunds;this.secure=secure;this.ips=ips;}
 @GetMapping("/cart") CartService.CartView cart(HttpServletRequest request){Long id=guests.current(request);return id==null?empty():carts.view(id);}
 public static CartService.CartView empty(){var zero=java.math.BigDecimal.ZERO;return new CartService.CartView(0L,java.util.List.of(),zero,zero,zero,"VND",Pricing.hash(java.util.List.of(),zero),false);}
 @PostMapping("/cart/items") CartService.CartView add(HttpServletRequest request,@Valid @RequestBody CartService.Add input){return carts.add(guests.create(request),input);}
 @PatchMapping("/cart/items/{id}") CartService.CartView update(HttpServletRequest request,@PathVariable Long id,@Valid @RequestBody CartService.Update input){return carts.update(guests.require(request),id,input);}
 @DeleteMapping("/cart/items/{id}") ResponseEntity<Void> remove(HttpServletRequest request,@PathVariable Long id,@RequestParam Long expectedVersion){carts.remove(guests.require(request),id,expectedVersion);return ResponseEntity.noContent().build();}
 @PostMapping("/cart/items/remove-many") ResponseEntity<Void> removeMany(HttpServletRequest request,@Valid @RequestBody CartService.RemoveMany input){carts.removeMany(guests.require(request),input);return ResponseEntity.noContent().build();}
 @PostMapping("/orders/preview") OrderService.Preview preview(HttpServletRequest request,@RequestBody OrderService.PreviewRequest input){return orders.preview(guests.require(request),input);}
 @PostMapping("/orders") ResponseEntity<OrderService.OrderView> checkout(HttpServletRequest request,@Valid @RequestBody OrderService.Checkout input){
  if(!"COD".equals(input.paymentMethod()) || input.options()!=null&&!"HOME_DELIVERY_STANDARD".equals(input.options().fulfillmentType()) || input.options()!=null&&(input.options().tradeInRequestId()!=null||input.options().installmentPlanId()!=null))throw BusinessException.invalid("Khách mua hàng dùng COD và giao tiêu chuẩn. Đăng nhập để dùng các lựa chọn khác.");
  var result=orders.checkout(guests.require(request),input,ips.resolve(request));String proof=guests.proof(result.order().id());
  return ResponseEntity.status(result.replay()?200:201).header(HttpHeaders.SET_COOKIE,ResponseCookie.from("guest_order_"+result.order().id(),proof).path("/").httpOnly(true).secure(secure).sameSite("Lax").maxAge(java.time.Duration.ofDays(30)).build().toString()).body(result.order());
 }
 @GetMapping("/orders/{id}") OrderService.OrderView detail(HttpServletRequest request,@PathVariable Long id){return orders.detail(id,guests.owner(request,id),false);}
 @PostMapping("/orders/{id}/cancel") OrderService.OrderView cancel(HttpServletRequest request,@PathVariable Long id,@Valid @RequestBody OrderService.Cancel input){return orders.cancel(id,guests.owner(request,id),false,input);}
 @PostMapping("/orders/{id}/refund") vn.techshop.payment.RefundService.RefundView refund(HttpServletRequest request,@PathVariable Long id,@Valid @RequestBody vn.techshop.payment.RefundService.RefundRequest input){return refunds.requestRefund(guests.owner(request,id),false,id,input);}
}
