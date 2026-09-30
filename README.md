# springboot · TechShop

TechShop là dự án website bán đồ công nghệ cho **một cửa hàng**, xây dựng bằng Java Spring Boot. Mục tiêu là hoàn thành một hệ thống có luồng mua hàng, quản lý tồn kho và xử lý đơn rõ ràng, có thể trình diễn trong hồ sơ Java Backend.

**Trạng thái: đã có bộ tài liệu phân tích và thiết kế v0.1; chưa có mã ứng dụng chạy được.** Các chức năng, API, database và thời gian bên dưới là kế hoạch triển khai, không phải tính năng đã hoàn thành. Baseline lập ngày 01/10/2026, theo múi giờ Asia/Ho_Chi_Minh.

## Phạm vi bản đầu tiên

- Khách vãng lai xem, tìm kiếm, lọc sản phẩm.
- Khách đăng ký, đăng nhập, quản lý giỏ hàng, đặt hàng COD, xem và hủy đơn còn chờ xác nhận.
- Admin quản lý danh mục, sản phẩm, điều chỉnh tồn kho có lịch sử; xử lý đơn và xem báo cáo cơ bản.
- Bảo vệ quyền truy cập, CSRF, chống đặt đơn trùng, ngăn bán vượt tồn kho, lưu giá và địa chỉ tại thời điểm đặt hàng.

Giả định làm việc: một người phát triển, một kho, tiền VND, giao hàng xử lý thủ công. Thanh toán online, marketplace nhiều người bán, đổi trả, biến thể sản phẩm và ứng dụng di động thuộc giai đoạn sau.

## Đọc tài liệu theo thứ tự

| Tài liệu | Nội dung |
|---|---|
| [Project charter](docs/00-project-charter.md) | Vấn đề, mục tiêu, phạm vi, giả định, rủi ro |
| [Đặc tả SRS](docs/01-spec.md) | Yêu cầu, quy tắc, use case, tiêu chí nghiệm thu, màn hình |
| [Kiến trúc](docs/02-architecture.md) | Module, lớp, bảo mật, transaction, triển khai |
| [Thiết kế dữ liệu](docs/03-data-design.md) | ERD, data dictionary, khóa, index, vòng đời dữ liệu |
| [Hợp đồng API](docs/04-api-contract.md) | Route, payload, quyền, lỗi và idempotency |
| [Các sơ đồ](docs/05-diagrams.md) | Hoạt động, tuần tự, trạng thái, quy trình phát triển |
| [Kế hoạch phát triển](docs/06-development-plan.md) | 6 tuần, dependency, milestone, DoR/DoD |
| [Backlog](docs/07-backlog.md) | Công việc, ưu tiên, ước lượng và đầu ra |
| [Kế hoạch kiểm thử](docs/08-test-plan.md) | Truy vết yêu cầu, test case và điều kiện phát hành |
| [Phân tích dữ liệu](docs/09-analytics-plan.md) | KPI, nguồn dữ liệu, quy tắc tính và kiểm soát chất lượng |
| [Vận hành](docs/10-operations.md) | CI/CD, cấu hình, backup, rollback, xử lý sự cố |
| [Quyết định thiết kế](docs/11-decisions-and-sources.md) | ADR, lựa chọn còn mở, tài liệu kỹ thuật chính thức |
| [DDL tham chiếu](database/schema.sql) | Database MySQL 8.4 dự kiến; chưa chạy kiểm chứng trên MySQL |
| [SQL báo cáo](database/analytics.sql) | Truy vấn mẫu, theo tham số thời gian UTC |
| [Đóng góp](CONTRIBUTING.md) | Cách chia nhánh, commit, PR và cập nhật spec |

Các sơ đồ dùng Mermaid, xem trực tiếp khi mở file Markdown trên GitHub. DDL ở database/ là bản thiết kế; khi khởi tạo ứng dụng sẽ chuyển thành migration Flyway. Hiện chưa có pom.xml, Maven Wrapper, Dockerfile hay pipeline build ứng dụng.

## Công nghệ dự kiến

Java 21; Spring Boot 4.1.1 làm baseline tại ngày lập kế hoạch; Spring MVC; Thymeleaf; Bootstrap 5; Spring Security với session; Spring Data JPA; MySQL 8.4; Flyway; JUnit; Spring Boot Test; Testcontainers; Maven; Docker và GitHub Actions. Dùng BOM của Spring Boot để quản lý các thư viện Spring, kiểm tra bản vá ổn định khi khởi tạo project.

## Cách bắt đầu

Clone repository trên máy có quyền truy cập GitHub:

```bash
git clone git@github.com:cong0905/springboot.git
cd springboot
git switch -c feature/project-bootstrap
```

1. Đọc charter và spec, xem các giả định A-01 đến A-10.
2. Thực hiện B-01 đến B-04 trong backlog: tạo project, môi trường local, migration và CI.
3. Làm lát cắt đầu tiên: MySQL → repository → service → controller → trang danh sách sản phẩm.
4. Viết hướng dẫn chạy thật vào README khi lát cắt đó hoạt động. Không coi lệnh triển khai dự kiến trong tài liệu là các script đang tồn tại.

## Mốc hoàn thành

Bản MVP đạt yêu cầu khi khách đặt được đơn COD; admin xử lý đến DELIVERED; kiểm thử đồng thời và phân quyền đạt; báo cáo đối chiếu đúng dữ liệu; một người khác chạy được từ README; có bản demo và bằng chứng kiểm thử. Chi tiết ở [kế hoạch](docs/06-development-plan.md) và [test plan](docs/08-test-plan.md).

## Theo dõi thay đổi

| Phiên bản | Ngày | Nội dung |
|---|---|---|
| 0.1 | 01/10/2026 | Baseline phân tích, thiết kế và kế hoạch phát triển; các giả định chưa phải yêu cầu đã được chủ dự án xác nhận riêng |

Không có giấy phép mã nguồn được chọn trong baseline này. Không ghi tài khoản demo thật, mật khẩu, khóa hoặc dữ liệu khách hàng vào repository.
