# Chat AI Gemini và chuyển hỗ trợ sang admin

## Trạng thái bàn giao

Đã triển khai backend, giao diện khách/admin và migration V26 cho MySQL/H2. AI tắt mặc định. Khi chưa có API key/model, khách vẫn chat với nhân viên bình thường; giao diện ghi “AI chưa được cấu hình”.

Kiểm thử Gemini dùng gateway/HTTP giả lập, không dùng key thật và không gọi Google. Chỉ xác nhận kết nối Gemini thật sau khi cấu hình key và chạy smoke test với model mà tài khoản của bạn có quyền truy cập.

## Cấu hình khi có API key

Key nằm ở backend, không đưa vào JavaScript, HTML, Git hoặc tin nhắn chat. Tạo key trong [Google AI Studio](https://aistudio.google.com/apikey), kiểm tra quyền truy cập và chọn model còn được hỗ trợ từ [danh sách model chính thức](https://ai.google.dev/gemini-api/docs/models).

Ứng dụng dùng REST `generateContent` và JSON schema. Không cố định tên model vì khả năng truy cập phụ thuộc tài khoản. Model được chọn phải hỗ trợ `generateContent` và structured output.

### Chạy bằng IDE / Maven trên Windows

Thêm biến môi trường vào Run Configuration của IDE, hoặc vào cửa sổ PowerShell dùng để khởi động app:

```powershell
$env:APP_AI_ENABLED='true'
$env:GEMINI_MODEL='<model-ban-duoc-quyen-su-dung>'
$secureKey=Read-Host 'Gemini API key' -AsSecureString
$env:GEMINI_API_KEY=[System.Net.NetworkCredential]::new('', $secureKey).Password
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=local'
```

Khởi động lại backend sau khi thay cấu hình. Spring Boot không tự đọc `.env` khi chạy Maven/IDE. Không chạy hai backend cùng port; dừng phiên hiện có rồi khởi động lại bằng cấu hình phù hợp.

### Chạy bằng Docker Compose

Điền `APP_AI_ENABLED=true`, `GEMINI_API_KEY` và `GEMINI_MODEL` trong file `.env` cục bộ (đã được Git bỏ qua), rồi tạo lại service `app` bằng cấu hình Compose hiện có. `.env.example` chứa tên biến và giá trị mặc định, không chứa key thật.

### Biến cấu hình

| Biến | Mặc định | Ý nghĩa |
|---|---|---|
| `APP_AI_ENABLED` | `false` | Bật khả năng dùng AI |
| `GEMINI_API_KEY` | trống | Key chỉ được gửi trong header tới Google |
| `GEMINI_MODEL` | trống | ID model Google được phép dùng |
| `APP_AI_USER_TURNS_PER_MINUTE` | 2 | Lượt hỏi AI của một khách/phút |
| `APP_AI_USER_TURNS_PER_DAY` | 20 | Lượt hỏi AI của một khách/ngày |
| `APP_AI_GLOBAL_REQUESTS_PER_MINUTE` | 10 | Tổng HTTP request Gemini/phút |
| `APP_AI_GLOBAL_REQUESTS_PER_DAY` | 200 | Tổng HTTP request Gemini/ngày |

Một lượt tư vấn sản phẩm thường dùng hai HTTP request: phân tích nhu cầu, sau đó viết câu trả lời từ sản phẩm hợp lệ. Lượt HANDOFF có thể chỉ dùng một request. Mỗi lần gọi/retry đều trừ hạn mức request; request lỗi cũng được tính. Đây là giới hạn ứng dụng, không thay thế quota/billing của Google. Bộ đếm được lưu database; ngày tính theo Asia/Ho_Chi_Minh.

Thiết lập backend bổ sung: `app.ai.turn-timeout-seconds` (mặc định 30, tối đa 60), `app.ai.max-output-tokens` (800), `app.ai.max-concurrent-calls` (2, tối đa 4), `app.ai.worker-enabled` (true). Không hiển thị key trên endpoint trạng thái.

## Luồng sử dụng

1. Khách đăng nhập, mở `/chat`, bấm **Tư vấn bằng AI**, đọc thông báo và chọn đồng ý gửi câu hỏi tới Gemini.
2. Khách gửi câu hỏi. Tin được lưu ngay; AI xử lý nền và trang lấy phản hồi mỗi 3 giây. Phản hồi có nhãn **Trợ lý AI · Gemini**.
3. Backend dùng bộ lọc được xác thực để tìm tối đa 8 sản phẩm công khai, đang hoạt động và có tồn khả dụng. Thẻ sản phẩm lấy giá từ/tồn khả dụng/đường dẫn từ database; không lấy giá hoặc URL do AI tự sinh. Thông tin là ảnh chụp tại lúc tư vấn; giá của phiên bản cụ thể được xem trên trang sản phẩm.
4. Khách bấm **Gặp nhân viên** hoặc AI gặp yêu cầu riêng tư/thiếu dữ liệu/lỗi/hết quota thì chuyển sang **Chờ nhân viên**.
5. Admin mở `/admin/chat`, chọn khách và bấm **Tiếp nhận** hoặc gửi tin trả lời. AI dừng cho cuộc trò chuyện đó. Admin bấm **Kết thúc hỗ trợ** khi xong; khách có thể bật AI lại hoặc gửi yêu cầu mới.

Khi nhân viên đang hỗ trợ, khách không bật lại AI được. Phiên AI mới không gửi lịch sử hỗ trợ trước đó của khách/admin tới Google. Email và số điện thoại Việt Nam trong ngữ cảnh AI được che; việc che mẫu không đảm bảo nhận diện mọi dạng dữ liệu cá nhân, nên UI yêu cầu khách không nhập thông tin riêng tư. Câu hỏi về đơn hàng, hoàn tiền, thanh toán, mật khẩu, tài khoản hoặc địa chỉ nhà được chuyển sang nhân viên trước khi gọi AI.

AI tư vấn, không sửa giỏ hàng, tạo đơn, hoàn tiền hoặc truy cập dữ liệu tài khoản. Đoạn trả lời là văn bản của mô hình và có thể sai; số liệu sản phẩm chính thức hiển thị trong thẻ từ database. Chưa bổ sung kho tri thức chính sách/RAG/embedding; yêu cầu chính sách chưa có bằng chứng được chuyển nhân viên.

## Công việc nền và độ tin cậy

Migration V26 lưu chế độ hỗ trợ, mốc phiên AI, liên kết phản hồi với tin khách, hàng đợi `ai_chat_jobs`, lease/deadline/token/generation, trạng thái lỗi và số token. Khóa database chỉ giữ lúc nhận việc/ghi kết quả; không giữ trong lúc chờ mạng.

Mỗi tin khách chỉ tạo một job. Client UUID chống gửi trùng; `in_reply_to_message_id` unique chống phát hành trùng phản hồi AI. Worker làm mới job sau khi khóa cuộc trò chuyện. Khi khách chuyển nhân viên hoặc admin tiếp nhận, generation tăng và công việc cũ bị hủy; kết quả trả về muộn không được ghi vào chat.

Worker thử tối đa hai lần cho lỗi 429/5xx/mạng. Hai lần dùng chung deadline 30 giây từ khi bắt đầu xử lý, bao gồm backoff. Job có lease để phục hồi sau sự cố tiến trình; job quá deadline được chuyển sang nhân viên thay vì gọi lại vô hạn. Các lỗi auth/model/schema và dữ liệu không hợp lệ chuyển thẳng sang nhân viên. Lỗi nội bộ được lưu bằng mã ngắn; response lỗi Google/key không đưa vào nội dung chat.

## Kiểm thử và kết nối thật

```powershell
.\mvnw.cmd '-Dtest=ChatIntegrationTest,AiChatIntegrationTest,GeminiRestClientTest' test
```

Kiểm thử tích hợp dùng hệ thống nghiệp vụ và database thật trong profile test, chỉ thay Gemini gateway bằng giả lập. Kiểm thử transport dùng HTTP server cục bộ. QA trình duyệt phân biệt phần chat nhân viên thực với phần giao diện AI có phản hồi giả lập.

Sau khi có key: bật cấu hình, đăng nhập bằng khách, bật AI với đồng ý, gửi một câu tư vấn sản phẩm có trong danh mục. Xác minh nhãn AI, thẻ sản phẩm, token/job và số request hôm nay trên trang admin. Bấm Gặp nhân viên và kiểm tra admin tiếp nhận/trả lời. Nếu lỗi key/model/quota, xem `error_code` của job và đối chiếu [tài liệu API](https://ai.google.dev/api/generate-content), [structured output](https://ai.google.dev/gemini-api/docs/structured-output), [quota](https://ai.google.dev/gemini-api/docs/rate-limits); không in key hoặc raw response vào log.

Tắt AI bằng `APP_AI_ENABLED=false` và khởi động lại. Dữ liệu chat giữ lại; khách tiếp tục chat với nhân viên. Không đổi profile/database local để chạy demo hay kiểm thử.
