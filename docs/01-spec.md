# SRS · Đặc tả yêu cầu TechShop v0.1

Nguồn phạm vi: [charter](00-project-charter.md). Các con số nghiệp vụ là giả định A-01–A-10. Từ khóa MUST trong tài liệu chỉ mức bắt buộc của thiết kế dự kiến.

## 1. Actor, thuật ngữ và quyền

GUEST là người chưa đăng nhập; CUSTOMER là tài khoản mua hàng; ADMIN là nhân sự quản trị. Một tài khoản có một role. ADMIN không dùng luồng mua hàng trong v0.1; nếu cần mua, dùng tài khoản CUSTOMER riêng.

SKU là mã duy nhất của sản phẩm. Stock là số lượng còn có thể bán; hàng được giữ bằng cách trừ stock ngay lúc đặt đơn thành công. Subtotal là tổng tiền hàng theo giá snapshot; total = subtotal + shipping_fee. PENDING là đơn đã đặt, chưa xác nhận; DELIVERED là giao thành công và đã xác nhận thu COD.

| Năng lực | GUEST | CUSTOMER | ADMIN |
|---|---|---|---|
| Xem sản phẩm ACTIVE thuộc danh mục ACTIVE | Có | Có | Có |
| Đăng ký | Có | Không cần | Không cần |
| Giỏ hàng/checkout | Đăng nhập trước | Giỏ của mình | Không |
| Xem/hủy đơn khách | Không | Đơn của mình, hủy PENDING | Quản trị qua route admin |
| Sửa danh mục/sản phẩm/tồn kho | Không | Không | Có |
| Cập nhật trạng thái đơn/báo cáo | Không | Không | Có |

Không cho phép đăng ký role ADMIN từ request. Tài khoản admin đầu tiên tạo qua tác vụ bootstrap được vận hành kiểm soát, không có mật khẩu cố định trong source.

## 2. Danh sách yêu cầu chức năng

| ID | Yêu cầu | Ưu tiên | Nghiệm thu tóm tắt |
|---|---|---|---|
| FR-01 | Đăng ký, đăng nhập, logout bằng session | Must | Email unique, hash mật khẩu, logout vô hiệu session |
| FR-02 | Kiểm tra role và quyền sở hữu | Must | Khách không truy cập admin/đơn người khác |
| FR-03 | Danh sách/chi tiết sản phẩm, tìm/lọc/sắp xếp/phân trang | Must | Chỉ công khai hàng và danh mục ACTIVE; thứ tự ổn định |
| FR-04 | Admin CRUD danh mục | Must | Mã danh mục unique; có thể ngừng hoạt động, không xoá cứng |
| FR-05 | Admin quản lý sản phẩm/ảnh tham chiếu | Must | SKU unique, giá không âm, ảnh từ bộ asset cho phép |
| FR-06 | Điều chỉnh tồn kho và ghi ledger | Must | Không âm kho; có actor, lý do, delta và tồn sau thay đổi |
| FR-07 | Giỏ hàng bền vững theo tài khoản | Must | Tối đa 20 dòng, 1–99/dòng; không giữ hàng trước checkout |
| FR-08 | Preview giá và đặt đơn COD | Must | Backend tính tiền; snapshot; tồn kho cập nhật nguyên tử |
| FR-09 | Chống đặt đơn trùng | Must | Cùng key + payload trả cùng order; payload khác trả 409 |
| FR-10 | Xem lịch sử/chi tiết đơn cá nhân | Must | Chỉ chủ đơn đọc; snapshot không đổi theo catalog |
| FR-11 | Hủy đơn hợp lệ và hoàn tồn đúng một lần | Must | Customer chỉ PENDING; admin thêm CONFIRMED |
| FR-12 | Admin xử lý trạng thái, lịch sử đơn | Must | Đúng state machine, chống cập nhật đồng thời |
| FR-13 | Dashboard vận hành và báo cáo | Must | Doanh thu theo DELIVERED và delivered_at; bộ lọc thời gian rõ |

