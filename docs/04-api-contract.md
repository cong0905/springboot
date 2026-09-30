# Hợp đồng HTTP/API dự kiến

Đây là contract thiết kế, chưa có endpoint chạy. Giao diện Thymeleaf và JSON API là hai adapter cho **cùng application service**, không có hai bộ business rules. JSON phục vụ tương tác giỏ/admin và kiểm thử, cùng origin; v0.1 không mở API tích hợp bên ngoài.

## 1. Quy ước chung

- API prefix /api/v1. Content-Type application/json, Accept application/json. JSON trả UTC ISO-8601 với Z; field camelCase.
- Authentication bằng session cookie JSESSIONID, không Authorization Bearer. Role/owner được lấy từ principal.
- GET /api/v1/csrf public trả token từ session hiện tại, headerName=X-CSRF-TOKEN, parameterName=_csrf; không cache response. Mọi POST/PATCH/DELETE kể cả register/login/logout gửi CSRF. Không log token.
- Login/logout dùng form Spring Security; POST /login nhận application/x-www-form-urlencoded: username=email, password, _csrf. Thành công redirect 303 về trang phù hợp; thất bại redirect trang login có lỗi chung. GET /login render HTML. Không có JSON JWT login.
- API chưa auth trả 401 JSON; thiếu role hoặc sai CSRF trả 403 JSON; HTML chưa auth redirect login. Login rate limit 429 theo policy ở architecture.
- Page bắt đầu 0, size default12/max50; order list default20; sort allowlist. Response page: content, page, size, totalElements, totalPages.
- Giá VND JSON là số nguyên (BigDecimal khi deserialize server); id/version là số nguyên. Không nhận unknown fields. Read-only fields không nằm trong request DTO.
- POST tạo resource trả 201 + Location; GET/PATCH trả 200; DELETE giỏ trả 204. Checkout replay trả 200 + Idempotent-Replay: true; lần đầu trả 201. Form adapter tương ứng redirect 303.
- Request mutation với version gửi expectedVersion trong body; DELETE giỏ dùng query expectedVersion. Đây không phải ETag/If-Match; không phối trộn hai cơ chế ở v0.1.

## 2. Danh mục endpoint

| ID | Method/route | Quyền | Request/response | Lỗi nghiệp vụ |
|---|---|---|---|---|
| API-01 | GET /api/v1/csrf | Public | token/headerName/parameterName | 500 |
| API-02 | POST /api/v1/auth/register | GUEST | RegisterRequest → UserSummary | 400,409 EMAIL_EXISTS |
| API-03 | GET /api/v1/me | Authenticated | UserSummary | 401 |
| API-04 | GET /api/v1/categories | Public | ACTIVE category list | 400 |
| API-05 | GET /api/v1/products | Public | keyword/categoryId/minPrice/maxPrice/sort/page/size → ProductPage | 400 |
| API-06 | GET /api/v1/products/{id} | Public | ProductDetail | 404 |
| API-07 | GET /api/v1/cart | CUSTOMER | CartView với version/pricingHash | 401,403 |
| API-08 | POST /api/v1/cart/items | CUSTOMER | productId,quantity,expectedVersion → CartView | 400,404,409 CART_CHANGED/CART_LIMIT |
| API-09 | PATCH /api/v1/cart/items/{itemId} | CUSTOMER | quantity,expectedVersion → CartView | 400,404,409 |
| API-10 | DELETE /api/v1/cart/items/{itemId}?expectedVersion=N | CUSTOMER | 204 | 404,409 |
| API-11 | POST /api/v1/orders | CUSTOMER | CheckoutRequest → OrderDetail | 400,409,404 |
| API-12 | GET /api/v1/orders | CUSTOMER | status optional,page,size → OrderPage of principal | 400 |
| API-13 | GET /api/v1/orders/{id} | CUSTOMER owner | OrderDetail + history | 404 |
| API-14 | POST /api/v1/orders/{id}/cancel | CUSTOMER owner | reason,expectedVersion → OrderDetail | 400,404,409 |
| API-15 | GET /api/v1/admin/categories | ADMIN | All categories, page,size,status | 400 |
| API-16 | POST /api/v1/admin/categories | ADMIN | code,name,status → CategoryView | 400,409 CODE_EXISTS |
| API-17 | PATCH /api/v1/admin/categories/{id} | ADMIN | name,status,expectedVersion → CategoryView; code immutable | 400,404,409 |
| API-18 | GET /api/v1/admin/products | ADMIN | page,size,status,categoryId,keyword → ProductPage incl inactive | 400 |
| API-19 | GET /api/v1/admin/products/{id} | ADMIN | ProductDetail incl version/inactive | 404 |
| API-20 | POST /api/v1/admin/products | ADMIN | ProductCreate → ProductDetail stock=0 | 400,404,409 SKU_EXISTS |
| API-21 | PATCH /api/v1/admin/products/{id} | ADMIN | ProductUpdate → ProductDetail; không SKU/stock | 400,404,409 STALE_VERSION |
| API-22 | POST /api/v1/admin/products/{id}/stock-adjustments | ADMIN | delta,reason,expectedVersion → StockResult | 400,404,409 INSUFFICIENT_STOCK/STALE_VERSION |
| API-23 | GET /api/v1/admin/products/{id}/stock-movements | ADMIN | page,size → MovementPage | 404 |
| API-24 | GET /api/v1/admin/orders | ADMIN | status,keywordId,createdFrom,createdTo,page,size → OrderPage | 400 |
| API-25 | GET /api/v1/admin/orders/{id} | ADMIN | OrderDetail + history | 404 |
| API-26 | POST /api/v1/admin/orders/{id}/transitions | ADMIN | targetStatus,expectedVersion,codCollected optional → OrderDetail | 400,404,409 |
| API-27 | POST /api/v1/admin/orders/{id}/cancel | ADMIN | reason,expectedVersion → OrderDetail | 400,404,409 |
| API-28 | GET /api/v1/admin/reports/summary | ADMIN | fromDate,toDate Việt Nam → ReportSummary | 400 |
| API-29 | GET /api/v1/admin/reports/top-products | ADMIN | fromDate,toDate,limit default10/max50 → ProductMetric[] | 400 |

