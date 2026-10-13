# TechZone — kế hoạch thực hiện từng bước

Ngày lập: 02/10/2026. Tham chiếu: 12 ảnh PNG trong `techzone/`. Đích triển khai: dự án Spring Boot/Thymeleaf hiện tại. Bản này là kế hoạch triển khai theo ảnh, không phải chứng nhận các chức năng chưa làm.

## Nguyên tắc nghiệm thu

- Giữ nghiệp vụ giá, cart version, CSRF, phân quyền, preview hash, idempotency, IMEI, hoàn tồn và bảo hành đang có. Không viết lại checkout chỉ để đổi giao diện.
- Giao diện thống nhất **TechZone** theo hai ảnh trang chủ/danh mục. Các ảnh còn dùng logo CellphoneS chỉ tham khảo bố cục. Chữ, ngày, hotline, lượt xem, đánh giá, khuyến mãi trong ảnh không phải dữ liệu cần sao chép.
- Desktop ưu tiên 1440 px; bổ sung kiểm tra 1024/768/390/320 px. Không có bản mobile trong bộ ảnh: thiết kế mobile từ cùng component, không coi ảnh desktop là hợp đồng mobile.
- Mỗi bước thực hiện theo: đọc ảnh + hợp đồng dữ liệu → sửa một nhóm nhỏ → kiểm tra API/quyền khi có nghiệp vụ → chụp desktop/mobile → cập nhật trạng thái/checklist và Graphify.
- Migrations mới phải có cả MySQL và H2, tiếp sau V9 tại thời điểm kiểm tra. Không chạy SQL seed phá bảng.
- Nhãn `Đạt` chỉ dùng sau kiểm tra; `Có nền tảng` nghĩa là đã có mã nghiệp vụ nhưng chưa nghiệm thu theo mẫu TechZone. Các ô chưa đánh dấu là phần còn lại, không có nút giả để coi là xong.

## Đối chiếu 12 ảnh

| Ảnh trong techzone | Thành phần quan sát | Nền tảng hiện tại / phần cần làm |
| --- | --- | --- |
| TechZone Vietnamese Electronics Marketplace.png | Header đỏ, tìm kiếm lớn, navigation danh mục, hero + 2 banner, lợi ích, deal/member, danh mục, card mua hàng | `/products`, fragments, 40 sản phẩm/6 danh mục; đổi layout/brand, bổ sung thao tác card và giỏ badge |
| danh mục.png | Breadcrumb, banner danh mục, hãng, sidebar lọc, tab sắp xếp, grid | Có category/keyword/min-max-price/newest/price sort; cần brand/storage/facets, nhu cầu và tính năng chỉ sau khi có dữ liệu chuẩn |
| Trang chi tiết sản phẩm.png | Gallery, bộ nhớ/màu, khuyến mãi, cửa hàng, buy/cart, thông số/review/liên quan | Có variants/album/specs/stores/reviews, đổi bố cục và chọn phiên bản theo ảnh; không bịa ưu đãi |
| Trang giỏ hang.png | Chọn dòng, số lượng, xoá nhóm, voucher, tổng tiền, gợi ý | Có add/update/delete/version/preview; chọn một phần cần hợp đồng backend trước UI |
| thanh toán.png | Ảnh tổng hợp detail/cart và checkout 5 bước; có đặt không tài khoản | Checkout hiện cần CUSTOMER, có recipient/fulfillment/payment/preview; guest checkout là phần mới, không mở quyền API đang có một cách máy móc |
| Trang đăng nhập đăng ký.png | Phone/email, OTP, đăng ký, quên mật khẩu, social login | Hiện email + password + BCrypt/CSRF/throttle; OTP/reset/social chưa có provider |
| Trang tài khoản cá nhân.png | Form tên/email/phone/ngày sinh/giới tính, membership, đơn gần đây | `/profile` hiện chỉ redirect `/member`; cần bảng/trường dữ liệu và update API |
| trang tài khoản cas nhân.png | Profile view + avatar, voucher/address; danh sách đơn có tabs/search/timeline | Có member và orders list/status; address/avatar/voucher lưu riêng chưa có |
| Trang chi tiết đơn hang.png | Timeline, items, recipient, shipping/payment, cancel/reorder | Có order ownership/history/cancel; reorder và thay địa chỉ theo trạng thái cần bổ sung; tracking nhà vận chuyển ngoài chưa có |
| Trang so sánh sản phẩm.png | Card lớn, thêm/xoá, bảng cấu hình, highlight, share | Có comparison 2–3 sản phẩm và session selection; đổi UI, normalize units trước tự chọn bên tốt hơn |
| Trang hệ thống cửa hang.png | Region/search/list + map, distance, branch detail/photos | Có chi nhánh/toạ độ/region/in-stock và link bản đồ; map interactive/ảnh/giờ mở cửa chưa đầy đủ |
| tin tức.png | Sidebar loại bài, bài nổi bật/latest, search, detail, newsletter | Chưa có article/CMS/newsletter; cần module mới và nội dung tự viết có nguồn |

