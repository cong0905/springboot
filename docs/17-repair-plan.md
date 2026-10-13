# Kế hoạch sửa lỗi, bảo mật và hoàn thiện TechShop / TechZone

Ngày lập: **02/10/2026**, giờ Việt Nam. Phạm vi: mã nguồn hiện tại tại `D:\springboot`.

**Thứ tự thực hiện: xác nhận lỗi → bảo mật → tiền/đơn/kho → chức năng chính → giao diện và chức năng mở rộng → nghiệm thu.** Đây là kế hoạch sửa chữa dựa trên rà soát ban đầu; các mục dưới đây chưa được sửa trong lượt lập kế hoạch.

## 1. Căn cứ và giới hạn

- Đã truy vấn Graphify trước khi rà soát source; graph hiện có 2.369 nodes, 5.328 edges. Dùng graph để tìm module; đối chiếu mã nguồn hiện tại để kết luận, vì CLI cảnh báo graph dùng hệ ID cũ.
- Stack kiểm tra từ `pom.xml`: Java 21, Spring Boot 4.1.1, Thymeleaf, Spring Security, JPA, Flyway; MySQL cho ứng dụng mặc định, H2 cho demo/test.
- HEAD khi kiểm tra: `6e66ee1646202953c39145ee420e9f90e804e411`, nhánh `feature/techshop-first-implementation`. Có nhiều thay đổi chưa commit, gồm 41 tệp tracked thay đổi và nhiều tệp/module mới. HEAD không đại diện đầy đủ cho working tree.
- Đã đọc đường đi auth, catalog, cart, checkout, payment, pickup, inventory, trade-in, installment, loyalty, warranty, export và tài liệu chức năng. Chưa kiểm thử khai thác toàn diện, chưa nghiệm thu production và chưa chạy lại toàn bộ browser E2E trong lượt này.
- Không đọc/in nội dung `.env`. Chưa quét dependency/secret toàn bộ lịch sử Git; không kết luận đã có CVE hay secret bị lộ khi chưa có bằng chứng.
- Kết quả kiểm thử baseline và dấu vết source được lưu riêng tại [bằng chứng](../qa/repair-plan-20261002/baseline.json). Chỉ dùng kết quả của lần chạy này cho source tương ứng, không lấy báo cáo cũ làm bằng chứng hiện tại.

Các nhãn bằng chứng:

| Nhãn | Ý nghĩa |
| --- | --- |
| Xác nhận từ mã | Đã thấy hành vi/thiếu kiểm soát trong source, chưa có nghĩa đã khai thác thành công ở môi trường thật |
| Cần tái hiện | Có đường đi đáng nghi hoặc thiếu chính sách; cần test để chốt tác động |
| Thiếu chức năng | Chưa có hợp đồng/backend/UI đầy đủ, khác với chức năng đã tồn tại nhưng bị lỗi |
| Phụ thuộc triển khai | Mức rủi ro phụ thuộc môi trường/config thực tế |

P0 chỉ dùng khi xác nhận chiếm quyền quản trị, ghi nhận tiền giả, truy cập trái phép nghiêm trọng hoặc hỏng dữ liệu diện rộng. **Rà soát này chưa xác nhận lỗi P0.** P1 là ưu tiên xử lý trước mở rộng tính năng; P2 xử lý sau các invariant tiền/đơn/kho; P3 là mở rộng theo roadmap.

## 2. Backlog sửa chữa có căn cứ

