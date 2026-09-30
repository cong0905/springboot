package vn.techshop;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import vn.techshop.cart.*;
import vn.techshop.catalog.*;
import vn.techshop.identity.*;
import vn.techshop.inventory.*;
import vn.techshop.order.*;
import vn.techshop.reporting.*;
import vn.techshop.shared.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CommerceIntegrationTest {
    @Autowired IdentityService identity; @Autowired UserRepository users;
    @Autowired CatalogService catalog; @Autowired ProductRepository products;
    @Autowired CartService carts; @Autowired OrderService orders; @Autowired OrderRepository orderRepo;
    @Autowired InventoryService inventory; @Autowired ReportingService reports;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @Autowired JdbcTemplate jdbc; @Autowired MockMvc mvc;
    Long admin,customer,other,product,category;
    @BeforeEach void fixture() {
        try(var connection=java.util.Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            String url=connection.getMetaData().getURL();
            if(!url.startsWith("jdbc:h2:mem:techshop-test") && !url.matches("jdbc:mysql://[^/]+/techshop_test(?:\\?.*)?"))
                throw new IllegalStateException("Tests require H2 techshop-test or a dedicated MySQL techshop_test database");
        } catch(java.sql.SQLException e) { throw new IllegalStateException(e); }
        for(String table:List.of("stock_movements","order_status_history","order_items","orders","cart_items","carts","product_images","products","categories","users"))jdbc.update("delete from "+table);
        String key=UUID.randomUUID().toString();
        admin=identity.bootstrapAdmin(new IdentityService.Registration("Admin Test","admin"+key+"@test.local","TestAdmin2026!")).id();
        customer=identity.register(new IdentityService.Registration("Customer Test","user"+key+"@test.local","TestUser2026!")).id();
        other=identity.register(new IdentityService.Registration("Other Test","other"+key+"@test.local","TestOther2026!")).id();
        category=catalog.createCategory(new CatalogService.CategoryInput("CAT-"+key,"Danh mục kiểm thử","ACTIVE")).id();
        var p=catalog.createProduct(new CatalogService.ProductInput(category,"SKU-"+key,"Sản phẩm kiểm thử",new BigDecimal("600000"),"ACTIVE","Mô tả"));product=p.id();inventory.adjust(product,admin,new InventoryService.Adjustment(3,"Nhập kho kiểm thử",p.version()));
    }
    TechPrincipal principal(Long id) {return new TechPrincipal(users.findById(id).orElseThrow());}
    void add(Long user,int qty) {carts.add(user,new CartService.Add(product,qty,carts.view(user).version()));}
    OrderService.Checkout request(Long user,String key) {var cart=carts.view(user);return new OrderService.Checkout(key,cart.version(),cart.total(),cart.pricingHash(),"Khách Kiểm Thử","0900000000","Địa chỉ giả lập phục vụ kiểm thử",null);}
    @Test void checkoutReplaySnapshotsCancelAndLedgerAreConsistent() {
        add(customer,2);var request=request(customer,UUID.randomUUID().toString());var placed=orders.checkout(customer,request);
        assertThat(placed.replay()).isFalse();assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(orders.checkout(customer,request).order().id()).isEqualTo(placed.order().id());
        assertThat(carts.view(customer).items()).isEmpty();
        var p=catalog.detail(product,true);catalog.updateProduct(product,new CatalogService.ProductUpdate(category,"Tên đã thay đổi",new BigDecimal("700000"),"ACTIVE","Mô tả mới",p.version()));
        var snapshot=orders.detail(placed.order().id(),customer,false);assertThat(snapshot.items().getFirst().name()).isEqualTo("Sản phẩm kiểm thử");assertThat(snapshot.items().getFirst().unitPrice()).isEqualByComparingTo("600000");
        orders.cancel(snapshot.id(),customer,false,new OrderService.Cancel("Không còn nhu cầu",snapshot.version()));orders.cancel(snapshot.id(),customer,false,new OrderService.Cancel("Gửi lại yêu cầu hủy",snapshot.version()));
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from stock_movements where product_id=? and movement_type='CANCEL'",Long.class,product)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select sum(delta) from stock_movements where product_id=?",Long.class,product)).isEqualTo(3);
    }
    @Test void priceChangeAndKeyReuseDoNotMutateStock() {
        add(customer,1);var r=request(customer,UUID.randomUUID().toString());var p=catalog.detail(product,true);
        catalog.updateProduct(product,new CatalogService.ProductUpdate(category,p.name(),new BigDecimal("700000"),"ACTIVE",p.description(),p.version()));
        assertThatThrownBy(() -> orders.checkout(customer,r)).isInstanceOfSatisfying(BusinessException.class,e -> assertThat(e.code()).isEqualTo("PRICE_CHANGED"));
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(3);assertThat(carts.view(customer).items()).hasSize(1);
        var current=request(customer,r.checkoutKey());orders.checkout(customer,current);
        var changed=new OrderService.Checkout(current.checkoutKey(),current.cartVersion(),current.expectedTotal(),current.expectedPricingHash(),current.recipientName(),current.phone(),"Một địa chỉ khác dùng để kiểm thử",null);
        assertThatThrownBy(() -> orders.checkout(customer,changed)).isInstanceOfSatisfying(BusinessException.class,e -> assertThat(e.code()).isEqualTo("CHECKOUT_KEY_REUSED"));
    }
    @Test void missingCsrfWrongRoleOwnershipAndUnknownFieldsAreRejected() throws Exception {
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/cart/items").with(user(principal(customer))).contentType("application/json").content("{\"productId\":"+product+",\"quantity\":1,\"expectedVersion\":0}")).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/orders").with(user(principal(customer)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType("application/json").content("{\"fullName\":\"Demo User\",\"email\":\"unknown@test.local\",\"password\":\"TestUser2026!\",\"role\":\"ADMIN\"}")).andExpect(status().isBadRequest());
        add(customer,1);var order=orders.checkout(customer,request(customer,UUID.randomUUID().toString())).order();
        mvc.perform(get("/api/v1/orders/"+order.id()).with(user(principal(other)))).andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/orders/"+order.id()+"/cancel").with(user(principal(other))).with(csrf()).contentType("application/json").content("{\"reason\":\"Hủy thử không có quyền\",\"expectedVersion\":0}")).andExpect(status().isNotFound());
    }
    @Test void coreTemplatesRenderForCustomerAndAdmin() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk()).andExpect(view().name("products"));
        mvc.perform(get("/products/"+product).with(user(principal(customer)))).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());mvc.perform(get("/register")).andExpect(status().isOk());
        add(customer,1);mvc.perform(get("/cart").with(user(principal(customer)))).andExpect(status().isOk());mvc.perform(get("/checkout").with(user(principal(customer)))).andExpect(status().isOk());
        var order=orders.checkout(customer,request(customer,UUID.randomUUID().toString())).order();
        mvc.perform(get("/orders").with(user(principal(customer)))).andExpect(status().isOk());mvc.perform(get("/orders/"+order.id()).with(user(principal(customer)))).andExpect(status().isOk());
        for(String path:List.of("/admin","/admin/products","/admin/products/new","/admin/products/"+product+"/edit","/admin/categories","/admin/orders","/admin/orders/"+order.id()))mvc.perform(get(path).with(user(principal(admin)))).andExpect(status().isOk());
    }
    @Test void staleCartAndNegativeStockAreRejected() {
        Long version=carts.view(customer).version();carts.add(customer,new CartService.Add(product,1,version));
        assertThatThrownBy(() -> carts.add(customer,new CartService.Add(product,1,version))).isInstanceOfSatisfying(BusinessException.class,e -> assertThat(e.code()).isEqualTo("CART_CHANGED"));
        var p=catalog.detail(product,true);
        assertThatThrownBy(() -> inventory.adjust(product,admin,new InventoryService.Adjustment(-4,"Điều chỉnh không hợp lệ",p.version()))).isInstanceOf(BusinessException.class);
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(3);
    }
    @Test void transitionAndRevenueUseDeliveredTime() {
        add(customer,1);var o=orders.checkout(customer,request(customer,UUID.randomUUID().toString())).order();
        assertThatThrownBy(() -> orders.transition(o.id(),admin,new OrderService.Transition("DELIVERED",o.version(),true))).isInstanceOf(BusinessException.class);
        var confirmed=orders.transition(o.id(),admin,new OrderService.Transition("CONFIRMED",o.version(),false));
        var shipped=orders.transition(o.id(),admin,new OrderService.Transition("SHIPPED",confirmed.version(),false));
        assertThatThrownBy(() -> orders.transition(o.id(),admin,new OrderService.Transition("DELIVERED",shipped.version(),false))).isInstanceOf(BusinessException.class);
        var delivered=orders.transition(o.id(),admin,new OrderService.Transition("DELIVERED",shipped.version(),true));
        jdbc.update("update orders set created_at=?,delivered_at=? where id=?",LocalDateTime.parse("2020-01-01T00:00:00"),LocalDateTime.parse("2021-10-01T17:30:00"),o.id());
        var report=reports.summary(LocalDate.of(2021,10,2),LocalDate.of(2021,10,2));assertThat(report.deliveredOrders()).isEqualTo(1);assertThat(report.placedOrders()).isZero();assertThat(report.merchandiseRevenue()).isEqualByComparingTo("600000");assertThat(report.shippingCollected()).isEqualByComparingTo("30000");assertThat(delivered.history()).hasSize(4);
        jdbc.update("update orders set delivered_at=? where id=?",LocalDateTime.parse("2021-10-02T17:00:00"),o.id());
        assertThat(reports.summary(LocalDate.of(2021,10,2),LocalDate.of(2021,10,2)).deliveredOrders()).isZero();
        jdbc.update("update orders set delivered_at=? where id=?",LocalDateTime.parse("2021-10-01T17:00:00"),o.id());
        assertThat(reports.summary(LocalDate.of(2021,10,2),LocalDate.of(2021,10,2)).deliveredOrders()).isEqualTo(1);
    }
    @Test void lastUnitConcurrentCheckoutHasOneWinner() throws Exception {
        var p=catalog.detail(product,true);inventory.adjust(product,admin,new InventoryService.Adjustment(-2,"Giảm tồn còn một",p.version()));add(customer,1);add(other,1);
        var first=request(customer,UUID.randomUUID().toString());var second=request(other,UUID.randomUUID().toString());var gate=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            var a=executor.submit(() -> compete(gate,customer,first));var b=executor.submit(() -> compete(gate,other,second));
            var results=List.of(a.get(15,TimeUnit.SECONDS),b.get(15,TimeUnit.SECONDS));assertThat(results).containsExactlyInAnyOrder("SUCCESS","INSUFFICIENT_STOCK");
        }
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from order_items where product_id=?",Long.class,product)).isEqualTo(1);
    }
    @Test void simultaneousSameKeyAndCancellationDoNotDuplicateMovements() throws Exception {
        add(customer,1);var input=request(customer,UUID.randomUUID().toString());var gate=new CyclicBarrier(2);Long orderId;
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<Long> task=() -> {gate.await(10,TimeUnit.SECONDS);return orders.checkout(customer,input).order().id();};var a=executor.submit(task);var b=executor.submit(task);orderId=a.get(15,TimeUnit.SECONDS);assertThat(b.get(15,TimeUnit.SECONDS)).isEqualTo(orderId);
        }
        var order=orders.detail(orderId,customer,false);var cancelGate=new CyclicBarrier(2);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Callable<String> task=() -> {cancelGate.await(10,TimeUnit.SECONDS);return orders.cancel(order.id(),customer,false,new OrderService.Cancel("Hủy đồng thời kiểm thử",order.version())).status();};var a=executor.submit(task);var b=executor.submit(task);assertThat(a.get(15,TimeUnit.SECONDS)).isEqualTo("CANCELLED");assertThat(b.get(15,TimeUnit.SECONDS)).isEqualTo("CANCELLED");
        }
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(jdbc.queryForObject("select count(*) from stock_movements where product_id=? and movement_type in ('ORDER','CANCEL')",Long.class,product)).isEqualTo(2);
    }
    @Test void checkoutWritesRollBackTogetherWhenTransactionFails() {
        add(customer,1);var input=request(customer,UUID.randomUUID().toString());var before=carts.view(customer);
        var tx=new org.springframework.transaction.support.TransactionTemplate(transactions);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            orders.checkout(customer,input);
            throw new IllegalStateException("Simulated failure before commit");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(carts.view(customer).items()).hasSize(1);
        assertThat(carts.view(customer).version()).isEqualTo(before.version());
        assertThat(jdbc.queryForObject("select count(*) from orders",Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from order_items",Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from order_status_history",Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from stock_movements where movement_type='ORDER'",Long.class)).isZero();
        assertThat(orders.checkout(customer,input).replay()).isFalse();
    }
    @Test void loginCreatesSessionAndLogoutInvalidatesIt() throws Exception {
        String email=users.findById(customer).orElseThrow().getEmail();
        var login=mvc.perform(post("/login").with(csrf()).param("username",email.toUpperCase(Locale.ROOT)).param("password","TestUser2026!"))
            .andExpect(status().isSeeOther()).andExpect(redirectedUrl("/products")).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false);
        mvc.perform(get("/api/v1/me").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("CUSTOMER"));
        mvc.perform(post("/logout").session(session).with(csrf())).andExpect(status().isSeeOther());
        assertThat(session.isInvalid()).isTrue();
    }
    @Test void inactiveCatalogInvalidFiltersAndEmptyStatusAreHandled() throws Exception {
        catalog.updateCategory(category,new CatalogService.CategoryUpdate("Ẩn danh mục","INACTIVE",0L));
        assertThat(catalog.search(null,null,null,null,"newest",0,20,false,null).content()).isEmpty();
        mvc.perform(get("/api/v1/products/"+product)).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/products").param("minPrice","700000").param("maxPrice","600000")).andExpect(status().isBadRequest());
        mvc.perform(get("/orders").param("status","").with(user(principal(customer)))).andExpect(status().isOk());
    }
    private String compete(CyclicBarrier gate,Long user,OrderService.Checkout input) throws Exception {
        gate.await(10,TimeUnit.SECONDS);try {orders.checkout(user,input);return "SUCCESS";} catch(BusinessException e) {return e.code();}
    }
}
