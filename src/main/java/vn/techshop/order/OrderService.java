package vn.techshop.order;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.cart.*;
import vn.techshop.catalog.*;
import vn.techshop.inventory.*;
import vn.techshop.shared.*;

@Service
@Transactional(readOnly=true)
public class OrderService {
    public record Checkout(@NotBlank String checkoutKey,@NotNull Long cartVersion,@NotNull @DecimalMin("0") BigDecimal expectedTotal,@NotBlank @Pattern(regexp="[0-9a-f]{64}") String expectedPricingHash,@NotBlank @Size(min=2,max=100) String recipientName,@NotBlank @Pattern(regexp="0[0-9]{9}") String phone,@NotBlank @Size(min=10,max=500) String address,@Size(max=500) String note) {}
    public record Cancel(@NotBlank @Size(min=5,max=500) String reason,@NotNull Long expectedVersion) {}
    public record Transition(@NotBlank String targetStatus,@NotNull Long expectedVersion,Boolean codCollected) {}
    public record ItemView(Long productId,String sku,String name,BigDecimal unitPrice,int quantity,BigDecimal lineTotal) {}
    public record HistoryView(String fromStatus,String toStatus,Instant occurredAt,String reason) {}
    public record OrderView(Long id,String orderCode,String status,BigDecimal subtotal,BigDecimal shippingFee,BigDecimal total,String currency,Instant createdAt,Instant deliveredAt,Long version,String recipientName,String phone,String address,String note,List<ItemView> items,List<HistoryView> history) {}
    public record Summary(Long id,String orderCode,String status,BigDecimal total,String currency,Instant createdAt,Long version) {}
    public record CheckoutResult(OrderView order,boolean replay) {}
    private final OrderRepository orders;private final OrderItemRepository orderItems;private final OrderHistoryRepository histories;
    private final CartRepository carts;private final CartItemRepository cartItems;private final CartService cartService;
    private final ProductRepository products;private final CategoryRepository categories;private final InventoryService inventory;private final Clock clock;
    public OrderService(OrderRepository orders,OrderItemRepository orderItems,OrderHistoryRepository histories,CartRepository carts,CartItemRepository cartItems,CartService cartService,ProductRepository products,CategoryRepository categories,InventoryService inventory,Clock clock) {this.orders=orders;this.orderItems=orderItems;this.histories=histories;this.carts=carts;this.cartItems=cartItems;this.cartService=cartService;this.products=products;this.categories=categories;this.inventory=inventory;this.clock=clock;}

