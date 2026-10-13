# IMEI Lifecycle

> 22 nodes · cohesion 0.10

## Key Concepts

- **ImeiHistory** (20 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **ImeiHistoryRepository.java** (4 connections) — `src/main/java/vn/techshop/imei/ImeiHistoryRepository.java`
- **ImeiHistoryRepository** (4 connections) — `src/main/java/vn/techshop/imei/ImeiHistoryRepository.java`
- **.lockById()** (4 connections) — `src/main/java/vn/techshop/imei/ImeiHistoryRepository.java`
- **.getFromBranchId()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.getFromStatus()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.getId()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.getImeiId()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.getOccurredAt()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.getReason()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.getToBranchId()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.getToStatus()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.setFromBranchId()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.setFromStatus()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.setImeiId()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.setOccurredAt()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.setReason()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.setToBranchId()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **.setToStatus()** (1 connections) — `src/main/java/vn/techshop/imei/ImeiHistory.java`
- **Entity** (1 connections)
- **Table** (1 connections)
- **Lock** (1 connections)

## Relationships

- [Cart and Pricing](Cart_and_Pricing.md) (1 shared connections)
- [Branches and Inventory](Branches_and_Inventory.md) (1 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (1 shared connections)
- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/imei/ImeiHistory.java`
- `src/main/java/vn/techshop/imei/ImeiHistoryRepository.java`

## Audit Trail

- EXTRACTED: 27 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*