# Sơ đồ nghiệp vụ và hệ thống

Mermaid là nguồn sơ đồ, render trực tiếp trên GitHub. Đây là mô hình thiết kế dự kiến. ERD ở [data design](03-data-design.md); container/module ở [architecture](02-architecture.md). Actor ADMIN là người thao tác, không phải tác vụ tự động.

## D-01 · Phạm vi use case theo actor

Mermaid không có UML use-case native trong bộ này; sơ đồ dưới là bản đồ actor/chức năng, không trình bày như UML use-case chuẩn.

```mermaid
flowchart TD
  G["Guest"] --> V["Xem / tìm sản phẩm"]
  G --> R["Đăng ký / đăng nhập"]
  C["Customer"] --> V
  C --> K["Giỏ / đặt đơn COD"]
  C --> H["Xem / hủy đơn của mình"]
  A["Admin"] --> P["Quản lý catalog / kho"]
  A --> O["Xử lý đơn"]
  A --> T["Báo cáo vận hành"]
```

## D-02 · Activity đặt hàng

```mermaid
flowchart TD
  S["Khách mở checkout"] --> L{"Đã đăng nhập?"}
  L -->|Chưa| N["Đăng nhập"]
  N --> S
  L -->|Có| E{"Giỏ có hàng?"}
  E -->|Không| X["Về danh sách sản phẩm"]
  E -->|Có| P["Lấy preview / nhập địa chỉ"]
  P --> U["Xác nhận giá / gửi key và version"]
  U --> I{"Key đã thành công?"}
  I -->|Có| H{"Hash request khớp?"}
  H -->|Có| R["Trả đơn đã tạo"]
  H -->|Không| K["409 key bị dùng lại"]
  I -->|Chưa| V{"Giỏ / giá / tồn hợp lệ?"}
  V -->|Không| F["Rollback / báo lỗi / cập nhật preview"]
  F --> P
  V -->|Có| D["Ghi đơn / giảm tồn / ledger / clear giỏ"]
  D --> C{"Commit thành công?"}
  C -->|Có| R
  C -->|Không| B["Rollback toàn bộ"]
  B --> Q["Cho retry cùng key nếu lỗi tạm thời"]
  Q --> U
```

Validate quyền, body và CSRF trước nhánh idempotency. Retry cùng key chỉ dùng khi payload không thay đổi. Giá/giỏ thay đổi phải preview và xác nhận lại với key mới; không tự đặt theo giá mới.

## D-03 · Sequence đăng nhập

```mermaid
sequenceDiagram
  actor U as Khách
  participant B as Trình duyệt
  participant S as Spring Security
  participant I as Identity service
  participant D as MySQL
  U->>B: Mở login
  B->>S: GET login
  S-->>B: Form và CSRF token
  U->>B: Nhập email và password
  B->>S: POST login với CSRF
  S->>I: Load user theo normalized email
  I->>D: Query user
  D-->>I: User hoặc không tồn tại
  I-->>S: UserDetails
  S->>S: PasswordEncoder.matches
  alt Hợp lệ
    S->>S: Đổi session id và lưu context
    S-->>B: Set cookie và redirect 303
    B-->>U: Trang theo role
  else Không hợp lệ
    S-->>B: Redirect login với lỗi chung
    B-->>U: Yêu cầu thử lại
  end
```

Không log password/token; rate limit chặn trước khi xử lý quá nhiều lần đăng nhập. Response redirect thực tế cần cấu hình success/failure handler trong bootstrap security.

## D-04 · Sequence checkout và idempotency

```mermaid
sequenceDiagram
  actor B as Khách qua trình duyệt
  participant O as Checkout service
  participant C as Cart service
  participant I as Inventory service
  participant D as MySQL
  B->>O: POST order với CSRF, key, version và preview hash
  O->>O: Validate principal và canonical request hash
  O->>D: BEGIN transaction
  O->>C: Lock cart của principal
  C->>D: SELECT cart FOR UPDATE
  O->>D: Tìm order theo user và checkout key
  alt Key đã có và hash khớp
    D-->>O: Order snapshot cũ
    O->>D: COMMIT read path
    O-->>B: 200, Idempotent-Replay true
  else Key đã có và hash khác
    O->>D: ROLLBACK
    O-->>B: 409 CHECKOUT_KEY_REUSED
  else Key chưa có
    O->>C: Kiểm tra version và items
    O->>I: Lock products theo id tăng dần
    I->>D: SELECT products FOR UPDATE
    D-->>I: Giá, status, tồn hiện tại
    I-->>O: Product snapshots để kiểm tra
    O->>O: Validate category, stock, total và pricingHash
    alt Hợp lệ
      O->>D: INSERT order, items, initial history
      O->>I: Giảm stock và ghi ORDER movements
      I->>D: UPDATE stock và INSERT ledger
      O->>C: Clear items và tăng cart version
      C->>D: DELETE items, UPDATE cart
      O->>D: COMMIT
      O-->>B: 201 và Location order
    else Giỏ, giá, tồn hoặc DB không hợp lệ
      O->>D: ROLLBACK tất cả
      O-->>B: 400 hoặc 409 hoặc 500 theo lỗi
    end
  end
```

Các thông điệp gọi service ở đây cùng một transaction do CheckoutService mở. API không lưu userId từ request. Nếu mất response sau commit, gửi lại cùng key/hash.

## D-05 · Activity hủy đơn

