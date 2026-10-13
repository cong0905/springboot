# Hướng dẫn đa kênh

## Hàng hóa và kho

Admin tạo sản phẩm, mở sửa để thêm biến thể (SKU duy nhất/bất biến, màu, dung lượng, giá nguyên VND) và thông số theo nhóm/tên. Sản phẩm thường còn tồn phải điều chỉnh về 0 trước khi tạo biến thể đầu tiên. Khi đã quản lý serial, nhập hàng bằng `/admin/commerce`, không điều chỉnh số lượng tổng thủ công.

Thêm chi nhánh/tọa độ/mã tỉnh và mã khu vực tùy chọn; nhập 1–500 serial, mỗi dòng một máy. Serial duy nhất toàn hệ thống, 5–40 ký tự chữ/số/gạch ngang. USB scanner nhập như bàn phím; camera cần BarcodeDetector/quyền camera và chưa có kiểm chứng thiết bị thật. Điều chuyển chỉ máy IN_STOCK; ghi lịch sử nguồn/đích, tồn tổng không đổi.

Ảnh tải ở form quản lý ảnh hiện có, sau đó gán mã biến thể trong Album theo phiên bản. Ảnh chung dùng variantId null. Khách chọn màu sẽ đổi cả album ảnh/giá/tồn trước hoặc sau đăng nhập. Trang sản phẩm và `/branches` lọc theo mã tỉnh/khu vực được cấu hình ở chi nhánh; `inStock=true` chỉ trả cửa hàng còn máy. Danh sách công khai và checkout không hiển thị chi nhánh ngừng hoạt động.

## Đặt và nhận hàng

Giỏ lưu các phiên bản riêng; đơn snapshot giá/SKU/tên/màu/dung lượng. Chọn cách nhận rồi tính preview. Các trường pickup/tọa độ/gói trả góp chỉ được áp dụng khi phù hợp dịch vụ và cách thanh toán. Đổi lựa chọn giá sẽ hủy preview cũ; phản hồi cũ không bật nút đặt; lỗi 409 buộc tính lại giá. Khi thay đổi giỏ/giá/hash cần tính và xác nhận lại. Retry cùng UUID và nội dung trả lại đơn cũ; UUID dùng với nội dung khác bị từ chối.

| Dịch vụ | Quy tắc |
|---|---|
| Tiêu chuẩn | 30.000 ₫ dưới 1 triệu tiền hàng; miễn phí từ 1 triệu |
| Nhận cửa hàng | Chọn chi nhánh đủ toàn bộ giỏ; phí 0; giữ 24 giờ |
| Giao 2h | Tọa độ hợp lệ, chi nhánh đủ hàng gần nhất, bán kính ≤20km; 20.000 + 5.000/km làm tròn lên |

Một đơn IMEI xuất từ một chi nhánh; đặt hàng thường và hàng IMEI trong các đơn riêng. Giao 2h hiện là routing/bảng phí demo.

Trạng thái `PENDING → CONFIRMED → SHIPPED → DELIVERED`. Admin xác nhận biên nhận khi giao xong; pickup dùng UUID khách cung cấp, xử lý các bước trong transaction. Đơn đã giao gọi pickup lại không cộng điểm/bán máy lần nữa.

Job 60 giây hủy pickup PENDING/CONFIRMED chưa trả tiền khi hết 24 giờ và hoàn kho. Đơn đã trả tiền cần đối soát, không tự hủy; cancel báo REFUND_REQUIRED. Chưa có refund gateway. Mã nhận máy và QR PNG 256×256 hiển thị trong đơn. Endpoint `/api/v1/orders/{id}/pickup-qr.png` kiểm tra owner; đường dẫn admin tương ứng cần ROLE_ADMIN. QR chỉ chứa pickup code, không chứa thông tin cá nhân, đặt Cache-Control no-store. Hết hạn/chưa trả trả lỗi 409; đã giao/hủy không cấp QR. Camera trên đơn admin có fallback nhập tay/USB, chưa nghiệm thu thiết bị. Job dùng UTC như JPA để giữ đủ 24 giờ ở máy chạy giờ Việt Nam. Chưa gửi SMS/email.

Trang đơn của khách có nút **Tiếp tục thanh toán** dùng `paymentUrl` từ server và đúng transaction đã lưu cho MOCK/VNPay. Thất bại có thể thử lại cùng giao dịch; khi đã trả tiền hoặc đã giao/hủy, link không còn hiển thị. Không tạo thêm transaction khi tải lại trang đơn.

## Thu cũ, trả góp và hội viên

Khách vào `/trade-in` nhận giá dự kiến, cửa hàng thẩm định và duyệt ở `/admin/commerce`. Phiếu APPROVED gắn owner, dùng trong 7 ngày và một lần; trừ không quá tiền hàng. Hủy đơn chưa trả tiền trả phiếu về APPROVED. Không cho thu cùng một serial qua nhiều phiếu.

Trả góp là hồ sơ demo; plan/lịch chia tiền, phí chuyển đổi và phần dư kỳ cuối được server tính, tổng các kỳ đúng số tiền. Hồ sơ cần APPROVED trước giao; chưa kết nối bên tài chính. Admin cấu hình model thu cũ, mức khấu trừ và plan ở /admin/commerce; thêm mới hoặc ngừng, không sửa hồi tố điều khoản. Demo đã seed mẫu.

`/member`: tích 1% tiền hàng sau ưu đãi, 1 điểm = 1.000 ₫, làm tròn xuống. S-Mem trên 10 triệu; S-Vip trên 30 triệu. Giảm 1%/3% theo hạng; chỉ DELIVERED cộng điểm và có unique ledger theo đơn. Chưa redeem điểm.

## Bảo hành

DELIVERED kích hoạt 12 tháng theo serial, ngày Việt Nam. `/warranty` chỉ hiển thị serial/gói/ngày; `/member` có danh sách máy riêng. Số điện thoại là kiểm tra bổ sung và không trả danh sách máy công khai.

Phiếu sửa: `RECEIVED → REPAIRING → COMPLETED → HANDED_OVER`, không bỏ bước. Đổi máy trong 30 ngày: admin nhập mã thẻ và serial thay thế, phải cùng phiên bản/chi nhánh và IN_STOCK. Máy cũ DEFECTIVE, máy mới SOLD, trừ tồn qua ledger và giữ thời hạn bảo hành gốc. Không cộng lại doanh thu/điểm; chưa đổi sang model khác hoặc refund tự động.

## API thực thi

[OpenAPI](openapi.json) gồm các endpoint mới và checkout. POST `/login` dùng session; GET `/api/v1/csrf` lấy token, gửi cùng cookie và X-CSRF-TOKEN khi mutation. Admin API cần ROLE_ADMIN; customer API lấy principal, không nhận userId/role từ body.

POST `/api/v1/orders/preview`:

```json
{"couponCode":null,"options":{"fulfillmentType":"CLICK_AND_COLLECT","pickupBranchId":1}}
```

Dùng cartVersion/total/pricingHash trả về làm cartVersion/expectedTotal/expectedPricingHash khi POST `/api/v1/orders`, cùng options, checkoutKey UUID, thông tin liên hệ và paymentMethod.

VNPay kiểm tra HMAC-SHA512, merchant, số tiền x100 và status theo [tài liệu chính thức](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html). Cần cấu hình merchant/IPN reachable để thử sandbox thật; chưa có bằng chứng giao dịch ngoài ứng dụng.
