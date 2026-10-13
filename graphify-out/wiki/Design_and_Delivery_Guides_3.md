# Design and Delivery Guides

> 52 nodes · cohesion 0.06

## Key Concepts

- **plan.md** (40 connections) — `plan.md`
- **docs\11-decisions-and-sources.md** (9 connections) — `docs/11-decisions-and-sources.md`
- **Phase 2: branches, multi-warehouse inventory and IMEI, V6** (7 connections) — `plan.md`
- **Phase 1: variants and technical specifications, V5** (6 connections) — `plan.md`
- **Phase 3: Click & Collect and instant 2h order routing, V7** (6 connections) — `plan.md`
- **Phase5 loyalty/warranty; target V9 name differs from actual V9__loyalty_and_warranty.sql** (6 connections) — `plan.md`
- **Phase 4: trade-in and installments, V8** (5 connections) — `plan.md`
- **No merchant transaction, MoMo, BNPL/bank, courier or SMS/email integration evidence** (3 connections) — `docs/12-implementation-status.md`
- **Phase 6: CellphoneS-style storefront and store/admin portal** (3 connections) — `plan.md`
- **Phase 7: E2E, 100-way last-IMEI race, optimization and acceptance** (3 connections) — `plan.md`
- **Record commit/environment/expected/actual/evidence; never mark unrun cases pass** (2 connections) — `docs/08-test-plan.md`
- **ADR-13: H2 quick tests plus real MySQL CI service; no local Docker** (2 connections) — `docs/11-decisions-and-sources.md`
- **No nationwide province/district selector, bank promotions, electronic invoice or pickup QR** (2 connections) — `docs/12-implementation-status.md`
- **Roadmap target: branch_inventories: branch/variant/quantity/reservedQuantity/version** (2 connections) — `plan.md`
- **Roadmap target: ProductComparisonService and /api/v1/products/compare for 2-3 products** (2 connections) — `plan.md`
- **Maven verify old/new tests zero failures/errors; demo and MySQL Compose start; IMEI safety; responsive desktop/mobile** (2 connections) — `plan.md`
- **Every MySQL migration requires H2 demo-compatible counterpart** (2 connections) — `plan.md`
- **Roadmap target: ImeiHistory logs import/transfer/sale/return state changes** (2 connections) — `plan.md`
- **Roadmap target: product_items_imei with globally unique imei_number** (2 connections) — `plan.md`
- **IMEI allocation must use PESSIMISTIC_WRITE; double allocation forbidden** (2 connections) — `plan.md`
- **Roadmap target: installments: 3/6/9/12 months, fees, 0% interest, approval and BNPL mocks** (2 connections) — `plan.md`
- **Roadmap target: OrderRoutingService picks nearest stocked branch by Haversine** (2 connections) — `plan.md`
- **Task3.3 remains open for SMS/email/QR; pickup reservation/expiry/receipt implemented locally** (2 connections) — `plan.md`
- **Task4.2 remains open for bank/financial/BNPL integrations; installment demo implemented** (2 connections) — `plan.md`
- **Task6.1 remains open for nationwide geography selector and bank-specific offers; responsive UI local** (2 connections) — `plan.md`
- *... and 27 more nodes in this community*

## Relationships

- No strong cross-community connections detected

## Source Files

- `docs/08-test-plan.md`
- `docs/11-decisions-and-sources.md`
- `docs/12-implementation-status.md`
- `plan.md`

## Audit Trail

- EXTRACTED: 70 (93%)
- INFERRED: 5 (7%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*