```mermaid
flowchart TD
  A["Nhận yêu cầu hủy"] --> O{"Có quyền trên đơn?"}
  O -->|Không| N["404 hoặc 403 theo route"]
  O -->|Có| L["Lock order"]
  L --> C{"Đã CANCELLED?"}
  C -->|Có| R["Trả đơn cũ / không hoàn kho nữa"]
  C -->|Chưa| V{"State và version hợp lệ?"}
  V -->|Không| E["409 / rollback"]
  V -->|Có| P["Lock products theo id tăng dần"]
  P --> K["Hoàn stock / CANCEL ledger"]
  K --> H["Ghi CANCELLED / reason / history"]
  H --> T{"Commit thành công?"}
  T -->|Có| R
  T -->|Không| E
```

## D-06 · Sequence cạnh tranh confirm/cancel

```mermaid
sequenceDiagram
  actor C as Customer
  actor A as Admin
  participant O as Order service
  participant D as MySQL
  C->>O: Cancel order version 0
  A->>O: Confirm order version 0
  O->>D: Cancel lấy write lock order
  D-->>O: PENDING version 0
  O->>D: Hoàn tồn, CANCELLED, version 1, history
  O->>D: COMMIT và release lock
  O-->>C: Cancel thành công
  O->>D: Confirm lấy write lock sau đó
  D-->>O: CANCELLED version 1
  O->>D: ROLLBACK vì state/version không hợp lệ
  O-->>A: 409, reload đơn
```

Nếu confirm lấy lock trước, đơn thành CONFIRMED/version1; customer cancel version0 thua và không hoàn tồn. Admin vẫn có thể cancel CONFIRMED bằng yêu cầu mới sau reload. Sơ đồ là một interleaving, test phải kiểm tra cả hai thứ tự.

## D-07 · State machine đơn COD

```mermaid
stateDiagram-v2
  direction TB
  [*] --> PENDING: Checkout commit
  PENDING --> CONFIRMED: Admin xác nhận
  PENDING --> CANCELLED: Customer hoặc Admin hủy
  CONFIRMED --> SHIPPED: Admin bàn giao giao hàng
  CONFIRMED --> CANCELLED: Admin hủy trước giao
  SHIPPED --> DELIVERED: Admin xác nhận thu COD
  CANCELLED --> [*]
  DELIVERED --> [*]
```

| Transition | Điều kiện | Tác động tồn/tiền |
|---|---|---|
| Tạo → PENDING | Checkout hợp lệ | Giảm tồn; chưa ghi doanh thu |
| PENDING → CONFIRMED | Admin + version hiện tại | Không thay tồn |
| CONFIRMED → SHIPPED | Admin + version hiện tại | Không thay tồn |
| SHIPPED → DELIVERED | Admin + codCollected=true | Ghi delivered_at; đủ điều kiện báo cáo thu COD |
| PENDING → CANCELLED | Owner hoặc admin + lý do | Hoàn tồn đúng một lần |
| CONFIRMED → CANCELLED | Admin + lý do | Hoàn tồn đúng một lần |

Không có transition SHIPPED → CANCELLED. Hàng giao thất bại/đổi trả phải bổ sung thiết kế riêng, không dùng chỉnh stock để che sai trạng thái đơn.

## D-08 · Class diagram miền nghiệp vụ cốt lõi

```mermaid
classDiagram
  class Cart {
    Long id
    Long userId
    Long version
  }
  class CartItem {
    Long productId
    int quantity
  }
  class Product {
    Long id
    String sku
    BigDecimal price
    int stockQuantity
    Long version
  }
  class Order {
    Long id
    String status
    BigDecimal totalAmount
    Long version
    transitionTo(targetStatus)
  }
  class OrderItem {
    String skuSnapshot
    String nameSnapshot
    BigDecimal unitPrice
    int quantity
  }
  Cart "1" *-- "0..20" CartItem
  CartItem "*" --> "1" Product
  Order "1" *-- "1..20" OrderItem
  OrderItem "*" --> "1" Product
```

Class diagram là domain sketch, không quy định getter/setter hay cascade JPA. Checkout/cancel là application orchestration; entity không gọi repository hoặc HTTP.

## D-09 · Quy trình phát triển và quality gate

```mermaid
flowchart TD
  B["Backlog / yêu cầu"] --> R["Làm rõ AC / dependency / thiết kế"]
  R --> D{"Đạt Definition of Ready?"}
  D -->|Chưa| R
  D -->|Có| I["Branch / implement lát cắt / test"]
  I --> P["PR + cập nhật tài liệu"]
  P --> C{"CI và review đạt?"}
  C -->|Chưa| I
  C -->|Có| M["Merge / staging demo"]
  M --> U{"UAT đạt?"}
  U -->|Chưa| F["Bug / sửa theo AC"]
  F --> I
  U -->|Có| L["Release checklist / tag / bàn giao"]
  L --> N["Theo dõi lỗi và backlog vòng sau"]
  N --> B
```

## D-10 · Sequence truy vấn báo cáo

```mermaid
sequenceDiagram
  actor A as Admin
  participant W as Report controller
  participant S as Reporting service
  participant D as MySQL
  A->>W: fromDate và toDate theo ngày Việt Nam
  W->>W: Check ADMIN và validate range
  W->>S: Query range
  S->>S: Convert local days sang UTC half-open
  S->>D: Aggregate DELIVERED theo delivered_at
  D-->>S: Revenue, phí giao, số đơn
  S->>D: Count placed theo created_at và current status
  D-->>S: Operational counts
  S->>S: Tính AOV và metadata thời gian
  S-->>W: ReportSummary
  W-->>A: KPI với nhãn và khoảng ngày rõ
```