## Thứ tự các bước nhỏ

Mỗi dòng có một đầu ra để nghiệm thu riêng. Không dồn các thay đổi migration, auth và checkout vào cùng một lần sửa.

| ID | Công việc | Tệp/module chính | Điều kiện qua bước | Trạng thái |
| --- | --- | --- | --- | --- |
| TZ-01 | Kiểm kê 12 ảnh, routes/dữ liệu, xác định khác biệt | Tài liệu này + Graphify | Từng ảnh có ánh xạ và giới hạn đúng thực tế | Đạt phân tích |
| TZ-02 | Tokens màu/chữ/khoảng cách, brand và header/footer dùng chung | fragments + techzone.css + StorefrontModel | Header/search/menu đúng ở guest/customer/admin; không tràn 320 px | Đạt nhóm 1 |
| TZ-03 | Trang chủ: hero 2 slide + 2 promo, benefits, deal/member, category tiles | products + techzone.css | Mọi CTA tới dữ liệu/trang hoạt động, slider + reduced motion + ảnh đầy đủ | Đạt nhóm 1 |
| TZ-04 | Card sản phẩm: giá/giá gốc/đánh giá lấy dữ liệu thật; thêm giỏ và chọn variant | WebController + techzone.js | Guest tới login; customer chọn đúng SKU; version/CSRF đúng; badge tăng | Đạt nhóm 1 |
| TZ-05 | Chuẩn hoá brand/spec và truy vấn lọc server, counts theo filter | catalog + migrations nếu cần | Lọc hãng/storage/price kết hợp, phân trang giữ tham số, không lọc bằng tên giả | Chưa làm |
| TZ-06 | Trang danh mục: breadcrumb/banner/sidebar/chips/sort/grid | products/category template | Desktop gần ảnh danh mục, mobile filter panel dùng được, empty/reset đúng | Chưa làm |
| TZ-07 | Bố cục chi tiết: gallery + thông tin + khối mua | product + CSS | Ảnh thumbnail/zoom và 3 cột desktop, 1 cột mobile, không tràn | Chưa làm |
| TZ-08 | Chips màu/dung lượng, giá/tồn/album theo variant và buy-now | product + JS + variants/cart | Không tạo combination không tồn tại; hết hàng disabled; buy-now không mất giỏ cũ | Chưa làm |
| TZ-09 | Thông số, review breakdown, sản phẩm tương tự | product + reviews/catalog | Rating/count thật, không bịa verified buyer; review và quyền xoá đúng | Chưa làm |
| TZ-10 | So sánh: card 2–3 thiết bị, xoá/thêm/share/hàng khác biệt | compare + comparison | Share URL dùng IDs thực; thông số thiếu là —; không tự kết luận sai đơn vị | Chưa làm |
| TZ-11 | Backend yêu thích bền vững, ownership và unique user/product | wishlist + migrations/tests | Không truy cập danh sách người khác, toggle idempotent, sản phẩm ngừng bán xử lý đúng | Chưa làm |
| TZ-12 | UI yêu thích header/card/list | wishlist + cards + JS | Heart đồng bộ, reload còn dữ liệu, guest/login rõ | Chưa làm |
| TZ-13 | Đăng nhập/đăng ký theo layout mẫu, giữ auth hiện có | login/register/security | Email/password/CSRF/throttle/role vẫn đúng; không có social button chưa hoạt động | Chưa làm |
| TZ-14 | Hồ sơ: model/API update, phone/date/gender | identity + migrations/tests | Validate, ownership, không lộ hash, refresh session tên đúng | Chưa làm |
| TZ-15 | Account shell + profile form + đơn gần đây + membership | profile + account fragments | Sidebar chung, dữ liệu thật, save báo lỗi/đạt đúng | Chưa làm |
| TZ-16 | Sổ địa chỉ CRUD/default, chọn ở checkout | address + migrations + UI | Một địa chỉ default/user, không dùng địa chỉ người khác, snapshot đơn không bị sửa ngược | Chưa làm |
| TZ-17 | Đổi mật khẩu; reset/OTP contracts và provider | identity/security + UI | Expiry/attempt/resend/rate limit/token một lần; demo chỉ là demo, chưa gửi SMS thật | Chưa làm; gửi thật cần cấu hình provider |
| TZ-18 | Avatar và voucher của tôi | storage/coupon/profile | Upload đúng MIME/kích thước/quyền; voucher điều kiện thật; không sửa total phía client | Chưa làm |
| TZ-19 | Giỏ hàng layout và +/-/delete/coupon/summary | cart + JS | Đồng bộ version, subtotal/discount/shipping do server tính, cart rỗng đúng | Chưa làm |
| TZ-20 | Hợp đồng checkout chỉ các dòng đã chọn + xoá nhiều | cart/order/API/tests | Lock đúng subset, hash đúng subset, các dòng không chọn còn nguyên, rollback không mất dòng | Chưa làm |
| TZ-21 | Checkout wizard: khách → địa chỉ → vận chuyển → thanh toán → xác nhận | checkout + JS | Giữ draft khi lùi, validate từng bước, đổi option làm preview hết hiệu lực; double-submit không tạo hai đơn | Chưa làm |
| TZ-22 | Đối chiếu guest checkout theo ảnh và xây luồng guest riêng | identity/cart/order/security | Quyền truy cập đơn của guest bằng proof/token an toàn, không mở API customer; auth tests | Chưa làm |
| TZ-23 | Thanh toán và thông báo trạng thái | payment + checkout/order | COD/mock đúng, callback/retry không ghi nhận thanh toán giả; VNPay thật chỉ khi có cấu hình hợp lệ | Có nền tảng; chưa nghiệm thu TechZone |
| TZ-24 | Đơn của tôi: tabs/search/date/cards/timeline | orders + order query | Filter đúng owner/status/date, paginated, empty và nhãn theo enum thật | Chưa làm |
| TZ-25 | Chi tiết đơn: timeline/recipient/payment/IMEI/cancel | order template + service | Các action theo trạng thái/quyền; huỷ hoàn tồn và điểm đúng | Chưa làm |
| TZ-26 | Mua lại và thay địa chỉ trước fulfillment | order/cart + tests | Kiểm tra giá/tồn hôm nay, không ghi lại snapshot cũ, chỉ đổi ở trạng thái cho phép | Chưa làm |
| TZ-27 | Trang hội viên/points/voucher theo account shell | member + loyalty | Không tạo điểm/ưu đãi từ số trong ảnh; ledger/history đúng | Có nền tảng; chưa nghiệm thu TechZone |
| TZ-28 | Cửa hàng: region/query/list/detail/giờ/ảnh | branch + migrations/UI | Tồn/branch thực, counts và bộ lọc kết hợp; không bịa 100 cửa hàng hay khoảng cách | Chưa làm |
| TZ-29 | Bản đồ và vị trí gần nhất | branch + JS/map config | Location xin quyền từ browser khi bấm, thiếu quyền vẫn xem list; coords hợp lệ | Chưa làm |
| TZ-30 | Article/CMS: categories/slug/publish/search/ownership | news + migrations/admin/tests | Public chỉ bài published, HTML/content được xử lý an toàn, slug unique | Chưa làm |
| TZ-31 | News list/detail/sidebar/cards và seed nội dung | news templates + local assets | Search/categories/pagination thật; nội dung có nguồn và không dùng tin/đếm giả trong ảnh | Chưa làm |
| TZ-32 | Newsletter và OAuth/tích hợp ngoài | email/social config | Opt-in và gửi/provider được kiểm chứng; không tuyên bố gửi thật bằng log demo | Chưa làm; cần provider |
| TZ-33 | Quản trị nội dung phục vụ storefront | admin banners/categories/specs/news | ADMIN only, data-driven banners/schedule, không hard-code ưu đãi | Chưa làm |
| TZ-34 | H2 tests, MySQL tests có môi trường, browser E2E toàn luồng | tests/qa/docs | Auth/cart/checkout/order/inventory và 12 nhóm màn hình; desktop/mobile screenshots; không viện dẫn kết quả cũ cho code mới | Chưa làm |

