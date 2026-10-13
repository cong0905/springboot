package vn.techshop.guest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import jakarta.servlet.http.Cookie;
import vn.techshop.catalog.*;
import vn.techshop.cart.*;
import vn.techshop.identity.*;
import vn.techshop.order.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class GuestCheckoutTest {
 @Autowired MockMvc mvc;@Autowired IdentityService identity;@Autowired CatalogService catalog;@Autowired vn.techshop.inventory.InventoryService inventory;@Autowired CartService carts;@Autowired OrderService orders;@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Test void guestCartAndProofNeverGrantCustomerApiAccessOrOtherGuestOrder() throws Exception {
  String key=java.util.UUID.randomUUID().toString();Long actor=identity.bootstrapAdmin(new IdentityService.Registration("Guest QA Admin",key+"@test.local","GuestTestPass2026!")).id();
  var c=catalog.createCategory(new CatalogService.CategoryInput(key,"Guest catalog","ACTIVE"));var p=catalog.createProduct(new CatalogService.ProductInput(c.id(),key,"Guest product",new java.math.BigDecimal("100000"),"ACTIVE",null));inventory.adjust(p.id(),actor,new vn.techshop.inventory.InventoryService.Adjustment(4,"Guest test stock",p.version()));
  var first=new MockHttpSession();String before=first.getId();String add="{\"productId\":"+p.id()+",\"quantity\":1,\"expectedVersion\":0}";
  mvc.perform(post("/api/v1/guest/cart/items").session(first).contentType("application/json").content(add)).andExpect(status().isForbidden());
  mvc.perform(post("/api/v1/guest/cart/items").session(first).with(csrf()).contentType("application/json").content(add)).andExpect(status().isOk());assertThat(first.getId()).isNotEqualTo(before);
  Long owner=(Long)first.getAttribute("guestOwner");Long item=carts.view(owner).items().getFirst().itemId();
  var second=new MockHttpSession();mvc.perform(post("/api/v1/guest/cart/items").session(second).with(csrf()).contentType("application/json").content(add)).andExpect(status().isOk());
  mvc.perform(delete("/api/v1/guest/cart/items/"+item).session(second).param("expectedVersion",carts.view((Long)second.getAttribute("guestOwner")).version().toString()).with(csrf())).andExpect(status().isNotFound());
  var preview=orders.preview(owner,new OrderService.PreviewRequest(null,null));var request=new OrderService.Checkout(java.util.UUID.randomUUID().toString(),preview.cartVersion(),preview.total(),preview.pricingHash(),"Guest Recipient","0912345678","123 Hanoi Street, Hanoi",null,null,"COD");
  String body=new tools.jackson.databind.json.JsonMapper().writeValueAsString(request);
  var result=mvc.perform(post("/api/v1/guest/orders").session(first).with(csrf()).contentType("application/json").content(body)).andExpect(status().isCreated()).andReturn();
  Long id=new tools.jackson.databind.json.JsonMapper().readTree(result.getResponse().getContentAsString()).get("id").longValue();
  String header=result.getResponse().getHeader("Set-Cookie");assertThat(header).contains("HttpOnly").contains("SameSite=Lax");String[] part=header.split(";",2)[0].split("=",2);var proof=new Cookie(part[0],part[1]);
  assertThat(carts.view(owner).items()).isEmpty();mvc.perform(post("/api/v1/guest/orders").session(first).with(csrf()).contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
  mvc.perform(get("/api/v1/orders/"+id).cookie(proof)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/v1/guest/orders/"+id).session(second)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/guest/orders/"+id)).andExpect(status().isNotFound());
  mvc.perform(get("/api/v1/guest/orders/"+id).cookie(proof)).andExpect(status().isOk());
  mvc.perform(get("/guest/orders/"+id).cookie(proof)).andExpect(status().isOk());
  mvc.perform(get("/guest/products/"+p.id())).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("/api/v1/guest/cart/items")));
  jdbc.update("update guest_order_proofs set expires_at=? where order_id=?",java.sql.Timestamp.from(java.time.Instant.now().minusSeconds(60)),id);
  mvc.perform(get("/api/v1/guest/orders/"+id).cookie(proof)).andExpect(status().isNotFound());
 }
}