API-24 date filters dùng YYYY-MM-DD theo ngày Việt Nam, inclusive input → half-open UTC. API-12 không cho userId để mở rộng scope. API-26 chỉ CONFIRMED/SHIPPED/DELIVERED, hủy dùng endpoint riêng. Stock log endpoint không trả địa chỉ/phone.

## 3. Schema request

| Schema | Required fields | Optional fields/quy tắc |
|---|---|---|
| RegisterRequest | fullName,email,password | Không role; theo validation SRS |
| CartAdd | productId,quantity,expectedVersion | Add cộng quantity; result phải ≤99 |
| CartUpdate | quantity,expectedVersion | Set quantity tuyệt đối |
| CheckoutRequest | checkoutKey,cartVersion,expectedTotal,expectedPricingHash,recipientName,phone,address | note nullable/≤500; paymentMethod không nhận, server cố định COD |
| CancelRequest | reason,expectedVersion | reason canonical trim; replay CANCELLED không đòi version trùng |
| CategoryCreate | code,name,status | status ACTIVE/INACTIVE |
| CategoryUpdate | name,status,expectedVersion | PATCH contract cập nhật toàn bộ editable fields, không partial-null semantics |
| ProductCreate | categoryId,sku,name,price,status | description nullable, images default[] |
| ProductUpdate | categoryId,name,price,status,expectedVersion | description nullable, images default[] thay toàn bộ; stock/sku bị từ chối |
| ProductImage | assetPath,altText,sortOrder | sortOrder≥0; tối đa5 ảnh, đường dẫn allowlist |
| StockAdjustment | delta,reason,expectedVersion | delta≠0, balance không vượt INT max và không âm |
| TransitionRequest | targetStatus,expectedVersion | codCollected bắt buộc true cho DELIVERED; absent/false cho status khác |

PATCH category/product dùng full editable DTO giúp form nhất quán; nếu chuyển sang partial PATCH phải bổ sung phân biệt omitted/null trước khi implement. Body lớn giới hạn 64KB vì không upload trong MVP.

## 4. Schema response

| Schema | Fields |
|---|---|
| UserSummary | id,fullName,email,role; không passwordHash |
| CategoryView | id,code,name,status,version; public có thể bỏ version |
| ProductSummary | id,sku,name,price,currency,stockQuantity,thumbnailPath,categoryId |
| ProductDetail | ProductSummary + description,images,status,version,categoryName |
| CartView | version,items[{itemId,productId,name,unitPrice,quantity,lineTotal,available,warningCode}],subtotal,shippingFee,total,currency,pricingHash,canCheckout |
| OrderSummary | id,orderCode,status,subtotal,shippingFee,total,currency,createdAt,deliveredAt,version |
| OrderDetail | OrderSummary + recipientName,phone,address,note,items[{productId,sku,name,unitPrice,quantity,lineTotal}],history[{fromStatus,toStatus,occurredAt,reason}] |
| StockResult | productId,stockQuantity,version,movementId |
| Movement | id,type,delta,balanceAfter,reason,actorId,occurredAt; chỉ admin |
| ReportSummary | fromDate,toDate,timeZone,placedOrders,deliveredOrders,merchandiseRevenue,shippingCollected,codCollectedTotal,aov,statusCountsAsOf |
| ProductMetric | productId,sku,name,unitsSold,merchandiseRevenue; tên hiện tại từ catalog, tiền theo snapshot |

