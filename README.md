# springboot · TechShop

Website bán đồ công nghệ cho một cửa hàng, dùng **Java 21, Spring Boot 4.1.1, Thymeleaf, Spring Security, JPA, Flyway và MySQL 8.4**.

**Đã có bản code đầu tiên chạy được.** Có catalog, đăng ký/đăng nhập, giỏ hàng, đặt hàng COD, hủy/xử lý đơn, điều chỉnh tồn kho và báo cáo. Đây là bản phát triển đầu tiên; chưa đạt toàn bộ điều kiện phát hành MVP trong spec. Xem [trạng thái triển khai](docs/12-implementation-status.md) để biết phần đã kiểm chứng và công việc còn lại.

## Chạy demo nhanh

Cài **JDK 21**, kiểm tra `java -version` và `JAVA_HOME` trỏ đến JDK 21. Maven Wrapper đã có trong repo; lần chạy đầu cần Internet để tải Maven và dependencies.

```bash
git clone git@github.com:cong0905/springboot.git
cd springboot
git switch feature/techshop-first-implementation
./mvnw spring-boot:run -Dspring-boot.run.profiles=demo
```

Windows PowerShell, sau khi clone và vào thư mục:

```powershell
git switch feature/techshop-first-implementation
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Mở **http://localhost:8080**. Demo dùng H2 trong bộ nhớ, tự tạo 2 danh mục, 6 sản phẩm và tồn kho 20 mỗi sản phẩm. Dữ liệu demo mất khi dừng ứng dụng. Các tài khoản bên dưới chỉ dành cho profile `demo`; không sử dụng profile này khi đưa ứng dụng lên Internet.

| Vai trò | Email demo | Mật khẩu demo |
|---|---|---|
| Admin | admin@techshop.test | DemoAdmin2026! |
| Khách hàng | customer@techshop.test | DemoCustomer2026! |

Luồng thử: khách đăng nhập → mở sản phẩm → thêm giỏ → đặt COD → xem đơn. Admin đăng nhập ở phiên khác → quản lý đơn → xác nhận → bàn giao vận chuyển → xác nhận đã giao và thu COD → xem dashboard. Khách có thể hủy đơn `PENDING`; admin hủy được `PENDING`/`CONFIRMED`.

## Chạy bằng MySQL và Docker Compose

Cần Docker với Compose v2. Sao chép cấu hình mẫu:

```bash
cp .env.example .env
```

PowerShell dùng `Copy-Item .env.example .env`. Sửa `.env`: đặt `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` và, nếu cần tạo quản trị viên lần đầu, đặt:

```dotenv
APP_BOOTSTRAP_ADMIN_ENABLED=true
APP_BOOTSTRAP_ADMIN_EMAIL=your-admin@example.com
APP_BOOTSTRAP_ADMIN_PASSWORD=your-own-admin-password
```

Mật khẩu admin cần 10–64 ký tự, không quá 72 byte UTF-8. Dùng mật khẩu riêng, không commit `.env`. Bootstrap chỉ tạo admin mới; không nâng quyền một tài khoản khách hàng đã tồn tại.

```bash
docker compose up --build -d
docker compose logs -f app
```

Mở http://localhost:8080. MySQL lưu dữ liệu trong volume `mysql_data`. Sau khi tạo admin, đặt `APP_BOOTSTRAP_ADMIN_ENABLED=false`, xóa mật khẩu admin khỏi `.env` và chạy `docker compose up -d app` để tạo lại container với cấu hình mới. `docker compose down` dừng dịch vụ và giữ dữ liệu; thay mật khẩu trong `.env` không tự đổi mật khẩu của database đã khởi tạo.

Docker Compose trong repo phục vụ môi trường local. Khi triển khai thật cần HTTPS/reverse proxy, profile `prod`, secrets, kiểm tra vận hành và backup/restore theo [operations](docs/10-operations.md).

### Chạy Java trực tiếp với MySQL

Tạo database `techshop` và tài khoản có quyền migration, đặt `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` trong môi trường rồi chạy `./mvnw spring-boot:run` (Windows dùng `.\mvnw.cmd`). Java chạy trực tiếp không tự đọc file `.env`; file đó được Compose đọc. Không chạy thủ công `database/schema.sql` trước Flyway: migration `V1__initial_schema.sql` tạo schema trên database rỗng. JPA chỉ validate, không tự sửa schema.

## Kiểm thử và build

```bash
./mvnw verify
java -jar target/springboot-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo
```

Windows: `.\mvnw.cmd verify`. Mặc định có **14 bài kiểm thử**, gồm 11 integration tests và 3 unit tests. Các case chính: ownership/role/CSRF, từ chối trường đăng ký `role`, đăng nhập/đăng xuất, preview giá, retry cùng key, snapshot, tồn kho, rollback, checkout/hủy đồng thời, luồng trạng thái, doanh thu theo thời điểm giao và render các trang.

Chạy cùng bộ test trên **database MySQL riêng chỉ dành cho test** bằng biến môi trường:

| Biến | Ví dụ |
|---|---|
| TEST_DB_URL | jdbc:mysql://localhost:3306/techshop_test?connectionTimeZone=UTC |
| TEST_DB_USERNAME | techshop_test |
| TEST_DB_PASSWORD | mật khẩu local của bạn |
| TEST_MIGRATIONS | classpath:db/migration |

**Integration tests xóa dữ liệu nghiệp vụ trong database test trước mỗi case. Không trỏ TEST_DB_URL vào database sử dụng thật.** Workflow [CI](.github/workflows/ci.yml) có hai job độc lập: H2 và MySQL 8.4 service. Kết quả thực tế ghi trong [implementation status](docs/12-implementation-status.md); cấu hình workflow không tự chứng minh các job đã chạy thành công.

## Cấu trúc mã nguồn

| Thư mục | Trách nhiệm |
|---|---|
| src/main/java/vn/techshop/identity | Tài khoản, principal, đăng ký |
| catalog | Danh mục, sản phẩm, tìm kiếm/lọc |
| cart | Giỏ hàng, version và preview giá/phí |
| order | Checkout, snapshot, idempotency, cancel, trạng thái |
| inventory | Điều chỉnh và sổ biến động tồn kho |
| reporting | KPI và sản phẩm bán chạy |
| config | Security, HTML/admin controllers, seed và bootstrap |
| shared | Validation, phân trang, lỗi và request ID |
| src/main/resources/db/migration | Flyway MySQL, nguồn schema ứng dụng |
| src/main/resources/db/demo | Migration tương thích H2 cho demo/test nhanh |
| templates và static | Giao diện Thymeleaf, CSS, JavaScript, SVG đóng gói |
| src/test | Unit và integration tests |

Controllers HTML và REST dùng chung services. Checkout khóa giỏ và khóa sản phẩm theo ID; ghi đơn, snapshot, ledger và xóa giỏ trong một transaction. Unique `(user_id, checkout_key)` và request hash chống tạo lại đơn. Hủy đơn khóa đơn rồi hoàn tồn đúng một lần. API dùng session, CSRF và role/ownership ở server; `GET /api/v1/csrf` cấp token cho API client cùng session.

## Bộ tài liệu BA/DA

Các tài liệu 00–11 là baseline thiết kế; [12](docs/12-implementation-status.md) mô tả code hiện tại và chênh lệch so với baseline.

| Tài liệu | Nội dung |
|---|---|
| [Charter](docs/00-project-charter.md) | Phạm vi, giả định, rủi ro |
| [SRS](docs/01-spec.md) | Yêu cầu, use case, quy tắc và AC |
| [Kiến trúc](docs/02-architecture.md) | Module, security, transaction |
| [Dữ liệu](docs/03-data-design.md) | ERD, dictionary, index |
| [API contract](docs/04-api-contract.md) | Hợp đồng mục tiêu; xem tài liệu 12 về khác biệt hiện tại |
| [Sơ đồ](docs/05-diagrams.md) | Activity, sequence, state, phát triển |
| [Kế hoạch](docs/06-development-plan.md) | Milestone và dependency |
| [Backlog](docs/07-backlog.md) | Công việc và ước lượng |
| [Test plan](docs/08-test-plan.md) | Kịch bản nghiệm thu mục tiêu |
| [Analytics](docs/09-analytics-plan.md) | KPI, timezone và đối chiếu |
| [Operations](docs/10-operations.md) | Backup, rollback, triển khai |
| [ADR/nguồn](docs/11-decisions-and-sources.md) | Quyết định và tài liệu chính thức |
| [Implementation status](docs/12-implementation-status.md) | Code, kiểm chứng, giới hạn, bước tiếp theo |
| [Schema tham chiếu](database/schema.sql) | Bản thiết kế được chuyển thành migration V1 |
| [SQL tham chiếu](database/analytics.sql) | Truy vấn báo cáo thiết kế |
| [Đóng góp](CONTRIBUTING.md) | Branch, commit và review |

Thanh toán online, nhiều cửa hàng, đổi trả và biến thể sản phẩm thuộc giai đoạn sau. Chưa chọn giấy phép mã nguồn. Không commit credentials thật hoặc dữ liệu khách hàng.
