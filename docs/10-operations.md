# Kế hoạch CI/CD, vận hành và bàn giao

Thiết kế vận hành dự kiến. **Chưa có ứng dụng, pipeline build, Dockerfile, hosting hay backup đã chạy.** Không có lệnh nào trong tài liệu được thực hiện để deploy website trong lượt lập kế hoạch.

## 1. Môi trường

| Môi trường | Dữ liệu | Cấu hình | Mục đích |
|---|---|---|---|
| dev | Seed giả lập, MySQL local | application-dev + env | Develop và demo nội bộ |
| test | DB/container mới cho từng suite | application-test | Migration/integration/security/race |
| staging/demo | Dữ liệu giả lập riêng | application-prod-like + secrets | UAT, load, backup restore |
| production thật | Chưa nằm trong rollout v0.1 | Cần quyết định hosting/policy riêng | Chỉ mở sau thiết kế bổ sung vận hành |

Không dùng dữ liệu khách thật làm seed. Không reuse credential dev cho production. GitHub repository là nơi lưu source/tài liệu, không phải hosting ứng dụng Java.

## 2. Cấu hình cần triển khai ở B-02/B-22

| Key dự kiến | Quy tắc |
|---|---|
| SPRING_PROFILES_ACTIVE | dev/test/prod; explicit |
| SPRING_DATASOURCE_URL | JDBC MySQL với UTC và TLS phù hợp môi trường |
| SPRING_DATASOURCE_USERNAME / PASSWORD | Secret môi trường, không nằm trong git |
| SERVER_PORT | Default8080 sau reverse proxy |
| SERVER_SERVLET_SESSION_TIMEOUT | Baseline30 phút idle; kiểm thử hết session |
| SPRING_JPA_HIBERNATE_DDL_AUTO | validate; schema qua Flyway |
| APP_BOOTSTRAP_ADMIN_ENABLED | Chỉ bật tác vụ bootstrap kiểm soát lần đầu; không tạo admin mỗi restart |
| APP_BOOTSTRAP_ADMIN_EMAIL / PASSWORD | Nếu bootstrap được chọn, nhận từ secret, không log; tắt sau tạo |

Tên APP_* là đề xuất custom property, cần code mapping khi implement. `.env` không được Spring Boot tự load nếu chưa có cơ chế riêng; Docker Compose có thể nhận `.env` và truyền environment. README chạy thật phải giải thích cơ chế được triển khai, không chỉ bảo “tạo .env”.

## 3. CI dự kiến

PR và push main: checkout → Java21 → Maven cache → `./mvnw -B verify` → unit/MVC/MySQL Testcontainers → lưu test reports → package JAR/container khi cần. Runner phải có Docker để chạy Testcontainers. Không skip race/security vì thiếu môi trường; đánh dấu blocked và sửa pipeline.

Các file app pipeline, Maven Wrapper và Dockerfile chỉ tạo ở B-02/B-04/B-22. Hiện repo có PR/issue template tài liệu, chưa có CI build ứng dụng. Docs gate kiểm tra local links, Mermaid syntax, requirement IDs và tổng estimate; feature gate thêm compile/test/database.

## 4. CD/release procedure dự kiến

1. Chọn commit đã pass CI/UAT và ghi release note, giới hạn, schema diff.
2. Snapshot backup, xác minh có thể restore và xác định migration có tương thích app cũ không.
3. Deploy staging bằng image tag/commit immutable; migrate và start.
4. Smoke test public catalog, login, checkout COD demo, ownership/admin, cancel/report.
5. Kiểm tra logs, health, error rate, DB totals/ledger; ghi bằng chứng.
6. Đối với môi trường thật, chỉ deploy khi chủ dự án có yêu cầu và quyền phù hợp, không suy ra từ yêu cầu “đẩy tài liệu lên GitHub”.

## 5. Backup/restore

Mục tiêu demo sau khi cấu hình: backup mỗi24 giờ, giữ7 bản gần nhất, mã hóa/lưu quyền hạn chế ngoài host DB. Đây là policy đề xuất, chưa được tạo. Backup bao gồm schema/data; static assets đóng gói source có thể phục dựng từ commit. Khi upload được bổ sung, phải backup/object versioning riêng.

Restore drill: chọn backup → restore DB cô lập → chạy application cùng phiên bản tương thích → đối soát DQ queries → smoke flow → ghi thời gian và điểm dữ liệu gần nhất. RPO24h/RTO2h là target, phải ghi actual. Không thử restore đè DB đang có dữ liệu cần giữ.

## 6. Rollback

Rollback app về image/commit trước chỉ khi schema vẫn tương thích. Ưu tiên expand/contract migration: thêm cột trước, chuyển app, xóa cột ở release sau. Flyway không tự “undo” mọi migration. Migration phá vỡ cần kế hoạch riêng; restore backup có thể mất dữ liệu phát sinh sau backup nên phải đưa ra quyết định cụ thể trước khi dùng cho hệ thống thật.

Không sửa migration đã áp dụng để che checksum. Lỗi schema được sửa bằng migration mới sau khi đánh giá hoặc phục hồi đã được chốt.

## 7. Observability

Structured log: timestamp UTC, level, requestId, route, status, latency; business logs chỉ orderId/productId/actorId nội bộ, không name/phone/address/password/session/token. Error response có requestId, không stack trace. Admin timeline/ledger mới là audit nghiệp vụ, log console không thay thế.

Health readiness kiểm tra app + DB và migration; liveness không phụ thuộc truy vấn nặng. Metric: HTTP errors/p95, DB pool saturation, checkout success/conflict, lock timeout. Chưa chốt monitoring vendor; chọn khi staging có nhu cầu.

## 8. Runbook cơ bản

| Sự cố | Kiểm tra đầu tiên | Hành động dự kiến |
|---|---|---|
| DB connection fail | Health, env, DB availability/pool | Khôi phục DB/config; không bỏ qua persistence để trả success |
| Checkout409 tăng | code CART_CHANGED/PRICE_CHANGED/STOCK/LOCK | Phân biệt business conflict với lock contention; đọc query/log có requestId |
| Người dùng không biết đơn có tạo chưa | user + checkoutKey/requestId | Tra snapshot/order; retry đúng key, không tạo key mới trước kiểm tra |
| Ledger lệch stock | DQ-02/03, movements, order history | Tạm dừng mutation liên quan, xác định bug; không sửa ledger tùy ý |
| Revenue sai | range UTC/local, delivered_at, join fanout | Đối soát Q-01 với từng order; không lấy tổng PENDING |
| State SHIPPED giao thất bại | Quy trình MVP chưa hỗ trợ | Không cancel/hoàn stock bằng shortcut; bổ sung quy trình nhận hàng trả trước vận hành thật |
| Deploy fail | CI/image, migration, readiness | Rollback theo schema compatibility, giữ bằng chứng lỗi |

## 9. Checklist bàn giao

- README có prerequisites, versions pin, clone/config/build/run/test thật.
- Migration + seed demo tách khỏi production; tài khoản demo được tạo an toàn.
- AC/UAT, CI reports, load report, restore drill và DQ kết quả.
- API/UI nhất quán với spec; giới hạn COD/return nêu rõ.
- Video3–5 phút: catalog, checkout, admin, cancel, report, test core.
- Release tag v0.1.0-mvp sau hoàn thành, danh sách known issues, người tiếp nhận và quyền do PO quyết định.
