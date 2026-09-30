package vn.techshop.order;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OrderStateTest {
    @Test void cannotSkipStepsOrLeaveTerminalState() {
        assertThat(OrderState.PENDING.canMoveTo(OrderState.CONFIRMED)).isTrue();
        assertThat(OrderState.PENDING.canMoveTo(OrderState.DELIVERED)).isFalse();
        assertThat(OrderState.DELIVERED.canMoveTo(OrderState.CONFIRMED)).isFalse();
        assertThat(OrderState.CANCELLED.canMoveTo(OrderState.SHIPPED)).isFalse();
    }
}
