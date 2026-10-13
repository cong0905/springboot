# Trạng thái triển khai TechShop đa kênh

**Snapshot lịch sử 01/10/2026.** Source và kết quả sau đợt sửa chữa được cập nhật tại [kehoanh.md](../kehoanh.md), [hợp đồng mới](18-repair-continuation.md) và [evidence](../qa/repair-continuation/README.md); số tests/migrations trong tài liệu này không đại diện bản hiện tại.

Ngày kiểm chứng: 01/10/2026, Asia/Ho_Chi_Minh. Bản này chạy local với nghiệp vụ đa kênh; chưa nghiệm thu production. Báo cáo này thay thế báo cáo baseline có nhận định hoàn thành mâu thuẫn. Xem [kế hoạch](../plan.md), [hướng dẫn](13-omnichannel-guide.md), [OpenAPI](openapi.json).

## Chức năng đã triển khai

| Nhóm | Hành vi thực thi |
|---|---|
| Nền tảng | Java 21, Boot 4.1.1, session/BCrypt/CSRF, CUSTOMER/ADMIN, DTO records, owner guard, Flyway V1–V9 cho H2/MySQL |
| Catalog | Tìm/lọc/sort/phân trang, autocomplete; khách chưa đăng nhập chọn phiên bản/album/tồn; nhiều ảnh, upload PNG/JPEG/WebP ≤5MB; phục vụ ảnh ngoài JAR và volume Docker |
| Biến thể | SKU duy nhất, màu/dung lượng/giá/version; album riêng; thông số nhóm/tên, so sánh 2–3 sản phẩm |
| Kho chi nhánh | Tạo chi nhánh/tọa độ/mã khu vực; lọc mã tỉnh/khu vực/chỉ còn hàng, không hiển thị chi nhánh ngừng hoạt động; nhập 1–500 serial; tồn khả dụng/giữ; điều chuyển IN_STOCK; lịch sử IMEI, ledger tổng |
| Đơn | Giỏ theo phiên bản; preview server; snapshot, UUID replay, version/hash; đặt/giữ/xóa giỏ trong transaction; hủy hoàn kho một lần |
| Đa kênh | Giao chuẩn; giao 2h trong 20km theo Haversine/bảng phí; pickup giữ 24h/mã nhận/QR PNG có owner/role và no-store, job hết hạn dùng UTC; thu COD và in phiếu nội bộ |
| Ưu đãi | Coupon min/cap/hết hạn/lượt dùng; thu cũ theo giá+tình trạng+trợ giá, thẩm định, owner/7 ngày/một lần |
| Trả góp | Kỳ 3/6/9/12 tháng, phí/lịch nguyên VND, phần dư kỳ cuối; hồ sơ demo duyệt trước giao; admin thêm/ngừng gói |
| Hội viên | Ledger theo DELIVERED, 1% tiền hàng thành điểm, hạng >10m/>30m, giảm 1%/3%; trang hội viên và máy đã mua |
| Bảo hành | 12 tháng theo IMEI, tra serial/điện thoại kiểm tra bổ sung; sửa theo tuần tự; đổi máy cùng biến thể trong 30 ngày |
| Thanh toán | COD; MOCK chỉ demo/test; VNPay ký/kiểm HMAC-SHA512, merchant/amount/status/replay ở IPN; browser return không ghi nhận tiền; đơn chưa trả có paymentUrl dùng lại transaction đã lưu |
| Quản trị | Catalog/ảnh, tồn/IMEI, bảng thu cũ/gói, thẩm định, trả góp, sửa/đổi máy, đơn/CSV, doanh thu đã giao theo kênh/chi nhánh |
| UI | Đỏ/trắng responsive; đổi phiên bản/ảnh/giá, tồn cửa hàng, so sánh, thu cũ, hội viên, bảo hành; USB input/camera có fallback |

Scalar foreign keys được bảo vệ bởi DB constraints. Không đổi migration nền V1–V4. database/schema.sql là thiết kế baseline; Flyway là schema thực thi.

## Bằng chứng thực thi

