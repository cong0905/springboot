package vn.techshop;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import vn.techshop.catalog.CatalogService;
import vn.techshop.config.RetailDemoCatalog;
import vn.techshop.variant.ProductVariantService;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:techshop-retail-catalog-test;MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000")
@ActiveProfiles("demo")
class RetailDemoCatalogTest {
    @Autowired RetailDemoCatalog seed;
    @Autowired CatalogService catalog;
    @Autowired ProductVariantService variants;
    @Autowired JdbcTemplate jdbc;

    @Test void sampleCatalogHasRealPhotosAndRerunPreservesProductsAndStock() throws Exception {
        var samples = seed.samples().products();
        assertThat(samples.size()).isGreaterThanOrEqualTo(30);
        assertThat(samples.stream().map(RetailDemoCatalog.SampleProduct::category).distinct()).hasSize(6);
        for (var sample : samples) {
            Long productId = jdbc.queryForObject("select id from products where sku=?", Long.class, sample.sku());
            var product = catalog.detail(productId, false);
            assertThat(product.name()).isEqualTo(sample.name());
            assertThat(product.description()).contains("Dữ liệu mẫu TechShop");
            assertThat(product.price()).isEqualByComparingTo(sample.samplePrice());
            assertThat(product.stockQuantity()).isEqualTo(8);
            assertThat(product.thumbnailPath()).startsWith("/assets/products/real/");
            assertThat(product.images()).hasSize(sample.images().size());
            for (var image : product.images()) assertThat(new ClassPathResource("static" + image).exists()).isTrue();
            var productVariants = variants.list(productId);
            assertThat(productVariants).hasSize(1);
            assertThat(productVariants.getFirst().available()).isEqualTo(product.stockQuantity());
            assertThat(productVariants.getFirst().images()).containsExactlyElementsOf(product.images());
            assertThat(variants.specifications(productId)).isNotEmpty();
        }
        // Rerunning must not reset or duplicate catalogue rows or inventory.
        Long variantId = jdbc.queryForObject("select v.id from product_variants v join products p on p.id=v.product_id where p.sku=?", Long.class, samples.getFirst().sku());
        Long stockBefore = jdbc.queryForObject("select coalesce(sum(quantity),0) from branch_inventories where variant_id=?", Long.class, variantId);
        Map<String, Long> counts = snapshot();
        seed.run();
        assertThat(snapshot()).isEqualTo(counts);
        assertThat(jdbc.queryForObject("select coalesce(sum(quantity),0) from branch_inventories where variant_id=?", Long.class, variantId)).isEqualTo(stockBefore);
    }

    private Map<String, Long> snapshot() {
        Map<String, Long> values = new LinkedHashMap<>();
        for (String table : new String[] {"products", "categories", "product_images", "product_variants", "product_specifications", "product_items_imei", "stock_movements"}) {
            values.put(table, jdbc.queryForObject("select count(*) from " + table, Long.class));
        }
        values.put("totalStock", jdbc.queryForObject("select sum(stock_quantity) from products", Long.class));
        return values;
    }
}