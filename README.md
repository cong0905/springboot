# TechShop · Thiết kế website bán đồ công nghệ

Bộ tài liệu phân tích và thiết kế cho website bán hàng của **một cửa hàng**. Nội dung tập trung vào luồng mua hàng COD, kiểm soát tồn kho, xử lý đơn và quản trị.

> **Trạng thái nhánh `main`:** có đặc tả, thiết kế SQL và kế hoạch phát triển; chưa có mã ứng dụng Spring Boot, `pom.xml` hoặc hướng dẫn chạy ứng dụng trên nhánh này. Các tính năng bên dưới là phạm vi dự kiến.

## Phạm vi dự kiến

- Khách xem, tìm kiếm và lọc sản phẩm.
- Người dùng đăng ký/đăng nhập, quản lý giỏ hàng, đặt COD, xem và hủy đơn theo điều kiện.
- Admin quản lý danh mục, sản phẩm, tồn kho và đơn hàng.
- Thiết kế chống đặt trùng, bán vượt tồn và lưu thông tin đơn tại thời điểm mua.

Giả định: một cửa hàng, một kho, tiền VND và giao hàng xử lý thủ công. Marketplace nhiều người bán, thanh toán online, đổi trả và mobile app thuộc phạm vi sau.

## Lộ trình đọc tài liệu

| Thứ tự | Tài liệu | Nội dung |
| --- | --- | --- |
| 1 | [Project charter](docs/00-project-charter.md) | Bài toán, mục tiêu, phạm vi và giả định |
| 2 | [Đặc tả SRS](docs/01-spec.md) | Yêu cầu, use case, quy tắc và tiêu chí nghiệm thu |
| 3 | [Kiến trúc](docs/02-architecture.md) | Module, lớp, bảo mật và transaction |
| 4 | [Thiết kế dữ liệu](docs/03-data-design.md) | ERD, bảng, khóa, index và vòng đời |
| 5 | [Hợp đồng API](docs/04-api-contract.md) | Route, payload, quyền và lỗi |
| 6 | [Sơ đồ](docs/05-diagrams.md) | Hoạt động, tuần tự và trạng thái |
| 7 | [Kế hoạch phát triển](docs/06-development-plan.md) | Milestone, dependency và điều kiện hoàn thành |
| 8 | [Backlog](docs/07-backlog.md) | Công việc và ưu tiên |
| 9 | [Kiểm thử](docs/08-test-plan.md) | Test case và truy vết yêu cầu |
| 10 | [Phân tích dữ liệu](docs/09-analytics-plan.md) | KPI, nguồn dữ liệu và quy tắc tính |
| 11 | [Vận hành](docs/10-operations.md) | Cấu hình, backup và rollback dự kiến |
| 12 | [Quyết định thiết kế](docs/11-decisions-and-sources.md) | ADR và lựa chọn kỹ thuật |

## Hướng công nghệ

Thiết kế hướng tới Java, Spring Boot, Spring MVC, Thymeleaf, Spring Security, JPA, MySQL, Flyway và Maven. Phiên bản cùng thư viện cuối cùng cần được xác nhận khi bootstrap ứng dụng; danh sách này không khẳng định các dependency đã được cài.

## Bắt đầu

```powershell
git clone https://github.com/cong0905/springboot.git
cd springboot
```

Đọc charter và SRS, sau đó thực hiện nhóm bootstrap trong backlog: tạo ứng dụng, cấu hình database, migration và CI. Lát cắt đầu tiên dự kiến là database → repository → service → controller → trang sản phẩm.

## Cấu trúc & giới hạn

- `docs/`: tài liệu phân tích, thiết kế và kế hoạch.
- [database/schema.sql](database/schema.sql): DDL tham chiếu; cần kiểm chứng trên MySQL trước khi dùng.
- [database/analytics.sql](database/analytics.sql): truy vấn báo cáo tham chiếu.
- [CONTRIBUTING.md](CONTRIBUTING.md): quy trình nhánh, commit và cập nhật đặc tả.

Các sơ đồ Mermaid có thể xem trực tiếp trên GitHub. Tài liệu thiết kế và SQL tham chiếu chưa phải bằng chứng ứng dụng chạy hoặc kiểm thử đã đạt. Chưa chọn giấy phép mã nguồn cho baseline này.

**Tác giả:** [Nguyễn Đức Công](https://github.com/cong0905).