| ID | Ưu tiên | Bằng chứng / vấn đề | Phạm vi và đầu ra sửa chữa | Nghiệm thu |
| --- | --- | --- | --- | --- |
| R-01 | P1 | Xác nhận từ mã: CSV chỉ escape dấu phẩy/quote/newline, không xử lý formula; tên/địa chỉ khách được phép bắt đầu bằng ký tự công thức | `ReportExportService.java` và hợp đồng export: xuất trường người dùng dưới dạng text an toàn, giữ dấu tiếng Việt, số điện thoại và cấu trúc cột | Payload lành tính `=1+1`, `+1+1`, `@SUM(1,1)`, tab/CR/quote/delimiter không được thực thi như formula khi mở bằng công cụ đích; kiểm tra cả lưu/mở lại nếu tiếp tục dùng CSV |
| R-02 | P1 | Xác nhận từ mã: tra cứu đơn công khai nhận ID tuần tự + số điện thoại; hiển thị tên, số, địa chỉ, ghi chú; chưa có limiter riêng | `LookupOrderController`, `OrderService.lookup`, `order-lookup.html`: giới hạn theo IP và định danh, DTO công khai tối thiểu, che dữ liệu; xem đầy đủ chỉ bằng owner session hoặc proof/token một lần | Đổi mã/phone không trả thông tin người khác; vượt ngưỡng nhận 429; kết quả công khai không chứa full address/note/pickup token/payment URL; không suy ra đơn tồn tại từ lỗi khác nhau |
| R-03 | P1 | Xác nhận từ mã: throttle login theo cặp IP+username; khi map đạt 10.000 entries thì không lưu key mới; state chỉ nằm trong một process | `LoginAttempts`, `SecurityConfig`: hạn mức account và IP độc lập, chống spray và giới hạn tài nguyên; lưu limiter chung khi chạy nhiều instance | Dùng nhiều IP cho cùng account và nhiều account cho cùng IP vẫn chạm giới hạn; đầy cache không mở đường đăng nhập không giới hạn; không khóa account vĩnh viễn; phục hồi sau expiry |
| R-04 | P1 | Xác nhận từ mã: giao mọi đơn đều bắt `codCollected=true`, kể cả online đã PAID/trả góp; pickup tự truyền true | `OrderService.transition`, `PickupService.collect`, `order.html`: tách xác nhận giao hàng, thu COD và xác nhận tài trợ; giữ state machine/order version | COD chưa thu không hoàn tất; online PAID hoàn tất mà không đánh dấu thu COD; installment được duyệt theo hợp đồng riêng; gửi lại không ghi thêm tiền/điểm/bảo hành |
| R-05 | P1 | Cần tái hiện/chốt chính sách: expiry chỉ áp dụng pickup; đơn online giao nhà thất bại/bỏ thanh toán và hồ sơ trả góp REJECTED có thể tiếp tục giữ kho | `PaymentService`, `InstallmentService.decision`, `PickupService`, `OrderService.cancel`: timeout/rejection/retry và giải phóng reservation có kiểm soát | Test hết hạn, từ chối, callback đến muộn và cancel cùng lúc; hoàn global stock/branch reservation/IMEI đúng một lần; giao dịch thật đã nhận tiền đi đối soát, không tự hủy như đơn chưa trả |
| R-06 | P1 | Thiếu quy trình nghiệp vụ: PAID bị chặn hủy với `REFUND_REQUIRED`; chưa có luồng xử lý hoàn tiền đầy đủ | Mô hình refund/reconciliation và màn hình admin; trước hết có ledger xử lý thủ công được kiểm toán, tích hợp gateway sau khi có cấu hình và xác minh hợp đồng | Có requested/processing/succeeded/failed và actor/reason/reference; retry không hoàn hai lần; chỉ trả kho/thu hồi điểm/bảo hành theo trạng thái hoàn trả hàng thực tế |
| R-07 | P1 kiểm chứng | Cần tái hiện: các luồng nhiều bảng và cạnh tranh phải được kiểm lại trên MySQL hiện tại | Bộ kiểm thử auth/ownership, checkout last-unit, coupon, IMEI, payment-vs-cancel, pickup-vs-expiry, trade-in và loyalty | Invariant đúng sau commit/rollback trên H2 và MySQL DB test riêng; không bán một serial hai lần, âm tồn, duplicate order, duplicate refund/points; lỗi tranh chấp có response rõ |
| R-08 | P2 | Xác nhận từ mã: `findByCode` fallback từ mã TXN không tồn tại sang giao dịch mới nhất của order | `PaymentService.findByCode`, mock/return/resume callers: lookup chính xác theo transaction code, tách rõ lookup theo order nếu có nhu cầu | Sai suffix hoặc mã giao dịch cũ không ánh xạ âm thầm sang giao dịch khác. IPN đang lookup exact và verify chữ ký/amount: phải giữ bảo vệ này; chưa xác nhận bypass IPN |
| R-09 | P2 | Xác nhận từ mã: `WarrantyService.transition` dùng `queryForObject` cho ID có thể không tồn tại; error handler không có mapping missing-row tương ứng | Các service JDBC + `Errors`: missing resource trả 404, input sai 400, stale/state conflict 409; ràng buộc DTO lồng nhau | Repair ID không tồn tại và các path tương tự không trở thành 500; giữ error code/requestId; không lộ SQL/stacktrace |
| R-10 | P2 | Xác nhận từ mã: phân trang `orders.html` chỉ giữ page/status, bỏ keyword/fromDate/toDate | Template đơn + query/controller; thêm kiểm tra fromDate ≤ toDate | Lọc đơn admin → trang sau/trước vẫn giữ cùng bộ lọc và kết quả; reset xóa đầy đủ; owner scope không thay đổi |
| R-11 | P2 | Xác nhận từ mã: upload kiểm MIME, kích thước, vài byte signature nhưng không decode nội dung ảnh | `FileStorageService`: kiểm ảnh thực, giới hạn dimensions/pixels, đồng bộ multipart cap; encoder hỗ trợ WEBP hoặc policy định dạng rõ; kiểm quyền và lưu trữ | File chỉ có header ảnh nhưng hỏng bị từ chối; ảnh lớn quá ngưỡng không gây cạn memory; upload hợp lệ và restart còn ảnh; giữ UUID filename/path traversal guard/rollback đang có |
| R-12 | P2 | Phụ thuộc triển khai: cookie secure mặc định false, demo bật mock; payment URL/client IP đang dùng localhost/127.0.0.1 ở các call site | Production profile/config validation, HTTPS, trusted proxy/client IP, tắt demo/mock, kiểm bootstrap-admin và logging secret | Production không khởi động với tổ hợp config nguy hiểm theo policy; cookie qua HTTPS có Secure; mock không dùng được; không tin mọi X-Forwarded-For; lỗi chỉ chứa mã kiểm tra |
| R-13 | P2 | Xác nhận từ mã: handler lỗi bất ngờ chỉ log class, khó truy ra nguyên nhân; CI hiện chạy H2/MySQL tests nhưng chưa có bước scan dependency/secret/browser riêng | Structured log có requestId + diagnostic cause phù hợp, che PII/token; thêm scan có báo cáo, CI lưu evidence và browser job | Có thể truy lỗi bằng requestId; log không chứa password/token/địa chỉ đầy đủ; findings scanner được triage chứ không coi mọi cảnh báo là exploit |
| R-14 | P2 | Xác nhận cấu trúc truy vấn: trang catalog lấy ảnh/rating/variants/tồn cho từng sản phẩm và biến thể | Batch query/projection cho listing và route candidates sau khi nghiệp vụ đã ổn | Đo query count và p95 trước/sau với bộ dữ liệu cố định; kết quả/giá/tồn không thay đổi; không đặt mục tiêu tốc độ tùy ý khi chưa đo baseline |

