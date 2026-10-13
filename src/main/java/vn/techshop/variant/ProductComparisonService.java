package vn.techshop.variant;
import java.util.*;
import org.springframework.stereotype.Service;
import vn.techshop.catalog.CatalogService;
import vn.techshop.shared.BusinessException;
@Service
public class ProductComparisonService {
    public record Row(String group,String name,List<String> values) {}
    public record Comparison(List<CatalogService.ProductView> products,List<Row> specifications) {}
    private final CatalogService catalog;private final ProductVariantService variants;
    public ProductComparisonService(CatalogService catalog,ProductVariantService variants) {this.catalog=catalog;this.variants=variants;}
    public Comparison compare(List<Long> ids) {
        if(ids==null||ids.size()<2||ids.size()>3||new HashSet<>(ids).size()!=ids.size())throw BusinessException.invalid("Chọn 2–3 sản phẩm khác nhau để so sánh.");
        var products=ids.stream().map(id->catalog.detail(id,false)).toList();
        var maps=ids.stream().map(id->{var m=new LinkedHashMap<String,String>();for(var s:variants.specifications(id))m.put(s.group()+"\u0000"+s.name(),SpecificationDisplay.value(s.value()));return m;}).toList();
        var keys=new LinkedHashSet<String>();maps.forEach(m->keys.addAll(m.keySet()));
        return new Comparison(products,keys.stream().map(k->new Row(k.split("\u0000")[0],k.split("\u0000")[1],maps.stream().map(m->m.getOrDefault(k,"—")).toList())).toList());
    }
}
