package vn.techshop.config;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.techshop.identity.TechPrincipal;
import vn.techshop.branch.BranchInventoryService;
import vn.techshop.variant.ProductComparisonService;
import vn.techshop.loyalty.LoyaltyService;
import vn.techshop.tradein.TradeInService;
import vn.techshop.installment.InstallmentService;
import vn.techshop.warranty.WarrantyService;
@Controller
public class OmnichannelWeb {
    @org.springframework.beans.factory.annotation.Autowired private CommerceSettingsService settings;
    private final BranchInventoryService branches;private final ProductComparisonService comparison;private final LoyaltyService loyalty;private final TradeInService tradein;private final InstallmentService installments;private final WarrantyService warranty;
    public OmnichannelWeb(BranchInventoryService branches,ProductComparisonService comparison,LoyaltyService loyalty,TradeInService tradein,InstallmentService installments,WarrantyService warranty) {this.branches=branches;this.comparison=comparison;this.loyalty=loyalty;this.tradein=tradein;this.installments=installments;this.warranty=warranty;}
    @ModelAttribute void common(@AuthenticationPrincipal TechPrincipal user,Model m) {m.addAttribute("viewer",user);m.addAttribute("isAdmin",user!=null&&"ADMIN".equals(user.role()));m.addAttribute("isCustomer",user!=null&&"CUSTOMER".equals(user.role()));}
    @org.springframework.beans.factory.annotation.Autowired private vn.techshop.coupon.CouponService coupons;
    @GetMapping("/vouchers") String vouchers(@AuthenticationPrincipal TechPrincipal user,Model m){m.addAttribute("vouchers",coupons.assigned(user.id()));return "vouchers";}
    @GetMapping("/branches") String branches(Model m) {m.addAttribute("branches",branches.publicBranches(null,null));return "branches";}
    @GetMapping("/compare") String compare(@RequestParam(required=false) List<Long> ids,Model m) {m.addAttribute("comparison",ids==null?null:comparison.compare(ids));return "compare";}
    @GetMapping("/member") String member(@AuthenticationPrincipal TechPrincipal user,Model m) {m.addAttribute("member",loyalty.member(user.id()));m.addAttribute("cards",warranty.owned(user.id()));return "member";}
    @GetMapping("/trade-in") String tradein(@AuthenticationPrincipal TechPrincipal user,Model m) {m.addAttribute("devices",tradein.devices());m.addAttribute("criteria",tradein.criteria());m.addAttribute("quotes",tradein.quotes(user.id()));return "trade-in";}
    @GetMapping("/warranty") String warranty(@RequestParam(required=false) String query,@RequestParam(required=false) String phone,Model m) {m.addAttribute("query",query);m.addAttribute("cards",query==null||query.isBlank()?List.of():warranty.lookup(query,phone==null||phone.isBlank()?null:phone));return "warranty";}
    @GetMapping("/admin/commerce") String admin(Model m) {m.addAttribute("tradeDevices",settings.devices());m.addAttribute("tradeCriteria",tradein.criteria());m.addAttribute("installmentPlans",settings.plans());m.addAttribute("branches",branches.branches());m.addAttribute("items",branches.items(null));m.addAttribute("quotes",tradein.quotes(null));m.addAttribute("applications",installments.applications());m.addAttribute("tickets",warranty.tickets());return "admin/commerce";}
}