## 3. Quy tắc nghiệp vụ

| ID | Quy tắc |
|---|---|
| BR-01 | Email trim và lowercase trước kiểm tra/lưu; unique database là lớp bảo vệ cuối cùng |
| BR-02 | Đăng ký chỉ tạo CUSTOMER; mật khẩu không lưu hoặc log dạng rõ |
| BR-03 | Product INACTIVE hoặc category INACTIVE không xuất hiện công khai và không checkout được |
| BR-04 | Giỏ không giữ tồn; số lượng trong giỏ có thể vượt tồn hiện tại, UI cảnh báo, checkout phải từ chối nếu thiếu |
| BR-05 | VND dùng BigDecimal và DECIMAL(15,0); input giá nguyên, tổng dòng = snapshot_price × quantity |
| BR-06 | shipping_fee = 0 khi subtotal ≥ 1.000.000, ngược lại 30.000; server quyết định |
| BR-07 | Checkout kiểm tra cartVersion và giá preview; thay giỏ/giá/phí trả 409, khách xem preview mới trước khi đặt lại |
| BR-08 | Transaction checkout khoá cart trước, sau đó các product theo id tăng dần; lấy giá/status/tồn mới từ bản ghi khoá; không âm stock |
| BR-09 | Trong một transaction: order + items + histories + stock movements + giảm stock + xoá cart items + tăng cartVersion; lỗi thì rollback toàn bộ |
| BR-10 | Key UUID scope theo user, unique(user_id, checkout_key); hash request canonical; cùng key/hash trả order cũ trước kiểm tra giỏ mới |
| BR-11 | Snapshot gồm SKU, tên, đơn giá, số lượng, tên người nhận, điện thoại, địa chỉ và phí giao; dữ liệu đơn không sửa sau đặt |
| BR-12 | Customer chỉ hủy PENDING; admin hủy PENDING/CONFIRMED, lý do 5–500 ký tự; trạng thái khác trả 409 |
| BR-13 | Cancel khoá order rồi product id tăng dần; status + version kiểm tra trong lock; hoàn stock/ledger/history cùng transaction; CANCELLED lặp lại cùng yêu cầu hợp lệ trả order cũ |
| BR-14 | Chỉ chuyển PENDING → CONFIRMED → SHIPPED → DELIVERED; PENDING/CONFIRMED → CANCELLED; không đi lùi/bỏ bước |
| BR-15 | DELIVERED yêu cầu admin xác nhận codCollected=true; ghi delivered_at; trạng thái cuối không sửa qua API |
| BR-16 | Thay đổi giá/tên không tác động order_items; hàng đã bán không xoá cứng; SKU không tái sử dụng |
| BR-17 | Product editing dùng version, stock thay đổi chỉ qua InventoryService; cập nhật giá không ghi đè stock |
| BR-18 | Mọi timestamp lưu UTC với microsecond; ngày báo cáo chuyển từ Asia/Ho_Chi_Minh sang khoảng UTC nửa mở |

Category đổi sang INACTIVE không sửa product.status. Checkout kiểm tra category ở thời điểm validation; product rows được khoá và kiểm tra trước commit. Nếu cần đảm bảo category không thay đổi trong toàn transaction, phase thực thi phải khoá category theo quy tắc lock tổng quát và bổ sung test; bản MVP định nghĩa quyết định category theo validation point, không cam kết đồng bộ cứng với thao tác admin diễn ra sau đó.

## 4. Use case và tiêu chí Given–When–Then

### UC-01 · Đăng ký và đăng nhập (FR-01/02)

Tiền điều kiện: chưa đăng nhập. Luồng: nhập thông tin → validate → chuẩn hóa email → hash → tạo CUSTOMER → chuyển trang login → đăng nhập → đổi session id → chuyển trang phù hợp. Lỗi trùng email trả thông báo không tạo tài khoản thứ hai; đăng nhập sai dùng thông báo chung, không tiết lộ email có tồn tại.

