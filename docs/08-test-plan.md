# Kế hoạch kiểm thử và truy vết

## 1. Trạng thái và chiến lược

**Chưa thực thi kiểm thử ứng dụng: repository hiện chỉ có tài liệu và SQL thiết kế.** Các test dưới là kế hoạch, không phải báo cáo pass. Khi code được viết, lưu kết quả CI/UAT, môi trường và commit SHA thay vì đánh dấu đạt theo cảm tính.

- Unit: fee policy, canonical hashing, validation, state machine, money và thời gian với Clock cố định.
- MVC/security: route, session, CSRF, role, owner, error DTO/HTML.
- Integration MySQL 8.4 bằng Testcontainers: constraint, migration, checkout/cancel, lock/race/rollback.
- UI/UAT: toàn luồng thật trên staging; thao tác browser/keyboard tại 360px và 1440px.
- Load/operational: p95 có dataset, health, log không PII, backup restore.

Không dùng mock repository để chứng minh transaction/locking. Không dùng H2 để kết luận test InnoDB đã đạt. Đồng thời dùng executor + barrier/latch trên ít nhất hai DB connection; xác minh DB sau commit, không chỉ status HTTP.

## 2. Traceability và test cases

Tất cả TC ở trạng thái Planned. Critical = chặn release nếu fail.

| TC | Yêu cầu/AC | Setup và hành động | Kết quả bắt buộc | Cấp / critical |
|---|---|---|---|---|
| TC-01 | FR-01,AC-01 | Register email khác hoa/thường và có spaces với email đã có | 409, một user duy nhất | Integration / Có |
| TC-02 | FR-01/02,AC-02 | Register thêm role=ADMIN/userId | 400 field không hợp lệ, không có quyền admin | MVC / Có |
| TC-03 | FR-01,AC-03 | Login đúng/sai; logout; dùng session cũ | Đổi session id khi login; logout vô hiệu; lỗi login chung | Security / Có |
| TC-04 | FR-02,NFR-01 | CUSTOMER gọi admin và GUEST gọi cart | 403 và 401; không đọc/sửa dữ liệu | Security / Có |
| TC-05 | FR-03,AC-04 | Hàng/category inactive, gọi search/detail public | Không có trong list, detail404 | Integration / Có |
| TC-06 | FR-03,AC-05 | Cùng giá/ngày, page/sort, filter min/max | Sort có tie-breaker; size cap50; range sai400 | Integration / Không |
| TC-07 | FR-07,AC-06/07 | Add cùng product; quantity0/100; 21 dòng | Cộng đúng một dòng; input invalid400; dòng21 conflict | Integration / Có |
| TC-08 | FR-07,AC-08 | Hai sửa giỏ với cùng version | Một thắng, một409; version tăng một bước mỗi mutation | Concurrency / Có |
| TC-09 | FR-08,AC-09 | Subtotal999999/1000000 và zero-priced product | Fee30000/0; phép tính BigDecimal đúng | Unit / Có |
| TC-10 | FR-08,AC-10 | Hai user mua sản phẩm stock1 bằng hai connection | Một order, một409, stock0, một ORDER movement | Concurrency / Có |
| TC-11 | FR-09,AC-11 | Submit đồng thời và retry key/hash thành công sau clear cart | Cùng order id; một stock debit; 201 rồi200 replay | Concurrency / Có |
| TC-12 | FR-09,AC-12 | Key đã thành công, đổi địa chỉ hoặc expectedTotal | 409 KEY_REUSED; không order mới | Integration / Có |
| TC-13 | FR-08,AC-13 | Đổi giá sau preview; hai dòng đổi giá mà tổng giữ nguyên | 409 PRICE_CHANGED nhờ pricingHash; giỏ/tồn giữ nguyên | Integration / Có |
| TC-14 | FR-08,AC-14 | Inject exception sau insert item/stock movement trước commit | Không order/history/ledger mới, stock và giỏ như cũ | Integration / Có |
| TC-15 | FR-10/11,AC-15 | User A đọc/cancel order và sửa item của B | 404, không expose PII, dữ liệu B không đổi | Security / Có |
| TC-16 | FR-11,AC-16 | Owner cancel PENDING quantity2 | Stock+2, CANCEL movement đúng2, history/version+1 | Integration / Có |
| TC-17 | FR-11,AC-17 | Hai cancel và một retry sau CANCELLED | Hoàn tồn một lần; retry200, không movement/history mới | Concurrency / Có |
| TC-18 | FR-11/12,AC-18 | Confirm/cancel chạy đồng thời, kiểm tra cả hai interleaving | Một transition thắng; loser409; tồn theo state thắng | Concurrency / Có |
| TC-19 | FR-05,AC-19 | Hai admin edit metadata version cũ, cạnh tranh stock adjust | Stale409; metadata không ghi đè stock; SKU không đổi | Integration / Có |
| TC-20 | FR-06,AC-20 | Stock2, delta-3; delta0; adjustment dương | Negative409, zero400; valid có reason/actor/ledger | Integration / Có |
| TC-21 | FR-12,AC-21 | PENDING→DELIVERED, SHIPPED→CANCELLED, đi lùi | 409; state/history/tồn không đổi | Unit + Integration / Có |
| TC-22 | FR-12,AC-22 | SHIPPED→DELIVERED với false rồi true | false400; true đúng delivered_at/COD/history | Integration / Có |
| TC-23 | FR-13,AC-23/24 | Đơn tạo tháng trước giao tháng này; canceled order | Revenue theo delivered_at, canceled không tính | Integration / Có |
| TC-24 | FR-13,AC-25 | Order giao 16:59:59 và17:00:00 UTC biên ngày VN | Được chia đúng ngày/range half-open, không double count | Unit + Integration / Có |
| TC-25 | NFR-01 | Mutation thiếu/sai CSRF kể cả login/logout/register | 403; GET không gây mutation; cookie flags prod đúng | Security / Có |
| TC-26 | NFR-06/08 | Fresh MySQL, migrate, start, repeat migration | Không duplicate; schema validate pass; seed test riêng | Integration / Có |
| TC-27 | NFR-03 | 1000 products/10000 orders,20 concurrent users, cấu hình NFR-03 | Ghi p95/throughput/errors/query; không mất invariant | Load / Có với target đã thỏa thuận |
| TC-28 | NFR-04 | Keyboard/labels/empty/error ở360px và1440px | Không mất control, focus rõ, field lỗi hiểu được | Manual / Không |
| TC-29 | NFR-05/07 | Gây lỗi có requestId; kiểm tra log; backup và restore cô lập | Không PII/secrets; restore đối soát, ghi RPO/RTO thực | Operational / Có |
| TC-30 | FR-13,NFR-02 | Reconcile order totals, ledger sum, canceled movements | SQL quality checks trả0 rows lỗi | Integration / Có |
| TC-31 | FR-08/09 | Mất response sau commit, client retry payload y hệt | Trả đơn cũ, không trừ tồn hai lần | Integration / Có |
| TC-32 | FR-04/05 | Duplicate category code/SKU, category inactive, ảnh không allowlist | Unique409, public ẩn hàng, ảnh invalid400 | Integration / Có |
| TC-33 | FR-01/02 | >10 login thất bại/15 phút; user khác cùng IP | 429 theo policy; không lộ account; không khóa vô hạn | Security / Có |
| TC-34 | FR-08,NFR-02 | Multi-item checkout id khác thứ tự và stock adjustment/cancel cạnh tranh | Lock cùng thứ tự, không lost update/âm kho; timeout đúng409 | Concurrency / Có |

