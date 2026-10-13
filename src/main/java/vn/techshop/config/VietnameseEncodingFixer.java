package vn.techshop.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Tự động chuẩn hóa và khôi phục font chữ tiếng Việt có dấu (UTF-8)
 * trong cơ sở dữ liệu, sửa các lỗi biến thành dấu hỏi (?) do công cụ import trước đó.
 */
@Component
@Order(1)
public class VietnameseEncodingFixer implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    public VietnameseEncodingFixer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(String... args) {
        // 1. Sửa danh mục tiếng Việt
        jdbc.update("UPDATE categories SET name = 'Điện thoại Smartphone' WHERE code = 'PHONE'");
        jdbc.update("UPDATE categories SET name = 'Laptop & MacBook' WHERE code = 'LAPTOP'");
        jdbc.update("UPDATE categories SET name = 'Máy tính bảng iPad & Android' WHERE code = 'TABLET'");
        jdbc.update("UPDATE categories SET name = 'Tai nghe & Loa Bluetooth' WHERE code = 'AUDIO'");
        jdbc.update("UPDATE categories SET name = 'Đồng hồ thông minh Smartwatch' WHERE code = 'WATCH'");
        jdbc.update("UPDATE categories SET name = 'Phụ kiện Củ sạc & Cáp kết nối' WHERE code = 'GEAR'");

        // 2. Sửa thông tin chuỗi chi nhánh cửa hàng
        jdbc.update("UPDATE store_branches SET name = 'TechShop Thái Hà', address = '123 Thái Hà, P. Trung Liệt, Q. Đống Đa, Hà Nội' WHERE code = 'HN-TH'");
        jdbc.update("UPDATE store_branches SET name = 'TechShop Cầu Giấy', address = '100 Cầu Giấy, P. Quan Hoa, Q. Cầu Giấy, Hà Nội' WHERE code = 'HN-CG'");
        jdbc.update("UPDATE store_branches SET name = 'TechShop Trần Quang Khải', address = '213 Trần Quang Khải, P. Tân Định, Quận 1, TP. Hồ Chí Minh' WHERE code = 'HCM-TQK'");
        jdbc.update("UPDATE store_branches SET name = 'TechShop Nguyễn Thị Thập', address = '350 Nguyễn Thị Thập, P. Tân Quy, Quận 7, TP. Hồ Chí Minh' WHERE code = 'HCM-NTT'");

        // 3. Sửa tên sản phẩm mẫu nếu bị lỗi dấu hỏi
        jdbc.update("UPDATE products SET name = 'iPhone 16 Pro Max 256GB - Chính hãng VN/A' WHERE sku = 'PHONE-IP16PM'");
        jdbc.update("UPDATE products SET name = 'Samsung Galaxy S25 Ultra 5G 256GB' WHERE sku = 'PHONE-S25U'");
        jdbc.update("UPDATE products SET name = 'MacBook Pro 14 M4 Pro (24GB / 512GB SSD)' WHERE sku = 'LAPTOP-MBP14M4'");
        jdbc.update("UPDATE products SET name = 'iPad Pro 11 inch M4 Ultra Retina XDR (256GB)' WHERE sku = 'TABLET-IPADM4-11'");
        jdbc.update("UPDATE products SET name = 'Apple Watch Series 10 Nhôm 46mm GPS' WHERE sku = 'WATCH-AWS10-46'");
        jdbc.update("UPDATE products SET name = 'Tai nghe chụp tai Sony WH-1000XM5 Hi-Res Audio' WHERE sku = 'AUDIO-SONY-XM5'");
        jdbc.update("UPDATE products SET name = 'Loa Bluetooth di động Marshall Emberton II' WHERE sku = 'AUDIO-MARSHALL-E2'");
        jdbc.update("UPDATE products SET name = 'Củ sạc nhanh Anker GaNPrime 67W 3 Cổng (A2668)' WHERE sku = 'GEAR-ANKER-67W'");

        // 4. Sửa tên người dùng mẫu
        jdbc.update("UPDATE users SET full_name = 'Ban Quản Trị TechShop' WHERE email = 'admin@techshop.test'");
        jdbc.update("UPDATE users SET full_name = 'Nguyễn Văn An (S-VIP)' WHERE email = 'customer@techshop.test'");
        jdbc.update("UPDATE users SET full_name = 'Trần Hoàng Long (S-MEM)' WHERE email = 'hoanglong@techshop.test'");
        jdbc.update("UPDATE users SET full_name = 'Lê Linh Đan (S-NEW)' WHERE email = 'linhdan@techshop.test'");

        // 5. Sửa màu sắc biến thể
        jdbc.update("UPDATE product_variants SET color_name = 'Đen Titan' WHERE color_name LIKE '%?en%' OR color_name LIKE '%Titan%' AND color_name NOT LIKE 'Bạc%'");
        jdbc.update("UPDATE product_variants SET color_name = 'Bạc Titan' WHERE color_name LIKE '%B?c%'");
        jdbc.update("UPDATE product_variants SET color_name = 'Bạc' WHERE color_name = 'B?c'");
        jdbc.update("UPDATE product_variants SET color_name = 'Đen' WHERE color_name = '?en'");
    }
}
