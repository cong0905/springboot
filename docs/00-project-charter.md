# Project charter · TechShop

## 1. Bối cảnh và vấn đề

Dự án mô phỏng một cửa hàng công nghệ đang nhận đơn và kiểm tra tồn kho thủ công. Nhu cầu là có danh mục trực tuyến, giỏ hàng, đơn mua có lịch sử và màn hình quản trị thống nhất. Đây là giả định cho bài toán portfolio; chưa có khảo sát khách hàng hay số liệu bán hàng thật.

BA chịu trách nhiệm chuyển nhu cầu thành quy tắc và tiêu chí nghiệm thu. DA chịu trách nhiệm định nghĩa dữ liệu, KPI và kiểm tra báo cáo. Khi một người làm toàn bộ, vẫn tách các đầu ra này để tránh suy diễn tính năng hoặc kết quả phân tích.

## 2. Mục tiêu và dấu hiệu thành công

| ID | Mục tiêu | Bằng chứng |
|---|---|---|
| G-01 | Hoàn thành luồng mua hàng COD xuyên suốt | Kịch bản UAT từ đăng ký đến giao thành công, có đơn và lịch sử |
| G-02 | Không bán vượt tồn hoặc tạo trùng đơn | Test cạnh tranh mua sản phẩm cuối và gửi lại checkout |
| G-03 | Quyền truy cập nhất quán | Khách không vào admin và không đọc đơn người khác |
| G-04 | Báo cáo có định nghĩa, đối chiếu được | SQL doanh thu khớp tổng đơn DELIVERED trong cùng khoảng thời gian |
| G-05 | Project có thể bàn giao | README chạy thật, migration, CI, video demo và release checklist |

Mốc sáu tuần là mục tiêu kế hoạch, không phải cam kết ngày giao. Không đặt mục tiêu tăng doanh thu theo phần trăm khi chưa có dữ liệu nền.

## 3. Stakeholder và trách nhiệm

| Vai trò | Mối quan tâm | Người phụ trách dự kiến |
|---|---|---|
| Chủ dự án/Product Owner | Chốt phạm vi, nghiệm thu, ưu tiên | Công |
| Khách hàng | Tìm hàng, đặt hàng chính xác, theo dõi đơn | Persona giả định; Công thực hiện UAT |
| Admin cửa hàng | Sản phẩm, kho, đơn, doanh thu | Persona giả định |
| BA/DA | Yêu cầu, traceability, mô hình dữ liệu và KPI | Công với bộ tài liệu này làm baseline |
| Developer | Backend, giao diện, tích hợp | Công; có thể chia lại khi lập nhóm |
| QA/Reviewer | Kiểm thử rủi ro, review độc lập | Công; nhờ đồng đội review nếu có |

Không gán nhiệm vụ cho các thành viên từ dự án khác. Nếu làm nhóm, phân công lại theo module và năng lực trước khi bắt đầu.

## 4. Phạm vi

**Must:** tài khoản/session; danh mục và sản phẩm; tìm/lọc/phân trang; giỏ hàng theo tài khoản; checkout COD; trừ/hoàn tồn; trạng thái và lịch sử đơn; admin; báo cáo vận hành; kiểm thử và hướng dẫn chạy.

**Should:** giao diện responsive, lịch sử điều chỉnh kho, skeleton/loading thích hợp, dữ liệu demo giả lập, đo thời gian xử lý và query.

**Could sau MVP:** mã giảm giá, đánh giá sau mua, email, upload ảnh, lưu nhiều địa chỉ, reset mật khẩu qua email.

**Ngoài v0.1:** JWT/mobile, microservices, Redis/Kafka, nhiều người bán/kho, thanh toán thực, đơn vị vận chuyển, hóa đơn tài chính, đổi trả/bảo hành, kết nối kế toán. Thất bại giao hàng sau SHIPPED chưa có quy trình trong MVP; phải bổ sung trước khi dùng cho cửa hàng thật.

