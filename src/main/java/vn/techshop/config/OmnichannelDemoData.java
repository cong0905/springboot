package vn.techshop.config;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.techshop.catalog.CatalogService;
import vn.techshop.identity.UserRepository;
import vn.techshop.variant.ProductVariantService;
import vn.techshop.branch.BranchInventoryService;
@Component @Profile({"demo", "local"}) @Order(10)
public class OmnichannelDemoData implements CommandLineRunner {
    private final CatalogService catalog;private final ProductVariantService variants;private final BranchInventoryService branches;private final JdbcTemplate jdbc;private final UserRepository users;
    public OmnichannelDemoData(CatalogService catalog,ProductVariantService variants,BranchInventoryService branches,JdbcTemplate jdbc,UserRepository users) {this.catalog=catalog;this.variants=variants;this.branches=branches;this.jdbc=jdbc;this.users=users;}
    public void run(String... args) {
        if (branches.branches().stream().anyMatch(b -> "HN-TH".equals(b.code()))) return;
        Long admin=users.findByEmail("admin@techshop.test").orElseThrow().getId();
        var cat=catalog.createCategory(new CatalogService.CategoryInput("PHONE","Điện thoại","ACTIVE"));
        var thaiha=branches.create(new BranchInventoryService.BranchInput("TechShop Thái Hà","HN-TH","123 Thái Hà, Đống Đa, Hà Nội","HN","0900000001",21.0126,105.8197,"DONG-DA"));
        var caugiay=branches.create(new BranchInventoryService.BranchInput("TechShop Cầu Giấy","HN-CG","100 Cầu Giấy, Hà Nội","HN","0900000002",21.0365,105.7908,"CAU-GIAY"));
        String[][] models={{"PHONE-NOVA","Nova Pro 256","14990000","Chip N8","6.7 inch OLED","5000 mAh"},{"PHONE-PIXEL","Pixel Max 256","17990000","Chip P9","6.5 inch AMOLED","4800 mAh"}};
        for(var m:models) {
            var p=catalog.createProduct(new CatalogService.ProductInput(cat.id(),m[0],m[1],new BigDecimal(m[2]),"ACTIVE","Sản phẩm minh họa tính năng đa kênh. Thông số và IMEI trong demo là dữ liệu mẫu."));
            int color=0;
            for(String c:List.of("Đen","Bạc")) {
                var v=variants.create(p.id(),new ProductVariantService.Input(m[0]+"-"+(++color),c,"256GB",new BigDecimal(m[2]),new BigDecimal(m[2]).add(new BigDecimal("1000000"))));
                var image=catalog.addImage(p.id(),"/assets/products/phone-"+(color==1?"black":"silver")+".svg",m[1]+" · "+c+" · minh họa",color-1);
                variants.assignImage(p.id(),image.getId(),v.id());
                for(var b:List.of(thaiha,caugiay)) {
                    var serials=new ArrayList<String>();for(int i=1;i<=5;i++)serials.add("DEMO-"+v.id()+"-"+b.id()+"-"+i);
                    branches.receive(admin,new BranchInventoryService.Import(v.id(),b.id(),serials));
                }
            }
            variants.specification(p.id(),new ProductVariantService.SpecInput("Hiệu năng","CPU",m[3],0));
            variants.specification(p.id(),new ProductVariantService.SpecInput("Màn hình","Tấm nền",m[4],1));
            variants.specification(p.id(),new ProductVariantService.SpecInput("Pin","Dung lượng",m[5],2));
        }
        jdbc.update("insert into trade_in_devices(model,base_price,subsidy_amount) values('Nova đời cũ',3000000,500000)");
        jdbc.update("insert into trade_in_devices(model,base_price,subsidy_amount) values('Pixel đời cũ',4000000,500000)");
        jdbc.update("insert into trade_in_criterias(name,deduction_percent) values('Đẹp như mới',0),('Xước nhẹ',10),('Pin chai / màn xước',25),('Lỗi chức năng',60)");
        for(int months:List.of(3,6,9,12))jdbc.update("insert into installment_plans(provider,months,fee_percent) values(?,?,0)","TechShop demo 0%",months);
        jdbc.update("insert into member_profiles(user_id) select id from users where role='CUSTOMER'");
    }
}