- AC-01: Given email đã tồn tại với khác hoa/thường, When đăng ký, Then bị từ chối và số user không tăng.
- AC-02: Given khách đăng ký với role=ADMIN được thêm vào payload, When submit, Then không cấp quyền admin và request field không hợp lệ bị từ chối.
- AC-03: Given session đang đăng nhập, When logout POST với CSRF hợp lệ, Then session không truy cập được tài nguyên riêng tư nữa.

### UC-02 · Tìm sản phẩm (FR-03)

Luồng: tìm keyword → chọn category/giá/sort → xem page → mở chi tiết. Giá từ 0 trở lên, minPrice ≤ maxPrice, page zero-based, size 1–50, default 12. Keyword ≤ 100 ký tự; trim; tìm chuỗi theo tên, không xem keyword là regex/SQL. Sort allowlist: newest, price_asc, price_desc; cùng giá/ngày dùng id làm tie-breaker. Không có kết quả là trang rỗng có hướng dẫn bỏ lọc.

- AC-04: Given hàng/category INACTIVE, When search hoặc gọi detail public, Then không lộ hàng (detail 404).
- AC-05: Given nhiều sản phẩm cùng giá, When đi giữa các page trong dữ liệu cố định, Then kết quả theo price và id không bị trùng do thứ tự không xác định.

### UC-03 · Quản lý giỏ hàng (FR-07)

Tiền điều kiện CUSTOMER. Mỗi user có một cart được tạo khi đăng ký. Add cùng product cộng quantity; set quantity là giá trị tuyệt đối. Khoá cart khi mutation, increment version; với request version cũ trả 409. Giá hiển thị lấy catalog hiện tại; giỏ lưu product và quantity, không lưu giá làm giá thanh toán.

- AC-06: Given giỏ đã có 2 sản phẩm X, When thêm 3 X, Then một dòng X có quantity=5.
- AC-07: Given quantity=0 hoặc >99, When add/update, Then 400, cart không đổi.
- AC-08: Given cartVersion cũ, When sửa giỏ, Then 409 và UI yêu cầu reload.

### UC-04 · Đặt đơn (FR-08/09)

Tiền điều kiện CUSTOMER, giỏ không rỗng. Preview trả version, danh sách dòng, đơn giá hiện tại, subtotal, shipping_fee, total và pricingHash. Khách nhập thông tin giao, xác nhận tổng và submit key UUID + version + expectedTotal + expectedPricingHash. Server canonicalizes thông tin, khoá cart, tìm order theo key. Nếu key đã thành công và hash khớp, trả order cũ. Nếu chưa có, kiểm tra version, khoá sản phẩm theo id, validate stock/status/category/giá, tính lại total và pricingHash; giá từng dòng hoặc phí thay đổi đều trả 409, kể cả khi total tình cờ không đổi. Tạo đơn và trừ tồn trong một transaction, thành công chuyển trang chi tiết đơn qua POST/Redirect/GET.

- AC-09: Given subtotal=999.999, When preview, Then shipping_fee=30.000; Given subtotal=1.000.000, Then fee=0.
- AC-10: Given stock X=1 và hai user đồng thời mua 1 X, When commit, Then một đơn thành công, một 409; stock=0.
- AC-11: Given đơn đã tạo, When retry cùng key/hash dù giỏ đã trống, Then cùng order id, không trừ tồn lần hai.
- AC-12: Given cùng key nhưng địa chỉ/expectedTotal/version khác, When submit, Then 409 CHECKOUT_KEY_REUSED.
- AC-13: Given giá thay sau preview, When submit, Then 409 PRICE_CHANGED và không tạo đơn/trừ tồn.
- AC-14: Given lỗi DB sau khi ghi một order_item, When rollback, Then không có đơn, không giảm stock, giỏ còn nguyên.

Không gọi dịch vụ mạng trong transaction. Mất kết nối sau commit được phục hồi bằng retry cùng key. Khi đổi payload sau lỗi xung đột, UI tạo key mới. Key thành công được giữ cùng vòng đời order, không có TTL trong v0.1.

