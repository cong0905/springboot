package vn.techshop.cart;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.catalog.*;
import vn.techshop.shared.*;

@Service
@Transactional(readOnly=true)
public class CartService {
    public record Add(@NotNull Long productId,@NotNull @Min(1) @Max(99) Integer quantity,@NotNull Long expectedVersion) {}
    public record Update(@NotNull @Min(1) @Max(99) Integer quantity,@NotNull Long expectedVersion) {}
    public record ItemView(Long itemId,Long productId,String name,BigDecimal unitPrice,int quantity,BigDecimal lineTotal,boolean available,String warningCode) {}
    public record CartView(Long version,List<ItemView> items,BigDecimal subtotal,BigDecimal shippingFee,BigDecimal total,String currency,String pricingHash,boolean canCheckout) {}
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final Clock clock;
    public CartService(CartRepository carts,CartItemRepository items,ProductRepository products,CategoryRepository categories,Clock clock) {this.carts=carts;this.items=items;this.products=products;this.categories=categories;this.clock=clock;}
    public CartView view(Long userId) { return view(carts.findByUserId(userId).orElseThrow(BusinessException::missing)); }
    public CartView view(Cart cart) {
        var rows=items.findByCartIdOrderByProductIdAsc(cart.getId());var views=new ArrayList<ItemView>();BigDecimal subtotal=BigDecimal.ZERO;
        for(var row:rows) {
            var p=products.findById(row.getProductId()).orElseThrow(BusinessException::missing);var c=categories.findById(p.getCategoryId()).orElseThrow(BusinessException::missing);
            boolean active="ACTIVE".equals(p.getStatus()) && "ACTIVE".equals(c.getStatus());boolean available=active && p.getStockQuantity()>=row.getQuantity();BigDecimal line=p.getPrice().multiply(BigDecimal.valueOf(row.getQuantity()));subtotal=subtotal.add(line);
            views.add(new ItemView(row.getId(),p.getId(),p.getName(),p.getPrice(),row.getQuantity(),line,available,available?null:(active?"INSUFFICIENT_STOCK":"PRODUCT_UNAVAILABLE")));
        }
        var fee=rows.isEmpty()?BigDecimal.ZERO:Pricing.fee(subtotal);
        var lines=views.stream().map(v -> new Pricing.Line(v.productId(),v.quantity(),v.unitPrice())).toList();
        return new CartView(cart.getVersion(),List.copyOf(views),subtotal,fee,subtotal.add(fee),"VND",Pricing.hash(lines,fee),!views.isEmpty() && views.stream().allMatch(ItemView::available));
    }
    @Transactional public CartView add(Long userId,Add input) {
        Rules.quantity(input.quantity());var cart=lock(userId,input.expectedVersion());
        var product=products.findById(input.productId()).orElseThrow(BusinessException::missing);
        var category=categories.findById(product.getCategoryId()).orElseThrow(BusinessException::missing);
        if(!"ACTIVE".equals(product.getStatus()) || !"ACTIVE".equals(category.getStatus())) throw BusinessException.conflict("PRODUCT_UNAVAILABLE","Sản phẩm đang ngừng bán.");
        var item=items.findByCartIdAndProductId(cart.getId(),product.getId()).orElse(null);
        if(item==null) {if(items.countByCartId(cart.getId())>=20) throw BusinessException.conflict("CART_LIMIT","Giỏ hàng tối đa 20 sản phẩm.");item=new CartItem();item.setCartId(cart.getId());item.setProductId(product.getId());item.setQuantity(0);item.setCreatedAt(clock.instant());}
        Rules.quantity(item.getQuantity()+input.quantity());item.setQuantity(item.getQuantity()+input.quantity());item.setUpdatedAt(clock.instant());items.save(item);touch(cart);return view(cart);
    }
    @Transactional public CartView update(Long userId,Long itemId,Update input) {
        Rules.quantity(input.quantity());var cart=lock(userId,input.expectedVersion());var item=items.findByIdAndCartId(itemId,cart.getId()).orElseThrow(BusinessException::missing);
        item.setQuantity(input.quantity());item.setUpdatedAt(clock.instant());items.save(item);touch(cart);return view(cart);
    }
    @Transactional public void remove(Long userId,Long itemId,Long expectedVersion) {
        var cart=lock(userId,expectedVersion);items.delete(items.findByIdAndCartId(itemId,cart.getId()).orElseThrow(BusinessException::missing));touch(cart);
    }
    private Cart lock(Long userId,Long expected) {var c=carts.lockByUserId(userId).orElseThrow(BusinessException::missing);Rules.version(c.getVersion(),expected,"CART_CHANGED");return c;}
    public void touch(Cart cart) {var now=clock.instant();cart.setUpdatedAt(now.isAfter(cart.getUpdatedAt())?now:cart.getUpdatedAt().plusNanos(1000));carts.saveAndFlush(cart);}
}