## Hợp đồng giao diện cho nhóm đầu

- Brand `TechZone`, primary #e60012, text #162033, background #f6f8fb, border #e5e9f0; radius card 10–14 px.
- Container desktop rộng tối đa 1380 px; header hai tầng (toolbar + categories), trust strip trên cùng. Menu category lấy danh mục active từ database. Không tạo nút danh mục dẫn tới nội dung rỗng để giống số lượng trong ảnh.
- Trang chủ bỏ sidebar dọc kiểu cũ, hero chiếm khoảng 72% và 2 promo chiếm 28%; dịch vụ, 2 strip deal/member, category cards và catalogue tiếp sau.
- Hero dùng ảnh sản phẩm có trong catalogue (Galaxy S24, iPhone 16, MacBook) và thông tin sản phẩm thực. Không chép S25/S26/giá giảm/đếm ngược từ ảnh nếu dữ liệu không có.
- Card hiển thị giá catalog, giá gốc khi variant có cùng giá bán và original lớn hơn, đánh giá từ review stats; không hiển thị star/count giả. Quick-add giữ chọn variant thực và gọi CartService API có expectedVersion.
- Wishlist/profile/news sẽ được triển khai ở bước tương ứng trước khi thêm nút dẫn tới các trang đó.

## Theo dõi bàn giao

