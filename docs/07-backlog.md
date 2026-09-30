# Backlog triển khai

Trạng thái tất cả task: **Planned**. Giờ là ước lượng cho người có nền tảng Java/SQL, chưa phải giờ đã làm. Các yêu cầu được lấy từ [SRS](01-spec.md). Một task gồm code và test trực tiếp của feature; B-20 là regression xuyên module. Tổng Must 90 giờ + dự phòng 18 giờ = capacity 108 giờ.

| ID | Task / kết quả cần có | FR/NFR | Phụ thuộc | Giờ | Ưu tiên |
|---|---|---|---|---:|---|
| B-01 | Review giả định, chốt AC và giới hạn MVP | G-01–05 | Không | 2 | Must |
| B-02 | Bootstrap Java21/Boot/Maven wrapper; config dev/test/prod, MySQL local | NFR-06 | B-01 | 3 | Must |
| B-03 | Chuyển DDL thành Flyway; mapping entity, constraint, seed giả lập | NFR-02/06 | B-02 | 4 | Must |
| B-04 | CI Maven verify và docs validation; kiểm tra secrets/config | NFR-08 | B-02 | 2 | Must |
| B-05 | Catalog public, detail, search/filter/page/sort, template cơ bản | FR-03 | B-03 | 4 | Must |
| B-06 | Register/login/logout/session; hash/validation/admin bootstrap | FR-01 | B-03 | 5 | Must |
| B-07 | Role/owner guards, CSRF/API errors, login limit | FR-02,NFR-01 | B-06 | 4 | Must |
| B-08 | Admin categories, unique code, inactive/version | FR-04 | B-05/07 | 2 | Must |
| B-09 | Admin product/asset images, SKU immutable, DTO/version | FR-05 | B-08 | 4 | Must |
| B-10 | Inventory adjustment và ledger, lock product, balance check | FR-06 | B-09 | 4 | Must |
| B-11 | Cart/items, lock/version, giới hạn, cùng SKU cộng số lượng | FR-07 | B-05/07 | 5 | Must |
| B-12 | Pricing preview, phí giao, pricingHash, hiển thị cảnh báo | FR-08 | B-11 | 3 | Must |
| B-13 | Checkout transaction, snapshot, stock ORDER, clear cart | FR-08,NFR-02 | B-10/12 | 7 | Must |
| B-14 | Key/hash idempotency, retry/replay và unique conflict path | FR-09 | B-13 | 4 | Must |
| B-15 | Order list/detail của chủ đơn, timeline/snapshot | FR-10 | B-13/07 | 3 | Must |
| B-16 | Customer/admin cancel, hoàn tồn đúng một lần, race test | FR-11 | B-14/15 | 4 | Must |
| B-17 | Admin orders/transition, version/history, COD confirm | FR-12 | B-16 | 3 | Must |
| B-18 | Reporting queries/KPI/date timezone và dashboard | FR-13 | B-17 | 4 | Must |
| B-19 | Hoàn thiện responsive, form errors, focus/labels/empty states | NFR-04 | B-15/17/18 | 3 | Must |
| B-20 | Security, MySQL concurrency/rollback regression, report reconciliation | NFR-01/02/08 | B-14/16/17/18 | 8 | Must |
| B-21 | Load dataset, đo p95/query/N+1 và ghi kết quả thực tế | NFR-03 | B-20 | 2 | Must |
| B-22 | Docker/staging, health/secrets, backup restore drill | NFR-05/06/07 | B-20 | 6 | Must |
| B-23 | README chạy thật, UAT checklist và biên bản bàn giao | G-05 | B-21/22 | 2 | Must |
| B-24 | Video demo, release notes, kiểm tra cuối và tag MVP | G-01–05 | B-23 | 2 | Must |

Ước lượng thấp cho B-13/16/20 là rủi ro cần theo dõi, không cắt test để giữ lịch. B-13 có thể tách 13a snapshots, 13b locking/transaction, 13c adapter/UI trong cùng 7 giờ ước lượng; nếu actual vượt, re-estimate và dùng buffer/giãn lịch. B-22 phụ thuộc quyết định hosting và quyền triển khai; đây chưa là yêu cầu deploy ngay.

## User story theo epic

| Epic | User story | AC liên quan | Task |
|---|---|---|---|
| E-01 Identity | Là khách, tôi đăng ký/đăng nhập để mua và xem đơn của mình | AC-01–03,15 | B-06/07 |
| E-02 Catalog | Là người mua, tôi tìm hàng theo tên/category/giá để chọn đúng sản phẩm | AC-04/05 | B-05 |
| E-03 Catalog admin | Là admin, tôi quản lý hàng và danh mục mà không mất lịch sử đơn | AC-19 | B-08/09 |
| E-04 Inventory | Là admin, tôi điều chỉnh kho có lý do để đối soát | AC-20 | B-10 |
| E-05 Cart | Là khách, tôi sửa số lượng và thấy tổng mới trước khi mua | AC-06–08 | B-11/12 |
| E-06 Checkout | Là khách, tôi đặt COD một lần dù mạng chậm/bấm lại | AC-09–14 | B-13/14 |
| E-07 Orders | Là khách, tôi xem/hủy đơn còn chờ và biết tồn kho được hoàn đúng | AC-15–18 | B-15/16 |
| E-08 Fulfillment | Là admin, tôi xử lý đơn đúng thứ tự và xác nhận đã thu COD | AC-21/22 | B-17 |
| E-09 Analytics | Là admin, tôi xem doanh thu và hàng bán chạy theo ngày Việt Nam | AC-23–25 | B-18 |
| E-10 Delivery quality | Là người bàn giao, tôi cần project chạy lại được và tests đáng tin | NFR-01–08 | B-04/19–24 |

## Backlog sau MVP, chưa ước lượng

P2-01 upload ảnh; P2-02 reset password/email; P2-03 nhiều địa chỉ; P2-04 delivery failed/returns; P2-05 coupons; P2-06 đánh giá sau mua; P2-07 online payments; P2-08 SKU variants/multi-warehouse. Mỗi mục cần spec/AC/effort mới trước khi vào sprint.

## Task template khi tạo GitHub Issue

```markdown
Title: [B-13] Checkout COD nguyên tử
Problem: Khách cần tạo một đơn chính xác từ giỏ, không bán vượt tồn.
Scope: Order snapshots, lock/stock ledger, clear cart, adapter và transaction.
References: FR-08, BR-05–11, AC-09/10/13/14.
Depends on: B-10, B-12.
Acceptance: Happy path pass, price conflict rollback, last-unit test pass.
Validation: MySQL integration test + UI demo + log không có PII.
Estimate: 7h, cập nhật sau triển khai.
Out of scope: Payment gateway và email.
```

Không giả định issue, project board, branch protection hoặc người nhận đã được tạo. Bộ tài liệu là đầu vào cho việc đó.
