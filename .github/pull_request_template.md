## Vấn đề và behavior sau thay đổi

Mô tả trigger, kết quả trước/sau và giá trị cho người dùng.

## Phạm vi và truy vết

- Backlog:
- FR / BR / AC:
- Schema / API / tài liệu ảnh hưởng:
- CR nếu thay yêu cầu:

## Validation

Ghi lệnh/test thực sự đã chạy, môi trường và kết quả; không đánh dấu test chưa chạy là pass.

## Checklist

- [ ] AC của task đạt, có bằng chứng.
- [ ] Quyền/ownership/CSRF kiểm tra khi liên quan.
- [ ] Money/stock/idempotency/rollback có critical test khi liên quan.
- [ ] Không secrets/PII trong source, payload hoặc log.
- [ ] Spec/API/data/diagram/test/README cập nhật khi behavior thay đổi.
- [ ] Giới hạn và rủi ro còn lại được ghi rõ.
