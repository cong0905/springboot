package vn.techshop.branch;

import jakarta.validation.constraints.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.techshop.variant.*;
import vn.techshop.catalog.*;
import vn.techshop.inventory.InventoryService;
import vn.techshop.shared.*;

@Service
@Transactional(readOnly=true)
public class BranchInventoryService {
    public record BranchInput(@NotBlank @Size(max=100) String name,@NotBlank @Size(max=50) String code,
        @NotBlank @Size(max=500) String address,@NotBlank @Size(max=20) String provinceCode,
        @NotBlank @Pattern(regexp="0[0-9]{9}") String phone,@DecimalMin("-90") @DecimalMax("90") double latitude,
        @DecimalMin("-180") @DecimalMax("180") double longitude,@Size(max=20) String districtCode) {
        public BranchInput(String name,String code,String address,String provinceCode,String phone,double latitude,double longitude) {
            this(name,code,address,provinceCode,phone,latitude,longitude,null);
        }
    }
    public record Branch(Long id,String name,String code,String address,String provinceCode,String phone,double latitude,double longitude,boolean active,String districtCode,String openingHours,String photoPath,Long version) {
        public Branch(Long id,String name,String code,String address,String provinceCode,String phone,double latitude,double longitude,boolean active,String districtCode){this(id,name,code,address,provinceCode,phone,latitude,longitude,active,districtCode,null,null,0L);}
    }
    public record Details(@Size(max=300) String openingHours,@Size(max=500) String photoPath,@NotNull Long expectedVersion) {}
    @Transactional public Branch details(Long id,Details in){
        String photo=Rules.optional(in.photoPath(),500);
        if(photo!=null&&(!photo.startsWith("/assets/")||photo.contains("..")||photo.contains("\\")||photo.contains("?")||photo.contains("#")))throw BusinessException.invalid("Ảnh phải là tài nguyên nội bộ đã tải lên.");
        if(jdbc.queryForObject("select count(*) from store_branches where id=?",Integer.class,id)==0)throw BusinessException.missing();
        if(jdbc.update("update store_branches set opening_hours=?,photo_path=?,version=version+1 where id=? and version=?",Rules.optional(in.openingHours(),300),photo,id,in.expectedVersion())!=1)throw BusinessException.conflict("STALE_VERSION","Thông tin cửa hàng đã thay đổi. Tải lại trang.");
        return branches().stream().filter(b->b.id().equals(id)).findFirst().orElseThrow();
    }
    public record Stock(Long branchId,String name,String address,String provinceCode,double latitude,double longitude,int available,int reserved,String districtCode) {}
    public record Import(@NotNull Long variantId,@NotNull Long branchId,@NotEmpty @Size(max=500) List<String> imeiNumbers) {}
    public record Transfer(@NotNull Long fromBranch,@NotNull Long toBranch,@NotEmpty @Size(max=500) List<String> imeiNumbers) {}
    public record Item(Long id,Long variantId,Long branchId,String imeiNumber,String status,Long orderItemId) {}
    private final JdbcTemplate jdbc;private final ProductRepository products;private final ProductVariantService variants;private final InventoryService inventory;private final Clock clock;
    public BranchInventoryService(JdbcTemplate jdbc,ProductRepository products,ProductVariantService variants,InventoryService inventory,Clock clock) {this.jdbc=jdbc;this.products=products;this.variants=variants;this.inventory=inventory;this.clock=clock;}
    public List<Branch> branches() {return jdbc.query("select * from store_branches order by id",(r,n)->new Branch(r.getLong("id"),r.getString("name"),r.getString("code"),r.getString("address"),r.getString("province_code"),r.getString("phone"),r.getDouble("latitude"),r.getDouble("longitude"),r.getBoolean("is_active"),r.getString("district_code"),r.getString("opening_hours"),r.getString("photo_path"),r.getLong("version")));}
    public Map<Long,Map<Long,Integer>> availability(Collection<Long> variantIds){
        var result=new HashMap<Long,Map<Long,Integer>>();if(variantIds.isEmpty())return result;
        var params=new org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate(jdbc);
        params.query("select i.branch_id,i.variant_id,i.quantity-i.reserved_quantity available from branch_inventories i join store_branches b on b.id=i.branch_id where b.is_active=true and i.variant_id in (:ids)",Map.of("ids",variantIds),(org.springframework.jdbc.core.RowCallbackHandler)r->result.computeIfAbsent(r.getLong("branch_id"),k->new HashMap<>()).put(r.getLong("variant_id"),r.getInt("available")));return result;
    }
    public List<Branch> publicBranches(String provinceCode,String districtCode) {
        String province=Rules.optional(provinceCode,20),district=Rules.optional(districtCode,20);
        return branches().stream().filter(Branch::active)
            .filter(b->province==null||province.equals(b.provinceCode()))
            .filter(b->district==null||district.equals(b.districtCode())).toList();
    }
    public Branch branch(Long id) {return branches().stream().filter(b->b.id().equals(id)&&b.active()).findFirst().orElseThrow(BusinessException::missing);}
    @Transactional public Branch create(BranchInput in) {
        if(!Double.isFinite(in.latitude())||!Double.isFinite(in.longitude())||Math.abs(in.latitude())>90||Math.abs(in.longitude())>180)throw BusinessException.invalid("Tọa độ không hợp lệ.");
        jdbc.update("insert into store_branches(name,code,address,province_code,phone,latitude,longitude,district_code,is_active) values(?,?,?,?,?,?,?,?,true)",Rules.text(in.name(),1,100),Rules.text(in.code(),1,50),Rules.text(in.address(),10,500),Rules.text(in.provinceCode(),1,20),in.phone(),in.latitude(),in.longitude(),Rules.optional(in.districtCode(),20));
        return branches().stream().filter(b->b.code().equals(in.code())).findFirst().orElseThrow();
    }
    public List<Stock> findStoresWithStock(Long variantId,String provinceCode) {
        return findStoresWithStock(variantId,provinceCode,null,false);
    }
    public List<Stock> findStoresWithStock(Long variantId,String provinceCode,String districtCode,boolean inStock) {
        String province=Rules.optional(provinceCode,20),district=Rules.optional(districtCode,20);
        return jdbc.query("select b.*,i.quantity-i.reserved_quantity available,i.reserved_quantity from branch_inventories i join store_branches b on b.id=i.branch_id where i.variant_id=? and b.is_active=true and (? is null or b.province_code=?) and (? is null or b.district_code=?) order by b.id",(r,n)->new Stock(r.getLong("id"),r.getString("name"),r.getString("address"),r.getString("province_code"),r.getDouble("latitude"),r.getDouble("longitude"),r.getInt("available"),r.getInt("reserved_quantity"),r.getString("district_code")),variantId,province,province,district,district)
            .stream().filter(s->!inStock||s.available()>0).toList();
    }
    public List<Item> items(Long branchId) {return jdbc.query("select * from product_items_imei where (? is null or branch_id=?) order by id",(r,n)->new Item(r.getLong("id"),r.getLong("variant_id"),r.getLong("branch_id"),r.getString("imei_number"),r.getString("status"),r.getObject("order_item_id",Long.class)),branchId,branchId);}
    private void lockInventory(Long branchId,Long variantId) {
        if(jdbc.queryForObject("select count(*) from branch_inventories where branch_id=? and variant_id=?",Integer.class,branchId,variantId)==0)
            jdbc.update("insert into branch_inventories(branch_id,variant_id,quantity,reserved_quantity,version) values(?,?,0,0,0)",branchId,variantId);
        jdbc.queryForList("select id from branch_inventories where branch_id=? and variant_id=? for update",branchId,variantId);
    }
    @Transactional public void receive(Long actorId,Import in) {
        branch(in.branchId());var v=jdbc.queryForObject("select product_id from product_variants where id=?",Long.class,in.variantId());
        var product=products.lockById(v).orElseThrow(BusinessException::missing);variants.require(in.variantId(),v);
        var numbers=clean(in.imeiNumbers());lockInventory(in.branchId(),in.variantId());
        for(String number:numbers) {
            if(jdbc.queryForObject("select count(*) from product_items_imei where imei_number=?",Integer.class,number)>0)throw BusinessException.conflict("IMEI_EXISTS","IMEI/serial đã nhập kho.");
            jdbc.update("insert into product_items_imei(variant_id,branch_id,imei_number,status,imported_at) values(?,?,?,'IN_STOCK',?)",in.variantId(),in.branchId(),number,Timestamp.from(clock.instant()));
            Long id=jdbc.queryForObject("select id from product_items_imei where imei_number=?",Long.class,number);history(id,null,"IN_STOCK",null,in.branchId(),"Nhập kho");
        }
        jdbc.update("update branch_inventories set quantity=quantity+?,version=version+1 where branch_id=? and variant_id=?",numbers.size(),in.branchId(),in.variantId());
        inventory.apply(product,actorId,null,"ADJUSTMENT",numbers.size(),"Nhập IMEI tại chi nhánh "+in.branchId());
    }
    static List<String> clean(List<String> input) {
        if(input==null||input.isEmpty()||input.size()>500)throw BusinessException.invalid("Nhập 1–500 IMEI/serial.");
        var clean=input.stream().map(s->s==null?"":s.trim().toUpperCase(Locale.ROOT)).sorted().toList();
        if(new HashSet<>(clean).size()!=clean.size()||clean.stream().anyMatch(s->!s.matches("[A-Z0-9-]{5,40}")))throw BusinessException.invalid("IMEI/serial phải duy nhất, 5–40 ký tự chữ, số hoặc dấu gạch.");return clean;
    }
    @Transactional public void transfer(Transfer in) {
        branch(in.fromBranch());branch(in.toBranch());if(in.fromBranch().equals(in.toBranch()))throw BusinessException.invalid("Hai kho phải khác nhau.");
        var numbers=clean(in.imeiNumbers());var selected=items(in.fromBranch()).stream().filter(i->numbers.contains(i.imeiNumber())).toList();
        if(selected.size()!=numbers.size())throw BusinessException.invalid("Máy không thuộc kho nguồn.");
        var productIds=selected.stream().map(i->jdbc.queryForObject("select product_id from product_variants where id=?",Long.class,i.variantId())).distinct().sorted().toList();
        productIds.forEach(id->products.lockById(id).orElseThrow(BusinessException::missing));
        for(var i:selected.stream().sorted(Comparator.comparing(Item::id)).toList()) {
            String status=jdbc.queryForObject("select status from product_items_imei where id=? for update",String.class,i.id());
            if(!"IN_STOCK".equals(status))throw BusinessException.conflict("IMEI_UNAVAILABLE","Chỉ điều chuyển máy còn trong kho.");
            for(Long b:List.of(in.fromBranch(),in.toBranch()).stream().sorted().toList())lockInventory(b,i.variantId());
            jdbc.update("update product_items_imei set branch_id=? where id=?",in.toBranch(),i.id());
            jdbc.update("update branch_inventories set quantity=quantity-1,version=version+1 where branch_id=? and variant_id=?",in.fromBranch(),i.variantId());
            jdbc.update("update branch_inventories set quantity=quantity+1,version=version+1 where branch_id=? and variant_id=?",in.toBranch(),i.variantId());
            history(i.id(),status,status,in.fromBranch(),in.toBranch(),"Điều chuyển nội bộ");
        }
    }
    @Transactional public List<String> reserve(Long variantId,Long branchId,Long orderItemId,int quantity) {
        // Caller holds product locks in ascending order. SQL FOR UPDATE locks both the balance and physical items.
        lockInventory(branchId,variantId);
        var ids=jdbc.queryForList("select id from product_items_imei where variant_id=? and branch_id=? and status='IN_STOCK' order by id limit ? for update",Long.class,variantId,branchId,quantity);
        if(ids.size()!=quantity)throw BusinessException.conflict("INSUFFICIENT_BRANCH_STOCK","Chi nhánh không còn đủ máy.");
        for(Long id:ids) {
            jdbc.update("update product_items_imei set status='RESERVED',order_item_id=? where id=?",orderItemId,id);history(id,"IN_STOCK","RESERVED",branchId,branchId,"Giữ máy cho đơn");
        }
        jdbc.update("update branch_inventories set reserved_quantity=reserved_quantity+?,version=version+1 where branch_id=? and variant_id=?",quantity,branchId,variantId);
        return jdbc.queryForList("select imei_number from product_items_imei where order_item_id=? order by id",String.class,orderItemId);
    }
    @Transactional public void release(Long orderItemId) {settle(orderItemId,false);}
    @Transactional public void sell(Long orderItemId) {settle(orderItemId,true);}
    @Transactional public void returnSoldItem(Long orderItemId) {
        var rows=jdbc.queryForList("select id,variant_id,branch_id from product_items_imei where order_item_id=? and status='SOLD' order by id for update",orderItemId);
        for(var r:rows) {
            Long id=((Number)r.get("id")).longValue(),b=((Number)r.get("branch_id")).longValue(),v=((Number)r.get("variant_id")).longValue();
            lockInventory(b,v);
            jdbc.update("update product_items_imei set status='IN_STOCK',order_item_id=null,sold_at=null where id=?",id);
            jdbc.update("update branch_inventories set quantity=quantity+1,version=version+1 where branch_id=? and variant_id=?",b,v);
            history(id,"SOLD","IN_STOCK",b,b,"Nhận trả hàng / hoàn tiền");
        }
    }
    private void settle(Long itemId,boolean sold) {
        var rows=jdbc.queryForList("select id,variant_id,branch_id from product_items_imei where order_item_id=? and status='RESERVED' order by id for update",itemId);
        for(var r:rows) {
            Long id=((Number)r.get("id")).longValue(),b=((Number)r.get("branch_id")).longValue(),v=((Number)r.get("variant_id")).longValue();lockInventory(b,v);
            jdbc.update("update product_items_imei set status=?,order_item_id=?,sold_at=? where id=?",sold?"SOLD":"IN_STOCK",sold?itemId:null,sold?Timestamp.from(clock.instant()):null,id);
            jdbc.update("update branch_inventories set reserved_quantity=reserved_quantity-1,quantity=quantity-?,version=version+1 where branch_id=? and variant_id=?",sold?1:0,b,v);
            history(id,"RESERVED",sold?"SOLD":"IN_STOCK",b,b,sold?"Giao máy":"Hủy giữ máy");
        }
    }
    private void history(Long id,String from,String to,Long fromBranch,Long toBranch,String reason) {
        jdbc.update("insert into imei_history(imei_id,from_status,to_status,from_branch_id,to_branch_id,reason,occurred_at) values(?,?,?,?,?,?,?)",id,from,to,fromBranch,toBranch,reason,Timestamp.from(clock.instant()));
    }
}
