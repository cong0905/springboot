package vn.techshop.cart;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import vn.techshop.catalog.*;
import vn.techshop.identity.*;
import vn.techshop.inventory.*;
import vn.techshop.order.*;
import vn.techshop.shared.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest @ActiveProfiles("test")
class SelectedCheckoutTest {
 @Autowired CartService carts;@Autowired CatalogService catalog;@Autowired IdentityService identity;@Autowired InventoryService inventory;@Autowired OrderService orders;
 @Test void subsetCheckoutKeepsOtherLinesAndRetryDoesNotConsumeThem(){
  String key=UUID.randomUUID().toString();Long user=identity.register(new IdentityService.Registration("Subset Owner",key+"@test.local","SubsetPass2026!")).id();
  Long cat=catalog.createCategory(new CatalogService.CategoryInput("C"+key,"Subset category","ACTIVE")).id();
  Long first=catalog.createProduct(new CatalogService.ProductInput(cat,"A"+key,"First item",new BigDecimal("100000"),"ACTIVE",null)).id();
  Long second=catalog.createProduct(new CatalogService.ProductInput(cat,"B"+key,"Second item",new BigDecimal("300000"),"ACTIVE",null)).id();
  for(Long id:List.of(first,second))inventory.adjust(id,user,new InventoryService.Adjustment(3,"Test stock",catalog.detail(id,true).version()));
  carts.add(user,new CartService.Add(first,1,carts.view(user).version()));carts.add(user,new CartService.Add(second,1,carts.view(user).version()));
  var ids=List.of(carts.view(user).items().stream().filter(i->i.productId().equals(first)).findFirst().orElseThrow().itemId());
  var preview=orders.preview(user,new OrderService.PreviewRequest(null,null,ids));
  var request=new OrderService.Checkout(UUID.randomUUID().toString(),preview.cartVersion(),preview.total(),preview.pricingHash(),"Recipient","0912345678","123 Hanoi Street, Hanoi",null,null,"COD",null,ids);
  assertThatThrownBy(()->orders.checkout(user,new OrderService.Checkout(request.checkoutKey(),preview.cartVersion(),preview.total().add(BigDecimal.ONE),preview.pricingHash(),"Recipient","0912345678","123 Hanoi Street, Hanoi",null,null,"COD",null,ids))).isInstanceOf(BusinessException.class);
  assertThat(carts.view(user).items()).hasSize(2);
  var result=orders.checkout(user,request);assertThat(result.order().items()).hasSize(1);assertThat(result.order().subtotal()).isEqualByComparingTo("100000");
  assertThat(carts.view(user).items()).extracting(CartService.ItemView::productId).containsExactly(second);
  assertThat(orders.checkout(user,request).order().id()).isEqualTo(result.order().id());assertThat(carts.view(user).items()).hasSize(1);
  assertThatThrownBy(()->orders.preview(user,new OrderService.PreviewRequest(null,null,ids))).isInstanceOf(BusinessException.class);
  var remaining=carts.view(user);assertThatThrownBy(()->carts.removeMany(user,new CartService.RemoveMany(List.of(remaining.items().getFirst().itemId(),Long.MAX_VALUE),remaining.version()))).isInstanceOf(BusinessException.class);
  assertThat(carts.view(user).items()).hasSize(1);carts.removeMany(user,new CartService.RemoveMany(List.of(remaining.items().getFirst().itemId()),remaining.version()));assertThat(carts.view(user).items()).isEmpty();
  var original=catalog.detail(first,true);catalog.updateProduct(first,new CatalogService.ProductUpdate(original.categoryId(),original.name(),new BigDecimal("150000"),"ACTIVE",original.description(),original.version()));
  orders.reorder(user,result.order().id());assertThat(carts.view(user).items().getFirst().unitPrice()).isEqualByComparingTo("150000");
  Long stranger=identity.register(new IdentityService.Registration("Stranger",key+"other@test.local","SubsetPass2026!")).id();assertThatThrownBy(()->orders.reorder(stranger,result.order().id())).isInstanceOf(BusinessException.class);assertThat(carts.view(stranger).items()).isEmpty();
 }
}
