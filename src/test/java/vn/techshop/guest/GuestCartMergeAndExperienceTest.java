package vn.techshop.guest;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import java.math.BigDecimal;
import java.util.UUID;
import vn.techshop.catalog.*;
import vn.techshop.cart.*;
import vn.techshop.identity.*;
import vn.techshop.inventory.InventoryService;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class GuestCartMergeAndExperienceTest {
    @Autowired MockMvc mvc;
    @Autowired IdentityService identity;
    @Autowired CatalogService catalog;
    @Autowired InventoryService inventory;
    @Autowired CartService carts;

    @Test
    void guestCartMergesIntoCustomerCartOnLoginAndRedirectsSafely() throws Exception {
        String key = UUID.randomUUID().toString();
        Long adminId = identity.bootstrapAdmin(new IdentityService.Registration("Admin " + key, key + "@test.local", "AdminPass123!")).id();

        // 1. Tạo catalog & sản phẩm
        var cat = catalog.createCategory(new CatalogService.CategoryInput(key, "Category " + key, "ACTIVE"));
        var prod1 = catalog.createProduct(new CatalogService.ProductInput(cat.id(), key + "-p1", "Phone " + key, new BigDecimal("15000000"), "ACTIVE", null));
        inventory.adjust(prod1.id(), adminId, new InventoryService.Adjustment(10, "Stock for p1", prod1.version()));

        var prod2 = catalog.createProduct(new CatalogService.ProductInput(cat.id(), key + "-p2", "Cable " + key, new BigDecimal("200000"), "ACTIVE", null));
        inventory.adjust(prod2.id(), adminId, new InventoryService.Adjustment(20, "Stock for p2", prod2.version()));

        // 2. Tạo tài khoản khách hàng thành viên
        String custEmail = "cust-" + key + "@test.local";
        String custPass = "CustPass123!";
        var customer = identity.register(new IdentityService.Registration("Customer " + key, custEmail, custPass));

        // Khách hàng thành viên đã có sẵn 1 cái cable trong giỏ hàng của họ
        var custCartView = carts.view(customer.id());
        carts.add(customer.id(), new CartService.Add(prod2.id(), 1, custCartView.version()));

        // 3. Khách vãng lai thao tác giỏ hàng trong session
        var guestSession = new MockHttpSession();
        // Guest thêm Phone (quantity = 2)
        mvc.perform(post("/api/v1/guest/cart/items")
                .session(guestSession)
                .with(csrf())
                .contentType("application/json")
                .content("{\"productId\":" + prod1.id() + ",\"quantity\":2,\"expectedVersion\":0}"))
                .andExpect(status().isOk());

        // Lấy version hiện tại của giỏ guest
        Long guestId = (Long) guestSession.getAttribute("guestOwner");
        assertThat(guestId).isNotNull();
        var guestCartAfterP1 = carts.view(guestId);

        // Guest thêm Cable (quantity = 3)
        mvc.perform(post("/api/v1/guest/cart/items")
                .session(guestSession)
                .with(csrf())
                .contentType("application/json")
                .content("{\"productId\":" + prod2.id() + ",\"quantity\":3,\"expectedVersion\":" + guestCartAfterP1.version() + "}"))
                .andExpect(status().isOk());

        // Kiểm tra StorefrontModel header cart count cho khách guest: 2 phone + 3 cable = 5 items
        mvc.perform(get("/guest/cart").session(guestSession))
                .andExpect(status().isOk())
                .andExpect(model().attribute("headerCartCount", 5));

        // 4. Khách vãng lai đăng nhập qua form login với redirect an toàn (/cart)
        mvc.perform(post("/login")
                .session(guestSession)
                .with(csrf())
                .param("username", custEmail)
                .param("password", custPass)
                .param("redirect", "/cart"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/cart"));

        // 5. Kiểm tra giỏ hàng của Customer sau khi merge
        var mergedCart = carts.view(customer.id());
        assertThat(mergedCart.items()).hasSize(2);

        var p1Item = mergedCart.items().stream().filter(i -> i.productId().equals(prod1.id())).findFirst().orElseThrow();
        assertThat(p1Item.quantity()).isEqualTo(2); // Được gộp từ guest

        var p2Item = mergedCart.items().stream().filter(i -> i.productId().equals(prod2.id())).findFirst().orElseThrow();
        assertThat(p2Item.quantity()).isEqualTo(4); // 1 ban đầu + 3 từ guest = 4

        // 6. Kiểm tra giỏ hàng guest cũ đã được xóa sạch
        var guestCartAfterMerge = carts.view(guestId);
        assertThat(guestCartAfterMerge.items()).isEmpty();

        // 7. Kiểm tra phòng vệ Open Redirect
        var evilSession = new MockHttpSession();
        mvc.perform(post("/login")
                .session(evilSession)
                .with(csrf())
                .param("username", custEmail)
                .param("password", custPass)
                .param("redirect", "https://evil.attacker.com/steal"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/products"));

        var slashSlashSession = new MockHttpSession();
        mvc.perform(post("/login")
                .session(slashSlashSession)
                .with(csrf())
                .param("username", custEmail)
                .param("password", custPass)
                .param("redirect", "//evil.attacker.com"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/products"));
    }
}
