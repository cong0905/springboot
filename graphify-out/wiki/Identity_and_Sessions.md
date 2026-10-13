# Identity and Sessions

> 52 nodes · cohesion 0.06

## Key Concepts

- **AppUser** (38 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.send()** (16 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.profileView()** (13 connections) — `src/main/java/vn/techshop/identity/IdentityService.java`
- **GooglePrincipal** (10 connections) — `src/main/java/vn/techshop/identity/GooglePrincipal.java`
- **.view()** (10 connections) — `src/main/java/vn/techshop/identity/IdentityService.java`
- **UserView** (10 connections) — `src/main/java/vn/techshop/identity/IdentityService.java`
- **.listUsers()** (7 connections) — `src/main/java/vn/techshop/identity/IdentityService.java`
- **.TechPrincipal()** (7 connections) — `src/main/java/vn/techshop/identity/TechPrincipal.java`
- **org.springframework.security.oauth2.core.oidc.user.OidcUser** (5 connections)
- **.getFullName()** (5 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.getId()** (5 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.findByEmailContainingIgnoreCaseOrFullNameContainingIgnoreCase()** (5 connections) — `src/main/java/vn/techshop/identity/UserRepository.java`
- **.view()** (4 connections) — `src/main/java/vn/techshop/chat/ChatService.java`
- **.getEmail()** (4 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.getRole()** (4 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **GooglePrincipal.java** (4 connections) — `src/main/java/vn/techshop/identity/GooglePrincipal.java`
- **.GooglePrincipal()** (3 connections) — `src/main/java/vn/techshop/identity/GooglePrincipal.java`
- **oidc** (2 connections) — `src/main/java/vn/techshop/identity/GooglePrincipal.java`
- **.getAvatarPath()** (2 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.getBirthDate()** (2 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.getCreatedAt()** (2 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.getGender()** (2 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.getPasswordHash()** (2 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.getPhone()** (2 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- **.getVersion()** (2 connections) — `src/main/java/vn/techshop/identity/AppUser.java`
- *... and 27 more nodes in this community*

## Relationships

- [Customer and Admin Chat](Customer_and_Admin_Chat.md) (7 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (2 shared connections)
- [Validation and Errors](Validation_and_Errors.md) (1 shared connections)
- [Cart and Pricing](Cart_and_Pricing.md) (1 shared connections)
- [Security and Web Controllers](Security_and_Web_Controllers.md) (1 shared connections)
- [Design and Delivery Guides](Design_and_Delivery_Guides.md) (1 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/chat/ChatService.java`
- `src/main/java/vn/techshop/identity/AppUser.java`
- `src/main/java/vn/techshop/identity/GooglePrincipal.java`
- `src/main/java/vn/techshop/identity/IdentityService.java`
- `src/main/java/vn/techshop/identity/TechPrincipal.java`
- `src/main/java/vn/techshop/identity/UserRepository.java`

## Audit Trail

- EXTRACTED: 65 (72%)
- INFERRED: 25 (28%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*