### Căn cứ source để người sửa kiểm tra nhanh

- R-01: `src/main/java/vn/techshop/reporting/ReportExportService.java`, `escapeCsv` và các trường recipient/address; `shared/Rules.java`, `text` chỉ kiểm độ dài.
- R-02: `order/OrderService.java:144–154,186`; `config/SecurityConfig.java:36`; `templates/order-lookup.html:65–72`. DTO nội bộ chứa pickupCode/paymentUrl nhưng template hiện tại **không** hiển thị hai trường đó; yêu cầu DTO riêng là phòng tránh mở rộng làm lộ chúng.
- R-03: `config/LoginAttempts.java:14–20`; `config/SecurityConfig.java:20–25,39–40`.
- R-04: `order/OrderService.java:164–175`; `order/PickupService.java:22–24`; `templates/order.html:3`. Đây là lỗi ngữ nghĩa/state, chưa có bằng chứng gateway bị trừ tiền hai lần.
- R-05: `order/PickupService.java:26–35`; `installment/InstallmentService.java:27–31`; `order/OrderService.java:103–113,156–162`.
- R-06: `order/OrderService.java:159`; `payment/PaymentService.java:108,184`.
- R-08: `payment/PaymentService.java:140–152`; IPN dùng exact code ở dòng 176.
- R-09: `warranty/WarrantyService.java:33–36`; `shared/Errors.java`, handler Exception.
- R-10: `templates/orders.html:1`, pagination; `config/WebController.java`, adminOrders; `order/OrderService.java:120–140`.
- R-11: `shared/FileStorageService.java:35–62`.
- R-12: `resources/application.yml:24–27,34–43`; `resources/application-demo.yml:11`; `order/OrderService.java:112`; `payment/PaymentService.java:161`.
- R-13: `shared/Errors.java`, unexpected; `.github/workflows/ci.yml`.
- R-14: `config/WebController.java:33–43`; `catalog/CatalogService.java:101–104`; `variant/ProductVariantService.java:45–50`; `order/OrderRoutingService.java:18`.

