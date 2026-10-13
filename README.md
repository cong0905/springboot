# TechShop · Spring Boot Commerce

Website bán công nghệ: **Java 21, Spring Boot 4.1.1, Thymeleaf, Spring Security, JPA/JDBC, Flyway, MySQL/H2**. Có catalog, session/CSRF, giỏ/version, checkout/snapshot/idempotency, coupon, đánh giá, quản trị và CSV. Bản đa kênh bổ sung biến thể/thông số/album, kho chi nhánh/IMEI, pickup/giao 2h, thu cũ, hồ sơ trả góp demo, hội viên và bảo hành/đổi máy.

Xem [kế hoạch và tiến độ sửa chữa](kehoanh.md), [hợp đồng chức năng mới và cấu hình SMTP/Google](docs/18-repair-continuation.md), [bằng chứng hiện tại](qa/repair-continuation/README.md) và [hướng dẫn đa kênh](docs/13-omnichannel-guide.md). Báo cáo 12 và OpenAPI là snapshot nghiệp vụ đa kênh trước đợt sửa chữa; các API mới được ghi tại tài liệu 18. Các tài liệu 00–11 là baseline thiết kế; tài liệu Laravel được giữ riêng ở `docs/reference-laravel-features.md`.

## Chạy demo

Cần **JDK 21**. PowerShell:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21'
.\mvnw.cmd verify
.\scripts\run-demo.ps1
# Nếu cổng 8080 đang được dùng:
.\scripts\run-demo.ps1 -Port 8091
```

Linux/macOS: `./mvnw verify`, rồi `java -jar target/springboot-0.1.0-SNAPSHOT.jar --spring.profiles.active=demo`. Mở http://localhost:8080.

| Vai trò | Email demo | Mật khẩu demo |
|---|---|---|
| Admin | admin@techshop.test | DemoAdmin2026! |
| Khách hàng | customer@techshop.test | DemoCustomer2026! |

Demo H2 mất dữ liệu khi dừng; chỉ chạy local. Có catalog mẫu mở rộng, biến thể/serial, chi nhánh Hà Nội, bảng thu cũ và gói trả góp demo. Giờ mở cửa/ảnh chi nhánh còn thiếu sẽ hiển thị chưa cập nhật.

Luồng: khách đăng nhập → điện thoại → chọn phiên bản → giỏ → checkout → chọn nhận cửa hàng → nhập liên hệ → **Tính lại giá & cửa hàng** → đặt. Khách có thể xem phiên bản/album/tồn cửa hàng trước đăng nhập và lọc theo mã tỉnh/khu vực cửa hàng. Admin mở đơn và nhập mã hoặc quét QR khách cung cấp để nhận máy. IMEI SOLD, bảo hành 12 tháng và điểm/hạng được cập nhật. Đơn giữ chưa trả tiền tự hủy sau 24 giờ.

## Kiểm thử

`.\mvnw.cmd verify` kiểm tra session/role/CSRF/ownership, replay, rollback, tồn/IMEI, callback/expiry/coupon cạnh tranh, refund/ledger điểm/bảo hành, profile/địa chỉ/guest, content và cấu hình provider. Số tests thực tế cùng XML đối chiếu log nằm ở `qa/repair-continuation/h2-final.json` và `mysql-final.json`; không lấy số 58 tests của bản cũ làm nghiệm thu source mới. Khi JAR demo cũ đang chạy, dùng `'-Dtechshop.build.directory=target-h2'` hoặc `target-repair` để tránh khóa file.

Tests xóa dữ liệu nghiệp vụ, chỉ dùng H2 `techshop-test` hoặc MySQL **techshop_test** riêng. `scripts/start-test-mysql.ps1` dùng binary MySQL đã cài, tạo instance loopback ở cổng 33317 và `.local/mysql-test`; không dùng cho production.

```powershell
.\scripts\start-test-mysql.ps1
$env:TEST_DB_URL='jdbc:mysql://127.0.0.1:33317/techshop_test?connectionTimeZone=UTC'
$env:TEST_DB_USERNAME='root'
$env:TEST_DB_PASSWORD=''
$env:TEST_MIGRATIONS='classpath:db/migration'
.\mvnw.cmd verify
Remove-Item Env:TEST_DB_URL,Env:TEST_DB_USERNAME,Env:TEST_DB_PASSWORD,Env:TEST_MIGRATIONS
```

Browser QA mở rộng: khởi động demo riêng rồi đặt `QA_BASE_URL=http://localhost:8094` và chạy `node scripts/repair_browser_qa.cjs`. Cần Playwright + Chrome; có thể đặt `PLAYWRIGHT_MODULE` và `CHROME_PATH`. Kiểm tra các luồng mua, profile/địa chỉ, guest, đổi mật khẩu/thu hồi session và CMS ở 1440/1024/768/390/320px; JSON phân biệt thao tác thật với lỗi 409 cố ý chèn. Backup/restore chỉ trên DB test sở hữu: `scripts/verify-test-restore.ps1`; CSV/Excel: `scripts/verify-csv-excel.ps1`.

