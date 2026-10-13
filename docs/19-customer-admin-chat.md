# Chat khách hàng – admin

Khách hàng đăng nhập mở **Chat hỗ trợ** hoặc `/chat`, gửi câu hỏi và xem lại phản hồi. Admin mở **Tin nhắn** trong thanh quản trị hoặc `/admin/chat`, chọn khách hàng và trả lời. Nút chat xuất hiện trong cửa hàng; tài khoản đăng nhập nhìn thấy số tin chưa đọc.

- Mỗi khách hàng có một cuộc trò chuyện riêng. Hộp thư và trạng thái đã đọc của đội admin được dùng chung giữa các admin.
- Lưu lịch sử trong database. Trang tải 50 tin gần nhất và có nút xem tin cũ; hộp thư phân trang 30 cuộc trò chuyện.
- Trang chat tự lấy tin mỗi 3 giây khi tab hiện; badge ở trang khác cập nhật mỗi 15 giây. Đây là polling HTTP, chưa có WebSocket hay thông báo đẩy ngoài trình duyệt.
- Nội dung văn bản tối đa 2.000 ký tự; Enter gửi, Shift + Enter xuống dòng. Nội dung HTML hiển thị như chữ thường.
- Gửi lại cùng `clientMessageId` và nội dung không tạo tin trùng. Giới hạn 30 tin mới/phút/tài khoản (giới hạn trong mỗi instance ứng dụng).
- Khách khác không truy cập được tin nhắn của nhau. Sender và vai trò lấy từ phiên đăng nhập, không nhận từ client. Account khách vãng lai bị chặn. POST luôn yêu cầu CSRF.
- Mốc đã đọc chỉ tăng tới ID thật thuộc cuộc trò chuyện, giữ tin đến sau mốc đó ở trạng thái chưa đọc. Các thao tác gửi dùng khóa cuộc trò chuyện và READ_COMMITTED để tránh snapshot cũ khi chạy MySQL.

## API

| Khách hàng | Mục đích |
| --- | --- |
| GET/POST `/api/v1/chat/messages` | Đọc/gửi trong cuộc trò chuyện của chính mình |
| POST `/api/v1/chat/read` | Đánh dấu đã đọc `{upToId}` |
| GET `/api/v1/chat/unread` | Số tin admin gửi chưa đọc |

| Admin | Mục đích |
| --- | --- |
| GET `/api/v1/admin/chat/conversations?page=0` | Hộp thư: items, page, totalPages, totalElements |
| GET/POST `/api/v1/admin/chat/conversations/{id}/messages` | Lịch sử/gửi trả lời |
| POST `/api/v1/admin/chat/conversations/{id}/read` | Đánh dấu đã đọc `{upToId}` |
| GET `/api/v1/admin/chat/unread` | Tổng tin khách gửi chưa đọc |

POST messages nhận `{content, clientMessageId}` với UUID được giữ nguyên khi retry. GET messages nhận `afterId` **hoặc** `beforeId` (không dùng đồng thời). Kết quả gồm messages, hasMore và hai mốc đã đọc. Không cache các phản hồi chat.

## Chạy và cập nhật

Migration Flyway `V25__customer_admin_chat.sql` có ở cả `db/migration` (MySQL) và `db/demo` (H2); tự chạy khi khởi động ứng dụng. Database MySQL giữ lịch sử qua các lần khởi động. Profile demo dùng H2 trong bộ nhớ nên dữ liệu demo mất khi tắt.

```powershell
.\mvnw.cmd verify
# Chạy theo cấu hình local hiện có của bạn; cần khởi động lại Java để nạp backend mới.
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Kiểm tra chat độc lập: `.\mvnw.cmd '-Dtest=ChatIntegrationTest' test`. QA trình duyệt: chạy demo trên cổng 8096, đặt PLAYWRIGHT_MODULE tới Playwright đã cài, rồi `node scripts/chat_qa.cjs`. Script QA chỉ dùng demo độc lập, tạo tài khoản test và tin kiểm thử; không chạy trên database khách thật. Chứng cứ lưu ở `qa/chat/`.
