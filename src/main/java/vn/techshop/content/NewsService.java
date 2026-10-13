package vn.techshop.content;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.shared.*;
@Service @Transactional(readOnly=true)
public class NewsService {
 public record Input(@NotBlank @Pattern(regexp="[a-z0-9]+(?:-[a-z0-9]+)*") @Size(max=160) String slug,@NotBlank @Size(min=5,max=200) String title,
  @NotBlank @Size(max=500) String excerpt,@NotBlank @Size(min=20,max=20000) String body,String status,String sourceUrl,Long expectedVersion){}
 public record Article(Long id,String slug,String title,String excerpt,String body,String status,String sourceUrl,Long version,Long authorId,java.time.Instant updatedAt){}
 private final JdbcTemplate jdbc;private final Clock clock;public NewsService(JdbcTemplate jdbc,Clock clock){this.jdbc=jdbc;this.clock=clock;}
 private org.springframework.jdbc.core.RowMapper<Article> mapper(){return(r,n)->new Article(r.getLong("id"),r.getString("slug"),r.getString("title"),r.getString("excerpt"),r.getString("body_text"),r.getString("status"),r.getString("source_url"),r.getLong("version"),r.getLong("author_id"),r.getTimestamp("updated_at").toInstant());}
 public PageView<Article> list(boolean admin,int page){if(page<0||page>100000)throw BusinessException.invalid("Phân trang không hợp lệ.");String where=admin?"":" where status='PUBLISHED'";Long total=jdbc.queryForObject("select count(*) from news_articles"+where,Long.class);return new PageView<>(jdbc.query("select * from news_articles"+where+" order by id desc limit 12 offset ?",mapper(),(long)page*12),page,12,total,(int)((total+11)/12));}
 public Article detail(Long id){return jdbc.query("select * from news_articles where id=?",mapper(),id).stream().findFirst().orElseThrow(BusinessException::missing);}
 public Article published(String slug){return jdbc.query("select * from news_articles where slug=? and status='PUBLISHED'",mapper(),slug).stream().findFirst().orElseThrow(BusinessException::missing);}
 @Transactional public Article save(Long actor,Long id,Input input){
  String slug=Rules.text(input.slug(),1,160);if(!slug.matches("[a-z0-9]+(?:-[a-z0-9]+)*"))throw BusinessException.invalid("Slug không hợp lệ.");
  String title=Rules.text(input.title(),5,200),excerpt=Rules.text(input.excerpt(),1,500),body=Rules.text(input.body(),20,20000),status=input.status()==null?"DRAFT":input.status();
  if(!Set.of("DRAFT","PUBLISHED","ARCHIVED").contains(status))throw BusinessException.invalid("Trạng thái bài viết không hợp lệ.");
  String source=Rules.optional(input.sourceUrl(),500);if(source!=null){try{var uri=java.net.URI.create(source);if(!Set.of("https","http").contains(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();}catch(IllegalArgumentException ex){throw BusinessException.invalid("Đường dẫn nguồn không hợp lệ.");}}
  if(id==null){var keys=new org.springframework.jdbc.support.GeneratedKeyHolder();String finalSource=source;jdbc.update(c->{var s=c.prepareStatement("insert into news_articles(slug,title,excerpt,body_text,status,source_url,author_id,updated_at) values(?,?,?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);s.setString(1,slug);s.setString(2,title);s.setString(3,excerpt);s.setString(4,body);s.setString(5,status);s.setString(6,finalSource);s.setLong(7,actor);s.setTimestamp(8,java.sql.Timestamp.from(clock.instant()));return s;},keys);id=Objects.requireNonNull(keys.getKey()).longValue();}
  else {var old=jdbc.query("select * from news_articles where id=? for update",mapper(),id).stream().findFirst().orElseThrow(BusinessException::missing);Rules.version(old.version(),input.expectedVersion(),"ARTICLE_CHANGED");jdbc.update("update news_articles set slug=?,title=?,excerpt=?,body_text=?,status=?,source_url=?,author_id=?,version=version+1,updated_at=? where id=?",slug,title,excerpt,body,status,source,actor,java.sql.Timestamp.from(clock.instant()),id);}
  return detail(id);
 }
}
