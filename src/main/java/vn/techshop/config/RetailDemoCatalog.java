package vn.techshop.config;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import vn.techshop.branch.BranchInventoryService;
import vn.techshop.catalog.CatalogService;
import vn.techshop.catalog.ProductRepository;
import vn.techshop.identity.UserRepository;
import vn.techshop.variant.ProductVariantService;

/** Real product names/photos, with explicitly fictional prices and stock; demo only. */
@Component
@Profile({"demo", "local"})
@Order(20)
public class RetailDemoCatalog implements CommandLineRunner {
    public record Photo(String assetPath, String sourceUrl, String contentType, long bytes, String sha256) {}
    public record Specification(String name, String value) {}
    public record SampleProduct(String sku, String name, String category, String brand,
            BigDecimal samplePrice, String storage, String sourceUrl, List<Photo> images,
            List<Specification> specifications) {}
    public record SampleCatalog(String note, String capturedAt, List<SampleProduct> products) {}

    private final CatalogService catalog;
    private final ProductRepository products;
    private final ProductVariantService variants;
    private final BranchInventoryService branches;
    private final UserRepository users;

    public RetailDemoCatalog(CatalogService catalog, ProductRepository products,
            ProductVariantService variants, BranchInventoryService branches, UserRepository users) {
        this.catalog = catalog;
        this.products = products;
        this.variants = variants;
        this.branches = branches;
        this.users = users;
    }

    public SampleCatalog samples() throws IOException {
        try (var input = new ClassPathResource("demo/retail-catalog.json").getInputStream()) {
            return JsonMapper.builder().build().readValue(input, SampleCatalog.class);
        }
    }

    @Override
    @Transactional
    public void run(String... args) throws IOException {
        var samples = samples();
        Long admin = users.findByEmail("admin@techshop.test").orElseThrow().getId();
        var stores = branches.branches().stream().filter(BranchInventoryService.Branch::active).toList();
        if (stores.isEmpty()) throw new IllegalStateException("Sample catalog needs demo branches");
        Map<String, Long> categoryIds = new HashMap<>();
        catalog.categories(true).forEach(category -> categoryIds.put(category.code(), category.id()));
        Map<String, String> categoryNames = Map.of("PHONE", "Điện thoại", "LAPTOP", "Laptop",
                "TABLET", "Máy tính bảng", "WATCH", "Đồng hồ", "AUDIO", "Âm thanh", "GEAR", "Phụ kiện");

        var orderedSamples = new ArrayList<>(samples.products());
        java.util.Collections.reverse(orderedSamples);
        for (var sample : orderedSamples) {
            // Never overwrite existing edits or replenish sold/reserved stock on rerun.
            if (products.existsBySku(sample.sku())) continue;
            Long categoryId = categoryIds.computeIfAbsent(sample.category(), code ->
                    catalog.createCategory(new CatalogService.CategoryInput(code, categoryNames.get(code), "ACTIVE")).id());
            for (var photo : sample.images()) {
                String resourcePath = "static" + photo.assetPath();
                if (!photo.assetPath().startsWith("/assets/products/real/")
                        || photo.assetPath().contains("..") || !new ClassPathResource(resourcePath).exists()) {
                    throw new IllegalStateException("Missing packaged sample photo: " + photo.assetPath());
                }
            }
            String description = sample.name() + " · " + sample.brand()
                    + ". Tên và ảnh tham khảo sản phẩm thực. [Dữ liệu mẫu TechShop] Giá bán, tồn kho và serial"
                    + " chỉ phục vụ trải nghiệm và kiểm thử; không phải báo giá hoặc hàng tồn thực tế.";
            var product = catalog.createProduct(new CatalogService.ProductInput(categoryId, sample.sku(),
                    sample.name(), sample.samplePrice(), "ACTIVE", description));
            catalog.brand(product.id(),sample.brand(),product.version());
            var variant = variants.create(product.id(), new ProductVariantService.Input(sample.sku() + "-STD",
                    "Phiên bản theo ảnh", sample.storage(), sample.samplePrice(), sample.samplePrice()));
            int imageOrder = 0;
            for (var photo : sample.images()) {
                var image = catalog.addImage(product.id(), photo.assetPath(), sample.name(), imageOrder++);
                variants.assignImage(product.id(), image.getId(), variant.id());
            }
            variants.specification(product.id(), new ProductVariantService.SpecInput("Thông tin", "Thương hiệu", sample.brand(), 0));
            variants.specification(product.id(), new ProductVariantService.SpecInput("Thông tin", "Model", sample.name(), 1));
            int specOrder = 2;
            for (var specification : sample.specifications()) {
                variants.specification(product.id(), new ProductVariantService.SpecInput("Cấu hình",
                        specification.name(), specification.value(), specOrder++));
            }
            for (var store : stores) {
                var serials = new ArrayList<String>();
                for (int unit = 1; unit <= 4; unit++) serials.add("SAMPLE-" + variant.id() + "-" + store.id() + "-" + unit);
                branches.receive(admin, new BranchInventoryService.Import(variant.id(), store.id(), serials));
            }
        }
    }
}