| Kiểm tra | Môi trường và kết quả |
|---|---|
| Maven verify H2 | 58 tests: 34 nền + 24 đa kênh; 0 failures/errors/skipped; qa/h2-tests.json |
| Maven verify MySQL | MySQL **8.0.43**, techshop_test riêng, loopback 33317; 58 tests, 0 failures/errors/skipped; qa/mysql-tests.json |
| Browser E2E | Chrome/Playwright, JAR demo thực; 1440/390/320px; HTTP 200, không tràn ngang, không lỗi JS; qa/browser-results.json |
| Mua và nhận máy | Browser: chọn phiên bản → giỏ → preview → pickup → admin nhập mã → DELIVERED → bảo hành/điểm/hạng |
| Cạnh tranh IMEI | 100 worker cùng máy cuối tại một chi nhánh; đúng một đơn thành công, một IMEI RESERVED |
| Rollback | Cố ý tắt MOCK gây lỗi khởi tạo sau ghi đơn/history/kho/IMEI; giỏ còn nguyên, không có đơn/reservation, tồn phục hồi |
| Bổ sung checkout/QR | QR được giải mã đối chiếu code; thử sai owner/role/CSRF, hết hạn/còn hạn/đã trả/đã giao; UI trì hoãn preview và lỗi 409 có nhãn fault injection |
| Upload | Multipart PNG thật → GET trả đúng byte/content type; từ chối tệp khai PNG giả |

JSON xuất từ Surefire XML chạy thực. Browser QA thay đổi dữ liệu demo, phải dùng demo mới. 100 worker kiểm tra tranh tồn, không phải đo throughput/p95 production. Kết quả MySQL 8.0.43 không xác nhận MySQL 8.4/Docker.

## Bất biến nghiệp vụ

- Checkout khóa cart → products theo ID → kho/IMEI bằng SQL FOR UPDATE; repository IMEI có PESSIMISTIC_WRITE. Đơn/snapshot/history/ledger/reservation/clear cart cùng transaction.
- Cancel khóa order → product → kho/IMEI; không hoàn tồn hai lần. SOLD/đổi máy có lock/history. Đổi máy trừ tồn thay thế, giữ hạn bảo hành gốc, không nhân đôi doanh thu/điểm.
- Payment khóa order trước transaction thanh toán; SUCCESS bất biến, callback không hồi sinh CANCELLED. PAID cần refund, chưa tự hoàn tiền.
- Customer API lấy principal, mutation cần CSRF, admin cần ROLE_ADMIN. Public warranty không liệt kê máy theo điện thoại.
- Test chỉ dọn H2 memory techshop-test hoặc MySQL techshop_test. Không seed demo trên MySQL mặc định.
- Model/gói thêm mới hoặc ngừng; không sửa hồi tố điều khoản cũ. Giá thu đã quote là snapshot.

## Những phần chưa nghiệm thu

1. **Ngoài ứng dụng:** cần merchant VNPay/IPN reachable để thử sandbox thật; chưa MoMo, BNPL/ngân hàng, hãng vận chuyển, SMS/email. Trả góp/phí giao xử lý nội bộ; pickup code và QR chỉ hiển thị trong đơn.
2. **Vận hành:** thiếu Docker nên chưa build/Compose MySQL 8.4; chưa HTTPS/staging, backup/restore drill, metrics/alert, nâng cấp dữ liệu production, UAT phát hành.
3. **Thiết bị:** chưa thử camera/USB scanner thật. BarcodeDetector tùy trình duyệt; nhập tay/serial mỗi dòng hoạt động.
4. **Nghiệp vụ mở rộng:** một đơn IMEI xuất từ một chi nhánh; hàng thường và hàng serial đặt riêng. Chưa chia đa kho, refund tự động, redeem điểm, đổi khác model, bảo hành VIP.
5. **UI mở rộng:** API lọc tỉnh theo mã; bản đồ là link tọa độ. Selector dùng mã tỉnh/khu vực đã cấu hình của cửa hàng, không phải danh mục địa giới toàn quốc. Chưa ưu đãi riêng ngân hàng, hóa đơn điện tử.

Các mục chưa đạt vẫn mở trong plan.md. Bản này hoàn thành phần triển khai và kiểm chứng local; không đánh dấu toàn bộ Definition of Done production đạt.

## Tiếp nhận

README hướng dẫn demo/H2/MySQL tests. APP_UPLOAD_DIR mặc định ./data/uploads; sao lưu cùng DB. Docker có mysql_data và app_uploads. Demo reset DB khi restart; ảnh upload ngoài DB cần dọn khi không dùng.

Graphify được cập nhật sau thay đổi; graphify-out/GRAPH_REPORT.md, graph.json, HTML và wiki là chỉ mục tra cứu. Controller/service/migration và QA là bằng chứng triển khai.