Không sửa bằng cách bỏ CSRF, bỏ ownership, tắt optimistic version hoặc nới toàn bộ payment/order endpoint thành public. Các bảo vệ có sẵn là yêu cầu hồi quy, không phải các lỗ hổng đã được phát hiện.

## 3. Chức năng còn thiếu và thứ tự hoàn thiện

Đối chiếu [spec gốc](01-spec.md) và [roadmap TechZone](16-techzone-implementation-plan.md). Roadmap giao diện có cả tính năng mới; một mục “Chưa làm” không tự động là lỗi của MVP gốc.

| ID | Ưu tiên | Hạng mục | Phụ thuộc / điều kiện hoàn tất |
| --- | --- | --- | --- |
| F-01 | P2 | Hồ sơ cá nhân thực, sửa tên/phone/ngày sinh/giới tính; đổi mật khẩu | Hiện `/profile` chỉ redirect `/member`, IdentityApi có GET me/register; bổ sung model/migration/API owner-only, refresh principal và thu hồi session theo policy khi đổi mật khẩu; TZ-14/15/17 |
| F-02 | P2 | Lọc hãng/dung lượng, giá và tồn theo đúng variant | Chuẩn hóa dữ liệu/migration, backend query và facet counts trước UI; không lọc hãng bằng đoán chuỗi tên; giữ filter qua pagination; TZ-05/06 |
| F-03 | P2 | Trang chi tiết, gallery/chọn variant, cart và checkout ổn định | Sửa R-04/05 trước; giá/tồn/album đúng SKU, hết hàng disabled, trạng thái chờ/lỗi rõ, buy-now không xóa giỏ cũ; TZ-07/08/09/19/21 |
| F-04 | P2 | Sổ địa chỉ, đơn của tôi và chi tiết đơn | CRUD owner-only, một default/user; order lưu snapshot; timeline/action theo enum thật; mua lại dùng giá/tồn hiện tại; TZ-16/24/25/26 |
| F-05 | P3 | Chọn một phần giỏ và xóa nhiều | Hợp đồng item IDs/preview hash/lock subset trước UI; checkout chỉ xóa dòng đã chọn, rollback giữ đủ giỏ; TZ-20 |
| F-06 | P3 | Wishlist, so sánh, thông tin cửa hàng và quản trị nội dung | Ownership/unique user-product; chuẩn hóa đơn vị thông số; dữ liệu giờ/ảnh/toạ độ thật; TZ-10/11/12/28/29/33 |
| F-07 | P3 | Guest checkout | Session/cart/proof truy cập đơn riêng, chống dò và idempotency; không mở API customer đang có; TZ-22 |
| F-08 | P3 / tích hợp | Quên mật khẩu/OTP/social login/avatar/voucher cá nhân | Token hết hạn/một lần/attempt limit; email/SMS/OAuth cần provider và cấu hình; không coi log demo là gửi thật; upload theo R-11; TZ-17/18/32 |
| F-09 | P3 / module mới | News/CMS/newsletter | Publish state, slug unique, xử lý nội dung HTML an toàn, opt-in; nội dung có nguồn; chỉ bắt đầu sau ổn định commerce; TZ-30/31/32 |

Ưu tiên F-01 đến F-04 cho đợt hoàn thiện chính. F-05 đến F-09 là đợt mở rộng; cấu hình provider/VNPay, chính sách guest/refund/trả góp phải được chốt trước phần tích hợp phụ thuộc chúng.

## 4. Các đợt thực hiện

Ước lượng dưới đây là **ngày công cho một người sửa và kiểm thử**, có thể thay đổi sau đợt 0; không phải cam kết ngày bàn giao. Không tính thời gian chờ provider hoặc phát sinh từ audit sâu.

