package vn.techshop.catalog;
import java.util.*;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.identity.UserRepository;
import vn.techshop.shared.*;
@Service @Transactional(readOnly=true)
public class WishlistService {
 public record Entry(Long id,String name,boolean available){}
 private final JdbcTemplate jdbc;private final CatalogService catalog;private final UserRepository users;private final Clock clock;
 public WishlistService(JdbcTemplate jdbc,CatalogService catalog,UserRepository users,Clock clock){this.jdbc=jdbc;this.catalog=catalog;this.users=users;this.clock=clock;}
 public List<Entry> list(Long owner){return jdbc.query("select p.id,p.name,p.status,c.status category_status from wishlists w join products p on p.id=w.product_id join categories c on c.id=p.category_id where w.user_id=? order by w.created_at desc,p.id",(r,n)->new Entry(r.getLong("id"),r.getString("name"),"ACTIVE".equals(r.getString("status"))&&"ACTIVE".equals(r.getString("category_status"))),owner);}
 @Transactional public void add(Long owner,Long product){users.lockById(owner).orElseThrow(BusinessException::missing);catalog.detail(product,false);
  if(jdbc.queryForObject("select count(*) from wishlists where user_id=? and product_id=?",Integer.class,owner,product)>0)return;
  if(jdbc.queryForObject("select count(*) from wishlists where user_id=?",Integer.class,owner)>=100)throw BusinessException.invalid("Tối đa 100 sản phẩm yêu thích.");
  jdbc.update("insert into wishlists(user_id,product_id,created_at) values(?,?,?)",owner,product,java.sql.Timestamp.from(clock.instant()));
 }
 @Transactional public void remove(Long owner,Long product){jdbc.update("delete from wishlists where user_id=? and product_id=?",owner,product);}
}
