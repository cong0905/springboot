package vn.techshop.content;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.techshop.catalog.*;
import vn.techshop.identity.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class ContentAndWishlistTest {
 @Autowired NewsService news;@Autowired WishlistService wishlists;@Autowired IdentityService identity;@Autowired CatalogService catalog;@Autowired MockMvc mvc;@Autowired vn.techshop.shared.FileStorageService files;
 Long owner(String suffix){return identity.register(new IdentityService.Registration("Feature Owner",java.util.UUID.randomUUID()+suffix+"@test.local","FeaturePass2026!")).id();}
 org.springframework.security.core.userdetails.UserDetails principal(Long id){return identity.loadUserByUsername(identity.profile(id).email());}
 @Test void wishlistIsOwnerScopedIdempotentAndProtected() throws Exception {
  Long a=owner("a"),b=owner("b");String key=java.util.UUID.randomUUID().toString();var c=catalog.createCategory(new CatalogService.CategoryInput(key,"Wishlist cat","ACTIVE"));var p=catalog.createProduct(new CatalogService.ProductInput(c.id(),key,"Wishlist item",new java.math.BigDecimal("100000"),"ACTIVE",null));
  wishlists.add(a,p.id());wishlists.add(a,p.id());assertThat(wishlists.list(a)).hasSize(1);assertThat(wishlists.list(b)).isEmpty();wishlists.remove(b,p.id());assertThat(wishlists.list(a)).hasSize(1);
  mvc.perform(put("/api/v1/me/wishlist/"+p.id()).with(user(principal(b)))).andExpect(status().isForbidden());
  mvc.perform(get("/wishlist").with(user(principal(a)))).andExpect(status().isOk());
 }
 @Test void newsHidesDraftAndEscapesHtmlThenEnforcesVersionAndAdmin() throws Exception {
  String key="news-"+java.util.UUID.randomUUID();var a=news.save(1L,null,new NewsService.Input(key,"News test title","Safe excerpt","<script>window.attack=true</script> Test body","DRAFT",null,null));
  mvc.perform(get("/news/"+key)).andExpect(status().isNotFound());
  news.save(1L,a.id(),new NewsService.Input(key,a.title(),a.excerpt(),a.body(),"PUBLISHED","https://example.org/source",a.version()));
  var result=mvc.perform(get("/news/"+key)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();assertThat(result).contains("&lt;script&gt;").doesNotContain("<script>window.attack");
  assertThatThrownBy(()->news.save(1L,a.id(),new NewsService.Input(key,a.title(),a.excerpt(),a.body(),"ARCHIVED",null,0L))).isInstanceOf(vn.techshop.shared.BusinessException.class);
  mvc.perform(get("/admin/news").with(user(principal(owner("customer"))))).andExpect(status().isForbidden());
  var admin=identity.bootstrapAdmin(new IdentityService.Registration("News Admin",java.util.UUID.randomUUID()+"@test.local","FeaturePass2026!"));
  mvc.perform(get("/admin/news/"+a.id()).with(user(identity.loadUserByUsername(admin.email())))).andExpect(status().isOk());
 }
 @Test void avatarUploadUsesRealDecoderAndOwnerVersion() throws Exception {
  Long a=owner("avatar");var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
  var file=new org.springframework.mock.web.MockMultipartFile("file","avatar.png","image/png",bytes.toByteArray());
  mvc.perform(multipart("/api/v1/me/avatar").file(file).param("expectedVersion","0").with(user(principal(a)))).andExpect(status().isForbidden());
  mvc.perform(multipart("/api/v1/me/avatar").file(file).param("expectedVersion","0").with(user(principal(a))).with(csrf())).andExpect(status().isNoContent());
  String path=identity.profile(a).avatarPath();try{
   assertThat(path).endsWith(".png");mvc.perform(get(path)).andExpect(status().isOk());
   mvc.perform(multipart("/api/v1/me/avatar").file(file).param("expectedVersion","0").with(user(principal(a))).with(csrf())).andExpect(status().isConflict());
  }finally{files.delete(path);}
 }
}
