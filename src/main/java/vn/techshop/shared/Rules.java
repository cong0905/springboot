package vn.techshop.shared;

import java.math.BigDecimal;
import java.util.Locale;

public final class Rules {
    private Rules() {}
    public static String text(String value, int min, int max) {
        String text = value == null ? "" : value.strip();
        if (text.length() < min || text.length() > max) throw BusinessException.invalid("Độ dài dữ liệu không hợp lệ.");
        return text;
    }
    public static String optional(String value, int max) {
        if (value == null || value.isBlank()) return null;
        return text(value, 1, max);
    }
    public static String email(String value) { return text(value, 3, 254).toLowerCase(Locale.ROOT); }
    public static String code(String value) {
        String code = text(value, 3, 50).toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z0-9-]+")) throw BusinessException.invalid("Mã chỉ gồm chữ, số và dấu gạch ngang.");
        return code;
    }
    public static String status(String value) {
        if (!"ACTIVE".equals(value) && !"INACTIVE".equals(value)) throw BusinessException.invalid("Trạng thái không hợp lệ.");
        return value;
    }
    public static void version(Long actual, Long expected, String code) {
        if (expected == null || !expected.equals(actual)) throw BusinessException.conflict(code, "Dữ liệu đã thay đổi. Vui lòng tải lại.");
    }
    public static void quantity(Integer value) {
        if (value == null || value < 1 || value > 99) throw BusinessException.invalid("Số lượng phải từ 1 đến 99.");
    }
    public static BigDecimal price(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(new BigDecimal("999999999")) > 0 || value.stripTrailingZeros().scale() > 0)
            throw BusinessException.invalid("Giá phải là số nguyên VND hợp lệ.");
        return value.setScale(0);
    }
}