statusCountsAsOf là số đơn theo trạng thái hiện tại toàn hệ thống tại response timestamp, không phải số transition trong range. deliveredOrders/revenue theo delivered_at; placedOrders theo created_at. aov = merchandiseRevenue/deliveredOrders làm tròn HALF_UP 0 chữ số, không có đơn trả null. Không dùng total đơn PENDING làm doanh thu.

## 5. Ví dụ checkout

Ví dụ dữ liệu giả lập; không phải dữ liệu khách thật. Giả định CartView có subtotal=600000, fee=30000, total=630000 và hash SHA-256 lấy từ server.

```json
{
  "checkoutKey": "b4598e80-c5a1-4c4e-b5e1-6b712c81b130",
  "cartVersion": 3,
  "expectedTotal": 630000,
  "expectedPricingHash": "9e65c8272f64e44e5a16e6a50a41a6a468dc92ba84f14c64245f34556e475e4c",
  "recipientName": "Khách Demo",
  "phone": "0900000000",
  "address": "Địa chỉ giả lập phục vụ kiểm thử hệ thống",
  "note": null
}
```

Hash trên chỉ minh hoạ hình thức 64 ký tự; không phải hash thực của giỏ. Client gửi nguyên pricingHash preview, không tự tính tiền/hash để server tin.

## 6. Idempotency và canonicalization

pricingHash = SHA-256 của canonical JSON gồm lines sorted productId [{productId,quantity,unitPrice}], currency, shippingFee. Server dùng integer decimal string trong canonical representation để tránh khác dấu thập phân/key-order; cùng dữ liệu tạo cùng hash.

requestHash = SHA-256 canonical JSON của principal.userId + cartVersion + expectedTotal + expectedPricingHash + recipientName trim + phone trim + address trim + note trim (blank/null thành null). checkoutKey không cần nằm trong hash vì đã là lookup key. Cùng key + requestHash khác trả CHECKOUT_KEY_REUSED. Nếu key đã thành công, kiểm tra quyền và hash rồi trả snapshot order cũ, không yêu cầu cart hiện tại còn items hay price vẫn giống.

Khoá cart bảo đảm hai checkout của cùng user serialize. Unique(user,key) là lớp bảo vệ DB. Nếu race vẫn dẫn tới unique conflict, rollback transaction hiện tại rồi đọc order trong transaction mới để so hash; không query tiếp trong transaction đã đánh dấu rollback-only. Không giữ transaction trong lúc render response.

Cancel idempotent theo trạng thái cuối của chính order; request lặp sau CANCELLED chỉ trả resource, không ghi thêm reason/history. Transition khác không có retry-idempotency key; UI reload khi xung đột và phải xác nhận hành động tiếp.

## 7. Hợp đồng lỗi

```json
{
  "code": "INSUFFICIENT_STOCK",
  "message": "Số lượng còn lại không đủ. Vui lòng kiểm tra giỏ hàng.",
  "requestId": "req-demo-001",
  "fieldErrors": [],
  "details": {"productId": 12, "availableQuantity": 1}
}
```

| HTTP | Code tiêu biểu | Client xử lý |
|---|---|---|
| 400 | VALIDATION_ERROR, INVALID_DATE_RANGE | Hiện lỗi field; giữ input không nhạy cảm |
| 401 | AUTHENTICATION_REQUIRED | Chuyển login, không tự submit đơn sau login |
| 403 | ACCESS_DENIED, CSRF_INVALID | Reload token/trang; không disable CSRF |
| 404 | RESOURCE_NOT_FOUND | Trang không tồn tại, không xác nhận owner khác |
| 409 | CART_CHANGED, PRICE_CHANGED, INSUFFICIENT_STOCK, PRODUCT_UNAVAILABLE | Reload giỏ/preview, xác nhận lại với key mới |
| 409 | CHECKOUT_KEY_REUSED, STALE_VERSION, INVALID_ORDER_TRANSITION | Không tự retry payload thay đổi |
| 409 | RETRYABLE_CONFLICT | Checkout retry cùng key/payload; thao tác khác reload |
| 429 | TOO_MANY_ATTEMPTS | Theo Retry-After; không loop login |
| 500 | INTERNAL_ERROR | Hiện requestId; không expose stack/SQL/secret |

## 8. Route HTML chính

GET /, /products, /products/{id}, /register, /login public. CUSTOMER: GET /cart, /checkout, /orders, /orders/{id}; POST /checkout dùng cùng CheckoutRequest semantics. ADMIN: GET /admin, /admin/categories, /admin/products, /admin/products/{id}/edit, /admin/orders, /admin/orders/{id}, /admin/reports. Form mutations gọi controller thuộc module tương ứng và application service dùng chung; adapter có thể dùng route POST riêng thay PATCH nếu form HTML yêu cầu.
