# Danh sách chức năng TechShop — Spring Boot

Danh sách phản ánh mã nguồn thực thi. Tài liệu Laravel cũ được giữ tại [bản tham chiếu](docs/reference-laravel-features.md), không mô tả công nghệ của repo này.

## Khách hàng

- Đăng ký/đăng nhập/đăng xuất; giỏ và đơn riêng theo tài khoản.
- Tìm/lọc/sort/phân trang; khách chưa đăng nhập chọn màu/dung lượng, album/thông số; so sánh 2–3 sản phẩm.
- Cửa hàng còn hàng, lọc mã tỉnh/khu vực và bỏ chi nhánh ngừng hoạt động; giao chuẩn, giao 2h theo tọa độ hoặc nhận tại cửa hàng.
- Coupon/chiết khấu hội viên; ước tính thu cũ, phiếu đã thẩm định; đăng ký gói trả góp nội bộ.
- Preview server chống phản hồi cũ/lỗi 409; đặt chống trùng, QR nhận máy theo owner/role, timeline/hủy hợp lệ, tra đơn bằng mã và điện thoại.
- COD, MOCK demo; thanh toán lại đúng giao dịch đã lưu; VNPay có chữ ký/IPN, cần merchant để thử cổng thật.
- Đánh giá; điểm/hạng hội viên, bảo hành máy đã mua, tra IMEI.

## Quản trị

- Danh mục/sản phẩm/giá/version/trạng thái; ảnh/album biến thể; thông số.
- Chi nhánh/tọa độ; nhập/điều chuyển IMEI, trạng thái và ledger tồn kho.
- Xử lý đơn, xác nhận giao/thu COD, nhận máy theo mã; in phiếu nội bộ.
- Bảng giá thu cũ/mức khấu trừ/gói trả góp; thêm/ngừng cung cấp, thẩm định/duyệt hồ sơ.
- Coupon/kiểm duyệt đánh giá; tiếp nhận/sửa/bàn giao, đổi máy cùng phiên bản trong 30 ngày.
- Doanh thu đã giao/phí/sản phẩm bán chạy/kênh/chi nhánh; lọc/xuất CSV.

## Nền tảng và kiểm chứng

Java 21, Spring Boot 4.1.1, Thymeleaf, Security, JPA/JDBC, Flyway V1–V9, H2 demo và MySQL. Session/CSRF/role/owner, transaction, khóa kho/IMEI, snapshot, idempotency.

Xem [README](README.md), [trạng thái](docs/12-implementation-status.md), [hướng dẫn](docs/13-omnichannel-guide.md). Không coi trả góp demo/giao 2h local hay IPN test là kết nối đối tác thật.
