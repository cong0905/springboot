# Quy trình đóng góp

Repository hiện ở giai đoạn tài liệu. Trước code, đọc [SRS](docs/01-spec.md), [architecture](docs/02-architecture.md), [backlog](docs/07-backlog.md) và [test plan](docs/08-test-plan.md).

## Branch và commit

- main là baseline tích hợp; feature/B-ID-name, fix/B-ID-name, docs/topic là nhánh làm việc.
- Một branch xử lý một mục tiêu có AC; không gom thay UI, schema và business rules không liên quan.
- Commit dùng feat(module), fix(module), test(module), docs, chore. Không commit secrets, .env thật, IDE/cache/target.
- Không force-push main hoặc reset lịch sử người khác. Pull --ff-only trước tạo branch; resolve conflict có review.

## PR

Mô tả vấn đề cụ thể và behavior cuối, liên kết B/FR/BR/AC, nêu schema/API impact, evidence và known limitations. Các template trong .github/ chỉ là form hỗ trợ, chưa tự tạo issue/PR hoặc pipeline.

PR code cần Maven verify sau khi wrapper tồn tại; feature touching DB cần MySQL tests; money/stock/owner/idempotency cần critical tests. Không thêm test chỉ mirror DTO/getters, không dùng một coverage badge làm bằng chứng transaction đúng.

PR docs cần local links đúng, ID traceability đầy đủ, estimate/capacity không mâu thuẫn, Mermaid render được trên GitHub, nguồn technical chính thức khi thay khả năng framework. Schema.sql là tham chiếu; migration chỉ trở thành authoritative khi có app/test chạy.

## Thay đổi yêu cầu

Nếu behavior khác spec, cập nhật CR và SRS/API/data/diagram/test/analytics cùng PR. Ví dụ hỗ trợ cancel sau SHIPPED là thay quy trình nghiệp vụ, không chỉ thêm enum. Giả định A-01–10 chỉ được đổi bằng quyết định ghi lại.

## Review và merge

Kiểm tra ownership ở server, CSRF, snapshot money, lock/rollback, PII, migration compatibility, test và README. Main merge sau review/CI phù hợp. Nếu làm một mình, tự review diff và ghi validation; chưa có nhánh protection tự động trong baseline này.
