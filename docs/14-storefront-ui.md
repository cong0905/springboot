# Giao diện cửa hàng TechShop

Giao diện được chỉnh trực tiếp trên dự án Spring Boot + Thymeleaf, tham khảo bố cục bán lẻ tại https://cellphones.com.vn/ ngày 02/10/2026. Giữ tên và dữ liệu của TechShop.

## Phần đã thay đổi

- Header đỏ, tìm kiếm có gợi ý, menu dịch vụ, tài khoản và đăng xuất.
- Trang `/` và `/products`: danh mục từ cơ sở dữ liệu, banner có điều khiển và tạm dừng, các ô dịch vụ, thẻ danh mục và lưới sản phẩm.
- Bộ lọc dùng API/route hiện có; có thể thu gọn trên điện thoại.
- Chọn 2–3 sản phẩm, lưu trong phiên trình duyệt rồi mở `/compare?ids=...`. Có thể bỏ chọn hoặc xóa danh sách.
- Footer dùng các route hiện có. Header và footer áp dụng cho các trang khách hàng và quản trị.
- Banner hội viên và lưới hồ sơ co giãn trên màn hình nhỏ.
- Giá, số sản phẩm và trạng thái còn hàng lấy từ dữ liệu hiện tại. Không tạo giá giảm, đánh giá hay bộ đếm khuyến mãi giả.

## Tệp giao diện

- `src/main/resources/templates/fragments.html`
- `src/main/resources/templates/products.html`
- `src/main/resources/templates/member.html`
- `src/main/resources/static/css/storefront.css` (nạp sau `app.css`)
- `src/main/resources/static/js/app.js`
- `src/main/resources/static/assets/storefront/hero-phone.svg`
- `src/main/resources/static/assets/storefront/hero-laptop.svg`

Hai ảnh banner là biến thể SVG của ảnh minh họa sẵn trong dự án. Dữ liệu sản phẩm trong profile `demo` là dữ liệu mẫu.

## Chạy

Trong PowerShell tại `D:\springboot`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
.\mvnw.cmd -DskipTests package
.\scripts\run-demo.ps1 -Port 8080
```

Mở http://localhost:8080/. Profile demo dùng H2 trong bộ nhớ, khởi động lại sẽ tạo lại dữ liệu mẫu. Các cấu hình MySQL hiện có không bị sửa.

## Kiểm tra và bằng chứng

- Render các template dùng test có sẵn `CommerceIntegrationTest#coreTemplatesRenderForCustomerAndAdmin`.
- Script `scripts/storefront_qa.cjs` chạy Chromium để kiểm tra trang, responsive, tìm kiếm, lọc, menu, carousel, so sánh, đăng nhập và đăng xuất.
- Kết quả chi tiết: `qa/storefront-20261002/results.json`.
- Ảnh desktop/mobile: `qa/storefront-20261002/`.
- Tôn trọng `prefers-reduced-motion`, có nhãn nút, điều khiển bàn phím và thông báo lựa chọn so sánh.

```powershell
$env:PLAYWRIGHT_MODULE = 'C:/Users/nguye/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/playwright'
node scripts/storefront_qa.cjs
```

## Khôi phục giao diện trước lần chỉnh này

Bản sao các tệp được thay thế nằm trong `.local/ui-backup-cellphones-20261002/`. Khôi phục đúng các tệp tương ứng rồi bỏ dòng nạp `storefront.css` nếu chỉ muốn quay về giao diện trước. Những thay đổi khác đã có sẵn trong workspace được giữ nguyên.