### UC-05 · Xem và hủy đơn (FR-10/11)

Khách mở danh sách đơn của mình, xem snapshot và timeline. Hủy nhập lý do, submit expectedVersion. Server kiểm tra ownership trước lock/đọc dữ liệu trả cho khách. Một đơn không thuộc khách trả 404. Order CANCELLED lặp lại trả trạng thái cũ sau kiểm tra ownership, không tạo stock movement mới; status khác và version cũ trả 409.

- AC-15: Given user A yêu cầu order của B, When detail/cancel, Then 404, không thay đổi dữ liệu.
- AC-16: Given đơn PENDING quantity=2, When hủy thành công, Then stock tăng 2, một CANCEL ledger/dòng, history có actor/reason.
- AC-17: Given hai cancel đồng thời, When hoàn tất, Then chỉ một lần hoàn tồn.
- AC-18: Given admin confirm và customer cancel đồng thời, When commit, Then chỉ một transition hợp lệ thắng; nhánh thua 409, không hoàn tồn sai.

### UC-06 · Quản trị (FR-04/05/06/12)

Admin quản lý danh mục, sản phẩm và stock từ form riêng. Không đặt stock trực tiếp trong DTO sửa sản phẩm. Điều chỉnh delta khác 0, lý do bắt buộc, tồn sau ≥0. V0.1 không cho sửa SKU sau tạo; ngừng bán thay xoá. Admin cập nhật trạng thái kèm expectedVersion; actor được lấy từ session.

- AC-19: Given product version cũ, When lưu thay đổi, Then 409, không ghi đè chỉnh sửa mới.
- AC-20: Given stock=2 và adjustment=-3, When submit, Then 409, không có ledger.
- AC-21: Given PENDING, When chuyển trực tiếp DELIVERED, Then 409 INVALID_ORDER_TRANSITION.
- AC-22: Given SHIPPED, When chuyển DELIVERED với codCollected=false, Then 400; true thì ghi delivered_at UTC.

### UC-07 · Báo cáo (FR-13)

Admin chọn ngày bắt đầu/kết thúc theo ngày Việt Nam; mặc định 7 ngày tính cả hôm nay; tối đa 366 ngày. Doanh thu hàng là subtotal đơn DELIVERED theo delivered_at; phí giao tách riêng; total thu COD = subtotal + phí. Đơn bị hủy không tính doanh thu. Không gọi số đơn đặt là doanh thu.

- AC-23: Given đơn đặt tháng trước nhưng giao tháng này, When xem tháng này, Then doanh thu thuộc tháng này.
- AC-24: Given đơn CANCELLED, When báo cáo bán chạy, Then không tính quantity của đơn đó.
- AC-25: Given đơn DELIVERED lúc 17:30 UTC ngày D, When xem ngày D+1 tại Việt Nam, Then đơn được tính đúng ngày địa phương.

## 5. Dữ liệu vào và validation

| Field | Quy tắc |
|---|---|
| fullName/recipientName | trim, 2–100 ký tự; không HTML |
| email | hợp lệ, tối đa 254 ký tự; normalize lowercase |
| password | 10–64 ký tự; baseline dùng BCrypt, tối đa 72 byte UTF-8 để tránh truncation; không trim |
| phone | 10 chữ số, bắt đầu 0, dùng chuỗi; quy tắc demo số điện thoại Việt Nam |
| address | trim, 10–500 ký tự, nội dung địa chỉ đầy đủ; v0.1 chưa mã hoá tỉnh/quận |
| productName | trim, 2–200 ký tự |
| SKU/categoryCode | uppercase, 3–50 ký tự, A–Z/0–9/dấu gạch ngang; unique |
| price | VND nguyên, 0–999.999.999; không dùng float/double |
| description | plain text, tối đa 5.000 ký tự |
| reason/note | lý do 5–500; note tùy chọn ≤500 ký tự |
| checkoutKey | UUID canonical; expectedTotal nguyên ≥0 |
| imagePath | đường dẫn /assets/products/ thuộc asset allowlist; không nhận URL tùy ý |