| Đợt | Mục tiêu | Công việc và thứ tự | Ước lượng | Điều kiện chuyển đợt |
| --- | --- | --- | --- | --- |
| 0 | Chốt baseline và bản đồ lỗi | Lưu working-tree manifest/diff; chạy verify hiện tại; đọc route/quyền; tái hiện R-01/02/03/04/05/09/10; xác minh DB test an toàn | 0,5–1 ngày | Mỗi lỗi có scenario, expected/actual, evidence, priority; P0 nếu có phải đưa lên trước |
| 1 | Bảo vệ người dùng và admin | R-01 → R-02 → R-03 → R-12; mở rộng ma trận ownership/CSRF/role cho toàn bộ module | 2–3 ngày | Không còn lỗi bảo mật P1 đã xác nhận trong phạm vi; negative tests đạt; production config có checklist thực |
| 2 | Khóa tính đúng đắn tiền/đơn/kho | R-04 → R-05 → R-08; R-06 ledger thủ công trước tích hợp; R-07 trên H2/MySQL | 3–5 ngày | State machine, retry, cancellation, reserve/release, points/warranty invariant đạt |
| 3 | Chức năng chính và lỗi giao diện | R-09/10/11, F-01 → F-02 → F-03 → F-04; cập nhật hợp đồng API/UI từng nhóm | 3–5 ngày | Luồng customer/admin chạy hết bằng browser; dữ liệu ownership/giá/tồn đúng; không có nút giả |
| 4 | Nghiệm thu và vận hành | R-13/14, browser desktop/mobile, dependency scan, migration/backup/restore staging, UAT và docs | 2–3 ngày | Đạt tiêu chí mục 6; lỗi còn lại có mức độ và lý do hoãn |
| 5 | Mở rộng TechZone | F-05 đến F-09 theo roadmap; chia migration/backend/UI/test từng tính năng | Ước lượng sau chốt phạm vi | Mỗi chức năng tự nghiệm thu; provider live có bằng chứng riêng |

Đợt sửa lõi 0–4 khoảng **10,5–17 ngày công** ở phạm vi hiện tại. Refund tự động/provider live và module mới phải ước lượng riêng. Nếu phát hiện P0, tạm dừng mở rộng để xử lý và kiểm chứng P0 trước.

## 5. Cách thực hiện từng ticket

1. Lưu scenario lỗi bằng account/DB test riêng; mô tả rõ role, API/UI, dữ liệu đầu vào, trạng thái trước/sau và tác động.
2. Viết regression test cho bug nghiệp vụ/bảo mật, hoặc browser assertion phù hợp cho lỗi UI. Test phải thất bại trên hành vi lỗi trước khi sửa khi có thể.
3. Sửa một nhóm nhỏ, giữ thay đổi chưa commit của người dùng; không reset/ghi đè module để tạo baseline sạch giả.
4. Nếu thay schema, tạo migration mới tiếp sau version cao nhất hiện tại (đã kiểm tra tới V9), có bản MySQL và H2; không chỉnh migration đã áp dụng hoặc chạy SQL phá dữ liệu.
5. Chạy kiểm tra liên quan, rồi kiểm tra hồi quy theo tác động; H2 không thay cho kiểm chứng lock/constraint MySQL.
6. Sau sửa code chạy `graphify update .`. Nếu graph giảm lớn, xem removal audit trước khi force; không thay graph bằng bản thiếu source.
7. Cập nhật docs API/trạng thái và evidence cùng nhóm sửa; mỗi PR/commit chỉ đóng ticket đã đủ bằng chứng.

Trạng thái ticket: **Mở → Đã tái hiện → Đang sửa → Chờ nghiệm thu → Đã nghiệm thu**, hoặc “Hoãn có lý do”. Hiện R/F đều Mở; chưa đánh dấu đã sửa.

Mẫu ticket:

```text
ID / ưu tiên / loại bằng chứng:
Vai trò + điều kiện + dữ liệu test:
Bước tái hiện:
Expected / Actual:
Tác động và phạm vi:
File / API:
Cách sửa + phụ thuộc:
Test âm / retry / concurrency cần có:
Evidence mới + source manifest:
Kết quả / lý do còn mở:
```

## 6. Ma trận nghiệm thu bắt buộc

