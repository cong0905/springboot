package vn.techshop.catalog;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.*;
@Service @Transactional(readOnly=true)
public class StorefrontCatalogService {
 public record Facet(String value,long count){}
 public record Facets(List<Facet> brands,List<Facet> storage){}
 private final NamedParameterJdbcTemplate jdbc;
 public StorefrontCatalogService(NamedParameterJdbcTemplate jdbc){this.jdbc=jdbc;}
 private static final String PRICE="coalesce(v.price,p.price)";
 private static final String AVAILABLE="coalesce(stock.available,case when v.id is null then p.stock_quantity else 0 end)";
 private static final String FROM=" from products p join categories c on c.id=p.category_id left join product_variants v on v.product_id=p.id and v.status='ACTIVE' left join (select i.variant_id,sum(i.quantity-i.reserved_quantity) available from branch_inventories i join store_branches b on b.id=i.branch_id where b.is_active=true group by i.variant_id) stock on stock.variant_id=v.id ";
 private record Query(String where,Map<String,Object> params){}
 private Query filters(String keyword,Long category,BigDecimal min,BigDecimal max,String brand,String storage,boolean inStock){
  if(keyword!=null&&keyword.length()>100)throw BusinessException.invalid("Từ khóa quá dài.");
  if(min!=null&&min.signum()<0||max!=null&&max.signum()<0||min!=null&&max!=null&&min.compareTo(max)>0)throw BusinessException.invalid("Khoảng giá không hợp lệ.");
  var args=new HashMap<String,Object>();var where=new StringBuilder(" where p.status='ACTIVE' and c.status='ACTIVE' and (v.id is not null or not exists(select 1 from product_variants pv where pv.product_id=p.id))");
  if(keyword!=null&&!keyword.isBlank()){args.put("keyword","%"+keyword.strip().toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%");where.append(" and lower(p.name) like :keyword escape '!'");}
  if(category!=null){args.put("category",category);where.append(" and p.category_id=:category");}
  if(min!=null){args.put("min",min);where.append(" and "+PRICE+">=:min");}if(max!=null){args.put("max",max);where.append(" and "+PRICE+"<=:max");}
  if(brand!=null&&!brand.isBlank()){args.put("brand",Rules.text(brand,1,80));where.append(" and p.brand=:brand");}
  if(storage!=null&&!storage.isBlank()){args.put("storage",Rules.text(storage,1,80));where.append(" and v.storage_capacity=:storage");}
  if(inStock)where.append(" and "+AVAILABLE+">0");return new Query(where.toString(),args);
 }
 public PageView<CatalogService.ProductView> search(String keyword,Long category,BigDecimal min,BigDecimal max,String sort,int page,int size,String brand,String storage,boolean inStock){
  PageView.request(page,size,org.springframework.data.domain.Sort.unsorted());var q=filters(keyword,category,min,max,brand,storage,inStock);
  String ordering=switch(sort){case "newest"->"p.created_at desc,p.id desc";case "price_asc"->"offer_price,p.id";case "price_desc"->"offer_price desc,p.id desc";default->throw BusinessException.invalid("Kiểu sắp xếp không hợp lệ.");};
  Long total=jdbc.queryForObject("select count(distinct p.id)"+FROM+q.where(),q.params(),Long.class);
  var args=new HashMap<>(q.params());args.put("limit",size);args.put("offset",(long)page*size);
  var rows=jdbc.query("select p.*,c.name category_name,min("+PRICE+") offer_price,sum("+AVAILABLE+") offer_stock"+FROM+q.where()+" group by p.id,c.name order by "+ordering+" limit :limit offset :offset",args,
   (r,n)->new CatalogService.ProductView(r.getLong("id"),r.getString("sku"),r.getString("name"),r.getString("description"),r.getBigDecimal("offer_price"),"VND",r.getInt("offer_stock"),r.getString("status"),r.getLong("version"),r.getLong("category_id"),r.getString("category_name"),"/assets/products/device.svg"));
  if(!rows.isEmpty()){
   var ids=rows.stream().map(CatalogService.ProductView::id).toList();var images=new HashMap<Long,List<String>>();
   jdbc.query("select product_id,asset_path from product_images where product_id in (:ids) order by sort_order,id",Map.of("ids",ids),(org.springframework.jdbc.core.RowCallbackHandler)r->images.computeIfAbsent(r.getLong("product_id"),k->new ArrayList<>()).add(r.getString("asset_path")));
   rows=rows.stream().map(p->{var album=images.getOrDefault(p.id(),List.of("/assets/products/device.svg"));return new CatalogService.ProductView(p.id(),p.sku(),p.name(),p.description(),p.price(),p.currency(),p.stockQuantity(),p.status(),p.version(),p.categoryId(),p.categoryName(),album.getFirst(),album);}).toList();
  }
  return new PageView<>(rows,page,size,total==null?0:total,total==null?0:(int)((total+size-1)/size));
 }
 public Facets facets(String keyword,Long category,BigDecimal min,BigDecimal max,String brand,String storage,boolean inStock){
  var q=filters(keyword,category,min,max,brand,storage,inStock);
  return new Facets(jdbc.query("select p.brand facet_value,count(distinct p.id) facet_count"+FROM+q.where()+" and p.brand is not null group by p.brand order by p.brand",q.params(),(r,n)->new Facet(r.getString("facet_value"),r.getLong("facet_count"))),
   jdbc.query("select v.storage_capacity facet_value,count(distinct p.id) facet_count"+FROM+q.where()+" and v.storage_capacity is not null group by v.storage_capacity order by v.storage_capacity",q.params(),(r,n)->new Facet(r.getString("facet_value"),r.getLong("facet_count"))));
 }
 public Map<Long,BigDecimal> originals(List<CatalogService.ProductView> products){
  var result=new HashMap<Long,BigDecimal>();products.forEach(p->result.put(p.id(),p.price()));if(products.isEmpty())return result;
  var prices=new HashMap<Long,BigDecimal>();products.forEach(p->prices.put(p.id(),p.price()));
  jdbc.query("select product_id,price,original_price from product_variants where status='ACTIVE' and product_id in (:ids)",Map.of("ids",result.keySet()),(org.springframework.jdbc.core.RowCallbackHandler)r->{Long id=r.getLong("product_id");if(r.getBigDecimal("price").compareTo(prices.get(id))==0)result.merge(id,r.getBigDecimal("original_price"),BigDecimal::max);});return result;
 }
}
