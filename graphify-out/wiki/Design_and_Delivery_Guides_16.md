# Design and Delivery Guides

> 15 nodes · cohesion 0.16

## Key Concepts

- **SecurityConfig.java** (13 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **RequestIdFilter.java** (7 connections) — `src/main/java/vn/techshop/shared/RequestIdFilter.java`
- **ioexception** (5 connections) — `src/main/java/vn/techshop/config/RetailDemoCatalog.java`
- **RequestIdFilter** (5 connections) — `src/main/java/vn/techshop/shared/RequestIdFilter.java`
- **.doFilterInternal()** (4 connections) — `src/main/java/vn/techshop/shared/RequestIdFilter.java`
- **org.springframework.web.filter.OncePerRequestFilter** (3 connections)
- **org.springframework.security.config.annotation.web.builders.HttpSecurity** (2 connections)
- **org.springframework.security.web.SecurityFilterChain** (2 connections)
- **servlet** (2 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **bcryptpasswordencoder** (1 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`
- **FilterChain** (1 connections)
- **HttpServletRequest** (1 connections)
- **HttpServletResponse** (1 connections)
- **org.springframework.core.annotation.Order** (1 connections)
- **usernamepasswordauthenticationfilter** (1 connections) — `src/main/java/vn/techshop/config/SecurityConfig.java`

## Relationships

- [Identity and Sessions](Identity_and_Sessions.md) (3 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (2 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (2 shared connections)
- [News and Newsletter](News_and_Newsletter.md) (1 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)
- [Commerce Integration Tests](Commerce_Integration_Tests.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/RetailDemoCatalog.java`
- `src/main/java/vn/techshop/config/SecurityConfig.java`
- `src/main/java/vn/techshop/shared/RequestIdFilter.java`

## Audit Trail

- EXTRACTED: 27 (100%)
- INFERRED: 0 (0%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*