## MySQL và Docker Compose

```powershell
Copy-Item .env.example .env
# Đặt DB_PASSWORD, MYSQL_ROOT_PASSWORD và bootstrap admin của bạn.
docker compose up --build -d
```

Compose dùng MySQL 8.4, volume `mysql_data` và `app_uploads`. Ảnh upload ngoài JAR ở `./data/uploads`, đổi bằng APP_UPLOAD_DIR; sao lưu cùng DB. Không chạy `database/schema.sql` trước Flyway. Java trực tiếp không đọc `.env`; đặt DB_URL/DB_USERNAME/DB_PASSWORD trong môi trường. MySQL mặc định không seed dữ liệu/tài khoản demo. Sau tạo admin, tắt bootstrap và xóa mật khẩu bootstrap khỏi `.env`. Compose chưa chạy tại máy này vì thiếu Docker; trước production cần HTTPS/cookie secure, secrets, backup/restore và [operations](docs/10-operations.md).

## Cấu trúc

`identity/catalog/cart` xử lý tài khoản và giỏ; `variant/branch/imei` xử lý hàng hóa; `order/inventory` xử lý đơn và ledger; `coupon/review/payment` xử lý ưu đãi/đánh giá/thanh toán; `tradein/installment/loyalty/warranty/reporting` xử lý đa kênh và hậu mãi. HTML/REST dùng cùng services, DTO records, open-in-view tắt. Checkout khóa cart → products theo ID → stock/IMEI `FOR UPDATE`; lưu đơn/snapshot/history/ledger/reservation và xóa giỏ trong một transaction. Hủy hoàn kho đúng một lần.

## Phạm vi tích hợp

- VNPay cần VNPAY_TMN_CODE + VNPAY_HASH_SECRET; đã kiểm tra chữ ký/số tiền/replay ở IPN, chưa giao dịch merchant thật. Browser return không tự ghi nhận tiền.
- MOCK chỉ bật demo/test. Trả góp là hồ sơ/lịch trả demo, chưa kết nối ngân hàng/BNPL.
- Giao 2h dùng tọa độ/Haversine và bảng phí local, chưa kết nối hãng vận chuyển. Mã nhận máy và QR hiển thị trong đơn, kiểm tra owner/role và hạn giữ hàng; chưa gửi SMS/email.
- BarcodeDetector là tiện ích tùy trình duyệt; camera/máy quét thật chưa kiểm chứng.
- Bảo hành công khai theo serial; điện thoại là kiểm tra bổ sung, không công khai danh sách máy theo số điện thoại.
- SMTP và Google OAuth có profiles `smtp`/`google`; đặt credentials ngoài repository theo tài liệu 18. Token reset/newsletter chỉ giữ hash và dùng một lần. Chưa nghiệm thu giao thư thật, Google Console/callback hoặc production HTTPS; SMS hoãn theo lựa chọn của người dùng.
- Guest dùng giỏ/session/proof riêng và chỉ COD giao tiêu chuẩn. Refund là ledger đối soát thủ công có chứng từ, chưa gọi provider hoàn tiền thật. Thu hồi bảo hành giữ lịch sử sửa chữa.

Chat khách/admin và AI Gemini: xem [chat riêng](docs/19-customer-admin-chat.md) và [cấu hình Gemini](docs/21-gemini-chat-setup.md). AI tắt mặc định, cần `APP_AI_ENABLED=true`, `GEMINI_API_KEY` và `GEMINI_MODEL` ở backend. Khách đồng ý bật AI, nhận tư vấn/thẻ sản phẩm hoặc chuyển nhân viên; admin tiếp nhận để dừng AI. Kiểm thử Google đang dùng giả lập; chưa nghiệm thu API key/model thật.

Chưa có giấy phép mã nguồn. Không commit credentials/dữ liệu khách thật.
