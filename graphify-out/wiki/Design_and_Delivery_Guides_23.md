# Design and Delivery Guides

> 14 nodes · cohesion 0.22

## Key Concepts

- **FileStorageService** (14 connections) — `src/main/java/vn/techshop/shared/FileStorageService.java`
- **UploadResourceConfig** (6 connections) — `src/main/java/vn/techshop/config/UploadResourceConfig.java`
- **AvatarApi** (6 connections) — `src/main/java/vn/techshop/identity/AvatarApi.java`
- **UploadResourceConfig.java** (5 connections) — `src/main/java/vn/techshop/config/UploadResourceConfig.java`
- **.addResourceHandlers()** (4 connections) — `src/main/java/vn/techshop/config/UploadResourceConfig.java`
- **.AvatarApi()** (3 connections) — `src/main/java/vn/techshop/identity/AvatarApi.java`
- **org.springframework.context.annotation.Configuration** (2 connections)
- **org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry** (2 connections)
- **org.springframework.web.servlet.config.annotation.WebMvcConfigurer** (2 connections)
- **.UploadResourceConfig()** (2 connections) — `src/main/java/vn/techshop/config/UploadResourceConfig.java`
- **.uploadDirectory()** (2 connections) — `src/main/java/vn/techshop/shared/FileStorageService.java`
- **Override** (1 connections)
- **RestController** (1 connections)
- **.FileStorageService()** (1 connections) — `src/main/java/vn/techshop/shared/FileStorageService.java`

## Relationships

- [Security and Web Controllers](Security_and_Web_Controllers.md) (3 shared connections)
- [Identity and Sessions](Identity_and_Sessions.md) (2 shared connections)
- [Gemini AI Advice and Quotas](Gemini_AI_Advice_and_Quotas.md) (1 shared connections)

## Source Files

- `src/main/java/vn/techshop/config/UploadResourceConfig.java`
- `src/main/java/vn/techshop/identity/AvatarApi.java`
- `src/main/java/vn/techshop/shared/FileStorageService.java`

## Audit Trail

- EXTRACTED: 25 (96%)
- INFERRED: 1 (4%)
- AMBIGUOUS: 0 (0%)

---

*Part of the graphify knowledge wiki. See [index](index.md) to navigate.*