API từ chối field không thuộc request schema; server không nhận userId, actorId, role hoặc stockBalance từ client để quyết định quyền/nghiệp vụ.

## 6. Màn hình và trạng thái UI

| Trang | Nội dung/điều khiển | Rỗng/lỗi |
|---|---|---|
| Trang chủ/danh sách | Keyword, category, giá, sort, page; thẻ tên/ảnh/giá/tồn | Không có kết quả; bỏ lọc |
| Chi tiết sản phẩm | Ảnh, mô tả, quantity, thêm giỏ | Hết hàng không checkout; hàng ẩn 404 |
| Register/login | Field, validation, link qua lại | Lỗi form tại field; auth lỗi chung |
| Giỏ hàng | Quantity, xóa, tiền tạm tính, cartVersion | Giỏ trống; hàng ngừng bán; version xung đột |
| Checkout | Snapshot preview, thông tin giao, COD, tổng, key | Giá/stock thay: giữ địa chỉ, reload preview, yêu cầu xác nhận lại |
| Đơn của tôi/chi tiết | Mã, trạng thái, snapshot, timeline, hủy khi PENDING | 404 nếu không phải chủ đơn |
| Admin catalog/kho | Table, filter, form, version; điều chỉnh delta/reason | Trùng SKU/code, stale version, negative stock |
| Admin orders | Lọc trạng thái, chi tiết, action hợp lệ, xác nhận COD | Conflict phải reload, không tự retry transition |
| Admin dashboard | Ngày, KPI, top product, đơn theo trạng thái | Không dữ liệu hiển thị 0; lỗi query báo lỗi, không giả lập số |

POST form thành công dùng redirect 303. Lỗi form render lại với dữ liệu không nhạy cảm; không trả lại password. Disable nút khi submit giúp UX nhưng không thay thế idempotency phía server.

## 7. Yêu cầu phi chức năng

| ID | Yêu cầu/target dự kiến | Cách xác minh |
|---|---|---|
| NFR-01 | Role + ownership ở server; CSRF cho mọi mutation; cookie HttpOnly/Secure trên HTTPS, SameSite=Lax | Security integration tests |
| NFR-02 | Không âm tồn/đơn trùng; transaction nguyên tử | MySQL concurrency + rollback tests |
| NFR-03 | Mục tiêu p95 catalog ≤500ms, checkout ≤1.000ms; 20 concurrent users, 1.000 sản phẩm/10.000 đơn, app 2 vCPU/2GB + DB tương đương, cùng vùng, warm-up 2 phút, chạy 5 phút | Load test ghi cả cấu hình thực tế; đây chưa là kết quả đo |
| NFR-04 | UI dùng được tại 360px và 1440px; field có label, keyboard, focus và thông báo lỗi | Manual responsive/accessibility checklist |
| NFR-05 | Request id, log lỗi, lịch sử order/stock; không log password/session/địa chỉ/điện thoại | Review log/observability |
| NFR-06 | Dev/test/prod tách cấu hình; secrets từ biến môi trường; Flyway, JPA validate | Cold start test |
| NFR-07 | Thiết kế phục hồi demo RPO 24h, RTO 2h sau khi có backup; không cam kết SLA dịch vụ | Restore drill có thời gian đo |
| NFR-08 | Maven verify, kiểm thử core bắt buộc; critical test phải pass; mục tiêu coverage core service ≥80% khi có code | CI report, không áp tỷ lệ cho DTO/generated |

## 8. Điều kiện nghiệm thu MVP

FR-01–FR-13 có bằng chứng theo [test plan](08-test-plan.md), luồng happy path và các cạnh tranh kho/đơn pass, không còn lỗi nghiêm trọng làm sai tiền/tồn/quyền, người khác chạy từ README thành công. Mọi NFR chưa đo phải ghi “chưa đo”, không đổi thành “đạt”.
