# AGENTS.md

## Project

助记单 (Help-Memory-System): Spring Boot 3.2.0 + Vue 3 knowledge management platform for environments, components, processes, and code snippets. Single-developer project; codebase is AI-generated.

- **Backend**: Java 21, Spring Data JPA, H2 file database, Jasypt field encryption, JWT auth
- **Frontend**: Vue 3.4, TypeScript, Element Plus, Vite 5, pnpm
- **Deployment**:一体化 — frontend `pnpm build` outputs to `backend/src/main/resources/static`, then only the Spring Boot JAR runs

## Commands

### Backend (`backend/`)

```bash
mvn spring-boot:run          # start on :8080
mvn clean package -DskipTests # build JAR
mvn test                     # no test cases exist — passes vacuously
```

### Frontend (`frontend/`)

```bash
pnpm install
pnpm dev         # :5173 with /api proxy to :8080
pnpm build       # outputs to ../backend/src/main/resources/static
pnpm lint        # eslint --fix
pnpm format      # prettier --write src/
```

### Full stack

```bash
cd frontend && pnpm install && pnpm build && cd ../backend && mvn spring-boot:run
```

## Gotchas

- **Frontend build into backend**: `pnpm build` writes directly to `backend/src/main/resources/static/`. You must build frontend before building the backend JAR. The Dockerfile assumes frontend is already built.
- **H2 relative path**: JDBC URL is `jdbc:h2:file:../data/mynewwork` (relative to `backend/`), so DB files live at project-root `data/`. The `data/` dir is gitignored.
- **No test suite**: No backend or frontend tests. `mvn test` runs zero tests. CI also skips tests (`-DskipTests`).
- **CI uses npm, not pnpm**: `.github/workflows/auto-build.yml` runs `npm i && npm run build` for frontend — inconsistent with local pnpm setup.
- **Dockerfile port mapping**: App listens on `:8080` internally, Dockerfile `EXPOSE 18080`. README shows `-p 18080:8080`.
- **Redis optional but configured**: `spring-boot-starter-data-redis` is a hard dependency in pom.xml, but the README and README describe it as optional. If Redis isn't running, expect startup warnings.
- **Schema auto-migration**: `spring.jpa.hibernate.ddl-auto=update` — Hibernate modifies schema on startup. No Flyway/Liquibase.
- **First-run init**: `DataInitializer.java` creates admin account (`admin`/`admin123`) and seed dict data on first startup. The `init-dict-data.sql` file exists but is NOT auto-executed — it's a reference.
- **Jasypt encryption**: password/username/url fields are encrypted at rest via Jasypt. Key from `JASYPT_ENCRYPTOR_PASSWORD` env var (defaults to `mySecretKey`). Don't insert plaintext into these columns manually.
- **CORS**: Hardcoded to `localhost:5173,localhost:3000` in `application.yml`. Add new origins in `app.security.cors-allowed-origins`.
- **SpaConfig**: `SpaConfig.java` forwards non-API routes to `index.html` for Vue Router history mode. Don't remove it.

## Architecture

```
backend/src/main/java/com/example/mynewwork/
  config/           # SecurityConfig, DataInitializer, SpaConfig, JpaAuditingConfig
  controller/       # REST controllers, all under /api/*
  service/          # Business logic, ActivityLogger
  repository/       # Spring Data JPA repos
  model/entity/     # 13 JPA entities (Environment, TechnicalComponent, BusinessProcess, Project, CodeSnippet, User, SysDictType, SysDictData, SystemConfig, ActivityLog, Iteration, IterationSyncHistory, IterationImportConfig)
  model/dto/        # ApiResponse, DashboardStats, HealthCheckResult
  security/         # JWT filter, TokenProvider, UserPrincipal, CustomUserDetailsService
  exception/        # GlobalExceptionHandler, custom exceptions

frontend/src/
  api/              # Axios wrappers per module
  views/            # One .vue per page (flat, no subdirectories)
  components/       # Reusable: PasswordDisplay, ConnectionStringDialog, ColumnSelector, UrlLink
  composables/      # useColumnVisibility, useKeyboardShortcuts
  layout/           # MainLayout.vue (sidebar + content)
  stores/           # Pinia stores
  types/            # TypeScript type definitions
  utils/            # connectionStrings.ts and others
```

## Adding a new module

Backend: entity → repository → service (inject `ActivityLogger`) → controller (`/api/xxx`).  
Frontend: types → api wrapper → view component → route in `router/index.ts` → menu item in `layout/MainLayout.vue`.

## Key URLs (when running locally)

| URL | Note |
|-----|------|
| `http://localhost:8080/` | Full app (一体化) |
| `http://localhost:5173/` | Frontend dev server |
| `http://localhost:8080/h2-console` | H2 console (JDBC URL must match exactly) |
| `http://localhost:8080/swagger-ui.html` | API docs |

## CLAUDE.md warning

`CLAUDE.md` contains stale content: references MySQL, application-prod.yml, Nginx deployment, Flyway, and other things that don't exist in the actual codebase. Prefer this file and `README.md` as sources of truth.
