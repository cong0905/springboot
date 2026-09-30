# Kế hoạch phân tích dữ liệu và dashboard

Không có dữ liệu bán hàng thật hoặc kết quả phân tích hiện tại. Đây là measurement plan để ứng dụng tạo dữ liệu đúng ngay từ đầu, có SQL đối chiếu và dashboard giải thích được.

## 1. Câu hỏi quản trị

1. Bao nhiêu đơn được đặt và bao nhiêu đơn đã giao thành công trong khoảng ngày?
2. Doanh thu hàng và phí giao thu được là bao nhiêu? Tiền COD đã xác nhận thu khác doanh thu hàng thế nào?
3. Sản phẩm nào bán nhiều và mang lại doanh thu hàng cao nhất?
4. Đơn hiện đang nằm ở bước nào, tồn kho nào thấp và có sai lệch ledger không?
5. Thời gian từ đặt đến giao là bao lâu? Có đủ dữ liệu để dùng chỉ số này hay chưa?

## 2. KPI dictionary

| ID | KPI | Công thức / nguồn | Time basis | Null/edge case |
|---|---|---|---|---|
| KPI-01 | Đơn đặt | COUNT orders trong range created_at | Ngày đặt | 0 nếu không có |
| KPI-02 | Đơn giao thành công | COUNT DELIVERED trong range delivered_at | Ngày giao | 0 nếu không có |
| KPI-03 | Doanh thu hàng | SUM orders.subtotal của DELIVERED | Ngày giao | 0; không gồm phí giao |
| KPI-04 | Phí giao thu | SUM shipping_fee của DELIVERED | Ngày giao | 0 |
| KPI-05 | Tổng thu COD xác nhận | SUM total_amount của DELIVERED | Ngày giao | KPI03+04; không khẳng định đã đối soát ngân hàng |
| KPI-06 | AOV tiền hàng | KPI03 / KPI02 | Ngày giao | null nếu denominator0; HALF_UP nguyên VND |
| KPI-07 | Units sold / top product | SUM order_items.quantity join DELIVERED | Ngày giao | snapshot price để tính revenue; group product_id |
| KPI-08 | Đơn theo trạng thái hiện tại | COUNT(*) group status toàn bộ orders | Snapshot asOf | Không phải transition count trong range |
| KPI-09 | Tồn thấp | Product ACTIVE có stock_quantity≤5 | Snapshot asOf | Ngưỡng5 là giả định dashboard; category active nếu báo cáo hàng bán được |
| KPI-10 | Thời gian giao trung bình | AVG(delivered_at-created_at) với DELIVERED trong range | Ngày giao | Chỉ mô tả; không coi là SLA giao hàng thật |

KPI-01 và KPI-02 dùng hai cohort thời gian khác nhau; không chia KPI02/KPI01 để gọi là conversion. Nếu muốn tỷ lệ hoàn thành cohort, lấy orders created trong cùng range và state tại cutoff, kèm cảnh báo đơn mới chưa có thời gian giao. Cancellation rate cũng cần định nghĩa cohort/cutoff; chưa đưa vào KPI Must.

## 3. Nguồn và lineage

| Sự kiện | Dữ liệu chính được ghi | Producer | Report consumer |
|---|---|---|---|
| Đặt thành công | orders/items snapshot, created_at, PENDING history, ORDER ledger | Checkout transaction | Đơn đặt, tồn, snapshots |
| Hủy hợp lệ | CANCELLED history/reason/actor/version và CANCEL ledger | Cancel transaction | State hiện tại, kiểm tra hoàn tồn |
| Xác nhận/giao | Order status/history | Admin transition | State hiện tại, timeline |
| Giao thành công và thu COD | DELIVERED, delivered_at, cod_collected=true | Admin transition | Revenue/AOV/units/time |
| Điều chỉnh kho | ADJUSTMENT delta/reason/actor/balance | Inventory service | Tồn thấp và ledger reconcile |

Các event này là transaction records, chưa có event bus. Không cần GA, data warehouse hoặc tracking thông tin cá nhân trong v0.1. Nếu cần phễu xem→thêm giỏ→checkout, bổ sung analytics event schema, consent và deduplication ở phase sau; database đơn không đủ để suy ra lượt xem.

## 4. Thiết kế dashboard

- Hàng KPI: đơn đặt, đơn giao, doanh thu hàng, phí giao, tổng thu COD, AOV; mỗi metric có time basis trong tooltip/label.
- Trend doanh thu hàng theo ngày địa phương, ngày không có dữ liệu hiển thị0 từ calendar series ở application.
- Bảng top products: SKU/tên hiện tại, units, doanh thu theo giá đã mua; không dùng price hiện tại nhân quantity.
- Panel vận hành: trạng thái hiện tại toàn hệ thống, tồn thấp; có asOf khác range báo cáo.
- Date filter theo ngày Việt Nam, default7 ngày; tối đa366 ngày. Dữ liệu demo phải ghi nhãn “demo”.

API Must hiện gồm summary/top-products. Daily trend, low-stock và delivery duration có query mẫu nhưng nếu cần endpoint/UI riêng phải thêm task/estimate; không tính là đã làm trong B-18. Không chọn thư viện chart trước khi cần triển khai.

## 5. Quy tắc SQL và kiểm soát chất lượng

[analytics.sql](../database/analytics.sql) chứa query tham chiếu. Query revenue dùng orders trực tiếp để tránh nhân số tiền khi join nhiều items. Top sản phẩm cộng line_total, không total_amount từ order. Params UTC start inclusive/end exclusive. Order status là trạng thái tại lúc query; v0.1 trạng thái cuối không thay đổi nên revenue không bị đảo do edit.

Quality checks: order subtotal khớp items; ledger sum khớp balance; ORDER/CANCEL delta khớp quantity và state; DELIVERED có cash flag/time; order có history version cuối khớp current version. Chạy đối soát sau fixture và restore. Tất cả check phải trả0 dòng vi phạm.

## 6. Cách bàn giao báo cáo

DA ghi metric definitions, SQL revision, params, timezone, dataset, expected/actual và commit. Dùng ít nhất một order nhiều items, một order đổi giá catalog, một cancel và hai order biên ngày. PO/UAT kiểm tra bằng tính tay trên fixture, không chỉ nhìn chart. Chưa có kết quả hay khuyến nghị kinh doanh từ dữ liệu thật.
