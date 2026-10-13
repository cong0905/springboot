package vn.techshop;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.techshop.cart.CartService;
import vn.techshop.catalog.CatalogService;
import vn.techshop.identity.IdentityService;
import vn.techshop.inventory.InventoryService;
import vn.techshop.order.OrderService;
import vn.techshop.payment.PaymentService;
import vn.techshop.reporting.ReportExportService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceComprehensiveTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IdentityService identity;

    @Autowired
    private CatalogService catalog;

    @Autowired
    private InventoryService inventory;

    @Autowired
    private CartService carts;

    @Autowired
    private OrderService orders;

    @Autowired
    private PaymentService payments;

    @Autowired
    private ReportExportService reportExportService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long customerId;
    private Long adminId;
    private Long productId;
    private Long categoryId;

    @BeforeEach
    void setUp() {
        for (String table : List.of("payment_transactions", "stock_movements", "order_status_history", "order_items", "orders", "cart_items", "carts", "product_reviews", "coupons", "product_images", "products", "categories", "users")) {
            jdbc.update("delete from " + table);
        }

        String key = UUID.randomUUID().toString();
        adminId = identity.bootstrapAdmin(new IdentityService.Registration("Admin Test", "admin" + key + "@test.local", "TestAdmin2026!")).id();
        customerId = identity.register(new IdentityService.Registration("Customer Test", "user" + key + "@test.local", "TestUser2026!")).id();

        var cat = catalog.createCategory(new CatalogService.CategoryInput("CAT-" + key, "Danh mục kiểm thử", "ACTIVE"));
        categoryId = cat.id();

        var p = catalog.createProduct(new CatalogService.ProductInput(categoryId, "SKU-" + key, "Sản phẩm kiểm thử", new BigDecimal("600000"), "ACTIVE", "Mô tả"));
        productId = p.id();

        inventory.adjust(productId, adminId, new InventoryService.Adjustment(20, "Nhập kho ban đầu", p.version()));
        carts.add(customerId, new CartService.Add(productId, 2, carts.view(customerId).version()));
    }

    @Test
    void testPublicOrderLookupAndWrongPhone() throws Exception {
        var cart = carts.view(customerId);
        var checkoutInput = new OrderService.Checkout(
            UUID.randomUUID().toString(),
            cart.version(),
            cart.total(),
            cart.pricingHash(),
            "Người Nhận Test",
            "0912345678",
            "123 Đường Công Nghệ, Quận 1, TP.HCM",
            "Giao giờ hành chính",
            null,
            "COD"
        );
        var result = orders.checkout(customerId, checkoutInput);
        String code = result.order().orderCode();

        // 1. Correct code and phone
        var lookupFound = orders.lookup(code, "0912345678");
        assertThat(lookupFound).isPresent();
        assertThat(lookupFound.get().orderCode()).isEqualTo(code);
        assertThat(lookupFound.get().maskedRecipientName()).isEqualTo("Ng*** Test");

        // 2. Wrong phone
        var lookupWrongPhone = orders.lookup(code, "0987654321");
        assertThat(lookupWrongPhone).isEmpty();

        // 3. Test HTTP GET & POST /order-lookup
        mockMvc.perform(get("/order-lookup"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Tra cứu hành trình đơn hàng")));

        mockMvc.perform(post("/order-lookup")
            .with(csrf())
            .param("orderCode", code)
            .param("phone", "0912345678"))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString(code)));
    }

    @Test
    void testAdminOrderFilteringByKeywordAndDate() {
        var cart = carts.view(customerId);
        var checkoutInput = new OrderService.Checkout(
            UUID.randomUUID().toString(),
            cart.version(),
            cart.total(),
            cart.pricingHash(),
            "Khách VIP Tìm Kiếm",
            "0933333333",
            "456 Đường Lê Lợi, TP.HCM",
            null,
            null,
            "COD"
        );
        var result = orders.checkout(customerId, checkoutInput);

        // Filter by keyword "VIP"
        var filteredByKeyword = orders.list(adminId, true, null, null, null, "VIP", 0, 10);
        assertThat(filteredByKeyword.content()).isNotEmpty();
        assertThat(filteredByKeyword.content().get(0).orderCode()).isEqualTo(result.order().orderCode());

        // Filter by phone "0933333333"
        var filteredByPhone = orders.list(adminId, true, null, null, null, "0933333333", 0, 10);
        assertThat(filteredByPhone.content()).isNotEmpty();

        // Filter by date range covering today
        LocalDate today = LocalDate.now();
        var filteredByDate = orders.list(adminId, true, null, today.minusDays(1), today.plusDays(1), null, 0, 10);
        assertThat(filteredByDate.content()).isNotEmpty();
    }

    @Test
    void testExportOrdersCsvReturnsUtf8BomAndHeaders() {
        byte[] csvBytes = reportExportService.exportOrdersCsv(LocalDate.now().minusDays(7), LocalDate.now().plusDays(1));
        assertThat(csvBytes).isNotEmpty();
        String csvContent = new String(csvBytes, java.nio.charset.StandardCharsets.UTF_8);

        // Verify UTF-8 BOM
        assertThat(csvContent.charAt(0)).isEqualTo('\uFEFF');
        // Verify CSV header columns
        assertThat(csvContent).contains("Mã đơn,Thời gian đặt,Thời gian giao,Khách hàng,Số điện thoại,Địa chỉ,Tạm tính,Giảm giá,Mã giảm giá,Phí giao,Tổng thanh toán,Tiền hoàn,Thu ròng,Phương thức thanh toán,Trạng thái thanh toán,Trạng thái đơn");
    }

    @Test
    void testProductImagesAddAndThumbnailUpdate() {
        // Add an image to the product
        String imagePath = "/assets/products/uploads/test-image-1.jpg";
        catalog.addImage(productId, imagePath, "Hình mẫu 1", 1);

        var updatedProduct = catalog.detail(productId, true);
        assertThat(updatedProduct.thumbnailPath()).isEqualTo(imagePath);
        assertThat(updatedProduct.images()).contains(imagePath);

        // Clean up
        var images = catalog.listImages(productId);
        for (var img : images) {
            if (imagePath.equals(img.getAssetPath())) {
                catalog.deleteImage(img.getId());
            }
        }
    }
}
