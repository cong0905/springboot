# Sửa chữa và chức năng bổ sung — cập nhật 04/10/2026

Theo dõi tiến độ chính tại `kehoanh.md`. Các kết quả cuối được lưu trong `qa/repair-continuation/`; số test trong nhật ký trước đó là lịch sử. Không dùng chúng thay cho kết quả của source hiện tại.

## Hợp đồng mới

Mọi mutation giữ CSRF. API customer đòi role CUSTOMER, admin đòi ADMIN. Ownership được lấy từ session; không nhận userId từ form khách hàng. Các form sửa hồ sơ/địa chỉ/ảnh/cửa hàng dùng expectedVersion. Sai owner/resource trả 404, stale/state conflict trả 409, input sai trả 400.

| Chức năng | Trang / API | Hành vi |
| --- | --- | --- |
| Hồ sơ | `/profile`, GET/PATCH `/api/v1/me/profile` | Họ tên, phone, ngày sinh, giới tính; không trả password hash |
| Mật khẩu | POST `/api/v1/me/password` | Kiểm mật khẩu hiện tại; vô hiệu tất cả phiên đăng nhập của tài khoản |
| Avatar | POST multipart `/api/v1/me/avatar` | `file`, `expectedVersion`; decode PNG/JPEG/WebP thật; tối đa 5MB, 8192 chiều, 16 triệu pixel; rollback xóa ảnh mới |
| Địa chỉ | `/addresses`, `/api/v1/me/addresses` | CRUD tối đa 10 địa chỉ, một mặc định; địa chỉ trong đơn là snapshot |
| Catalog | `/products`, GET `/api/v1/products`, `/api/v1/products/facets` | `brand`, `storage`, `minPrice`, `maxPrice`, `inStock`, category/keyword; các điều kiện variant phải cùng một variant |
| Giỏ được chọn | `/cart`, `/checkout?selectedItemIds=...` | Preview và checkout nhận `selectedItemIds`; chỉ tiêu thụ dòng đã chọn, hash/idempotency bao gồm selection; POST `/api/v1/cart/items/remove-many` xác thực toàn bộ trước xóa |
| Mua lại | POST `/api/v1/orders/{id}/reorder` | Owner-only, dùng giá/tồn hiện tại; lỗi một dòng rollback toàn bộ |
| Yêu thích | `/wishlist`, GET `/api/v1/me/wishlist`, PUT/DELETE `/api/v1/me/wishlist/{productId}` | Unique user-product, tối đa 100; không chia sẻ danh sách giữa tài khoản |
| Voucher | `/vouchers`, GET `/api/v1/coupons/mine`, POST `/api/v1/admin/coupons` | `assignedUserId` optional; voucher riêng không cho người khác preview/redeem, maxUses khóa khi redeem; không tự phát voucher giả |
| Cửa hàng | PATCH `/api/v1/admin/branches/{id}/details` | openingHours/photoPath/expectedVersion; ảnh nội bộ `/assets/`; thiếu dữ liệu hiển thị chưa cập nhật, không bịa giờ/ảnh cửa hàng thật |
| So sánh | `/compare?ids=...` | 2–3 sản phẩm, union thông số; chỉ chuẩn hóa định dạng đơn vị đã ghi rõ, không đoán số trần hoặc quy đổi TB/GB mơ hồ |
| Refund | `/admin/refunds`, admin refund/process APIs | Ledger thủ công REQUESTED → PROCESSING → SUCCEEDED/FAILED; thành công cần mã chứng từ; không thực hiện chuyển tiền qua ngân hàng |
| News CMS | `/news`, `/news/{slug}`, `/admin/news`, admin news APIs | Draft/published/archived + version; văn bản escaped, không thực thi HTML; không seed tin giả như tin đã xuất bản |

## SMTP, khôi phục mật khẩu và newsletter

