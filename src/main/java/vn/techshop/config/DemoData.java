package vn.techshop.config;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import vn.techshop.catalog.*;
import vn.techshop.identity.*;
import vn.techshop.inventory.*;

/** Explicit demo profile only. Reset on restart, never enabled for MySQL by default. */
@org.springframework.core.annotation.Order(0)
@Component
@Profile({"demo", "local"})
public class DemoData implements CommandLineRunner {
    private final IdentityService identity;
    private final CatalogService catalog;
    private final InventoryService inventory;
    private final vn.techshop.coupon.CouponService coupons;
    private final vn.techshop.review.ProductReviewService reviews;
    private final UserRepository users;

    public DemoData(IdentityService identity, CatalogService catalog, InventoryService inventory,
                    vn.techshop.coupon.CouponService coupons, vn.techshop.review.ProductReviewService reviews,
                    UserRepository users) {
        this.identity = identity;
        this.catalog = catalog;
        this.inventory = inventory;
        this.coupons = coupons;
        this.reviews = reviews;
        this.users = users;
    }

    public void run(String... args) {
        if (users.findByEmail("admin@techshop.test").isPresent()) return;
        var admin = identity.bootstrapAdmin(new IdentityService.Registration("Admin Demo", "admin@techshop.test", "DemoAdmin2026!"));
        var customer = identity.register(new IdentityService.Registration("Khách Demo", "customer@techshop.test", "DemoCustomer2026!"));
        var audio = catalog.createCategory(new CatalogService.CategoryInput("AUDIO", "Âm thanh", "ACTIVE"));
        var gear = catalog.createCategory(new CatalogService.CategoryInput("GEAR", "Phụ kiện", "ACTIVE"));
        String[][] samples = {
            {"HEADPHONE-01", "Tai nghe không dây Studio", "1290000", "Âm thanh rõ nét, thiết kế nhẹ và pin lâu.", audio.id().toString()},
            {"KEYBOARD-01", "Bàn phím cơ Compact", "890000", "Bàn phím nhỏ gọn cho góc học tập và làm việc.", gear.id().toString()},
            {"MOUSE-01", "Chuột không dây Precision", "450000", "Kết nối ổn định, thao tác chính xác.", gear.id().toString()},
            {"SPEAKER-01", "Loa Bluetooth Mini", "650000", "Âm thanh di động cho những chuyến đi.", audio.id().toString()},
            {"CHARGER-01", "Bộ sạc nhanh 65W", "590000", "Phụ kiện sạc gọn nhẹ cho bàn làm việc.", gear.id().toString()},
            {"HUB-01", "Hub USB-C đa cổng", "790000", "Mở rộng cổng kết nối cho laptop.", gear.id().toString()}
        };
        Long firstProductId = null;
        for (var s : samples) {
            var product = catalog.createProduct(new CatalogService.ProductInput(Long.valueOf(s[4]), s[0], s[1], new BigDecimal(s[2]), "ACTIVE", s[3]));
            inventory.adjust(product.id(), admin.id(), new InventoryService.Adjustment(20, "Nhập kho dữ liệu demo", product.version()));
            String asset = switch (s[0].split("-")[0]) {
                case "KEYBOARD" -> "keyboard";
                case "MOUSE" -> "mouse";
                case "SPEAKER" -> "speaker";
                case "CHARGER" -> "charger";
                case "HUB" -> "hub";
                default -> "device";
            };
            catalog.addImage(product.id(), "/assets/products/" + asset + ".svg", s[1] + " · minh họa", 0);
            if (firstProductId == null) firstProductId = product.id();
        }
        coupons.create(new vn.techshop.coupon.CouponService.CouponInput("TECH10", "Giảm 10% tối đa 100.000 ₫ cho đơn từ 200.000 ₫", "PERCENT", new BigDecimal("10"), new BigDecimal("200000"), new BigDecimal("100000"), 100, null, "ACTIVE"));
        coupons.create(new vn.techshop.coupon.CouponService.CouponInput("SALE50K", "Giảm 50.000 ₫ trực tiếp cho đơn từ 300.000 ₫", "FIXED", new BigDecimal("50000"), new BigDecimal("300000"), null, 100, null, "ACTIVE"));
        if (firstProductId != null) {
            reviews.submit(customer.id(), "Nguyễn Văn An", firstProductId, new vn.techshop.review.ProductReviewService.SubmitInput(5, "Tai nghe nghe rất êm, bass ấm và chống ồn tốt!"));
            reviews.submit(admin.id(), "Trần Minh", firstProductId, new vn.techshop.review.ProductReviewService.SubmitInput(5, "Pin rất trâu, dùng cả tuần mới phải sạc lại. Giao hàng nhanh đóng gói cẩn thận."));
            reviews.submit(customer.id(), "Lê Hoàng", firstProductId, new vn.techshop.review.ProductReviewService.SubmitInput(4, "Sản phẩm tốt trong tầm giá, phụ kiện đầy đủ."));
        }
    }
}
