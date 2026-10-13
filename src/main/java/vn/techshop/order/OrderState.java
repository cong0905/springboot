package vn.techshop.order;

import vn.techshop.shared.BusinessException;

public enum OrderState {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED;
    public boolean canMoveTo(OrderState target) {
        return switch(this) {case PENDING -> target==CONFIRMED;case CONFIRMED -> target==SHIPPED;case SHIPPED -> target==DELIVERED;default -> false;};
    }
    public static OrderState parse(String value) {
        try { return valueOf(value); } catch(Exception ex) { throw BusinessException.invalid("Trạng thái đơn không hợp lệ."); }
    }
}