## 3. Dữ liệu kiểm thử

Chỉ dùng tài khoản và địa chỉ giả lập: CUSTOMER-A, CUSTOMER-B, ADMIN; không commit mật khẩu thật. Product X stock1, Y stock2, Z stock0, W inactive, category inactive, hàng giá999999 và1000000. Test tạo fixtures trực tiếp hoặc migration test-only với password hash tạo trong test.

Report fixtures gồm PENDING,CONFIRMED,SHIPPED,DELIVERED,CANCELLED; đơn giao hai bên biên UTC17:00; sản phẩm đổi tên/giá sau mua; nhiều items để phát hiện join nhân đôi tổng order. Không dùng fixture có delivered_at cho CANCELLED vì vi phạm schema.

## 4. Chiến lược kiểm thử race/rollback

1. Tạo DB fixture và đọc balance/version ban đầu.
2. Dùng transaction/connection độc lập, barrier để bắt đầu cạnh tranh thật; không dựa vào sleep đoán thời điểm.
3. Thu cả response/exception, chờ tất cả hoàn tất, đọc DB trong transaction mới.
4. Assert count orders/items/ledger/history, stock, versions và cart đúng rule.
5. Với rollback, inject failure qua dependency test-only, không đưa endpoint lỗi vào prod.
6. Với timeout/deadlock, ghi outcome minh bạch; test không được pass khi cả request đều chết mà không có bằng chứng về business rule.

## 5. UAT demo xuyên suốt

Khách đăng ký/login → tìm hàng → thêm/sửa giỏ → preview → đặt COD → xem order. Admin xác nhận → bàn giao → xác nhận đã thu COD và DELIVERED. Khách xem timeline; admin đối chiếu doanh thu. Một đơn thứ hai hủy khi PENDING để xác minh hoàn tồn. Trình diễn retry checkout và thử đọc đơn user khác.

Biên bản UAT ghi ngày, tester, commit, môi trường, expected/actual, screenshot hoặc video, lỗi và quyết định. Không dùng dữ liệu thật để trình diễn.

## 6. Severity và release gate

S1: lộ quyền/dữ liệu, sai tiền, âm kho, trùng đơn, mất dữ liệu. S2: luồng chính không hoàn thành/transition hoặc report sai. S3: trải nghiệm/UI hoặc lỗi phụ có workaround. Chặn release khi S1/S2 còn mở; S3 cần PO chấp nhận và ghi release note.

Gate: tất cả Critical TC chức năng pass; CI Maven verify xanh; migration cold start; UAT; SQL đối soát; backup restore; README chạy lại; performance mục tiêu có kết quả hoặc deviation được ghi và chấp nhận trước release. Báo cáo không được ghi pass cho TC chưa chạy.

## 7. Template kết quả sau triển khai

| TC | Commit | Môi trường | Expected | Actual | Pass/Fail/Not run | Evidence |
|---|---|---|---|---|---|---|
| TC-10 | Điền SHA | MySQL8.4 / 2 connections | Một thắng, stock0 | Điền sau chạy | Not run | CI link/video |
