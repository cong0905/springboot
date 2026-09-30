package vn.techshop.shared;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.http.converter.HttpMessageNotReadableException;

@ControllerAdvice
public class Errors {
    private static final Logger LOG = LoggerFactory.getLogger(Errors.class);
    public record FieldError(String field, String message) {}
    public record ApiError(String code, String message, String requestId, List<FieldError> fieldErrors) {}
    @ExceptionHandler(BusinessException.class)
    Object business(BusinessException ex, HttpServletRequest request) { return result(request, ex.status(), ex.code(), ex.getMessage(), List.of()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    Object validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream().map(e -> new FieldError(e.getField(), e.getDefaultMessage())).toList();
        return result(request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Vui lòng kiểm tra dữ liệu nhập.", fields);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    Object malformed(Exception ex, HttpServletRequest request) { return result(request, HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Dữ liệu không hợp lệ.", List.of()); }
    @ExceptionHandler({PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    Object locking(Exception ex, HttpServletRequest request) { return result(request, HttpStatus.CONFLICT, "RETRYABLE_CONFLICT", "Dữ liệu đang được xử lý. Vui lòng tải lại hoặc thử lại cùng yêu cầu.", List.of()); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    Object constraint(Exception ex, HttpServletRequest request) { return result(request, HttpStatus.CONFLICT, "DATA_CONFLICT", "Dữ liệu bị trùng hoặc không còn hợp lệ.", List.of()); }
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    Object notFound(Exception ex, HttpServletRequest request) { return result(request,HttpStatus.NOT_FOUND,"NOT_FOUND","Không tìm thấy trang.",List.of()); }
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    Object method(Exception ex, HttpServletRequest request) { return result(request,HttpStatus.METHOD_NOT_ALLOWED,"METHOD_NOT_ALLOWED","Phương thức không được hỗ trợ.",List.of()); }
    @ExceptionHandler(Exception.class)
    Object unexpected(Exception ex, HttpServletRequest request) {
        LOG.error("Request {} failed with {}", request.getAttribute("requestId"), ex.getClass().getSimpleName());
        return result(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Có lỗi xử lý. Vui lòng thử lại.", List.of());
    }
    private Object result(HttpServletRequest request, HttpStatus status, String code, String message, List<FieldError> fields) {
        var error = new ApiError(code, message, Objects.toString(request.getAttribute("requestId"), ""), fields);
        if (request.getRequestURI().startsWith("/api/")) return ResponseEntity.status(status).body(error);
        var view = new ModelAndView("error-page", Map.of("problem", error));
        view.setStatus(status); return view;
    }
}