Người dùng chọn SMTP; SMS hoãn. Chạy thêm profile `smtp` sau khi thiết lập biến môi trường bên ngoài repository: `SMTP_HOST`, `SMTP_PORT` (587), `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, `APP_PUBLIC_URL`. Không đưa bí mật vào tài liệu/chat. Production bắt STARTTLS, cookie Secure, public URL HTTPS. Profile `smtp` bật mail; chưa cấu hình không hiển thị form gửi và API trả EMAIL_NOT_CONFIGURED.

POST `/api/v1/auth/forgot-password` trả cùng thông báo 202 cho email có/không có tài khoản. Token ngẫu nhiên 32 byte, chỉ SHA-256 trong DB, hạn 15 phút, dùng một lần. Email chứa `/reset-password#token=...`, JS xóa fragment sau khi đọc; token không nằm trong query/access log. POST `/api/v1/auth/reset-password` nhận token + newPassword và vô hiệu mọi phiên. Có hạn mức IP/email. Mail được gửi bất đồng bộ sau commit; gửi thất bại vô hiệu token và không log email/token.

Newsletter POST `/api/v1/newsletter` bắt consent. Email phải xác nhận qua POST `/api/v1/newsletter/confirm` trước khi SUBSCRIBED; token một lần/hạn 24h. Liên kết hủy qua POST `/api/v1/newsletter/unsubscribe`; GET chỉ mở màn xác nhận. Tests local dùng JavaMailSender test, không phải chứng cứ giao thư SMTP thật. Chưa có luồng phát chiến dịch email hàng loạt.

## Google OAuth

Thêm profile `google`, biến `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `APP_PUBLIC_URL`; Console đăng ký redirect **`APP_PUBLIC_URL/login/oauth2/code/google`**, đường bắt đầu `/oauth2/authorization/google`. OIDC xác minh token qua Spring Security và yêu cầu email_verified. Email trùng tài khoản hiện có không tự liên kết; người đã đăng nhập xác nhận mật khẩu qua `/api/v1/me/google-link`, sau đó hoàn thành OAuth trong hai phút. Subject Google được lưu riêng. Chưa có credentials/Console/live callback nên chưa nghiệm thu đăng nhập Google thật.

## Guest checkout

`/guest/products/{id}`, `/guest/cart`, `/guest/checkout`, `/guest/orders/{id}` dùng các API riêng dưới `/api/v1/guest`. Khách chưa có cart: GET không tạo user/DB row; POST thêm dòng mới tạo guest owner server-side và xoay session ID. Guest account không được login như tài khoản customer. Phiên khác không thể cập nhật/xóa giỏ.

Phạm vi hiện tại: COD + giao tiêu chuẩn; muốn trả góp/VNPay/thu cũ/pickup phải đăng nhập. Checkout trả proof cookie riêng cho order: HttpOnly, SameSite=Lax, Secure theo cấu hình, hạn 30 ngày. DB chỉ giữ hash; proof ràng buộc một order, không cấp quyền API customer. `/guest/orders` chỉ liệt kê theo guest session hiện tại; sau mất session dùng liên kết order trên trình duyệt còn proof. Chưa có email khôi phục đơn guest và job dọn guest bỏ giỏ; đây là việc vận hành còn mở, không coi mã đơn/phone là quyền xem chi tiết.

## Kiểm tra và vận hành

- Build đang dùng `-Dtechshop.build.directory=target-repair` vì JAR demo cũ trong `target` có thể còn bị process giữ. Không tắt demo của người dùng để build.
- H2: `./mvnw.cmd '-Dtechshop.build.directory=target-h2' verify`; MySQL dùng `target-repair` để hai lượt không ghi đè output. Evidence collector nhận `--reports` đúng thư mục, đối chiếu tên suite/tổng XML với log, từ chối lấy XML cũ.
- MySQL: instance riêng do `scripts/start-test-mysql.ps1` tạo, marker sở hữu `.local/mysql-test/.techshop-test-owned`, cổng 33317, DB `techshop_test`. Test guard không cho dùng DB nghiệp vụ. Thiết lập `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`, `TEST_MIGRATIONS=classpath:db/migration` trong shell kiểm thử.
- Browser: `QA_BASE_URL=http://localhost:8094`, `node scripts/repair_browser_qa.cjs`; cần Playwright và Chromium, optional `PLAYWRIGHT_MODULE`/`CHROME_PATH`. Các lỗi 409 chèn vào browser được ghi rõ là fault injection; đặt đơn và login còn lại chạy thật trên demo riêng.
- Scanner: Gitleaks 8.30.1 source và Git history, báo cáo redacted; OSV 2.6.0 quét CycloneDX từ **toàn bộ dependency runtime đã resolve**, gồm transitives. Không dùng scan raw POM có unresolved version để khẳng định an toàn. Bản vá Jackson 3.1.7/Tomcat 11.0.25 theo advisory nhà phát hành: https://github.com/FasterXML/jackson-core/security/advisories/GHSA-7hhh-6rmp-j9qf và https://tomcat.apache.org/security-11.html.
- `.github/workflows/ci.yml` bổ sung security/browser jobs + artifact, nhưng chưa có lần chạy GitHub Actions cho working tree chưa commit. Scanner và browser được chạy local để kiểm chứng source.
- Limiter hiện nằm trong một instance. Trước khi scale nhiều instance cần shared store (ví dụ Redis) và kiểm tra proxy đúng cấu hình. Trusted proxy chỉ nhận IP đã cấu hình; không tin X-Forwarded-For từ peer khác.
- Production HTTPS/VNPay/provider hoàn tiền thật, SMTP delivery, Google Console, dữ liệu giờ/ảnh cửa hàng thật cần staging/config thực tế. Không đánh dấu nghiệm thu production từ demo hoặc test doubles.

