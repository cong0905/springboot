package vn.techshop.shared;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    public BusinessException(HttpStatus status, String code, String message) {
        super(message); this.status = status; this.code = code;
    }
    public HttpStatus status() { return status; }
    public String code() { return code; }
    public static BusinessException missing() { return new BusinessException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Không tìm thấy dữ liệu."); }
    public static BusinessException invalid(String message) { return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message); }
    public static BusinessException conflict(String code, String message) { return new BusinessException(HttpStatus.CONFLICT, code, message); }
}
