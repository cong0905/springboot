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
    public record Add(@NotNull Long productId,@NotNull @Min(1) @Max(99) Integer quantity,@NotNull Long expectedVersion,Long variantId) {
        public Add(Long productId,Integer quantity,Long expectedVersion) {this(productId,quantity,expectedVersion,null);}
    }
    public record Update(@NotNull @Min(1) @Max(99) Integer quantity,@NotNull Long expectedVersion) {}
    public record RemoveMany(@NotNull @Size(min=1,max=20) List<@NotNull Long> itemIds,@NotNull Long expectedVersion) {}
    public record ItemView(Long itemId,Long productId,String name,BigDecimal unitPrice,int quantity,BigDecimal lineTotal,boolean available,String warningCode,Long variantId,String imagePath) {
        public ItemView(Long itemId,Long productId,String name,BigDecimal unitPrice,int quantity,BigDecimal lineTotal,boolean available,String warningCode,Long variantId) {
            this(itemId,productId,name,unitPrice,quantity,lineTotal,available,warningCode,variantId,"/assets/products/device.svg");
        }
    }
    public record CartView(Long version,List<ItemView> items,BigDecimal subtotal,BigDecimal shippingFee,BigDecimal total,String currency,String pricingHash,boolean canCheckout) {}
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductRepository products;
    private final CategoryRepository categories;
    private final Clock clock;
    private final vn.techshop.variant.ProductVariantService variants;
    private final vn.techshop.catalog.ProductImageRepository images;
    public CartService(CartRepository carts,CartItemRepository items,ProductRepository products,CategoryRepository categories,Clock clock,vn.techshop.variant.ProductVariantService variants,vn.techshop.catalog.ProductImageRepository images) {this.variants=variants;this.carts=carts;this.items=items;this.products=products;this.categories=categories;this.clock=clock;this.images=images;}
    public CartView view(Long userId) { return view(carts.findByUserId(userId).orElseThrow(BusinessException::missing)); }
    public CartView selected(Long userId,List<Long> ids){
        var all=view(userId);if(ids==null)return all;
        var checked=selection(ids,all.items().stream().map(ItemView::itemId).toList());
        var rows=all.items().stream().filter(i->checked.contains(i.itemId())).toList();
        var subtotal=rows.stream().map(ItemView::lineTotal).reduce(BigDecimal.ZERO,BigDecimal::add);var fee=Pricing.fee(subtotal);
        return new CartView(all.version(),rows,subtotal,fee,subtotal.add(fee),"VND",Pricing.hash(rows.stream().map(i->new Pricing.Line(i.variantId()==null?i.productId():-i.variantId(),i.quantity(),i.unitPrice())).toList(),fee),rows.stream().allMatch(ItemView::available));
    }
    public static Set<Long> selection(List<Long> ids,List<Long> owned){
        if(ids==null)return new HashSet<>(owned);
        if(ids.isEmpty()||ids.size()>20||ids.stream().anyMatch(Objects::isNull)||new HashSet<>(ids).size()!=ids.size())throw BusinessException.invalid("Chọn 1–20 dòng giỏ hàng khác nhau.");
        if(!owned.containsAll(ids))throw BusinessException.missing();return new HashSet<>(ids);
    }
    @Transactional public void removeMany(Long userId,RemoveMany input){
        var cart=lock(userId,input.expectedVersion());var rows=items.findByCartIdOrderByProductIdAsc(cart.getId());
        var selected=selection(input.itemIds(),rows.stream().map(CartItem::getId).toList());
        items.deleteAll(rows.stream().filter(i->selected.contains(i.getId())).toList());touch(cart);
    }
    public CartView view(Cart cart) {
        var rows=items.findByCartIdOrderByProductIdAsc(cart.getId());
        if(rows.isEmpty()) {
            return new CartView(cart.getVersion(),List.of(),BigDecimal.ZERO,BigDecimal.ZERO,BigDecimal.ZERO,"VND",Pricing.hash(List.of(),BigDecimal.ZERO),false);
        }
        var pIds=rows.stream().map(CartItem::getProductId).distinct().toList();
        var productMap=products.findAllById(pIds).stream().collect(java.util.stream.Collectors.toMap(Product::getId,java.util.function.Function.identity()));
        var catIds=productMap.values().stream().map(Product::getCategoryId).distinct().toList();
        var categoryMap=categories.findAllById(catIds).stream().collect(java.util.stream.Collectors.toMap(Category::getId,java.util.function.Function.identity()));
        var views=new ArrayList<ItemView>();BigDecimal subtotal=BigDecimal.ZERO;
        for(var row:rows) {
            var p=productMap.get(row.getProductId());if(p==null)throw BusinessException.missing();
            var c=categoryMap.get(p.getCategoryId());if(c==null)throw BusinessException.missing();
            var variant=row.getVariantId()==null?null:variants.forCart(row.getVariantId(),p.getId());
            var price=variant==null?p.getPrice():variant.getPrice();
            boolean active="ACTIVE".equals(p.getStatus()) && "ACTIVE".equals(c.getStatus()) && (row.getVariantId()==null?!variants.hasVariants(p.getId()):variant!=null&&"ACTIVE".equals(variant.getStatus()));boolean available=active && (variant==null?p.getStockQuantity():variants.available(variant.getId()))>=row.getQuantity();BigDecimal line=price.multiply(BigDecimal.valueOf(row.getQuantity()));subtotal=subtotal.add(line);
            var imgList=images.findByProductIdOrderBySortOrderAscIdAsc(p.getId()).stream().filter(img->img.getVariantId()==null||Objects.equals(img.getVariantId(),row.getVariantId())).toList();
            String thumb=imgList.isEmpty()?"/assets/products/device.svg":imgList.get(0).getAssetPath();
            views.add(new ItemView(row.getId(),p.getId(),p.getName()+(variant==null?"":" · "+variant.getColorName()+" / "+variant.getStorageCapacity()),price,row.getQuantity(),line,available,available?null:(active?"INSUFFICIENT_STOCK":"PRODUCT_UNAVAILABLE"),row.getVariantId(),thumb));
        }
        var fee=Pricing.fee(subtotal);
        var lines=views.stream().map(v -> new Pricing.Line(v.variantId()==null?v.productId():-v.variantId(),v.quantity(),v.unitPrice())).toList();
        return new CartView(cart.getVersion(),List.copyOf(views),subtotal,fee,subtotal.add(fee),"VND",Pricing.hash(lines,fee),!views.isEmpty() && views.stream().allMatch(ItemView::available));
    }
    @Transactional public CartView add(Long userId,Add input) {
        Rules.quantity(input.quantity());var cart=lock(userId,input.expectedVersion());
        var product=products.findById(input.productId()).orElseThrow(BusinessException::missing);
        var category=categories.findById(product.getCategoryId()).orElseThrow(BusinessException::missing);
        if(!"ACTIVE".equals(product.getStatus()) || !"ACTIVE".equals(category.getStatus())) throw BusinessException.conflict("PRODUCT_UNAVAILABLE","Sản phẩm đang ngừng bán.");
        if(input.variantId()!=null)variants.require(input.variantId(),product.getId());
        else if(variants.hasVariants(product.getId()))throw BusinessException.invalid("Vui lòng chọn màu và phiên bản.");
        var item=items.findByCartIdAndProductIdAndVariantId(cart.getId(),product.getId(),input.variantId()).orElse(null);
        if(item==null) {if(items.countByCartId(cart.getId())>=20) throw BusinessException.conflict("CART_LIMIT","Giỏ hàng tối đa 20 sản phẩm.");item=new CartItem();item.setCartId(cart.getId());item.setProductId(product.getId());item.setVariantId(input.variantId());item.setQuantity(0);item.setCreatedAt(clock.instant());}
        Rules.quantity(item.getQuantity()+input.quantity());item.setQuantity(item.getQuantity()+input.quantity());item.setUpdatedAt(clock.instant());items.save(item);touch(cart);return view(cart);
    }
    @Transactional public CartView update(Long userId,Long itemId,Update input) {
        Rules.quantity(input.quantity());var cart=lock(userId,input.expectedVersion());var item=items.findByIdAndCartId(itemId,cart.getId()).orElseThrow(BusinessException::missing);
        item.setQuantity(input.quantity());item.setUpdatedAt(clock.instant());items.save(item);touch(cart);return view(cart);
    }
    @Transactional public void remove(Long userId,Long itemId,Long expectedVersion) {
        var cart=lock(userId,expectedVersion);items.delete(items.findByIdAndCartId(itemId,cart.getId()).orElseThrow(BusinessException::missing));touch(cart);
    }
    @Transactional public void mergeGuestCart(Long guestUserId, Long customerUserId) {
        if (guestUserId == null || customerUserId == null || Objects.equals(guestUserId, customerUserId)) return;
        var guestCart = carts.findByUserId(guestUserId).orElse(null);
        if (guestCart == null) return;
        var guestRows = items.findByCartIdOrderByProductIdAsc(guestCart.getId());
        if (guestRows.isEmpty()) return;
        var customerCart = carts.findByUserId(customerUserId).orElseGet(() -> {
            var c = new Cart();
            c.setUserId(customerUserId);
            c.setVersion(0L);
            c.setCreatedAt(clock.instant());
            c.setUpdatedAt(clock.instant());
            return carts.save(c);
        });
        long currentCount = items.countByCartId(customerCart.getId());
        for (var guestItem : guestRows) {
            var p = products.findById(guestItem.getProductId()).orElse(null);
            if (p == null || !"ACTIVE".equals(p.getStatus())) continue;
            var c = categories.findById(p.getCategoryId()).orElse(null);
            if (c == null || !"ACTIVE".equals(c.getStatus())) continue;
            var variant = guestItem.getVariantId() == null ? null : variants.forCart(guestItem.getVariantId(), p.getId());
            if (guestItem.getVariantId() != null) {
                if (variant == null || !"ACTIVE".equals(variant.getStatus())) continue;
            } else if (variants.hasVariants(p.getId())) {
                continue;
            }
            int available = variant == null ? p.getStockQuantity() : variants.available(variant.getId());
            if (available <= 0) continue;
            var existingItem = items.findByCartIdAndProductIdAndVariantId(customerCart.getId(), p.getId(), guestItem.getVariantId()).orElse(null);
            if (existingItem != null) {
                int targetQty = Math.min(99, existingItem.getQuantity() + guestItem.getQuantity());
                if (targetQty > available && available > existingItem.getQuantity()) {
                    targetQty = available;
                } else if (targetQty > available && existingItem.getQuantity() >= available) {
                    targetQty = existingItem.getQuantity();
                }
                existingItem.setQuantity(targetQty);
                existingItem.setUpdatedAt(clock.instant());
                items.save(existingItem);
            } else {
                if (currentCount >= 20) continue;
                int addQty = Math.min(Math.min(99, guestItem.getQuantity()), available);
                if (addQty > 0) {
                    var newItem = new CartItem();
                    newItem.setCartId(customerCart.getId());
                    newItem.setProductId(p.getId());
                    newItem.setVariantId(guestItem.getVariantId());
                    newItem.setQuantity(addQty);
                    newItem.setCreatedAt(clock.instant());
                    newItem.setUpdatedAt(clock.instant());
                    items.save(newItem);
                    currentCount++;
                }
            }
        }
        items.deleteAll(guestRows);
        touch(customerCart);
        touch(guestCart);
    }
    private Cart lock(Long userId,Long expected) {var c=carts.lockByUserId(userId).orElseThrow(BusinessException::missing);Rules.version(c.getVersion(),expected,"CART_CHANGED");return c;}
    public void touch(Cart cart) {var now=clock.instant();cart.setUpdatedAt(now.isAfter(cart.getUpdatedAt())?now:cart.getUpdatedAt().plusNanos(1000));carts.saveAndFlush(cart);}
}