Migrations bổ sung V12–V21 gồm IP payment, profile/address, structured brand, điểm reversal audit, reset/Google, wishlist/avatar, news/newsletter, guest proof, branch metadata và coupon assignee. Giữ migrations cũ đã áp dụng.

## Bảo toàn lịch sử bảo hành sau trả hàng (R-19)

Refund trả hàng đầy đủ chỉ đặt `warranty_cards.revoked_at`, không xóa thẻ hoặc repair tickets. Thẻ đã thu hồi không xuất hiện ở tra cứu/owned, không nhận repair mới và không dùng đổi máy; admin vẫn xem và xử lý lịch sử repair hiện có. Thu hồi lặp giữ timestamp đầu. Khi cùng serial được bán lại, thẻ mới thuộc đơn mới; thẻ và lịch sử cũ giữ nguyên.

V22 là Java migration trong `db.migration` và `db.demo`, dùng chung `WarrantyHistoryMigration`: tên constraint unique do H2 tạo không cố định nên phải tra metadata. Thêm unique trên serial đang active (generated column) và order/serial trước khi bỏ unique serial toàn cục; giữ FK và dữ liệu cũ. MySQL DDL không rollback nguyên khối: trước triển khai phải backup DB/ảnh và kiểm thử bản sao, không sửa/xóa V9 đã áp dụng. Java migration có checksum cố định; mọi thay đổi schema tiếp theo cần version mới. Tham khảo [Flyway Java migrations](https://documentation.red-gate.com/flyway/flyway-concepts/migrations/java-based-migrations) và [H2 generated columns](https://h2database.com/html/grammar.html#column_definition).

## CSV trong Excel (R-01/R-18)

`scripts/verify-csv-excel.ps1` gọi exporter Java đã compile rồi mở CSV bằng Excel thật, lưu XLSX và mở lại. Payload công thức được prefix apostrophe; cột phone luôn dùng text guard để không mất số 0 đầu. Excel có thể giữ apostrophe hiển thị: đây là ký tự bảo vệ chủ ý, không phải formula. Bằng chứng trước/sau trong `qa/repair-continuation/csv-excel.json`; kiểm tra dùng dữ liệu test vô hại và một phiên Excel riêng.
