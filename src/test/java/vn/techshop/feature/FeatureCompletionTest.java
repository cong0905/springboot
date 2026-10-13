package vn.techshop.feature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.techshop.identity.*;
import vn.techshop.coupon.*;
import vn.techshop.branch.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @org.springframework.transaction.annotation.Transactional
class FeatureCompletionTest {
 @Autowired IdentityService identity;@Autowired CouponService coupons;@Autowired BranchInventoryService branches;@Autowired MockMvc mvc;
 @Test void personalVoucherCannotBeListedOrRedeemedByOtherCustomer() throws Exception {
  String key=java.util.UUID.randomUUID().toString();var a=identity.register(new IdentityService.Registration("Voucher Owner",key+"a@test.local","VoucherPass2026!"));var b=identity.register(new IdentityService.Registration("Other Owner",key+"b@test.local","VoucherPass2026!"));
  var code="PERSONAL"+key.substring(0,8);coupons.create(new CouponService.CouponInput(code,"Private voucher","FIXED",new BigDecimal("10000"),BigDecimal.ZERO,null,1,null,"ACTIVE",a.id()));
  var owner=identity.loadUserByUsername(a.email());var other=identity.loadUserByUsername(b.email());
  mvc.perform(get("/api/v1/coupons/mine").with(user(owner))).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value(code.toUpperCase()));
  mvc.perform(get("/api/v1/coupons/mine").with(user(other))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
  mvc.perform(post("/api/v1/admin/coupons").with(user(other)).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
  assertThatThrownBy(()->coupons.redeem(code,new BigDecimal("100000"),b.id())).isInstanceOf(vn.techshop.shared.BusinessException.class);
  assertThat(coupons.redeem(code,new BigDecimal("100000"),a.id())).isEqualByComparingTo("10000");
  assertThatThrownBy(()->coupons.redeem(code,new BigDecimal("100000"),a.id())).isInstanceOf(vn.techshop.shared.BusinessException.class);
  mvc.perform(get("/vouchers").with(user(owner))).andExpect(status().isOk());
 }
 @Test void branchDetailsUseVersionAndExplicitUnitsAreFormattedWithoutGuessing() throws Exception {
  var b=branches.create(new BranchInventoryService.BranchInput("Test store","BR"+java.util.UUID.randomUUID(),"123 Hanoi Test Road","HN","0912345678",21,105));
  assertThat(b.openingHours()).isNull();assertThat(b.photoPath()).isNull();
  var saved=branches.details(b.id(),new BranchInventoryService.Details("QA hours 09:00–18:00","/assets/products/device.svg",b.version()));assertThat(saved.version()).isEqualTo(b.version()+1);
  assertThatThrownBy(()->branches.details(b.id(),new BranchInventoryService.Details("Changed",null,b.version()))).isInstanceOf(vn.techshop.shared.BusinessException.class);
  assertThatThrownBy(()->branches.details(b.id(),new BranchInventoryService.Details("Changed","/assets/../secret",saved.version()))).isInstanceOf(vn.techshop.shared.BusinessException.class);
  mvc.perform(get("/branches")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("QA hours")));
  assertThat(vn.techshop.variant.SpecificationDisplay.value("16gb")).isEqualTo("16 GB");assertThat(vn.techshop.variant.SpecificationDisplay.value("6,7″")).isEqualTo("6.7 inch");assertThat(vn.techshop.variant.SpecificationDisplay.value("5000 mah")).isEqualTo("5000 mAh");assertThat(vn.techshop.variant.SpecificationDisplay.value("120")).isEqualTo("120");
 }
}
