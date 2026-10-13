# Commerce Web Pages

> 35 nodes · cohesion 0.10

## Key Concepts

- **Shared CSRF head loads app/storefront/TechZone styles and deferred app/TechZone scripts** (25 connections) — `src/main/resources/templates/fragments.html`
- **Red retail header with search, category disclosure and role-aware account actions; null-safe role flags** (23 connections) — `src/main/resources/templates/fragments.html`
- **Four-column shopping and support footer with scroll-to-top control** (21 connections) — `src/main/resources/templates/fragments.html`
- **Administrative dashboard, catalog, category, order and branch/IMEI navigation** (7 connections) — `src/main/resources/templates/fragments.html`
- **categories Thymeleaf template** (6 connections) — `src/main/resources/templates/admin/categories.html`
- **fragments template** (6 connections) — `src/main/resources/templates/fragments.html`
- **orders Thymeleaf template** (6 connections) — `src/main/resources/templates/orders.html`
- **products Thymeleaf template** (5 connections) — `src/main/resources/templates/admin/products.html`
- **branches template** (5 connections) — `src/main/resources/templates/branches.html`
- **mock-payment Thymeleaf template** (5 connections) — `src/main/resources/templates/mock-payment.html`
- **register Thymeleaf template** (5 connections) — `src/main/resources/templates/register.html`
- **trade-in template** (5 connections) — `src/main/resources/templates/trade-in.html`
- **login Thymeleaf template** (4 connections) — `src/main/resources/templates/login.html`
- **order-lookup Thymeleaf template** (4 connections) — `src/main/resources/templates/order-lookup.html`
- **error-page Thymeleaf template** (3 connections) — `src/main/resources/templates/error-page.html`
- **Apply approved customer trade-in request during checkout** (2 connections) — `src/main/resources/templates/checkout.html`
- **POST login using email in username field; error and registration feedback** (2 connections) — `src/main/resources/templates/login.html`
- **POST order code and phone; show items, payment state, contact and status timeline** (2 connections) — `src/main/resources/templates/order-lookup.html`
- **POST registration then redirect to login** (2 connections) — `src/main/resources/templates/register.html`
- **Physical store appraisal required; approved quote can be applied at checkout within seven days** (2 connections) — `src/main/resources/templates/trade-in.html`
- **Create categories and edit name/status with expectedVersion** (1 connections) — `src/main/resources/templates/admin/categories.html`
- **Category code and name validation** (1 connections) — `src/main/resources/templates/admin/categories.html`
- **Search, paginate and edit products with inventory and status** (1 connections) — `src/main/resources/templates/admin/products.html`
- **Active stores, province/district configured-code filters and coordinate map links** (1 connections) — `src/main/resources/templates/branches.html`
- **Store pickup reserves the device for 24 hours** (1 connections) — `src/main/resources/templates/branches.html`
- *... and 10 more nodes in this community*

## Relationships

- [Commerce Web Pages](Commerce_Web_Pages.md) (3 shared connections)

## Source Files

- `src/main/resources/static/css/storefront.css`
- `src/main/resources/static/css/techzone.css`
- `src/main/resources/templates/admin/categories.html`
- `src/main/resources/templates/admin/products.html`
- `src/main/resources/templates/branches.html`
- `src/main/resources/templates/checkout.html`
- `src/main/resources/templates/error-page.html`
- `src/main/resources/templates/fragments.html`
- `src/main/resources/templates/login.html`
- `src/main/resources/templates/mock-payment.html`
- `src/main/resources/templates/order-lookup.html`
- `src/main/resources/templates/orders.html`
- `src/main/resources/templates/register.html`
- `src/main/resources/templates/trade-in.html`

## Audit Trail

- EXTRACTED: 59 (98%)
- INFERRED: 1 (2%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*