    @Transactional public CheckoutResult checkout(Long userId,Checkout request) {
        String key=canonicalKey(request.checkoutKey());String name=Rules.text(request.recipientName(),2,100);String address=Rules.text(request.address(),10,500);String phone=Rules.text(request.phone(),10,10);String note=Rules.optional(request.note(),500);
        if(!phone.matches("0[0-9]{9}") || request.expectedTotal()==null || request.expectedTotal().signum()<0 || request.expectedTotal().stripTrailingZeros().scale()>0 || request.expectedPricingHash()==null || !request.expectedPricingHash().matches("[0-9a-f]{64}")) throw BusinessException.invalid("Thông tin đơn không hợp lệ.");
        String hash=Pricing.sha256("{\"userId\":"+userId+",\"cartVersion\":"+request.cartVersion()+",\"expectedTotal\":"+quote(request.expectedTotal().setScale(0).toPlainString())+",\"expectedPricingHash\":"+quote(request.expectedPricingHash())+",\"recipientName\":"+quote(name)+",\"phone\":"+quote(phone)+",\"address\":"+quote(address)+",\"note\":"+quote(note)+"}");
        var cart=carts.lockByUserId(userId).orElseThrow(BusinessException::missing);
        var existing=orders.findByUserIdAndCheckoutKey(userId,key);
        if(existing.isPresent()) {var order=existing.get();if(!order.getRequestHash().equals(hash))throw BusinessException.conflict("CHECKOUT_KEY_REUSED","Mã yêu cầu đã dùng với thông tin khác.");return new CheckoutResult(view(order),true);}
        Rules.version(cart.getVersion(),request.cartVersion(),"CART_CHANGED");var rows=cartItems.findByCartIdOrderByProductIdAsc(cart.getId());
        if(rows.isEmpty())throw BusinessException.invalid("Giỏ hàng đang trống.");
        var locked=new LinkedHashMap<Long,Product>();var pricing=new ArrayList<Pricing.Line>();BigDecimal subtotal=BigDecimal.ZERO;
        for(var row:rows) {
            var p=products.lockById(row.getProductId()).orElseThrow(BusinessException::missing);var category=categories.findById(p.getCategoryId()).orElseThrow(BusinessException::missing);
            if(!"ACTIVE".equals(p.getStatus()) || !"ACTIVE".equals(category.getStatus()))throw BusinessException.conflict("PRODUCT_UNAVAILABLE","Sản phẩm đang ngừng bán.");
            if(p.getStockQuantity()<row.getQuantity())throw BusinessException.conflict("INSUFFICIENT_STOCK","Số lượng còn lại không đủ.");
            locked.put(p.getId(),p);pricing.add(new Pricing.Line(p.getId(),row.getQuantity(),p.getPrice()));subtotal=subtotal.add(p.getPrice().multiply(BigDecimal.valueOf(row.getQuantity())));
        }
        BigDecimal fee=Pricing.fee(subtotal),total=subtotal.add(fee);
        if(total.compareTo(request.expectedTotal())!=0 || !Pricing.hash(pricing,fee).equals(request.expectedPricingHash()))throw BusinessException.conflict("PRICE_CHANGED","Giá đã thay đổi. Vui lòng xác nhận lại giỏ hàng.");
        var order=new PurchaseOrder();order.setUserId(userId);order.setCheckoutKey(key);order.setRequestHash(hash);order.setStatus("PENDING");order.setRecipientName(name);order.setPhone(phone);order.setAddress(address);order.setNote(note);order.setCurrency("VND");order.setSubtotal(subtotal);order.setShippingFee(fee);order.setTotalAmount(total);order.setCodCollected(false);order.setCreatedAt(clock.instant());order.setUpdatedAt(clock.instant());orders.saveAndFlush(order);
        for(var row:rows) {
            var p=locked.get(row.getProductId());var item=new OrderItem();item.setOrderId(order.getId());item.setProductId(p.getId());item.setSkuSnapshot(p.getSku());item.setNameSnapshot(p.getName());item.setUnitPrice(p.getPrice());item.setQuantity(row.getQuantity());item.setLineTotal(p.getPrice().multiply(BigDecimal.valueOf(row.getQuantity())));orderItems.saveAndFlush(item);
            inventory.apply(p,userId,item.getId(),"ORDER",-row.getQuantity(),"Đặt đơn COD");
        }
        history(order,userId,null,"Đặt đơn COD");cartItems.deleteByCartId(cart.getId());cartService.touch(cart);orders.flush();return new CheckoutResult(view(order),false);
    }

