package vn.techshop;

import java.math.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import vn.techshop.branch.*;
import vn.techshop.variant.*;
import vn.techshop.order.*;
import vn.techshop.loyalty.*;
import vn.techshop.tradein.*;
import vn.techshop.installment.*;
import vn.techshop.warranty.*;
import vn.techshop.payment.*;
import vn.techshop.shared.*;
import vn.techshop.config.CommerceSettingsService;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest(properties={"app.payment.vnpay.tmn-code=TESTSHOP","app.payment.vnpay.hash-secret=test-signature-key-not-real","app.payment.mock.enabled=true","app.pickup.expiry-interval-ms=3600000"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class OmnichannelIntegrationTest {
    @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;@Autowired IdentityService identity;@Autowired UserRepository users;
    @Autowired CatalogService catalog;@Autowired ProductRepository products;@Autowired ProductVariantService variants;
    @Autowired ProductComparisonService comparison;@Autowired BranchInventoryService branches;@Autowired OrderRoutingService routing;
    @Autowired CartService carts;@Autowired OrderService orders;@Autowired OrderRepository repo;@Autowired PickupService pickup;
    @Autowired LoyaltyService loyalty;@Autowired TradeInService tradein;@Autowired InstallmentService installments;
    @Autowired WarrantyService warranty;@Autowired PaymentService payments;
    @Autowired WarrantyExchangeService exchanges;@Autowired vn.techshop.reporting.CommerceReportingService commerceReports;
    Long admin,customer,other,product,variant,branch,farBranch,category;
    @BeforeEach void fixture() {
        clean();var key=UUID.randomUUID().toString();
        admin=identity.bootstrapAdmin(new IdentityService.Registration("Admin",key+"a@test.local","DemoAdmin2026!")).id();
        customer=identity.register(new IdentityService.Registration("Customer",key+"c@test.local","DemoCustomer2026!")).id();
        other=identity.register(new IdentityService.Registration("Other",key+"o@test.local","DemoCustomer2026!")).id();
        category=catalog.createCategory(new CatalogService.CategoryInput("PHONE-"+key,"Phones","ACTIVE")).id();
        product=catalog.createProduct(new CatalogService.ProductInput(category,"MODEL-"+key,"Phone Test",new BigDecimal("15000000"),"ACTIVE","Sample only")).id();
        variant=variants.create(product,new ProductVariantService.Input("VAR-"+key,"Đen","256GB",new BigDecimal("15000000"),new BigDecimal("16000000"))).id();
        branch=branches.create(new BranchInventoryService.BranchInput("Near","NEAR","123 Thái Hà Hà Nội","HN","0900000000",21.0126,105.8197)).id();
        farBranch=branches.create(new BranchInventoryService.BranchInput("Far","FAR","100 Cầu Giấy Hà Nội","HN","0900000001",21.04,105.78)).id();
        branches.receive(admin,new BranchInventoryService.Import(variant,branch,List.of("TEST-IMEI-001","TEST-IMEI-002")));
        jdbc.update("insert into trade_in_devices(model,base_price,subsidy_amount) values('Old Phone',3000000,500000)");
        jdbc.update("insert into trade_in_criterias(name,deduction_percent) values('Xước nhẹ',10)");
        jdbc.update("insert into installment_plans(provider,months,fee_percent) values('Demo',3,0)");
    }
    @AfterEach void cleanup() {clean();}
    void clean() {
        try(var c=Objects.requireNonNull(jdbc.getDataSource()).getConnection()) {
            String url=c.getMetaData().getURL();if(!url.startsWith("jdbc:h2:mem:techshop-test")&&!url.matches("jdbc:mysql://[^/]+/techshop_test(?:\\?.*)?"))throw new IllegalStateException("Dedicated test database required");
        } catch(java.sql.SQLException ex) {throw new IllegalStateException(ex);}
        for(String t:List.of("refund_reconciliations","repair_tickets","warranty_cards","point_transactions","member_profiles","installment_applications","trade_in_requests","imei_history","product_items_imei","branch_inventories","payment_transactions","product_reviews","stock_movements","order_status_history","order_items","orders","cart_items","carts","product_images","product_specifications","product_variants","products","categories","users","store_branches","trade_in_devices","trade_in_criterias","installment_plans","coupons"))jdbc.update("delete from "+t);
    }
    TechPrincipal principal(Long id) {return new TechPrincipal(users.findById(id).orElseThrow());}
    @Autowired vn.techshop.catalog.StorefrontCatalogService storefront;
    @Test void filtersMatchPriceStorageAndStockOnSameVariantWithoutGuessingBrand() throws Exception {
        catalog.brand(product,"Verified Brand",catalog.detail(product,true).version());
        var expensive=variants.create(product,new ProductVariantService.Input("SECOND-"+UUID.randomUUID(),"Silver","512GB",new BigDecimal("20000000"),new BigDecimal("21000000")));
        assertThat(storefront.search("Phone Test",category,null,new BigDecimal("16000000"),"price_asc",0,12,"Verified Brand","512GB",false).content()).isEmpty();
        assertThat(storefront.search("Phone Test",category,null,null,"newest",0,12,"Phone",null,false).content()).isEmpty();
        assertThat(storefront.search("Phone Test",category,null,null,"newest",0,12,"Verified Brand","512GB",true).content()).isEmpty();
        branches.receive(admin,new BranchInventoryService.Import(expensive.id(),branch,List.of("SECOND-"+UUID.randomUUID().toString().substring(0,20))));
        var filtered=storefront.search("Phone Test",category,new BigDecimal("19000000"),null,"price_asc",0,12,"Verified Brand","512GB",true);
        assertThat(filtered.content()).hasSize(1);assertThat(filtered.content().getFirst().price()).isEqualByComparingTo("20000000");assertThat(filtered.content().getFirst().stockQuantity()).isEqualTo(1);
        assertThat(storefront.facets("Phone Test",category,null,null,"Verified Brand","512GB",true).storage()).containsExactly(new vn.techshop.catalog.StorefrontCatalogService.Facet("512GB",1));
        mvc.perform(get("/products").param("brand","Verified Brand").param("storage","512GB").param("inStock","true")).andExpect(status().isOk());
    }
    @Test void adminRefundScreenUsesProtectedRealLedger() throws Exception {
        mvc.perform(get("/admin/refunds").with(user(principal(customer)))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/refunds").with(user(principal(admin)))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("Hoàn tiền và đối soát")));
    }
    void add(Long id,int qty) {carts.add(id,new CartService.Add(product,qty,carts.view(id).version(),variant));}
    OrderService.Options options(String type) {return new OrderService.Options(type,"CLICK_AND_COLLECT".equals(type)?branch:null,null,"HOME_DELIVERY_INSTANT_2H".equals(type)?21.0127:null,"HOME_DELIVERY_INSTANT_2H".equals(type)?105.8198:null,null,null);}
    OrderService.Checkout request(Long id,OrderService.Options o,String method) {
        var p=orders.preview(id,new OrderService.PreviewRequest(null,o));return new OrderService.Checkout(UUID.randomUUID().toString(),p.cartVersion(),p.total(),p.pricingHash(),"Customer Test","0900000000","123 Test Address Hanoi",null,null,method,o);
    }
    OrderService.OrderView deliver(Long id) {
        var o=orders.detail(id,admin,true);
        if(o.status().equals("PENDING"))o=orders.transition(id,admin,new OrderService.Transition("CONFIRMED",o.version(),false));
        if(o.status().equals("CONFIRMED"))o=orders.transition(id,admin,new OrderService.Transition("SHIPPED",o.version(),false));
        boolean needCod = "COD".equals(o.paymentMethod()) && !"PAID".equals(o.paymentStatus());
        return orders.transition(id,admin,new OrderService.Transition("DELIVERED",o.version(),needCod));
    }
    @Test void instantDeliveryRoutesReservesSellsActivatesWarrantyAndAwardsMember() {
        add(customer,1);var p=orders.preview(customer,new OrderService.PreviewRequest(null,options("HOME_DELIVERY_INSTANT_2H")));
        assertThat(p.assignedBranchId()).isEqualTo(branch);assertThat(p.shippingFee()).isEqualByComparingTo("25000");
        var req=request(customer,options("HOME_DELIVERY_INSTANT_2H"),"COD");var order=orders.checkout(customer,req).order();
        assertThat(order.items().getFirst().imeiNumbers()).hasSize(1);assertThat(orders.checkout(customer,req).replay()).isTrue();
        assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isEqualTo(1);
        var done=deliver(order.id());assertThat(done.status()).isEqualTo("DELIVERED");
        var card=warranty.lookup(done.items().getFirst().imeiNumbers().getFirst(),"0900000000").getFirst();
        assertThat(card.endDate()).isEqualTo(card.startDate().plusMonths(12));assertThat(loyalty.member(customer).rank()).isEqualTo("S_MEM");assertThat(loyalty.member(customer).pointsBalance()).isEqualTo(150);
        assertThat(jdbc.queryForObject("select count(*) from point_transactions",Integer.class)).isEqualTo(1);
        assertThat(branches.items(branch).stream().filter(i->i.status().equals("SOLD"))).hasSize(1);
    }
    @Test void pickupRequiresCodeAndCollectIsIdempotent() {
        add(customer,1);var o=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();
        assertThat(o.shippingFee()).isEqualByComparingTo("0");assertThat(o.holdExpiresAt()).isNotNull();
        assertThatThrownBy(()->pickup.collect(o.id(),admin,"wrong")).isInstanceOf(BusinessException.class);
        assertThat(pickup.collect(o.id(),admin,o.pickupCode()).status()).isEqualTo("DELIVERED");
        assertThat(pickup.collect(o.id(),admin,o.pickupCode()).status()).isEqualTo("DELIVERED");
        assertThat(jdbc.queryForObject("select count(*) from point_transactions",Integer.class)).isEqualTo(1);
    }
    @Test void pickupExpiresAndReleasesOnce() {
        add(customer,1);var o=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();
        holdUntil(o.id(),Instant.now().minusSeconds(1));
        pickup.expire();pickup.expire();assertThat(orders.detail(o.id(),customer,false).status()).isEqualTo("CANCELLED");
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isZero();
        assertThat(branches.items(branch).stream().allMatch(i->i.status().equals("IN_STOCK"))).isTrue();
    }
    @Test void publicBranchFiltersIncludeDistrictAndExcludeInactiveStores() throws Exception {
        jdbc.update("update store_branches set district_code='DONG-DA' where id=?",branch);
        jdbc.update("update store_branches set district_code='CAU-GIAY' where id=?",farBranch);
        jdbc.update("insert into branch_inventories(branch_id,variant_id,quantity,reserved_quantity,version) values(?,?,0,0,0)",farBranch,variant);
        mvc.perform(get("/api/v1/branches").param("provinceCode","HN").param("districtCode","DONG-DA"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].id").value(branch));
        mvc.perform(get("/api/v1/variants/"+variant+"/stores").param("districtCode","CAU-GIAY").param("inStock","true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/v1/variants/"+variant+"/stores").param("provinceCode","HN").param("districtCode","DONG-DA").param("inStock","true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].available").value(2));
        jdbc.update("update store_branches set is_active=false where id=?",branch);
        assertThat(branches.publicBranches("HN","DONG-DA")).isEmpty();
        assertThat(branches.findStoresWithStock(variant,"HN","DONG-DA",false)).isEmpty();
        mvc.perform(get("/branches")).andExpect(status().isOk()).andExpect(model().attribute("branches",branches.publicBranches(null,null)));
    }
    @Test void adminCanCreateDistrictAndInactiveProductRemainsEditable() throws Exception {
        mvc.perform(post("/api/v1/admin/branches").with(user(principal(admin))).with(csrf()).contentType("application/json")
            .content("{\"name\":\"Store\",\"code\":\"DISTRICT-TEST\",\"address\":\"123 Test Address\",\"provinceCode\":\"HN\",\"phone\":\"0900000000\",\"latitude\":21,\"longitude\":105,\"districtCode\":\"DONG-DA\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.districtCode").value("DONG-DA"));
        jdbc.update("update products set status='INACTIVE' where id=?",product);
        mvc.perform(get("/admin/products/"+product+"/edit").with(user(principal(admin)))).andExpect(status().isOk());
        mvc.perform(get("/products/"+product)).andExpect(status().isNotFound());
    }
    @Test void guestsCanInspectVariantAndStoresWithoutAccessToCheckout() throws Exception {
        String html=mvc.perform(get("/products/"+product)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("data-variant","data-product-id=\""+product+"\"","data-region-filters").doesNotContain("id=\"product-cart\"");
        mvc.perform(get("/api/v1/products/"+product+"/variants")).andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(variant));
        mvc.perform(get("/checkout")).andExpect(status().isSeeOther()).andExpect(header().string("Location","/login"));
    }
    @Test void pickupQrDecodesToCodeAndChecksOwnerAndRole() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();
        String url="/api/v1/orders/"+order.id()+"/pickup-qr.png";
        byte[] bytes=mvc.perform(get(url).with(user(principal(customer)))).andExpect(status().isOk())
            .andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store"))
            .andReturn().getResponse().getContentAsByteArray();
        var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(bytes));
        int[] pixels=image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
        var source=new com.google.zxing.RGBLuminanceSource(image.getWidth(),image.getHeight(),pixels);
        var bitmap=new com.google.zxing.BinaryBitmap(new com.google.zxing.common.HybridBinarizer(source));
        assertThat(new com.google.zxing.MultiFormatReader().decode(bitmap).getText()).isEqualTo(order.pickupCode());
        mvc.perform(get(url)).andExpect(status().isUnauthorized());
        mvc.perform(get(url).with(user(principal(other)))).andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/admin/orders/"+order.id()+"/pickup-qr.png").with(user(principal(customer)))).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/admin/orders/"+order.id()+"/pickup-qr.png").with(user(principal(admin)))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/admin/orders/"+order.id()+"/pickup").with(user(principal(admin))).contentType("application/json")
            .content("{\"pickupCode\":\""+order.pickupCode()+"\"}")).andExpect(status().isForbidden());
    }
    @Test void expiredPickupRejectsQrAndCollectionBeforeExpiryJobWithoutMutation() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();
        holdUntil(order.id(),Instant.now().minusSeconds(1));
        int history=jdbc.queryForObject("select count(*) from order_status_history where order_id=?",Integer.class,order.id());
        mvc.perform(get("/api/v1/orders/"+order.id()+"/pickup-qr.png").with(user(principal(customer))))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PICKUP_EXPIRED"));
        mvc.perform(post("/api/v1/admin/orders/"+order.id()+"/pickup").with(user(principal(admin))).with(csrf()).contentType("application/json")
            .content("{\"pickupCode\":\""+order.pickupCode()+"\"}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("PICKUP_EXPIRED"));
        assertThat(orders.detail(order.id(),customer,false).status()).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject("select count(*) from order_status_history where order_id=?",Integer.class,order.id())).isEqualTo(history);
        assertThat(branches.items(branch).stream().filter(i->i.status().equals("RESERVED"))).hasSize(1);
        pickup.expire();assertThat(branches.findStoresWithStock(variant,null).getFirst().available()).isEqualTo(2);
    }
    @Test void terminalAndHomeDeliveryOrdersDoNotExposePickupQr() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();
        pickup.collect(order.id(),admin,order.pickupCode());
        mvc.perform(get("/api/v1/orders/"+order.id()+"/pickup-qr.png").with(user(principal(customer)))).andExpect(status().isNotFound());
        add(customer,1);var cancelled=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();
        orders.cancel(cancelled.id(),customer,false,new OrderService.Cancel("Không còn nhu cầu",cancelled.version()));
        mvc.perform(get("/api/v1/orders/"+cancelled.id()+"/pickup-qr.png").with(user(principal(customer)))).andExpect(status().isNotFound());
        add(customer,1);var home=orders.checkout(customer,request(customer,options("HOME_DELIVERY_STANDARD"),"COD")).order();
        mvc.perform(get("/api/v1/orders/"+home.id()+"/pickup-qr.png").with(user(principal(customer)))).andExpect(status().isNotFound());
    }
    @Test void paidPickupRemainsCollectableAfterHoldExpiry() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"MOCK")).order();
        String code=jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,order.id());
        payments.completePayment(code,"00","MOCK","Test payment");
        holdUntil(order.id(),Instant.now().minusSeconds(1));
        mvc.perform(get("/api/v1/orders/"+order.id()+"/pickup-qr.png").with(user(principal(customer)))).andExpect(status().isOk());
        pickup.expire();assertThat(pickup.collect(order.id(),admin,order.pickupCode()).status()).isEqualTo("DELIVERED");
    }
    void holdUntil(Long id,Instant instant) {
        jdbc.update("update orders set hold_expires_at=? where id=?",LocalDateTime.ofInstant(instant,ZoneOffset.UTC),id);
    }
    @Test void expiryJobKeepsUnpaidPickupUntilUtcDeadline() {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();
        holdUntil(order.id(),Instant.now().plusSeconds(3600));pickup.expire();
        assertThat(orders.detail(order.id(),customer,false).status()).isEqualTo("PENDING");
        assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isEqualTo(1);
        holdUntil(order.id(),Instant.now().minusSeconds(1));pickup.expire();
        assertThat(orders.detail(order.id(),customer,false).status()).isEqualTo("CANCELLED");
    }
    @Test void orderPageResumesPersistedPaymentAndRemovesLinkOncePaid() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"MOCK")).order();
        String code=jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,order.id());
        assertThat(orders.detail(order.id(),customer,false).paymentUrl()).isEqualTo("/payment/mock-pay/"+code);
        String html=mvc.perform(get("/orders/"+order.id()).with(user(principal(customer)))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(html).contains("href=\"/payment/mock-pay/"+code+"\"");
        payments.completePayment(code,"FAILED","MOCK","Declined for test");
        assertThat(orders.detail(order.id(),customer,false).paymentUrl()).isEqualTo("/payment/mock-pay/"+code);
        payments.completePayment(code,"00","MOCK","Paid for test");
        assertThat(orders.detail(order.id(),customer,false).paymentUrl()).isNull();
        assertThat(jdbc.queryForObject("select count(*) from payment_transactions where order_id=?",Integer.class,order.id())).isEqualTo(1);
    }
    @Test void transferSerialAndRejectReservedMachine() {
        branches.transfer(new BranchInventoryService.Transfer(branch,farBranch,List.of("TEST-IMEI-001")));
        assertThat(branches.items(farBranch)).hasSize(1);assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        add(customer,1);var o=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();
        assertThatThrownBy(()->branches.transfer(new BranchInventoryService.Transfer(branch,farBranch,o.items().getFirst().imeiNumbers()))).isInstanceOf(BusinessException.class);
    }
    @Test void variantsRemainDistinctAndComparisonUsesUnionOfSpecifications() {
        var v=variants.create(product,new ProductVariantService.Input("SECOND-VARIANT","Bạc","512GB",new BigDecimal("16000000"),new BigDecimal("17000000")));
        branches.receive(admin,new BranchInventoryService.Import(v.id(),branch,List.of("SECOND-SERIAL-001")));
        add(customer,1);carts.add(customer,new CartService.Add(product,1,carts.view(customer).version(),v.id()));assertThat(carts.view(customer).items()).hasSize(2);
        var o=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();assertThat(o.items()).hasSize(2);assertThat(o.subtotal()).isEqualByComparingTo("31000000");
        var second=catalog.createProduct(new CatalogService.ProductInput(category,"SECOND-PRODUCT","Second",BigDecimal.TEN,"ACTIVE",null));
        variants.specification(product,new ProductVariantService.SpecInput("CPU","Chip","One",0));
        variants.specification(second.id(),new ProductVariantService.SpecInput("Pin","mAh","5000",0));
        assertThat(comparison.compare(List.of(product,second.id())).specifications()).hasSize(2);
        assertThatThrownBy(()->variants.create(product,new ProductVariantService.Input("SECOND-VARIANT","Bạc","256GB",BigDecimal.TEN,BigDecimal.TEN))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->comparison.compare(List.of(product,product))).isInstanceOf(BusinessException.class);
    }
    @Test void tradeInDiscountRequiresInspectionAndCannotBeReused() {
        var q=tradein.quote(customer,new TradeInService.Input(tradein.devices().getFirst().id(),tradein.criteria().getFirst().id(),"OLD-DEVICE-001"));
        assertThat(q.quotedAmount()).isEqualByComparingTo("3200000");add(customer,1);
        var o=new OrderService.Options("CLICK_AND_COLLECT",branch,null,null,null,q.id(),null);
        assertThatThrownBy(()->request(customer,o,"COD")).isInstanceOf(BusinessException.class);tradein.approve(q.id());
        var placed=orders.checkout(customer,request(customer,o,"COD")).order();assertThat(placed.total()).isEqualByComparingTo("11800000");
        assertThatThrownBy(()->tradein.credit(customer,q.id(),new BigDecimal("15000000"))).isInstanceOf(BusinessException.class);
        orders.cancel(placed.id(),customer,false,new OrderService.Cancel("Không còn nhu cầu",placed.version()));assertThat(tradein.quotes(customer).getFirst().status()).isEqualTo("APPROVED");
    }
    @Test void installmentIsExactAndBlocksDeliveryUntilApproval() {
        Long plan=installments.plans().getFirst().id();var schedule=installments.quote(plan,new BigDecimal("1000001"));
        assertThat(schedule.monthlyPayments().stream().reduce(BigDecimal.ZERO,BigDecimal::add)).isEqualByComparingTo(schedule.total());assertThat(schedule.monthlyPayments().getLast()).isEqualByComparingTo("333335");
        add(customer,1);var options=new OrderService.Options("CLICK_AND_COLLECT",branch,null,null,null,null,plan);var o=orders.checkout(customer,request(customer,options,"INSTALLMENT")).order();
        assertThatThrownBy(()->deliver(o.id())).isInstanceOf(BusinessException.class);installments.decision(o.id(),"APPROVED");assertThat(deliver(o.id()).status()).isEqualTo("DELIVERED");
    }
    @Test void hundredCustomersCompeteForLastSerialOnlyOneWins() throws Exception {
        branches.transfer(new BranchInventoryService.Transfer(branch,farBranch,List.of("TEST-IMEI-002")));
        var prepared=new ArrayList<Map.Entry<Long,OrderService.Checkout>>();
        for(int i=0;i<100;i++) {
            Long user=identity.register(new IdentityService.Registration("Race "+i,"race"+i+"@test.local","DemoCustomer2026!")).id();add(user,1);prepared.add(Map.entry(user,request(user,options("CLICK_AND_COLLECT"),"COD")));
        }
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(100);
        try {
            var futures=prepared.stream().map(p->pool.submit(()->{start.await();try {orders.checkout(p.getKey(),p.getValue());return true;}catch(BusinessException ex) {return false;}})).toList();
            start.countDown();int successes=0;for(var f:futures)if(f.get(90,TimeUnit.SECONDS))successes++;
            assertThat(successes).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from orders",Integer.class)).isEqualTo(1);
            assertThat(branches.items(branch).getFirst().status()).isEqualTo("RESERVED");assertThat(branches.findStoresWithStock(variant,null).getFirst().available()).isZero();
        } finally {pool.shutdownNow();}
    }
    @Test void failedPreviewOrPaymentRollsBackSerialAndCart() {
        add(customer,1);var req=request(customer,options("CLICK_AND_COLLECT"),"MOCK");
        // Force failure at payment initiation, after order, history, IMEI reservation and stock writes.
        org.springframework.test.util.ReflectionTestUtils.setField(payments,"mockEnabled",false);
        try {assertThatThrownBy(()->orders.checkout(customer,req)).isInstanceOf(BusinessException.class);}
        finally {org.springframework.test.util.ReflectionTestUtils.setField(payments,"mockEnabled",true);}
        assertThat(carts.view(customer).items()).hasSize(1);assertThat(branches.items(branch).stream().allMatch(i->"IN_STOCK".equals(i.status()))).isTrue();assertThat(repo.count()).isZero();
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isZero();
    }
    @Test void verifiedVnpayIpnChecksSignatureAmountReplayAndNeverRevivesCancelled() {
        add(customer,1);var o=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"VNPAY")).order();
        String code=jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,o.id());
        var p=new TreeMap<String,String>();p.put("vnp_TmnCode","TESTSHOP");p.put("vnp_TxnRef",code);p.put("vnp_Amount",o.total().multiply(BigDecimal.valueOf(100)).toPlainString());p.put("vnp_ResponseCode","00");p.put("vnp_TransactionStatus","00");
        assertThat(payments.vnPayIpn(p).get("RspCode")).isEqualTo("97");sign(p);assertThat(payments.vnPayIpn(p).get("RspCode")).isEqualTo("00");assertThat(payments.vnPayIpn(p).get("RspCode")).isEqualTo("02");assertThat(orders.detail(o.id(),customer,false).paymentStatus()).isEqualTo("PAID");
        assertThatThrownBy(()->orders.cancel(o.id(),customer,false,new OrderService.Cancel("Không còn nhu cầu",orders.detail(o.id(),customer,false).version()))).isInstanceOf(BusinessException.class);
        add(other,1);var cancelled=orders.checkout(other,request(other,options("CLICK_AND_COLLECT"),"VNPAY")).order();orders.cancel(cancelled.id(),other,false,new OrderService.Cancel("Không còn nhu cầu",cancelled.version()));
        p.put("vnp_TxnRef",jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,cancelled.id()));sign(p);assertThat(payments.vnPayIpn(p).get("RspCode")).isEqualTo("02");assertThat(orders.detail(cancelled.id(),other,false).status()).isEqualTo("CANCELLED");
    }
    void sign(Map<String,String> params) {
        params.remove("vnp_SecureHash");String s=params.entrySet().stream().map(e->e.getKey()+"="+URLEncoder.encode(e.getValue(),StandardCharsets.US_ASCII)).collect(java.util.stream.Collectors.joining("&"));params.put("vnp_SecureHash",PaymentService.hmacSHA512("test-signature-key-not-real",s));
    }
    @Test void uploadedImageIsServedOutsideJarAndRejectsFakeMime() throws Exception {
        var bytes=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        byte[] png=bytes.toByteArray();
        var image=new org.springframework.mock.web.MockMultipartFile("file","photo.txt","image/png",png);
        var result=mvc.perform(multipart("/api/v1/admin/products/"+product+"/images").file(image).with(user(principal(admin))).with(csrf())).andExpect(status().isCreated()).andReturn();
        String path=catalog.listImages(product).getFirst().getAssetPath();
        assertThat(path).endsWith(".png");
        try { mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(content().bytes(png)); }
        finally { new FileStorageService("./target/test-uploads").delete(path); }
        var fake=new org.springframework.mock.web.MockMultipartFile("file","fake.png","image/png","<script>fake</script>".getBytes(StandardCharsets.UTF_8));
        mvc.perform(multipart("/api/v1/admin/products/"+product+"/images").file(fake).with(user(principal(admin))).with(csrf())).andExpect(status().isBadRequest());
    }
    @Autowired CommerceSettingsService settings;
    @Test void adminCanConfigureOffersWithoutChangingExistingQuotes() throws Exception {
        String deviceBody="{\"model\":\"Admin phone\",\"basePrice\":2000000,\"subsidyAmount\":100000}";
        mvc.perform(post("/api/v1/admin/trade-in/devices").with(user(principal(customer))).with(csrf()).contentType("application/json").content(deviceBody)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/admin/trade-in/devices").with(user(principal(admin))).with(csrf()).contentType("application/json").content(deviceBody)).andExpect(status().isNoContent());
        var d=tradein.devices().stream().filter(x->x.model().equals("Admin phone")).findFirst().orElseThrow();
        var q=tradein.quote(customer,new TradeInService.Input(d.id(),tradein.criteria().getFirst().id(),"OLD-ADMIN-PHONE"));
        mvc.perform(post("/api/v1/admin/trade-in/devices/"+d.id()+"/archive").with(user(principal(admin))).with(csrf())).andExpect(status().isNoContent());
        assertThat(tradein.devices().stream().anyMatch(x->x.id().equals(d.id()))).isFalse();
        assertThat(tradein.quotes(customer).getFirst().quotedAmount()).isEqualByComparingTo(q.quotedAmount());
        mvc.perform(post("/api/v1/admin/installment/plans").with(user(principal(admin))).with(csrf()).contentType("application/json").content("{\"provider\":\"Admin plan\",\"months\":6,\"feePercent\":1.25}")).andExpect(status().isNoContent());
        var p=installments.plans().stream().filter(x->x.provider().equals("Admin plan")).findFirst().orElseThrow();
        assertThat(installments.quote(p.id(),BigDecimal.valueOf(1000000)).fee()).isEqualByComparingTo("12500");
        mvc.perform(post("/api/v1/admin/installment/plans/"+p.id()+"/archive").with(user(principal(admin))).with(csrf())).andExpect(status().isNoContent());
        assertThat(installments.plans().stream().anyMatch(x->x.id().equals(p.id()))).isFalse();
        assertThatThrownBy(()->settings.plan(new CommerceSettingsService.PlanInput("Invalid",0,BigDecimal.ZERO))).isInstanceOf(BusinessException.class);
        mvc.perform(get("/admin/commerce").with(user(principal(admin)))).andExpect(status().isOk());
    }
    @Test void apiRolesCsrfOwnershipAndPagesAreEnforced() throws Exception {
        mvc.perform(get("/api/v1/branches")).andExpect(status().isOk());mvc.perform(post("/api/v1/admin/imei/import").with(user(principal(customer))).with(csrf()).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/orders/preview").with(user(principal(customer))).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        for(String page:List.of("/branches","/compare","/warranty","/products/"+product))mvc.perform(get(page)).andExpect(status().isOk());
        for(String page:List.of("/member","/trade-in"))mvc.perform(get(page).with(user(principal(customer)))).andExpect(status().isOk());
        mvc.perform(get("/admin/commerce").with(user(principal(admin)))).andExpect(status().isOk());
        add(customer,1);mvc.perform(get("/checkout").with(user(principal(customer)))).andExpect(status().isOk());
        var o=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"MOCK")).order();String code=jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,o.id());
        mvc.perform(get("/payment/mock-pay/"+code).with(user(principal(other)))).andExpect(status().isNotFound());
        mvc.perform(get("/payment/mock-pay/"+code).with(user(principal(customer)))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/"+o.id()).with(user(principal(other)))).andExpect(status().isNotFound());
    }
    @Test void repairsEnforceSequentialStates() {
        add(customer,1);var o=deliver(orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order().id());
        var card=warranty.owned(customer).getFirst();warranty.receive(card.id(),"Màn hình không hoạt động");var t=warranty.tickets().getFirst();
        assertThatThrownBy(()->warranty.transition(t.id(),"COMPLETED")).isInstanceOf(BusinessException.class);
        for(String s:List.of("REPAIRING","COMPLETED","HANDED_OVER"))warranty.transition(t.id(),s);
        assertThat(warranty.tickets().getFirst().status()).isEqualTo("HANDED_OVER");assertThat(warranty.lookup(o.items().getFirst().imeiNumbers().getFirst(),"0999999999")).isEmpty();
    }
    @Test void warrantyExchangeConsumesReplacementWithoutDuplicatingSalesOrPoints() {
        add(customer,1);var o=deliver(orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order().id());
        var old=o.items().getFirst().imeiNumbers().getFirst();var replacement=branches.items(branch).stream().filter(i->i.status().equals("IN_STOCK")).findFirst().orElseThrow().imeiNumber();
        var card=warranty.owned(customer).getFirst();var result=exchanges.exchange(card.id(),replacement,admin);
        assertThat(result.previousSerial()).isEqualTo(old);assertThat(warranty.lookup(replacement,null)).hasSize(1);assertThat(warranty.lookup(old,null)).isEmpty();
        assertThat(products.findById(product).orElseThrow().getStockQuantity()).isZero();assertThat(loyalty.member(customer).pointsBalance()).isEqualTo(150);
        assertThatThrownBy(()->exchanges.exchange(card.id(),old,admin)).isInstanceOf(BusinessException.class);
        assertThat(commerceReports.channels(null,null).getFirst().goodsRevenue()).isEqualByComparingTo("15000000");
    }
    @Test void transitionSeparatesCodFromOnlinePaidAndInstallment() {
        add(customer, 1);
        var order = orders.checkout(customer, request(customer, options("HOME_DELIVERY_STANDARD"), "MOCK")).order();
        String code = jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?", String.class, order.id());
        payments.completePayment(code, "00", "MOCK", "Paid online for test");

        var fresh = orders.detail(order.id(), admin, true);
        assertThat(fresh.status()).isEqualTo("CONFIRMED");
        var shipped = orders.transition(order.id(), admin, new OrderService.Transition("SHIPPED", fresh.version(), false));

        assertThatThrownBy(() -> orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", shipped.version(), true)))
            .isInstanceOf(BusinessException.class);

        var delivered = orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", shipped.version(), false));
        assertThat(delivered.status()).isEqualTo("DELIVERED");
        assertThat(jdbc.queryForObject("select cod_collected from orders where id=?", Boolean.class, order.id())).isFalse();
        assertThat(delivered.paymentStatus()).isEqualTo("PAID");
        assertThat(delivered.history().getLast().reason()).contains("Đã thanh toán online");

        assertThatThrownBy(() -> orders.transition(order.id(), admin, new OrderService.Transition("DELIVERED", shipped.version(), false)))
            .isInstanceOf(BusinessException.class);
    }

    @Autowired vn.techshop.coupon.CouponService couponService;
    interface RacingAction {void run() throws Exception;}
    List<String> race(RacingAction... actions) throws Exception {
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(actions.length);
        try {
            var tasks=Arrays.stream(actions).map(action->pool.submit(()->{start.await();try{action.run();return "OK";}catch(BusinessException ex){return ex.code();}})).toList();
            start.countDown();var result=new ArrayList<String>();for(var task:tasks)result.add(task.get(30,TimeUnit.SECONDS));return result;
        } finally {pool.shutdownNow();}
    }
    @Test void racingCouponLastUseHasOneOrderAndRollsBackLosingCartAndSerial() throws Exception {
        String code="LAST"+UUID.randomUUID().toString().substring(0,8);
        couponService.create(new vn.techshop.coupon.CouponService.CouponInput(code,"Race fixture","FIXED",new BigDecimal("10000"),BigDecimal.ZERO,null,1,null,"ACTIVE"));
        add(customer,1);add(other,1);var options=options("CLICK_AND_COLLECT");
        var first=orders.preview(customer,new OrderService.PreviewRequest(code,options));var second=orders.preview(other,new OrderService.PreviewRequest(code,options));
        var a=new OrderService.Checkout(UUID.randomUUID().toString(),first.cartVersion(),first.total(),first.pricingHash(),"First buyer","0900000000","123 QA address Hanoi",null,code,"COD",options);
        var b=new OrderService.Checkout(UUID.randomUUID().toString(),second.cartVersion(),second.total(),second.pricingHash(),"Second buyer","0900000001","456 QA address Hanoi",null,code,"COD",options);
        assertThat(race(()->orders.checkout(customer,a),()->orders.checkout(other,b))).containsExactlyInAnyOrder("OK","COUPON_EXHAUSTED");
        assertThat(jdbc.queryForObject("select used_count from coupons where code=?",Integer.class,code.toUpperCase())).isEqualTo(1);
        assertThat(repo.count()).isEqualTo(1);assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isEqualTo(1);
        assertThat(carts.view(customer).items().size()+carts.view(other).items().size()).isEqualTo(1);assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(1);
    }
    @Test void racingPaymentCallbacksAndCancelKeepOnePaidOrOneReconciliation() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"MOCK")).order();
        String code=jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,order.id());
        assertThat(race(()->payments.completePayment(code,"00","QA","Race test"),()->payments.completePayment(code,"00","QA","Race replay"),()->orders.cancel(order.id(),customer,false,new OrderService.Cancel("QA concurrent cancellation",order.version())))).allMatch(r->Set.of("OK","PAYMENT_ORDER_CANCELLED","REFUND_REQUIRED","STALE_VERSION").contains(r));
        var actual=orders.detail(order.id(),customer,false);assertThat(jdbc.queryForObject("select status from payment_transactions where transaction_code=?",String.class,code)).isEqualTo("SUCCESS");
        if("CANCELLED".equals(actual.status())) {
            assertThat(jdbc.queryForObject("select count(*) from refund_reconciliations where order_id=?",Integer.class,order.id())).isEqualTo(1);
            assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isZero();assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);
        } else {
            assertThat(actual.paymentStatus()).isEqualTo("PAID");assertThat(jdbc.queryForObject("select count(*) from refund_reconciliations where order_id=?",Integer.class,order.id())).isZero();
            assertThat(jdbc.queryForObject("select count(*) from order_status_history where order_id=? and reason like 'Đã thanh toán online%'",Integer.class,order.id())).isEqualTo(1);assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isEqualTo(1);
        }
    }
    @Test void racingPaidPickupReplaysAndExpiryAwardOnlyOnce() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"MOCK")).order();String code=jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,order.id());payments.completePayment(code,"00","QA","Paid test");holdUntil(order.id(),Instant.now().minusSeconds(60));
        assertThat(race(()->pickup.collect(order.id(),admin,order.pickupCode()),()->pickup.collect(order.id(),admin,order.pickupCode()),()->pickup.expire())).containsOnly("OK");
        assertThat(orders.detail(order.id(),customer,false).status()).isEqualTo("DELIVERED");assertThat(branches.items(branch).stream().filter(item->"SOLD".equals(item.status()))).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from point_transactions where order_id=?",Integer.class,order.id())).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from warranty_cards where order_id=?",Integer.class,order.id())).isEqualTo(1);assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isZero();
    }
    @Test void racingExpiredUnpaidPickupAndTwoExpiryJobsReleaseOnce() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"COD")).order();holdUntil(order.id(),Instant.now().minusSeconds(60));
        assertThat(race(()->pickup.collect(order.id(),admin,order.pickupCode()),()->pickup.expire(),()->pickup.expire())).allMatch(r->Set.of("OK","PICKUP_EXPIRED","INVALID_ORDER_TRANSITION").contains(r));
        assertThat(orders.detail(order.id(),customer,false).status()).isEqualTo("CANCELLED");assertThat(products.findById(product).orElseThrow().getStockQuantity()).isEqualTo(2);assertThat(branches.findStoresWithStock(variant,null).getFirst().reserved()).isZero();
        assertThat(jdbc.queryForObject("select count(*) from stock_movements where order_item_id=? and movement_type='CANCEL'",Integer.class,jdbc.queryForObject("select id from order_items where order_id=?",Long.class,order.id()))).isEqualTo(1);assertThat(jdbc.queryForObject("select count(*) from point_transactions where order_id=?",Integer.class,order.id())).isZero();
    }

    @Test void racingSignedVnpayIpnReturnsSuccessAndReplayWithoutDuplicateHistory() throws Exception {
        add(customer,1);var order=orders.checkout(customer,request(customer,options("CLICK_AND_COLLECT"),"VNPAY")).order();String code=jdbc.queryForObject("select transaction_code from payment_transactions where order_id=?",String.class,order.id());
        var params=new TreeMap<String,String>();params.put("vnp_TmnCode","TESTSHOP");params.put("vnp_TxnRef",code);params.put("vnp_Amount",order.total().multiply(BigDecimal.valueOf(100)).toPlainString());params.put("vnp_ResponseCode","00");params.put("vnp_TransactionStatus","00");sign(params);
        var responses=new ConcurrentLinkedQueue<String>();var signed=Map.copyOf(params);
        assertThat(race(()->responses.add(payments.vnPayIpn(signed).get("RspCode")),()->responses.add(payments.vnPayIpn(signed).get("RspCode")))).containsOnly("OK");
        assertThat(responses).containsExactlyInAnyOrder("00","02");assertThat(orders.detail(order.id(),customer,false).paymentStatus()).isEqualTo("PAID");assertThat(jdbc.queryForObject("select count(*) from order_status_history where order_id=? and reason like 'Đã thanh toán online%'",Integer.class,order.id())).isEqualTo(1);
    }
}
