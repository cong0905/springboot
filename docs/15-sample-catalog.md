# Danh mục mẫu và ảnh sản phẩm thật

Danh mục demo bổ sung 32 model thật và 38 ảnh, tổng cộng 40 sản phẩm sau khi cộng 8 mẫu có sẵn. Sáu danh mục: điện thoại, laptop, máy tính bảng, đồng hồ, âm thanh và phụ kiện.

## Cách hoạt động

`RetailDemoCatalog` chỉ chạy với profile `demo`, sau các seed hiện có. Mỗi model có một phiên bản, thông số và 8 thiết bị mẫu: 4 tại mỗi chi nhánh. Serial có tiền tố `SAMPLE-`. Giá, tồn kho và serial được tạo để trải nghiệm, không phải dữ liệu bán hàng thực.

Seed bỏ qua SKU đã tồn tại; chạy lại không nhân bản hay bổ sung tồn kho. Database H2 demo chạy trong bộ nhớ nên khởi động lại sẽ tạo lại dữ liệu mẫu.

## Nguồn ảnh

Tên, ảnh và thông số tham khảo lấy từ trang sản phẩm công khai của CellphoneS. Ảnh được lưu trong `src/main/resources/static/assets/products/real/`, đóng gói cùng ứng dụng, không phụ thuộc tải ảnh từ bên ngoài khi duyệt trang. Manifest `src/main/resources/demo/retail-catalog.json` lưu URL nguồn, đường dẫn local, kích thước và SHA-256 của từng ảnh. Giá trong manifest là giá mẫu tự đặt.

Script `scripts/download_sample_catalog.cjs` thu thập lại dữ liệu; tùy chọn `--missing` chỉ bổ sung những model chưa có.

