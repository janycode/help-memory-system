# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

助记单 (Help-Memory-System) — 企业技术栈管理系统，用于集中管理环境配置、技术组件、业务流程、代码片段等知识资产。

- **单人项目**，代码由 AI 生成
- **一体化部署**：前端 `pnpm build` 产物输出到 `backend/src/main/resources/static`，仅需运行 Spring Boot JAR
- **H2 本地文件数据库**：开箱即用，无需安装 MySQL

## Tech Stack

| Layer | Stack |
|-------|-------|
| Backend | Java 21, Spring Boot 3.2.0, Spring Data JPA, Spring Security + JWT, H2 Database, Jasypt, Lombok, SpringDoc OpenAPI |
| Frontend | Vue 3.4, TypeScript 5.3, Vite 5, Element Plus, Pinia, Vue Router, Axios, pnpm |
| Build | Maven (backend), pnpm (frontend) |

## Commands

### Backend (`backend/`)

```bash
mvn spring-boot:run              # start on :8080
mvn clean package -DskipTests    # build JAR
mvn test                         # no test cases — passes vacuously
```

### Frontend (`frontend/`)

```bash
pnpm install
pnpm dev         # :5173 with /api proxy to :8080
pnpm build       # outputs to ../backend/src/main/resources/static
pnpm lint        # eslint --fix
pnpm format      # prettier --write src/
```

### Full Stack

```bash
cd frontend && pnpm install && pnpm build && cd ../backend && mvn spring-boot:run
```

### Docker

```bash
# Must build frontend first, then:
docker build -t help-memory-system .
docker run -d --name help-memory -p 18080:8080 -v $(pwd)/data:/app/data help-memory-system
```

## Architecture

```
backend/src/main/java/com/example/mynewwork/
  config/           # SecurityConfig, DataInitializer, SpaConfig, JpaAuditingConfig
  controller/       # REST controllers, all under /api/*
  service/          # Business logic, ActivityLogger
  repository/       # Spring Data JPA repos
  model/entity/     # 13 JPA entities
  model/dto/        # ApiResponse, DashboardStats, HealthCheckResult
  security/         # JWT filter, TokenProvider, UserPrincipal, CustomUserDetailsService
  exception/        # GlobalExceptionHandler, custom exceptions

frontend/src/
  api/              # Axios wrappers per module
  views/            # One .vue per page (flat structure, no subdirectories)
  components/       # Reusable: PasswordDisplay, ConnectionStringDialog, ColumnSelector, UrlLink
  composables/      # useColumnVisibility, useKeyboardShortcuts
  layout/           # MainLayout.vue (sidebar + content)
  stores/           # Pinia stores
  types/            # TypeScript type definitions
  utils/            # connectionStrings.ts
```

### Key Entities (13 JPA entities)

Environment, TechnicalComponent, BusinessProcess, Project, CodeSnippet, User, SysDictType, SysDictData, SystemConfig, ActivityLog, Iteration, IterationSyncHistory, IterationImportConfig

### Adding a New Module

**Backend:** entity → repository → service (inject `ActivityLogger`) → controller (`/api/xxx`)

**Frontend:** types → api wrapper → view component → route in `router/index.ts` → menu item in `layout/MainLayout.vue`

## Key URLs

| URL | Purpose |
|-----|---------|
| `http://localhost:8080/` | Full app (一体化) |
| `http://localhost:5173/` | Frontend dev server |
| `http://localhost:8080/h2-console` | H2 console (JDBC URL must match exactly) |
| `http://localhost:8080/swagger-ui.html` | API docs |

Default login: `admin` / `admin123`

## Gotchas

- **Frontend build into backend**: `pnpm build` writes directly to `backend/src/main/resources/static/`. Must build frontend before building backend JAR. Dockerfile assumes frontend is already built.
- **H2 relative path**: JDBC URL is `jdbc:h2:file:../data/mynewwork` (relative to `backend/`), so DB files live at project-root `data/`. The `data/` dir is gitignored.
- **No test suite**: No backend or frontend tests exist. `mvn test` runs zero tests.
- **Redis optional but configured**: `spring-boot-starter-data-redis` is a hard dependency in pom.xml. If Redis isn't running, expect startup warnings.
- **Schema auto-migration**: `spring.jpa.hibernate.ddl-auto=update` — Hibernate modifies schema on startup. No Flyway/Liquibase.
- **First-run init**: `DataInitializer.java` creates admin account (`admin`/`admin123`) and seed dict data on first startup. The `init-dict-data.sql` file exists but is NOT auto-executed — it's a reference.
- **Jasypt encryption**: password/username/url fields are encrypted at rest via Jasypt. Key from `JASYPT_ENCRYPTOR_PASSWORD` env var (defaults to `mySecretKey`). Don't insert plaintext into these columns manually.
- **CORS**: Hardcoded to `localhost:5173,localhost:3000` in `application.yml`. Add new origins in `app.security.cors-allowed-origins`.
- **SpaConfig**: `SpaConfig.java` forwards non-API routes to `index.html` for Vue Router history mode. Don't remove it.
- **Activity logging**: All CRUD controllers should inject `ActivityLogger` to record operations.

## Configuration

Only `application.yml` exists (no dev/prod profiles). Key config:

```yaml
server.port: 8080
spring.datasource.url: jdbc:h2:file:../data/mynewwork;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
jasypt.encryptor.password: ${JASYPT_ENCRYPTOR_PASSWORD:mySecretKey}
jwt.secret: ${JWT_SECRET:mySecretKeyForJWTTokenGeneration}
jwt.expiration: 86400000  # 24h
```

Override via environment variables for production:

```bash
export JASYPT_ENCRYPTOR_PASSWORD="your-strong-secret-key"
export JWT_SECRET="your-strong-jwt-secret-at-least-32-bytes"
```

## Coding Principles

1. **Don't assume** — state assumptions explicitly; present trade-offs when ambiguous
2. **Minimal code** — solve with least code; don't add unrequested features or abstractions
3. **Precise edits** — only touch what's necessary; match existing style; clean up only your own orphaned code
4. **Goal-driven** — define success criteria; verify before declaring done