## 5. Giả định dùng để tiếp tục triển khai

| ID | Giả định/đề xuất | Ảnh hưởng nếu thay đổi |
|---|---|---|
| A-01 | Một cửa hàng, một kho; mỗi sản phẩm có một SKU, không biến thể | Nhiều kho/biến thể cần bảng inventory/SKU riêng |
| A-02 | Khách phải đăng nhập trước khi tạo giỏ hàng và đặt đơn | Guest checkout cần quản lý giỏ và xác minh riêng |
| A-03 | COD duy nhất; DELIVERED đồng nghĩa admin đã xác nhận thu tiền | Thanh toán online cần payment ledger/webhook/idempotency riêng |
| A-04 | VND nguyên; giá niêm yết là giá cuối của bài toán demo | Thuế/hóa đơn thật cần xác nhận chuyên môn ngoài spec |
| A-05 | Phí giao cố định 30.000đ; miễn phí khi subtotal ≥ 1.000.000đ | Cần thay policy và test ranh giới nếu có biểu phí khác |
| A-06 | Khách hủy PENDING; admin hủy PENDING hoặc CONFIRMED | Sau SHIPPED cần quy trình hàng trả về, không tự hoàn tồn |
| A-07 | Tối đa 20 dòng/giỏ; mỗi dòng 1–99 sản phẩm | Thay giới hạn cần cập nhật API, UI và kiểm thử |
| A-08 | 1 developer, 6 ngày/tuần, 3 giờ/ngày, 6 tuần | 108 giờ; người mới cần 8–10 tuần hoặc giảm phạm vi |
| A-09 | Ảnh là đường dẫn asset được admin chọn từ bộ ảnh demo | Upload là phase 2; phải thêm kiểm tra tệp và lưu trữ |
| A-10 | Website tiếng Việt; DB lưu UTC, báo cáo ngày Việt Nam | Chuyển múi giờ cần cập nhật query và kiểm thử biên ngày |

Các giả định là baseline để làm việc, không có nghĩa đã thu thập và xác nhận yêu cầu thực tế. Thay đổi được ghi theo quy trình CR ở development plan.

## 6. Rủi ro và cách xử lý

| Rủi ro | Khả năng/tác động | Cách xử lý | Chủ trì |
|---|---|---|---|
| Phạm vi mở rộng vì thêm thanh toán/chat | Cao/Cao | Khoá Must, đưa đề xuất vào phase 2 | PO/BA |
| Thiếu nền tảng Spring Security/JPA | Trung bình/Cao | Lát cắt sớm, dự phòng 18 giờ, giãn lịch nếu cần | Dev |
| Race condition làm âm kho hoặc trùng đơn | Trung bình/Cao | Transaction, lock, unique key, test MySQL thật | Dev/QA |
| Báo cáo doanh thu sai vì lấy ngày tạo đơn | Trung bình/Cao | DELIVERED theo delivered_at, kiểm thử đối chiếu | DA |
| Môi trường triển khai chưa chọn | Trung bình/Trung bình | Chốt dịch vụ có Java/container và MySQL ở tuần 5; chưa giả định miễn phí | Dev |
| Quy trình giao thất bại/đổi trả chưa có | Cao/Cao nếu vận hành thật | Ghi giới hạn MVP; yêu cầu phase mới trước launch kinh doanh | PO |
| Sai lệch tài liệu, schema, API | Trung bình/Trung bình | Traceability, mỗi PR cập nhật spec cùng code | BA/Dev |

## 7. Đầu ra và điều kiện bàn giao

Đầu ra hiện tại: bộ Markdown, Mermaid, DDL tham chiếu, SQL báo cáo, backlog và test plan. Đầu ra sau phát triển: source chạy được, migration, seed demo riêng, pipeline, bản demo, video 3–5 phút, biên bản UAT và release note. Không coi bộ thiết kế hiện tại là hệ thống đã triển khai.
