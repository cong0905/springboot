# Design and Delivery Guides

> 19 nodes · cohesion 0.27

## Key Concepts

- **EmailOutboxService** (18 connections) — `src/main/java/vn/techshop/notification/EmailOutboxService.java`
- **org.springframework.beans.factory.ObjectProvider** (15 connections)
- **EmailOutboxService.java** (14 connections) — `src/main/java/vn/techshop/notification/EmailOutboxService.java`
- **org.springframework.mail.javamail.JavaMailSender** (12 connections)
- **RecoveryMail.java** (11 connections) — `src/main/java/vn/techshop/identity/RecoveryMail.java`
- **value** (10 connections) — `src/main/java/vn/techshop/config/AdminBootstrap.java`
- **NewsletterMail.java** (8 connections) — `src/main/java/vn/techshop/content/NewsletterMail.java`
- **NewsletterMail** (8 connections) — `src/main/java/vn/techshop/content/NewsletterMail.java`
- **RecoveryMail** (8 connections) — `src/main/java/vn/techshop/identity/RecoveryMail.java`
- **org.springframework.scheduling.annotation.Scheduled** (6 connections)
- **org.springframework.mail.SimpleMailMessage** (5 connections)
- **.NewsletterMail()** (5 connections) — `src/main/java/vn/techshop/content/NewsletterMail.java`
- **.RecoveryMail()** (5 connections) — `src/main/java/vn/techshop/identity/RecoveryMail.java`
- **org.slf4j.Logger** (4 connections)
- **org.springframework.transaction.event.TransactionalEventListener** (4 connections)
- **.EmailOutboxService()** (4 connections) — `src/main/java/vn/techshop/notification/EmailOutboxService.java`
- **.processPendingEmails()** (3 connections) — `src/main/java/vn/techshop/notification/EmailOutboxService.java`
- **OutboxItem** (3 connections) — `src/main/java/vn/techshop/notification/EmailOutboxService.java`
- **JdbcTemplate** (2 connections)

## Relationships

- [Commerce Reporting](Commerce_Reporting.md) (6 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (4 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (4 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (4 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (1 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (1 shared connections)
- [Branches and Inventory](Branches_and_Inventory.md) (1 shared connections)
- [Checkout and Orders](Checkout_and_Orders.md) (1 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/AdminBootstrap.java`
- `src/main/java/vn/techshop/content/NewsletterMail.java`
- `src/main/java/vn/techshop/identity/RecoveryMail.java`
- `src/main/java/vn/techshop/notification/EmailOutboxService.java`

## Audit Trail

- EXTRACTED: 70 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*