| Nhóm | Kiểm tra |
| --- | --- |
| Auth/quyền | Anonymous, customer A/B, admin; login đúng/sai, CSRF thiếu/sai, sở hữu cart/order/profile/review/QR; rate limit login/register/lookup; session expiry |
| Tiền/thanh toán | COD, MOCK test-only, VNPAY IPN đúng/sai chữ ký/merchant/amount/status, exact txn, callback trùng/đến muộn, return không ghi nhận tiền, INSTALLMENT approved/rejected |
| Đơn/kho | Đổi giá/tồn sau preview, double-submit cùng key, dùng lại key đổi payload, stale version; cạnh tranh last-unit/serial; cancellation/expiry không trả tồn hai lần |
| Đa kênh | Standard/2h/pickup, branch thiếu kho/ngừng hoạt động, tọa độ không hợp lệ, pickup hết hạn/QR sai/replay |
| Ưu đãi/hậu mãi | Coupon hết hạn/hết lượt, trade-in owner/đã dùng/hết hạn; tổng giảm không âm; điểm/bảo hành phát sinh một lần; refund không tạo tiền/điểm giả |
| Input/export/upload | Missing IDs và dữ liệu malformed trả đúng 4xx; CSV formula/control characters; file giả MIME/header, quá kích thước/pixel, sai quyền; không mất ảnh sau restart |
| Browser | Luồng mua và quản trị đầy đủ; loading/error/empty state; back/refresh/multiple tabs; viewport 1440/1024/768/390/320px; không tràn, không lỗi JS/network bất ngờ |
| Dữ liệu/ops | Fresh migration + upgrade trên bản sao DB; rollback có kế hoạch; backup DB và uploads + restore đo thực; staging HTTPS; scan dependency/secret có triage |

Điều kiện đóng đợt sửa lõi:

- Không còn P0/P1 **đã xác nhận** trong phạm vi nghiệm thu; phát hiện mới phải triage ngay, không hạ severity chỉ để đạt.
- Verify hiện tại thành công trên H2 và MySQL test riêng; browser E2E của source cuối thành công. Không sử dụng “x tests PASS” để kết luận mọi chức năng/bảo mật đã đầy đủ.
- Tiền, trạng thái đơn, stock ledger, branch inventory, IMEI, points và warranty khớp sau các luồng thành công/lỗi/retry.
- Mọi chức năng đưa lên UI có backend/validation/quyền và evidence; phần demo/provider chưa live được ghi đúng trạng thái.
- Có danh sách lỗi tồn, hướng dẫn vận hành và source manifest của lần nghiệm thu; production chỉ nghiệm thu khi các kiểm tra staging/backup/restore/config thực đạt.

## 7. Bằng chứng baseline và nguồn tham chiếu

Baseline chạy mới: `.\mvnw.cmd verify` kết thúc lúc **21:06:15 ngày 02/10/2026 (giờ Việt Nam)**, exit code 0, **BUILD SUCCESS; 59 tests, 0 failures, 0 errors, 0 skipped**, 9 test suites, khoảng 3 phút. Database dùng H2 in-memory. Chưa chạy lại MySQL/browser E2E trong lượt lập kế hoạch này.

Xem [baseline.json](../qa/repair-plan-20261002/baseline.json), [log verify](../qa/repair-plan-20261002/verify.log), [XML suites](../qa/repair-plan-20261002/surefire/) và [manifest source](../qa/repair-plan-20261002/source-manifest.json). Manifest chứa hash của 176 tệp source/config/scripts và được lấy sau verify; đây không phải snapshot trước khi chạy. Đây là bằng chứng chạy bộ test có sẵn; chưa phải bằng chứng những ticket R/F đã được sửa.

Nguồn kỹ thuật đã đối chiếu ngày 02/10/2026:

- [OWASP CSV Injection](https://community.owasp.org/attacks/CSV_Injection): quoting CSV không tự ngăn formula; cần chọn chiến lược theo công cụ nhận và kiểm tra lưu/mở lại.
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html): chống thử đăng nhập tự động, giới hạn theo account và logging/monitoring.
- [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html): kết hợp nhiều kiểm soát file; signature riêng lẻ không đủ.

Tài liệu này bổ sung kế hoạch sửa chữa; giữ nguyên các báo cáo và roadmap cũ để đối chiếu lịch sử.
