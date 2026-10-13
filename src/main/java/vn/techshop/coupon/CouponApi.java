package vn.techshop.coupon;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.techshop.cart.CartService;
import vn.techshop.identity.TechPrincipal;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponApi {
    public record ValidateRequest(@NotBlank String code) {}
    public record ValidateResponse(
        String code,
        String discountType,
        BigDecimal discountValue,
        BigDecimal discountAmount,
        String description,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal totalAfterDiscount
    ) {}

    private final CouponService coupons;
    private final CartService carts;

    public CouponApi(CouponService coupons, CartService carts) {
        this.coupons = coupons;
        this.carts = carts;
    }

    @GetMapping("/mine") public java.util.List<CouponService.PersonalCoupon> mine(@AuthenticationPrincipal TechPrincipal user){return coupons.assigned(user.id());}
    @PostMapping("/validate")
    public ValidateResponse validate(
        @AuthenticationPrincipal TechPrincipal user,
        @Valid @RequestBody ValidateRequest request
    ) {
        var cartView = carts.view(user.id());
        var result = coupons.validateAndCalculate(request.code(), cartView.subtotal(),user.id());
        BigDecimal totalAfter = cartView.subtotal().subtract(result.discountAmount()).max(BigDecimal.ZERO).add(cartView.shippingFee());

        return new ValidateResponse(
            result.code(),
            result.discountType(),
            result.discountValue(),
            result.discountAmount(),
            result.description(),
            cartView.subtotal(),
            cartView.shippingFee(),
            totalAfter
        );
    }
}
