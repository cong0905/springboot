package vn.techshop.catalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import javax.sql.DataSource;
import java.sql.*;
import java.math.BigDecimal;
import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Import(CatalogQueryBudgetTest.CounterConfig.class)
class CatalogQueryBudgetTest {
 static AtomicLong statements=new AtomicLong();
 @TestConfiguration static class CounterConfig {
  @Bean static BeanPostProcessor countedSource(){return new BeanPostProcessor(){public Object postProcessAfterInitialization(Object bean,String name){
   if(!(bean instanceof DataSource source))return bean;
   return new DelegatingDataSource(source){
    Connection counted(Connection c){return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,args)->{
     try{
      Object result=m.invoke(c,args);
      if(result instanceof Statement statement){
       Class<?> api=statement instanceof CallableStatement?CallableStatement.class:statement instanceof PreparedStatement?PreparedStatement.class:Statement.class;
       return Proxy.newProxyInstance(Statement.class.getClassLoader(),new Class[]{api},(sp,sm,sa)->{
        if(sm.getName().startsWith("execute"))statements.incrementAndGet();
        try{return sm.invoke(statement,sa);}catch(InvocationTargetException ex){throw ex.getCause();}
       });
      }
      return result;
     }catch(InvocationTargetException ex){throw ex.getCause();}
    });}
    public Connection getConnection() throws SQLException{return counted(super.getConnection());}
    public Connection getConnection(String u,String p)throws SQLException{return counted(super.getConnection(u,p));}
   };}};}
 }
 @Autowired MockMvc mvc;@Autowired CatalogService catalog;@Autowired vn.techshop.variant.ProductVariantService variants;
 @Autowired vn.techshop.branch.BranchInventoryService branches;@Autowired vn.techshop.order.OrderRoutingService routing;@Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Test void routingUsesTwoQueriesInsteadOfRepeatedStoreReads() {
  String key="route-"+java.util.UUID.randomUUID();var cat=catalog.createCategory(new CatalogService.CategoryInput(key,"Route budget","ACTIVE"));
  var needs=new java.util.ArrayList<vn.techshop.order.OrderRoutingService.Need>();
  for(int i=0;i<3;i++){var p=catalog.createProduct(new CatalogService.ProductInput(cat.id(),key+i,"Route item "+i,new BigDecimal("100000"),"ACTIVE",null));var v=variants.create(p.id(),new vn.techshop.variant.ProductVariantService.Input(key+i,"Black","128GB",new BigDecimal("100000"),new BigDecimal("100000")));needs.add(new vn.techshop.order.OrderRoutingService.Need(v.id(),1));}
  for(int i=0;i<8;i++){var b=branches.create(new vn.techshop.branch.BranchInventoryService.BranchInput("QA Route store",key+i,"123 QA Hanoi Route Road","HN","0912345678",21,105));for(var n:needs)jdbc.update("insert into branch_inventories(branch_id,variant_id,quantity,reserved_quantity,version) values(?,?,2,0,0)",b.id(),n.variantId());}
  var before=new java.util.ArrayList<Long>();var after=new java.util.ArrayList<Long>();long beforeQueries=0,afterQueries=0;
  for(int sample=0;sample<20;sample++){
   statements.set(0);long start=System.nanoTime();var candidates=branches.branches().stream().filter(vn.techshop.branch.BranchInventoryService.Branch::active).filter(b->needs.stream().allMatch(n->branches.findStoresWithStock(n.variantId(),null).stream().anyMatch(s->s.branchId().equals(b.id())&&s.available()>=n.quantity()))).toList();before.add((System.nanoTime()-start)/1_000_000);beforeQueries=Math.max(beforeQueries,statements.get());
   statements.set(0);start=System.nanoTime();var route=routing.route(needs,"HOME_DELIVERY_STANDARD",null,null,null,BigDecimal.ZERO);after.add((System.nanoTime()-start)/1_000_000);afterQueries=Math.max(afterQueries,statements.get());assertThat(route.branchId()).isEqualTo(candidates.getFirst().id());
  }
  before.sort(Long::compare);after.sort(Long::compare);System.out.println("ROUTING_MEASURE before_queries="+beforeQueries+" before_p95_ms="+before.get(18)+" after_queries="+afterQueries+" after_p95_ms="+after.get(18)+" samples=20 stores=8 variants=3");assertThat(afterQueries).isEqualTo(2);
 }
 @Test void twelveCardsHaveBoundedQueriesAndMeasuredLatency() throws Exception {
  String key="budget-"+java.util.UUID.randomUUID();var cat=catalog.createCategory(new CatalogService.CategoryInput(key,"Query budget","ACTIVE"));
  for(int i=0;i<12;i++){var product=catalog.createProduct(new CatalogService.ProductInput(cat.id(),key+i,key+" item "+i,new BigDecimal("100000"),"ACTIVE",null));variants.create(product.id(),new vn.techshop.variant.ProductVariantService.Input(key+i+"v","Black","128GB",new BigDecimal("100000"),new BigDecimal("110000")));}
  mvc.perform(get("/products").param("keyword",key)).andExpect(status().isOk());
  var elapsed=new java.util.ArrayList<Long>();long maxQueries=0;
  for(int i=0;i<20;i++){statements.set(0);long start=System.nanoTime();mvc.perform(get("/products").param("keyword",key)).andExpect(status().isOk());elapsed.add((System.nanoTime()-start)/1_000_000);maxQueries=Math.max(maxQueries,statements.get());}
  elapsed.sort(Long::compare);System.out.println("CATALOG_MEASURE queries="+maxQueries+" p95_ms="+elapsed.get(18)+" samples=20 cards=12");
  if(!Boolean.getBoolean("repair.measure.baseline"))assertThat(maxQueries).isLessThanOrEqualTo(12);
 }
}
