# Commerce Integration Tests

> 9 nodes · cohesion 0.22

## Key Concepts

- **.deliver()** (6 connections) — `src/main/java/vn/techshop/identity/RecoveryMail.java`
- **.deliver()** (5 connections) — `src/main/java/vn/techshop/content/NewsletterMail.java`
- **.enqueue()** (5 connections) — `src/main/java/vn/techshop/notification/EmailOutboxService.java`
- **.emailOutboxEnqueuesAndRetainsThroughRestarts()** (4 connections) — `src/test/java/vn/techshop/notification/EmailOutboxAndReviewModerationTest.java`
- **OptInEmail** (3 connections) — `src/main/java/vn/techshop/content/NewsletterService.java`
- **ResetEmail** (3 connections) — `src/main/java/vn/techshop/identity/RecoveryService.java`
- **.getPending()** (3 connections) — `src/main/java/vn/techshop/notification/EmailOutboxService.java`
- **org.springframework.scheduling.annotation.Async** (1 connections)
- **org.springframework.scheduling.annotation.Async** (1 connections)

## Relationships

- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (3 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (1 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/content/NewsletterMail.java`
- `src/main/java/vn/techshop/content/NewsletterService.java`
- `src/main/java/vn/techshop/identity/RecoveryMail.java`
- `src/main/java/vn/techshop/identity/RecoveryService.java`
- `src/main/java/vn/techshop/notification/EmailOutboxService.java`
- `src/test/java/vn/techshop/notification/EmailOutboxAndReviewModerationTest.java`

## Audit Trail

- EXTRACTED: 10 (71%)
- INFERRED: 4 (29%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*