# Quyết định thiết kế, điểm còn mở và nguồn

## 1. ADR baseline

Các ADR là **Proposed for implementation**; chưa có code/benchmark chứng minh. “Chọn” mô tả thiết kế đang được lập, không hàm ý stakeholder thật đã ký duyệt.

| ADR | Quyết định | Lý do | Trade-off / khi xem lại |
|---|---|---|---|
| ADR-01 | Modular monolith, một app/DB | Phạm vi nhỏ, một developer, atomic order/kho | Xem lại nếu có nhiều team/deploy độc lập |
| ADR-02 | Thymeleaf cùng origin, session auth | Một stack để hoàn thành và demo; CSRF/form rõ | Mobile/SPA cần contract/auth và session topology mới |
| ADR-03 | Java21, Boot4.1.1 baseline, BOM | Dùng Java21 phù hợp Boot hiện tại; quản lý dependency thống nhất | Pin patch khi bootstrap, kiểm tra release notes; không coi tutorial cũ là API hiện tại |
| ADR-04 | MySQL8.4 + Flyway, ddl-auto=validate | Schema versioned; constraint/transaction/locking thật | Migration phải chạy test MySQL; DDL chưa thực thi |
| ADR-05 | Stock trừ tại checkout, hoàn khi cancel trước giao | Dễ định nghĩa tồn có thể bán, không reserve từ cart | PENDING lâu giữ hàng; expiry/payment cần phase mới |
| ADR-06 | Lock cart/order và product theo thứ tự; @Version | Chặn race/lost update; hành vi dễ kiểm thử | Throughput thấp khi tranh sản phẩm; đo trước khi đổi |
| ADR-07 | Key idempotency gắn order thành công | Chống double submit/mất response; DB unique | Key giữ theo vòng đời order; payment cần ledger riêng |
| ADR-08 | Money VND DECIMAL(15,0)/BigDecimal | Tiền nguyên, phép tính chính xác | Multi-currency/thuế/discount cần policy mới |
| ADR-09 | Revenue theo DELIVERED + delivered_at | COD thủ công chỉ ghi khi xác nhận giao/thu tiền | Chưa là accounting ledger/đối soát cash độc lập |
| ADR-10 | Asset ảnh allowlist, không upload v0.1 | Giảm scope và rủi ro tệp, demo độc lập | Upload/object storage là phase tiếp theo |
| ADR-11 | Chính sách inactive thay delete | Giữ FK và lịch sử đơn/ledger | Xóa dữ liệu cá nhân phải có chính sách riêng |

## 2. Điểm cần xác nhận khi bắt đầu

1. TechShop bán đồ công nghệ có phù hợp chủ đề Công muốn làm hay cần đổi ngành hàng?
2. Một người hay nhóm? Có thể dành108 giờ và đã học Spring/Security/JPA chưa?
3. Chấp nhận login trước đặt hàng và COD duy nhất ở bản đầu?
4. Phí30000/ngưỡng1000000, giới hạn giỏ20 dòng/99 mỗi dòng có cần đổi?
5. Demo dùng Thymeleaf hay có yêu cầu học React bắt buộc? Nếu React, re-estimate, không giữ lịch giả.
6. Hosting/container/MySQL nào sẽ dùng, ngân sách và quyền deploy ra sao?
7. Có dùng cho bán hàng thật? Nếu có, phải thêm quy trình giao thất bại/đổi trả, dữ liệu, backup và yêu cầu vận hành trước launch.

Không dừng việc lập kế hoạch để chờ các điểm này; chúng đã được ghi thành giả định. Chốt trước task tương ứng, ghi CR và cập nhật tài liệu khi có thông tin mới.

## 3. Nguồn kỹ thuật chính thức

Kiểm tra tại thời điểm lập baseline 01/10/2026. Nguồn giúp xác minh khả năng framework/type, không xác minh business rules hoặc estimate của project.

| Chủ đề | Nguồn | Áp dụng trong thiết kế |
|---|---|---|
| Boot version/Java | [System requirements](https://docs.spring.io/spring-boot/system-requirements.html), [release4.1.1](https://spring.io/blog/2026/08/20/spring-boot-4-1-1-available-now) | Java21/Boot baseline; kiểm tra patch khi bootstrap |
| CSRF | [Spring Security CSRF](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html) | Giữ protection; form/token cho unsafe methods |
| Password hash | [Password storage](https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html) | Dùng PasswordEncoder; không lưu rõ |
| JPA lock | [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html) | @Lock/LockModeType cho query; order lock policy là thiết kế project |
| Migration | [Boot database initialization](https://docs.spring.io/spring-boot/how-to/data-initialization.html) | Một cơ chế tạo schema: Flyway; MySQL module; JPA validate |
| MySQL money | [Fixed-point types](https://dev.mysql.com/doc/refman/8.4/en/fixed-point-types.html) | DECIMAL cho tiền |
| MySQL constraints | [CHECK constraints](https://dev.mysql.com/doc/refman/8.4/en/create-table-check-constraints.html) | Nonnegative balance, totals/status checks |

Khả năng @Lock không tự bảo đảm mọi race được giải quyết; transaction scope, thứ tự lock và integration tests thuộc trách nhiệm implementation. Tương tự, bật Spring Security không tự kiểm tra ownership của order.
