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
@Component
@Profile("demo")
public class DemoData implements CommandLineRunner {
    private final IdentityService identity;private final CatalogService catalog;private final InventoryService inventory;
    public DemoData(IdentityService identity,CatalogService catalog,InventoryService inventory) {this.identity=identity;this.catalog=catalog;this.inventory=inventory;}
    public void run(String... args) {
        var admin=identity.bootstrapAdmin(new IdentityService.Registration("Admin Demo","admin@techshop.test","DemoAdmin2026!"));
        identity.register(new IdentityService.Registration("Khách Demo","customer@techshop.test","DemoCustomer2026!"));
        var audio=catalog.createCategory(new CatalogService.CategoryInput("AUDIO","Âm thanh","ACTIVE"));
        var gear=catalog.createCategory(new CatalogService.CategoryInput("GEAR","Phụ kiện","ACTIVE"));
        String[][] samples={{"HEADPHONE-01","Tai nghe không dây Studio","1290000","Âm thanh rõ nét, thiết kế nhẹ và pin lâu.",audio.id().toString()},{"KEYBOARD-01","Bàn phím cơ Compact","890000","Bàn phím nhỏ gọn cho góc học tập và làm việc.",gear.id().toString()},{"MOUSE-01","Chuột không dây Precision","450000","Kết nối ổn định, thao tác chính xác.",gear.id().toString()},{"SPEAKER-01","Loa Bluetooth Mini","650000","Âm thanh di động cho những chuyến đi.",audio.id().toString()},{"CHARGER-01","Bộ sạc nhanh 65W","590000","Phụ kiện sạc gọn nhẹ cho bàn làm việc.",gear.id().toString()},{"HUB-01","Hub USB-C đa cổng","790000","Mở rộng cổng kết nối cho laptop.",gear.id().toString()}};
        for(var s:samples) {var product=catalog.createProduct(new CatalogService.ProductInput(Long.valueOf(s[4]),s[0],s[1],new BigDecimal(s[2]),"ACTIVE",s[3]));inventory.adjust(product.id(),admin.id(),new InventoryService.Adjustment(20,"Nhập kho dữ liệu demo",product.version()));}
    }
}