Các kết quả sẽ lưu theo nhóm tại `qa/techzone-step01/`, snapshot trước sửa tại `.local/techzone-step01-backup/`. Nhóm tiếp theo chỉ đánh dấu Đạt khi kiểm tra của chính nhóm đó thành công. Tài liệu này thay mô tả mục tiêu giao diện TechZone cho các bước mới; kế hoạch omnichannel cũ vẫn là tài liệu nghiệp vụ tham khảo, không bị ghi đè.

## Kết quả nhóm 1 — TZ-01 đến TZ-04

- Đã triển khai header/footer TechZone, navigation lấy 6 danh mục active từ database và badge tổng số lượng trong giỏ.
- Trang chủ dùng bố cục hero + hai promo, benefits, deal/member strips, category tiles và card sản phẩm theo bộ ảnh TechZone. Hero Galaxy S24/MacBook dùng sản phẩm có trong catalogue hiện tại.
- Card lấy review stats thật, chỉ hiện giá gốc từ variant có cùng giá bán; không thêm rating/giảm giá giả. Customer chọn variant trong dialog rồi thêm giỏ qua API với CSRF và cart version; guest được chuyển login, admin không mua bằng card.
- Maven: 2 tests chọn lọc (render các template customer/admin và seed catalogue) đạt, package thành công trên source cuối của nhóm 1.
- Browser: **69 lượt màn hình** ở 1440/1024/768/390/320 px; slider, reduced motion, CTA tìm kiếm, guest login, quick-add một/nhiều variant, badge sau reload, giá gốc và review count, customer/admin đạt; **0 lỗi JavaScript**. Bằng chứng: `qa/techzone-step01/results.json` và PNG cùng thư mục.
- Đây là nghiệm thu nhóm giao diện nền và thao tác card. Chưa nghiệm thu toàn bộ 12 trang theo bộ ảnh; các bước TZ-05 trở đi giữ nguyên trạng thái riêng.
- Bước tiếp theo: TZ-05 chuẩn hoá/filter hãng-dung lượng phía server, sau đó TZ-06 banner/breadcrumb/sidebar danh mục. Sau hai bước này mới chuyển nhóm chi tiết sản phẩm.
