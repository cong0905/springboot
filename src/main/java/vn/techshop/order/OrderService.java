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
    public record Checkout(@NotBlank String checkoutKey,@NotNull Long cartVersion,@NotNull @DecimalMin("0") BigDecimal expectedTotal,@NotBlank @Pattern(regexp="[0-9a-f]{64}") String expectedPricingHash,@NotBlank @Size(min=2,max=100) String recipientName,@NotBlank @Pattern(regexp="0[0-9]{9}") String phone,@NotBlank @Size(min=10,max=500) String address,@Size(max=500) String note,String couponCode,String paymentMethod,Options options,List<Long> selectedItemIds) {
        public Checkout(String key,Long version,BigDecimal total,String hash,String name,String phone,String address,String note,String coupon,String method,Options options){this(key,version,total,hash,name,phone,address,note,coupon,method,options,null);}
        public Checkout(String key,Long version,BigDecimal total,String hash,String name,String phone,String address,String note,String coupon,String method) {this(key,version,total,hash,name,phone,address,note,coupon,method,null);}
        public Checkout(String checkoutKey, Long cartVersion, BigDecimal expectedTotal, String expectedPricingHash, String recipientName, String phone, String address, String note) {
            this(checkoutKey, cartVersion, expectedTotal, expectedPricingHash, recipientName, phone, address, note, null, "COD",null);
        }
        public Checkout(String checkoutKey, Long cartVersion, BigDecimal expectedTotal, String expectedPricingHash, String recipientName, String phone, String address, String note, String couponCode) {
            this(checkoutKey, cartVersion, expectedTotal, expectedPricingHash, recipientName, phone, address, note, couponCode, "COD",null);
        }
    }
    public record Options(String fulfillmentType,Long pickupBranchId,Instant pickupTime,Double latitude,Double longitude,Long tradeInRequestId,Long installmentPlanId) {}
    public record PreviewRequest(String couponCode,Options options,List<Long> selectedItemIds) {public PreviewRequest(String couponCode,Options options){this(couponCode,options,null);}}
    public record Preview(Long cartVersion,BigDecimal subtotal,BigDecimal discountAmount,BigDecimal shippingFee,BigDecimal total,String pricingHash,Long assignedBranchId,String fulfillmentType) {}
    public record Cancel(@NotBlank @Size(min=5,max=500) String reason,@NotNull Long expectedVersion) {}
    public record Transition(@NotBlank String targetStatus,@NotNull Long expectedVersion,Boolean codCollected) {}
    public record ItemView(Long productId,String sku,String name,BigDecimal unitPrice,int quantity,BigDecimal lineTotal,Long variantId,List<String> imeiNumbers) {}
    public record HistoryView(String fromStatus,String toStatus,Instant occurredAt,String reason) {}
    public record PublicItemView(String name, String sku, int quantity, BigDecimal lineTotal) {}
    public record PublicHistoryView(String toStatus, Instant occurredAt, String reason) {}
    public record PublicOrderView(String orderCode, String status, BigDecimal subtotal, BigDecimal discountAmount, String couponCode, BigDecimal shippingFee, BigDecimal total, String currency, Instant createdAt, Instant deliveredAt, String maskedRecipientName, String maskedPhone, String maskedAddress, String paymentMethod, String paymentStatus, List<PublicItemView> items, List<PublicHistoryView> history, BigDecimal refundedAmount) {
        public PublicOrderView(String orderCode, String status, BigDecimal subtotal, BigDecimal discountAmount, String couponCode, BigDecimal shippingFee, BigDecimal total, String currency, Instant createdAt, Instant deliveredAt, String maskedRecipientName, String maskedPhone, String maskedAddress, String paymentMethod, String paymentStatus, List<PublicItemView> items, List<PublicHistoryView> history) {
            this(orderCode, status, subtotal, discountAmount, couponCode, shippingFee, total, currency, createdAt, deliveredAt, maskedRecipientName, maskedPhone, maskedAddress, paymentMethod, paymentStatus, items, history, BigDecimal.ZERO);
        }
    }
    public record OrderView(Long id,String orderCode,String status,BigDecimal subtotal,BigDecimal discountAmount,String couponCode,BigDecimal shippingFee,BigDecimal total,String currency,Instant createdAt,Instant deliveredAt,Long version,String recipientName,String phone,String address,String note,String paymentMethod,String paymentStatus,String paymentUrl,List<ItemView> items,List<HistoryView> history,String fulfillmentType,Long assignedBranchId,String pickupCode,Instant holdExpiresAt,BigDecimal refundedAmount) {
        public OrderView(Long id,String orderCode,String status,BigDecimal subtotal,BigDecimal discountAmount,String couponCode,BigDecimal shippingFee,BigDecimal total,String currency,Instant createdAt,Instant deliveredAt,Long version,String recipientName,String phone,String address,String note,String paymentMethod,String paymentStatus,String paymentUrl,List<ItemView> items,List<HistoryView> history,String fulfillmentType,Long assignedBranchId,String pickupCode,Instant holdExpiresAt) {
            this(id,orderCode,status,subtotal,discountAmount,couponCode,shippingFee,total,currency,createdAt,deliveredAt,version,recipientName,phone,address,note,paymentMethod,paymentStatus,paymentUrl,items,history,fulfillmentType,assignedBranchId,pickupCode,holdExpiresAt,BigDecimal.ZERO);
        }
    }
    public record Summary(Long id,String orderCode,String status,BigDecimal total,String currency,Instant createdAt,Long version,String paymentStatus) {
        public Summary(Long id,String orderCode,String status,BigDecimal total,String currency,Instant createdAt,Long version) {
            this(id,orderCode,status,total,currency,createdAt,version,"UNPAID");
        }
    }
    public record CheckoutResult(OrderView order,boolean replay) {}
    private final OrderRepository orders;private final OrderItemRepository orderItems;private final OrderHistoryRepository histories;
    private final CartRepository carts;private final CartItemRepository cartItems;private final CartService cartService;
    private final ProductRepository products;private final CategoryRepository categories;private final InventoryService inventory;private final vn.techshop.coupon.CouponService couponService;private final vn.techshop.payment.PaymentService paymentService;private final Clock clock;
    private final vn.techshop.variant.ProductVariantService variants;
    private final vn.techshop.branch.BranchInventoryService branches;
    private final OrderRoutingService routing;
    private final vn.techshop.loyalty.LoyaltyService loyalty;
    private final vn.techshop.tradein.TradeInService tradein;
    private final vn.techshop.installment.InstallmentService installments;
    private final vn.techshop.warranty.WarrantyService warranty;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    @org.springframework.beans.factory.annotation.Value("${app.order.cod-expiry-hours:24}")
    private int codExpiryHours = 24;
    public OrderService(OrderRepository orders,OrderItemRepository orderItems,OrderHistoryRepository histories,CartRepository carts,CartItemRepository cartItems,CartService cartService,ProductRepository products,CategoryRepository categories,InventoryService inventory,vn.techshop.coupon.CouponService couponService,vn.techshop.payment.PaymentService paymentService,Clock clock,vn.techshop.variant.ProductVariantService variants,vn.techshop.branch.BranchInventoryService branches,OrderRoutingService routing,vn.techshop.loyalty.LoyaltyService loyalty,vn.techshop.tradein.TradeInService tradein,vn.techshop.installment.InstallmentService installments,vn.techshop.warranty.WarrantyService warranty,org.springframework.jdbc.core.JdbcTemplate jdbc) {this.variants=variants;this.branches=branches;this.routing=routing;this.loyalty=loyalty;this.tradein=tradein;this.installments=installments;this.warranty=warranty;this.jdbc=jdbc;this.orders=orders;this.orderItems=orderItems;this.histories=histories;this.carts=carts;this.cartItems=cartItems;this.cartService=cartService;this.products=products;this.categories=categories;this.inventory=inventory;this.couponService=couponService;this.paymentService=paymentService;this.clock=clock;}

    private static Options defaults(Options o) {return o==null?new Options("HOME_DELIVERY_STANDARD",null,null,null,null,null,null):new Options(o.fulfillmentType()==null?"HOME_DELIVERY_STANDARD":o.fulfillmentType(),o.pickupBranchId(),o.pickupTime(),o.latitude(),o.longitude(),o.tradeInRequestId(),o.installmentPlanId());}
    public Preview preview(Long userId,PreviewRequest request) {
        var c=cartService.selected(userId,request.selectedItemIds());if(!c.canCheckout())throw BusinessException.invalid("Giỏ hàng chưa đủ điều kiện đặt.");
        var o=defaults(request.options());
        var needs=c.items().stream().filter(i->i.variantId()!=null).map(i->new OrderRoutingService.Need(i.variantId(),i.quantity())).toList();
        if(!needs.isEmpty()&&needs.size()!=c.items().size())throw BusinessException.invalid("Đặt sản phẩm theo IMEI và sản phẩm thường trong các đơn riêng.");
        var route=routing.route(needs,o.fulfillmentType(),o.pickupBranchId(),o.latitude(),o.longitude(),c.shippingFee());
        BigDecimal discount=request.couponCode()==null||request.couponCode().isBlank()?BigDecimal.ZERO:couponService.validateAndCalculate(request.couponCode(),c.subtotal(),userId).discountAmount();
        discount=discount.add(loyalty.discount(userId,c.subtotal())).min(c.subtotal());
        discount=discount.add(tradein.credit(userId,o.tradeInRequestId(),c.subtotal().subtract(discount)));
        var lines=c.items().stream().map(i->new Pricing.Line(i.variantId()==null?i.productId():-i.variantId(),i.quantity(),i.unitPrice())).toList();
        return new Preview(c.version(),c.subtotal(),discount,route.shippingFee(),c.subtotal().subtract(discount).add(route.shippingFee()),Pricing.hash(lines,route.shippingFee()),route.branchId(),o.fulfillmentType());
    }
    @Transactional public CheckoutResult checkout(Long userId,Checkout request) {
        return checkout(userId,request,"127.0.0.1");
    }
    @Transactional public CheckoutResult checkout(Long userId,Checkout request,String clientIp) {
        String key=canonicalKey(request.checkoutKey());String name=Rules.text(request.recipientName(),2,100);String address=Rules.text(request.address(),10,500);String phone=Rules.text(request.phone(),10,10);String note=Rules.optional(request.note(),500);
        String couponCode=request.couponCode()!=null && !request.couponCode().isBlank()?request.couponCode().trim():null;
        String paymentMethod=(request.paymentMethod()==null || request.paymentMethod().isBlank())?"COD":request.paymentMethod().trim().toUpperCase();
        if(!phone.matches("0[0-9]{9}") || request.expectedTotal()==null || request.expectedTotal().signum()<0 || request.expectedTotal().stripTrailingZeros().scale()>0 || request.expectedPricingHash()==null || !request.expectedPricingHash().matches("[0-9a-f]{64}")) throw BusinessException.invalid("Thông tin đơn không hợp lệ.");
        var options=defaults(request.options());
        if(!Set.of("COD","MOCK","VNPAY","INSTALLMENT").contains(paymentMethod))throw BusinessException.invalid("Phương thức thanh toán không hợp lệ.");
        String hash=Pricing.sha256("{\"userId\":"+userId+",\"cartVersion\":"+request.cartVersion()+",\"expectedTotal\":"+quote(request.expectedTotal().setScale(0).toPlainString())+",\"expectedPricingHash\":"+quote(request.expectedPricingHash())+",\"recipientName\":"+quote(name)+",\"phone\":"+quote(phone)+",\"address\":"+quote(address)+",\"note\":"+quote(note)+",\"couponCode\":"+quote(couponCode)+",\"paymentMethod\":"+quote(paymentMethod)+",\"options\":"+quote(options.toString())+"}");
        var cart=carts.lockByUserId(userId).orElseThrow(BusinessException::missing);
        if(request.selectedItemIds()!=null)hash=Pricing.sha256(hash+"|selected:"+request.selectedItemIds().stream().sorted().toList());
        var existing=orders.findByUserIdAndCheckoutKey(userId,key);
        if(existing.isPresent()) {var order=existing.get();if(!order.getRequestHash().equals(hash))throw BusinessException.conflict("CHECKOUT_KEY_REUSED","Mã yêu cầu đã dùng với thông tin khác.");return new CheckoutResult(view(order),true);}
        Rules.version(cart.getVersion(),request.cartVersion(),"CART_CHANGED");var rows=cartItems.findByCartIdOrderByProductIdAsc(cart.getId());
        var selected=CartService.selection(request.selectedItemIds(),rows.stream().map(CartItem::getId).toList());
        rows=rows.stream().filter(i->selected.contains(i.getId())).toList();
        if(rows.isEmpty())throw BusinessException.invalid("Giỏ hàng đang trống.");
        var prices=new HashMap<Long,BigDecimal>();var variantMap=new HashMap<Long,vn.techshop.variant.ProductVariant>();
        var locked=new LinkedHashMap<Long,Product>();var pricing=new ArrayList<Pricing.Line>();BigDecimal subtotal=BigDecimal.ZERO;
        for(var row:rows) {
            var p=products.lockById(row.getProductId()).orElseThrow(BusinessException::missing);var category=categories.findById(p.getCategoryId()).orElseThrow(BusinessException::missing);
            if(!"ACTIVE".equals(p.getStatus()) || !"ACTIVE".equals(category.getStatus()))throw BusinessException.conflict("PRODUCT_UNAVAILABLE","Sản phẩm đang ngừng bán.");
            if(p.getStockQuantity()<row.getQuantity())throw BusinessException.conflict("INSUFFICIENT_STOCK","Số lượng còn lại không đủ.");
            var variant=row.getVariantId()==null?null:variants.require(row.getVariantId(),p.getId());
            if(variant==null&&variants.hasVariants(p.getId()))throw BusinessException.invalid("Chọn lại biến thể trước khi đặt.");
            BigDecimal price=variant==null?p.getPrice():variant.getPrice();prices.put(row.getId(),price);if(variant!=null)variantMap.put(row.getId(),variant);
            locked.put(p.getId(),p);pricing.add(new Pricing.Line(variant==null?p.getId():-variant.getId(),row.getQuantity(),price));subtotal=subtotal.add(price.multiply(BigDecimal.valueOf(row.getQuantity())));
        }
        var quoted=preview(userId,new PreviewRequest(couponCode,options,request.selectedItemIds()));
        BigDecimal fee=quoted.shippingFee();
        BigDecimal discount=BigDecimal.ZERO;
        if(couponCode!=null) {
            discount=couponService.redeem(couponCode,subtotal,userId);
        }
        discount=discount.add(loyalty.discount(userId,subtotal)).min(subtotal);
        discount=discount.add(tradein.credit(userId,options.tradeInRequestId(),subtotal.subtract(discount)));
        BigDecimal total=subtotal.subtract(discount).add(fee);
        if(total.compareTo(request.expectedTotal())!=0 || !Pricing.hash(pricing,fee).equals(request.expectedPricingHash()))throw BusinessException.conflict("PRICE_CHANGED","Giá đã thay đổi. Vui lòng xác nhận lại giỏ hàng.");
        var order=new PurchaseOrder();order.setUserId(userId);order.setCheckoutKey(key);order.setRequestHash(hash);order.setStatus("PENDING");order.setRecipientName(name);order.setPhone(phone);order.setAddress(address);order.setNote(note);order.setCouponCode(couponCode);order.setDiscountAmount(discount);order.setCurrency("VND");order.setSubtotal(subtotal);order.setShippingFee(fee);order.setTotalAmount(total);order.setPaymentMethod(paymentMethod);order.setPaymentStatus("COD".equals(paymentMethod)?"UNPAID":"PENDING");order.setCodCollected(false);order.setCreatedAt(clock.instant());order.setUpdatedAt(clock.instant());order.setFulfillmentType(options.fulfillmentType());order.setAssignedBranchId(quoted.assignedBranchId());
        if("CLICK_AND_COLLECT".equals(options.fulfillmentType())) {
            if(options.pickupTime()!=null&&(options.pickupTime().isBefore(clock.instant())||options.pickupTime().isAfter(clock.instant().plus(Duration.ofHours(24)))))throw BusinessException.invalid("Hẹn lấy máy trong vòng 24 giờ.");
            order.setPickupBranchId(options.pickupBranchId());order.setPickupTime(options.pickupTime());order.setHoldExpiresAt(clock.instant().plus(Duration.ofHours(24)));order.setPickupCode(UUID.randomUUID().toString());
        } else if(!"COD".equals(paymentMethod) && !"INSTALLMENT".equals(paymentMethod)) {
            order.setHoldExpiresAt(clock.instant().plus(Duration.ofMinutes(15)));
        } else if("INSTALLMENT".equals(paymentMethod)) {
            order.setHoldExpiresAt(clock.instant().plus(Duration.ofHours(48)));
        } else if("COD".equals(paymentMethod)) {
            order.setHoldExpiresAt(clock.instant().plus(Duration.ofHours(codExpiryHours)));
        }
        orders.saveAndFlush(order);
        tradein.use(userId,options.tradeInRequestId(),order.getId());
        if("INSTALLMENT".equals(paymentMethod)) {if(options.installmentPlanId()==null)throw BusinessException.invalid("Chọn gói trả góp.");installments.apply(order,options.installmentPlanId());}
        for(var row:rows) {
            var p=locked.get(row.getProductId());var item=new OrderItem();item.setOrderId(order.getId());item.setProductId(p.getId());var variant=variantMap.get(row.getId());item.setVariantId(row.getVariantId());item.setSkuSnapshot(variant==null?p.getSku():variant.getSkuCode());item.setNameSnapshot(p.getName()+(variant==null?"":" · "+variant.getColorName()+" / "+variant.getStorageCapacity()));item.setUnitPrice(prices.get(row.getId()));item.setQuantity(row.getQuantity());item.setLineTotal(prices.get(row.getId()).multiply(BigDecimal.valueOf(row.getQuantity())));orderItems.saveAndFlush(item);
            if(variant!=null)branches.reserve(variant.getId(),quoted.assignedBranchId(),item.getId(),row.getQuantity());
            inventory.apply(p,userId,item.getId(),"ORDER",-row.getQuantity(),"Đặt đơn "+paymentMethod);
        }
        history(order,userId,null,"Đặt đơn "+paymentMethod);
        var paymentResult="INSTALLMENT".equals(paymentMethod)?new vn.techshop.payment.PaymentService.PaymentResult(null,null,"INSTALLMENT"):paymentService.initiatePayment(order,paymentMethod,clientIp);
        cartItems.deleteAll(rows);cartService.touch(cart);orders.flush();
        return new CheckoutResult(view(order,paymentResult.paymentUrl()),false);
    }

    public PageView<Summary> list(Long userId,boolean admin,String status,int page,int size) {
        return list(userId, admin, status, null, null, null, page, size);
    }
    public PageView<Summary> list(Long userId,boolean admin,String status,LocalDate fromDate,LocalDate toDate,String keyword,int page,int size) {
        if(fromDate!=null && toDate!=null && fromDate.isAfter(toDate)) throw BusinessException.invalid("Từ ngày không được lớn hơn đến ngày.");
        String selected=status==null || status.isBlank()?null:status;
        if(selected!=null)OrderState.parse(selected);
        Specification<PurchaseOrder> spec=(root,query,cb) -> {
            var checks=new ArrayList<jakarta.persistence.criteria.Predicate>();
            if(!admin)checks.add(cb.equal(root.get("userId"),userId));
            if(selected!=null)checks.add(cb.equal(root.get("status"),selected));
            if(fromDate!=null)checks.add(cb.greaterThanOrEqualTo(root.get("createdAt"),fromDate.atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()));
            if(toDate!=null)checks.add(cb.lessThan(root.get("createdAt"),toDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant()));
            if(keyword!=null && !keyword.isBlank()) {
                String term="%" + keyword.strip().toLowerCase(Locale.ROOT) + "%";
                var kwPredicate=cb.or(cb.like(cb.lower(root.get("recipientName")),term),cb.like(root.get("phone"),term));
                try {
                    String clean=keyword.strip();
                    if(clean.toUpperCase(Locale.ROOT).startsWith("TS-")) clean=clean.substring(3);
                    Long idSearch=Long.parseLong(clean);
                    kwPredicate=cb.or(kwPredicate,cb.equal(root.get("id"),idSearch));
                } catch(NumberFormatException ignored) {}
                checks.add(kwPredicate);
            }
            return cb.and(checks.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
        return PageView.of(orders.findAll(spec,PageView.request(page,size,Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")))).map(o -> new Summary(o.getId(),code(o.getId()),o.getStatus(),o.getTotalAmount(),o.getCurrency(),o.getCreatedAt(),o.getVersion(),o.getPaymentStatus())));
    }
    public Optional<PublicOrderView> lookup(String codeOrId, String phone) {
        if(codeOrId==null || phone==null || !phone.matches("0[0-9]{9}")) return Optional.empty();
        try {
            String clean=codeOrId.trim();
            if(clean.toUpperCase(Locale.ROOT).startsWith("TS-")) clean=clean.substring(3);
            Long id=Long.parseLong(clean);
            return orders.findByIdAndPhone(id,phone.trim()).map(this::publicView);
        } catch(NumberFormatException e) {
            return Optional.empty();
        }
    }
    public OrderView detail(Long id,Long userId,boolean admin) {return view((admin?orders.findById(id):orders.findByIdAndUserId(id,userId)).orElseThrow(BusinessException::missing));}
    @Transactional public OrderView cancel(Long id,Long actorId,boolean admin,Cancel input) {
        String reason=Rules.text(input.reason(),5,500);var order=(admin?orders.lockById(id):orders.lockOwned(id,actorId)).orElseThrow(BusinessException::missing);
        if("CANCELLED".equals(order.getStatus()))return view(order);
        Rules.version(order.getVersion(),input.expectedVersion(),"STALE_VERSION");if("PAID".equals(order.getPaymentStatus()))throw BusinessException.conflict("REFUND_REQUIRED","Đơn đã trả tiền cần quy trình hoàn tiền tại cửa hàng.");String previous=order.getStatus();
        if(!"PENDING".equals(previous) && !(admin && "CONFIRMED".equals(previous)))throw BusinessException.conflict("INVALID_ORDER_TRANSITION","Đơn không thể hủy ở trạng thái hiện tại.");
        for(var item:orderItems.findByOrderIdOrderByProductIdAsc(id)) {var product=products.lockById(item.getProductId()).orElseThrow(BusinessException::missing);inventory.apply(product,actorId,item.getId(),"CANCEL",item.getQuantity(),reason);branches.release(item.getId());}
        tradein.cancel(id);order.setStatus("CANCELLED");order.setUpdatedAt(clock.instant());orders.saveAndFlush(order);history(order,actorId,previous,reason);return view(order);
    }
    @Transactional public OrderView transition(Long id,Long actorId,Transition input) {
        var order=orders.lockById(id).orElseThrow(BusinessException::missing);Rules.version(order.getVersion(),input.expectedVersion(),"STALE_VERSION");var current=OrderState.parse(order.getStatus());var target=OrderState.parse(input.targetStatus());
        if(!current.canMoveTo(target))throw BusinessException.conflict("INVALID_ORDER_TRANSITION","Không được bỏ bước hoặc chuyển ngược trạng thái.");
        if(!"COD".equals(order.getPaymentMethod())&&!"INSTALLMENT".equals(order.getPaymentMethod())&&!"PAID".equals(order.getPaymentStatus()))throw BusinessException.conflict("PAYMENT_REQUIRED","Đơn online chưa thanh toán.");
        if("INSTALLMENT".equals(order.getPaymentMethod()))installments.requireApproved(id);
        if(target==OrderState.CONFIRMED && "COD".equals(order.getPaymentMethod()) && !"CLICK_AND_COLLECT".equals(order.getFulfillmentType())) {
            order.setHoldExpiresAt(null);
        }
        if(target==OrderState.DELIVERED) {
            if(order.getHoldExpiresAt()!=null&&order.getHoldExpiresAt().isBefore(clock.instant())&&!"PAID".equals(order.getPaymentStatus()))throw BusinessException.conflict("PICKUP_EXPIRED","Đơn giữ máy đã hết hạn.");
            for(var item:orderItems.findByOrderIdOrderByProductIdAsc(id)) {products.lockById(item.getProductId()).orElseThrow(BusinessException::missing);branches.sell(item.getId());}
            if("COD".equals(order.getPaymentMethod())) {
                if(!Boolean.TRUE.equals(input.codCollected()))throw BusinessException.invalid("Cần xác nhận đã thu COD.");
                order.setCodCollected(true);
                order.setPaymentStatus("PAID");
            } else {
                if(Boolean.TRUE.equals(input.codCollected()))throw BusinessException.invalid("Đơn không áp dụng thu COD.");
                order.setCodCollected(false);
                if("INSTALLMENT".equals(order.getPaymentMethod())) {
                    order.setPaymentStatus("PAID");
                }
            }
            order.setDeliveredAt(clock.instant());}
        else if(Boolean.TRUE.equals(input.codCollected()))throw BusinessException.invalid("Chỉ xác nhận COD khi giao thành công.");
        order.setStatus(target.name());order.setUpdatedAt(clock.instant());orders.saveAndFlush(order);if(target==OrderState.DELIVERED) {loyalty.delivered(order);warranty.activate(order);}
        String reason = target==OrderState.DELIVERED
            ? ("COD".equals(order.getPaymentMethod()) ? "Đã giao và thu COD" : ("INSTALLMENT".equals(order.getPaymentMethod()) ? "Đã giao hàng (Hồ sơ trả góp duyệt)" : "Đã giao hàng (Đã thanh toán online)"))
            : "Cập nhật trạng thái";
        history(order,actorId,current.name(),reason);return view(order);
    }
    private void history(PurchaseOrder order,Long actorId,String previous,String reason) {var h=new OrderHistory();h.setOrderId(order.getId());h.setActorUserId(actorId);h.setFromStatus(previous);h.setToStatus(order.getStatus());h.setReason(reason);h.setOrderVersion(order.getVersion());h.setOccurredAt(clock.instant());histories.saveAndFlush(h);}
    private OrderView view(PurchaseOrder o) {
        return view(o, paymentService.resumeUrl(o));
    }
    private String resolvePaymentStatus(PurchaseOrder o, BigDecimal refunded) {
        if ("REFUNDED".equals(o.getPaymentStatus()) || (refunded != null && o.getTotalAmount() != null && o.getTotalAmount().compareTo(BigDecimal.ZERO) > 0 && refunded.compareTo(o.getTotalAmount()) >= 0)) {
            return "REFUNDED";
        }
        return o.getPaymentStatus() != null ? o.getPaymentStatus() : "UNPAID";
    }
    private OrderView view(PurchaseOrder o, String paymentUrl) {
        var lines=orderItems.findByOrderIdOrderByProductIdAsc(o.getId()).stream().map(i -> new ItemView(i.getProductId(),i.getSkuSnapshot(),i.getNameSnapshot(),i.getUnitPrice(),i.getQuantity(),i.getLineTotal(),i.getVariantId(),jdbc.queryForList("select imei_number from product_items_imei where order_item_id=? order by id",String.class,i.getId()))).toList();
        var timeline=histories.findByOrderIdOrderByOrderVersionAsc(o.getId()).stream().map(h -> new HistoryView(h.getFromStatus(),h.getToStatus(),h.getOccurredAt(),h.getReason())).toList();
        BigDecimal refunded = jdbc.queryForObject(
            "select coalesce(sum(amount), 0) from refund_reconciliations where order_id=? and status='SUCCEEDED'",
            BigDecimal.class, o.getId()
        );
        if (refunded == null) refunded = BigDecimal.ZERO;
        String payStatus = resolvePaymentStatus(o, refunded);
        return new OrderView(o.getId(),code(o.getId()),o.getStatus(),o.getSubtotal(),o.getDiscountAmount()!=null?o.getDiscountAmount():BigDecimal.ZERO,o.getCouponCode(),o.getShippingFee(),o.getTotalAmount(),o.getCurrency(),o.getCreatedAt(),o.getDeliveredAt(),o.getVersion(),o.getRecipientName(),o.getPhone(),o.getAddress(),o.getNote(),o.getPaymentMethod()!=null?o.getPaymentMethod():"COD",payStatus,paymentUrl,lines,timeline,o.getFulfillmentType(),o.getAssignedBranchId(),o.getPickupCode(),o.getHoldExpiresAt(),refunded);
    }
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return "***";
        String trimmed = name.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace > 0 && lastSpace < trimmed.length() - 1) {
            String firstWord = trimmed.substring(0, trimmed.indexOf(' '));
            String lastWord = trimmed.substring(lastSpace + 1);
            String firstPart = firstWord.length() >= 2 ? firstWord.substring(0, 2) : firstWord;
            return firstPart + "*** " + lastWord;
        }
        return (trimmed.length() > 2 ? trimmed.substring(0, 2) : (trimmed.charAt(0) + "")) + "***";
    }
    @Transactional public CartService.CartView reorder(Long userId,Long orderId){
        var previous=detail(orderId,userId,false);
        for(var item:previous.items())cartService.add(userId,new CartService.Add(item.productId(),item.quantity(),cartService.view(userId).version(),item.variantId()));
        return cartService.view(userId);
    }
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        String trimmed = phone.trim();
        return trimmed.substring(0, 2) + "****" + trimmed.substring(trimmed.length() - 4);
    }
    public static String maskAddress(String address) {
        if (address == null || address.isBlank()) return "***";
        int commaIndex = address.indexOf(',');
        if (commaIndex != -1 && commaIndex < address.length() - 1) {
            return "***," + address.substring(commaIndex + 1);
        }
        return "***";
    }
    private PublicOrderView publicView(PurchaseOrder o) {
        var lines = orderItems.findByOrderIdOrderByProductIdAsc(o.getId()).stream()
            .map(i -> new PublicItemView(i.getNameSnapshot(), i.getSkuSnapshot(), i.getQuantity(), i.getLineTotal()))
            .toList();
        var timeline = histories.findByOrderIdOrderByOrderVersionAsc(o.getId()).stream()
            .map(h -> new PublicHistoryView(h.getToStatus(), h.getOccurredAt(), h.getReason()))
            .toList();
        BigDecimal refunded = jdbc.queryForObject(
            "select coalesce(sum(amount), 0) from refund_reconciliations where order_id=? and status='SUCCEEDED'",
            BigDecimal.class, o.getId()
        );
        if (refunded == null) refunded = BigDecimal.ZERO;
        String payStatus = resolvePaymentStatus(o, refunded);
        return new PublicOrderView(
            code(o.getId()),
            o.getStatus(),
            o.getSubtotal(),
            o.getDiscountAmount() != null ? o.getDiscountAmount() : BigDecimal.ZERO,
            o.getCouponCode(),
            o.getShippingFee(),
            o.getTotalAmount(),
            o.getCurrency(),
            o.getCreatedAt(),
            o.getDeliveredAt(),
            maskName(o.getRecipientName()),
            maskPhone(o.getPhone()),
            maskAddress(o.getAddress()),
            o.getPaymentMethod() != null ? o.getPaymentMethod() : "COD",
            payStatus,
            lines,
            timeline,
            refunded
        );
    }
    private static String code(Long id) {return "TS-"+String.format(Locale.ROOT,"%08d",id);}
    private static String canonicalKey(String value) {if(value==null || !value.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))throw BusinessException.invalid("Mã yêu cầu phải là UUID.");return UUID.fromString(value).toString();}
    private static String quote(String value) {
        if(value==null)return "null";var out=new StringBuilder("\"");
        for(char c:value.toCharArray()) {if(c=='"' || c=='\\')out.append('\\').append(c);else if(c<32)out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}
        return out.append('"').toString();
    }
}
