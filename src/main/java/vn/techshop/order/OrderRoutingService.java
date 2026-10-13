package vn.techshop.order;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import vn.techshop.branch.BranchInventoryService;
import vn.techshop.shared.BusinessException;
@Service
public class OrderRoutingService {
    public record Need(Long variantId,int quantity) {}
    public record Route(Long branchId,double distanceKm,BigDecimal shippingFee) {}
    private final BranchInventoryService stock;
    public OrderRoutingService(BranchInventoryService stock) {this.stock=stock;}
    public Route route(List<Need> needs,String type,Long pickupBranchId,Double latitude,Double longitude,BigDecimal standardFee) {
        if(!Set.of("HOME_DELIVERY_STANDARD","HOME_DELIVERY_INSTANT_2H","CLICK_AND_COLLECT").contains(type))throw BusinessException.invalid("Cách nhận hàng không hợp lệ.");
        if(needs.isEmpty()) {if(!"HOME_DELIVERY_STANDARD".equals(type))throw BusinessException.invalid("Đơn đa kênh yêu cầu chọn biến thể.");return new Route(null,0,standardFee);}
        if("HOME_DELIVERY_INSTANT_2H".equals(type)&&(latitude==null||longitude==null||!Double.isFinite(latitude)||!Double.isFinite(longitude)||Math.abs(latitude)>90||Math.abs(longitude)>180))throw BusinessException.invalid("Cần tọa độ hợp lệ để giao 2h.");
        if("CLICK_AND_COLLECT".equals(type)&&pickupBranchId==null)throw BusinessException.invalid("Chọn cửa hàng nhận máy.");
        var available=stock.availability(needs.stream().map(Need::variantId).distinct().toList());
        var candidates=stock.branches().stream().filter(BranchInventoryService.Branch::active).filter(b->!"CLICK_AND_COLLECT".equals(type)||b.id().equals(pickupBranchId)).filter(b->needs.stream().allMatch(n->available.getOrDefault(b.id(),Map.of()).getOrDefault(n.variantId(),0)>=n.quantity())).toList();
        if(candidates.isEmpty())throw BusinessException.conflict("INSUFFICIENT_BRANCH_STOCK","Không có cửa hàng đủ toàn bộ máy trong giỏ.");
        boolean instant="HOME_DELIVERY_INSTANT_2H".equals(type);
        var branch=instant?candidates.stream().min(Comparator.comparingDouble(b->distance(latitude,longitude,b.latitude(),b.longitude()))).orElseThrow():candidates.getFirst();
        double km=instant?distance(latitude,longitude,branch.latitude(),branch.longitude()):0;
        if(instant&&km>20)throw BusinessException.invalid("Giao 2h hỗ trợ trong bán kính 20km.");
        return new Route(branch.id(),km,"CLICK_AND_COLLECT".equals(type)?BigDecimal.ZERO:instant?BigDecimal.valueOf(20000+5000L*(long)Math.ceil(km)):standardFee);
    }
    public static double distance(double a,double b,double c,double d) {
        double lat=Math.toRadians(c-a),lng=Math.toRadians(d-b);
        double h=Math.sin(lat/2)*Math.sin(lat/2)+Math.cos(Math.toRadians(a))*Math.cos(Math.toRadians(c))*Math.sin(lng/2)*Math.sin(lng/2);
        return 6371*2*Math.atan2(Math.sqrt(h),Math.sqrt(Math.max(0,1-h)));
    }
}
