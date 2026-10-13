package vn.techshop.cart;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class PricingTest {
    @Test void shippingThresholdIsInclusive() {
        assertThat(Pricing.fee(new BigDecimal("999999"))).isEqualByComparingTo("30000");
        assertThat(Pricing.fee(new BigDecimal("1000000"))).isEqualByComparingTo("0");
    }
    @Test void equalTotalDoesNotHideDifferentLinePrices() {
        var first=List.of(new Pricing.Line(1L,1,new BigDecimal("100")),new Pricing.Line(2L,1,new BigDecimal("200")));
        var changed=List.of(new Pricing.Line(1L,1,new BigDecimal("150")),new Pricing.Line(2L,1,new BigDecimal("150")));
        assertThat(Pricing.hash(first,new BigDecimal("30000"))).isNotEqualTo(Pricing.hash(changed,new BigDecimal("30000")));
        assertThat(Pricing.hash(first,BigDecimal.ZERO)).isEqualTo(Pricing.hash(List.of(first.get(1),first.get(0)),BigDecimal.ZERO));
    }
}