| SKU | Sản phẩm | Danh mục | Nguồn |
| --- | --- | --- | --- |
| REAL-IPHONE-15 | iPhone 15 128GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/iphone-15.html) |
| REAL-IPHONE-15-PLUS | iPhone 15 Plus 128GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/iphone-15-plus.html) |
| REAL-IPHONE-16 | iPhone 16 128GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/iphone-16.html) |
| REAL-IPHONE-16-PLUS | iPhone 16 Plus 128GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/iphone-16-plus.html) |
| REAL-IPHONE-16-PRO | iPhone 16 Pro 128GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/iphone-16-pro.html) |
| REAL-IPHONE-16-PRO-MAX | Điện thoại iPhone 16 Pro Max 256GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/iphone-16-pro-max.html) |
| REAL-GALAXY-S24-ULTRA | Samsung Galaxy S24 Ultra 12GB 256GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/samsung-galaxy-s24-ultra.html) |
| REAL-GALAXY-S24-PLUS | Samsung Galaxy S24 Plus 12GB 256GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/samsung-galaxy-s24-plus.html) |
| REAL-GALAXY-A55 | Samsung Galaxy A55 5G 8GB 128GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/samsung-galaxy-a55.html) |
| REAL-XIAOMI-14 | Xiaomi 14 12GB 256GB | PHONE | [Trang sản phẩm](https://cellphones.com.vn/xiaomi-14.html) |
| REAL-MACBOOK-AIR-M3-13 | MacBook Air M3 13 inch 2024 8CPU 10GPU 16GB - 256GB sạc 30W | LAPTOP | [Trang sản phẩm](https://cellphones.com.vn/macbook-air-m3-13-inch-2024-16gb-256gb-10gpu.html) |
| REAL-MACBOOK-AIR-M3-15 | MacBook Air M3 15 inch 2024 16GB - 256GB sạc 70W | LAPTOP | [Trang sản phẩm](https://cellphones.com.vn/macbook-air-m3-15-inch-2024-16gb-256gb.html) |
| REAL-MACBOOK-AIR-M2 | Apple MacBook Air M2 2024 8CPU 8GPU 8GB 256GB I Chính hãng Apple Việt Nam | LAPTOP | [Trang sản phẩm](https://cellphones.com.vn/macbook-air-m2-2022.html) |
| REAL-MACBOOK-PRO-M3 | MacBook Pro 14 M3 8GB - 512GB | LAPTOP | [Trang sản phẩm](https://cellphones.com.vn/macbook-pro-14-inch-m3-2023-8gb-512gb.html) |
| REAL-IPAD-10 | iPad Gen 10 10.9 inch 2022 Wifi 64GB I Chính hãng Apple Việt Nam | TABLET | [Trang sản phẩm](https://cellphones.com.vn/ipad-10-9-inch-2022.html) |
| REAL-IPAD-AIR-M2 | iPad Air 6 M2 11 inch Wifi 128GB | TABLET | [Trang sản phẩm](https://cellphones.com.vn/ipad-air-6-m2-11-inch.html) |
| REAL-IPAD-PRO-M4 | iPad Pro M4 11 inch Wifi 256GB | TABLET | [Trang sản phẩm](https://cellphones.com.vn/ipad-pro-m4-11-inch.html) |
| REAL-GALAXY-TAB-S9 | Samsung Galaxy Tab S9 5G 8GB 128GB | TABLET | [Trang sản phẩm](https://cellphones.com.vn/samsung-galaxy-tab-s9.html) |
| REAL-APPLE-WATCH-S9 | Apple Watch Series 9 41mm (GPS) viền nhôm dây cao su | WATCH | [Trang sản phẩm](https://cellphones.com.vn/apple-watch-series-9-41mm.html) |
| REAL-APPLE-WATCH-SE | Apple Watch SE 2022 40mm | WATCH | [Trang sản phẩm](https://cellphones.com.vn/apple-watch-se-2022-40mm.html) |
| REAL-GALAXY-WATCH6 | Samsung Galaxy Watch6 40mm Bluetooth | WATCH | [Trang sản phẩm](https://cellphones.com.vn/samsung-galaxy-watch-6.html) |
| REAL-AIRPODS-PRO-2 | Tai nghe Bluetooth Apple AirPods Pro 2 2023 USB-C | AUDIO | [Trang sản phẩm](https://cellphones.com.vn/apple-airpods-pro-2-usb-c.html) |
| REAL-AIRPODS-4 | Tai nghe Bluetooth Apple AirPods 4 | AUDIO | [Trang sản phẩm](https://cellphones.com.vn/apple-airpods-4.html) |
| REAL-SONY-WH-1000XM5 | Tai nghe Bluetooth chụp tai Sony WH-1000XM5 | AUDIO | [Trang sản phẩm](https://cellphones.com.vn/tai-nghe-chup-tai-sony-wh-1000xm5.html) |
| REAL-JBL-FLIP6 | Loa Bluetooth JBL Flip 6 | AUDIO | [Trang sản phẩm](https://cellphones.com.vn/loa-bluetooth-jbl-flip-6.html) |
| REAL-JBL-CHARGE5 | Loa Bluetooth JBL Charge 5 | AUDIO | [Trang sản phẩm](https://cellphones.com.vn/loa-bluetooth-jbl-charge-5.html) |
| REAL-GALAXY-BUDS2-PRO | Tai nghe Bluetooth True Wireless Samsung Galaxy Buds2 Pro | AUDIO | [Trang sản phẩm](https://cellphones.com.vn/samsung-galaxy-buds-2-pro.html) |
| REAL-APPLE-20W | Sạc nhanh Apple 20w USB-C chính hãng Apple Việt Nam | GEAR | [Trang sản phẩm](https://cellphones.com.vn/cu-sac-nhanh-iphone-20w-pd-type-c.html) |
| REAL-LOGITECH-MX-MASTER3S | Chuột không dây Logitech MX Master 3S | GEAR | [Trang sản phẩm](https://cellphones.com.vn/chuot-khong-day-logitech-mx-master-3s.html) |
| REAL-LOGITECH-K380 | Bàn phím Bluetooth Logitech K380 | GEAR | [Trang sản phẩm](https://cellphones.com.vn/ban-phim-khong-day-logitech-k380.html) |
| REAL-SAMSUNG-25W | Củ sạc nhanh Samsung 25W không kèm cáp (EP-T2510NBEGWW) | GEAR | [Trang sản phẩm](https://cellphones.com.vn/cu-sac-nhanh-samsung-25w-ep-t2510nbegww.html) |
| REAL-ANKER-737 | Pin sạc dự phòng Anker 737 Powercore A1289 24000 140W | GEAR | [Trang sản phẩm](https://cellphones.com.vn/pin-sac-du-phong-anker-737-powercore-24000-140w-a1289.html) |

## Kiểm tra

`RetailDemoCatalogTest`: đủ danh mục, ảnh đóng gói, giá mẫu, tồn kho đồng bộ với phiên bản và chạy lại không tạo trùng.

`scripts/catalog_photo_sheet.cjs`: giải mã toàn bộ ảnh và tạo bảng ảnh đối chiếu. `scripts/catalog_qa.cjs`: kiểm tra API, checksum 38 ảnh, bốn trang danh mục, giao diện 1440/390/320 px và thêm sản phẩm thật vào giỏ/checkout. Bằng chứng lưu tại `qa/catalog-20261002/`.
