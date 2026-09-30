package vn.techshop.shared;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public record PageView<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageView<T> of(Page<T> page) { return new PageView<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()); }
    public static PageRequest request(int page, int size, Sort sort) {
        if (page < 0 || size < 1 || size > 50) throw BusinessException.invalid("Phân trang không hợp lệ.");
        return PageRequest.of(page, size, sort);
    }
}