    public PageView<Summary> list(Long userId,boolean admin,String status,int page,int size) {
        String selected=status==null || status.isBlank()?null:status;
        if(selected!=null)OrderState.parse(selected);
        Specification<PurchaseOrder> spec=(root,query,cb) -> {var checks=new ArrayList<jakarta.persistence.criteria.Predicate>();if(!admin)checks.add(cb.equal(root.get("userId"),userId));if(selected!=null)checks.add(cb.equal(root.get("status"),selected));return cb.and(checks.toArray(jakarta.persistence.criteria.Predicate[]::new));};
        return PageView.of(orders.findAll(spec,PageView.request(page,size,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")))).map(o -> new Summary(o.getId(),code(o.getId()),o.getStatus(),o.getTotalAmount(),o.getCurrency(),o.getCreatedAt(),o.getVersion())));
    }
    public OrderView detail(Long id,Long userId,boolean admin) {return view((admin?orders.findById(id):orders.findByIdAndUserId(id,userId)).orElseThrow(BusinessException::missing));}
    @Transactional public OrderView cancel(Long id,Long actorId,boolean admin,Cancel input) {
        String reason=Rules.text(input.reason(),5,500);var order=(admin?orders.lockById(id):orders.lockOwned(id,actorId)).orElseThrow(BusinessException::missing);
        if("CANCELLED".equals(order.getStatus()))return view(order);
        Rules.version(order.getVersion(),input.expectedVersion(),"STALE_VERSION");String previous=order.getStatus();
        if(!"PENDING".equals(previous) && !(admin && "CONFIRMED".equals(previous)))throw BusinessException.conflict("INVALID_ORDER_TRANSITION","Đơn không thể hủy ở trạng thái hiện tại.");
        for(var item:orderItems.findByOrderIdOrderByProductIdAsc(id)) {var product=products.lockById(item.getProductId()).orElseThrow(BusinessException::missing);inventory.apply(product,actorId,item.getId(),"CANCEL",item.getQuantity(),reason);}
        order.setStatus("CANCELLED");order.setUpdatedAt(clock.instant());orders.saveAndFlush(order);history(order,actorId,previous,reason);return view(order);
    }
    @Transactional public OrderView transition(Long id,Long actorId,Transition input) {
        var order=orders.lockById(id).orElseThrow(BusinessException::missing);Rules.version(order.getVersion(),input.expectedVersion(),"STALE_VERSION");var current=OrderState.parse(order.getStatus());var target=OrderState.parse(input.targetStatus());
        if(!current.canMoveTo(target))throw BusinessException.conflict("INVALID_ORDER_TRANSITION","Không được bỏ bước hoặc chuyển ngược trạng thái.");
        if(target==OrderState.DELIVERED) {if(!Boolean.TRUE.equals(input.codCollected()))throw BusinessException.invalid("Cần xác nhận đã thu COD.");order.setCodCollected(true);order.setDeliveredAt(clock.instant());}
        else if(Boolean.TRUE.equals(input.codCollected()))throw BusinessException.invalid("Chỉ xác nhận COD khi giao thành công.");
        order.setStatus(target.name());order.setUpdatedAt(clock.instant());orders.saveAndFlush(order);history(order,actorId,current.name(),target==OrderState.DELIVERED?"Đã giao và thu COD":"Cập nhật trạng thái");return view(order);
    }
    private void history(PurchaseOrder order,Long actorId,String previous,String reason) {var h=new OrderHistory();h.setOrderId(order.getId());h.setActorUserId(actorId);h.setFromStatus(previous);h.setToStatus(order.getStatus());h.setReason(reason);h.setOrderVersion(order.getVersion());h.setOccurredAt(clock.instant());histories.saveAndFlush(h);}
    private OrderView view(PurchaseOrder o) {
        var lines=orderItems.findByOrderIdOrderByProductIdAsc(o.getId()).stream().map(i -> new ItemView(i.getProductId(),i.getSkuSnapshot(),i.getNameSnapshot(),i.getUnitPrice(),i.getQuantity(),i.getLineTotal())).toList();
        var timeline=histories.findByOrderIdOrderByOrderVersionAsc(o.getId()).stream().map(h -> new HistoryView(h.getFromStatus(),h.getToStatus(),h.getOccurredAt(),h.getReason())).toList();
        return new OrderView(o.getId(),code(o.getId()),o.getStatus(),o.getSubtotal(),o.getShippingFee(),o.getTotalAmount(),o.getCurrency(),o.getCreatedAt(),o.getDeliveredAt(),o.getVersion(),o.getRecipientName(),o.getPhone(),o.getAddress(),o.getNote(),lines,timeline);
    }
    private static String code(Long id) {return "TS-"+String.format(Locale.ROOT,"%08d",id);}
    private static String canonicalKey(String value) {if(value==null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw BusinessException.invalid("Mã yêu cầu phải là UUID.");return UUID.fromString(value).toString();}
    private static String quote(String value) {
        if(value==null)return "null";var out=new StringBuilder("\"");
        for(char c:value.toCharArray()) {if(c=='"' || c=='\\')out.append('\\').append(c);else if(c<32)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}
        return out.append('"').toString();
    }
}
