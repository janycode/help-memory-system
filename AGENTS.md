# AGENTS.md

## 项目简介

助记单（Help-Memory-System）：基于 Spring Boot 3.2.0 + Vue 3 的知识管理平台（环境配置、技术组件、业务流程、代码片段、迭代任务等）。单人项目，代码由 AI 生成。

- **后端**：Java 21、Spring Data JPA、H2 文件数据库、Jasypt 字段加密、JWT 认证
- **前端**：Vue 3.4、TypeScript、Element Plus、Vite 5、pnpm
- **部署**：一体化 —— 前端 `pnpm build` 产物输出到 `backend/src/main/resources/static`，之后只需运行 Spring Boot JAR

## 常用命令

### 后端（`backend/`）

```bash
mvn spring-boot:run           # 在 :8080 启动
mvn clean package -DskipTests # 打包 JAR
mvn test                      # 无测试用例 —— 空跑通过
```

### 前端（`frontend/`）

```bash
pnpm install
pnpm dev         # 开发服务器 :5173，/api 代理到 :8080
pnpm build       # 产物输出到 ../backend/src/main/resources/static
pnpm lint        # eslint --fix
pnpm format      # prettier --write src/
```

注意：前端**没有 typecheck / test 脚本**（vue-tsc 已安装但无对应脚本），`pnpm lint` 是唯一的静态校验手段。

### 全栈

```bash
cd frontend && pnpm install && pnpm build && cd ../backend && mvn spring-boot:run
```

### 编译构建验证（用户约定，必须遵守）

本项目的编译构建验证标准 = **先构建前端，再打包后端 jar**：

```bash
# 1. 先手动清空旧前端产物（safe-delete 会拦截 vite emptyDir 的批量删除）
rm -rf backend/src/main/resources/static
# 2. 构建前端（产物输出到 backend/src/main/resources/static）
cd frontend && pnpm build
# 3. 打包后端 JAR（打包成功即代表编译没问题）
cd ../backend && mvn clean package -DskipTests
```

- **打包出 jar 即代表编译没问题** —— 无需再依赖其他验证手段。
- 顺序不可颠倒：先构建前端再打包后端，否则 JAR 不含前端资源。

## 注意事项（Gotchas）

- **前端构建进后端**：`pnpm build` 直接写入 `backend/src/main/resources/static/`。打包后端 JAR 前必须先构建前端。Dockerfile 假设前端已构建完成。
- **CI 顺序陷阱**：`.github/workflows/auto-build.yml` 先打包后端、再构建前端，因此 CI 产出的 JAR **不含**前端资源；且其上传的 `frontend/dist` 路径不存在（vite 实际输出到 `backend/src/main/resources/static`），该上传步骤会失败。以本地构建为准。
- **CI 使用 npm 而非 pnpm**：工作流前端用 `npm i && npm run build` —— 与本地 pnpm 配置不一致。
- **H2 相对路径**：JDBC URL `jdbc:h2:file:../data/mynewwork` 相对 `backend/` 目录，数据库文件在项目根目录 `data/`（已 gitignore）。删掉 `data/*` 可重置数据库。
- **无测试套件**：前后端均无测试。`mvn test` 运行 0 个测试。
- **首次启动初始化**：`DataInitializer.java` 创建 `admin/admin123` 与 `testuser/123456`，并初始化字典、菜单权限、系统配置。**注意**：admin 密码每次启动若与 `admin123` 不一致都会被重置。
- **Redis 硬依赖**：`spring-boot-starter-data-redis` 是 pom.xml 中的硬依赖，但未运行 Redis 时仅启动警告，不影响主功能。
- **Schema 自动迁移**：`spring.jpa.hibernate.ddl-auto=update` —— 启动时自动改表结构。无 Flyway/Liquibase。
- **Jasypt 加密**：`password`、`username`、`url` 字段加密存储（`PBEWithMD5AndDES`，无 IV）。密钥来自 `JASYPT_ENCRYPTOR_PASSWORD` 环境变量（默认 `mySecretKey`）。不要向这些列手动插入明文。
- **CORS**：`application.yml` 中硬编码 `http://localhost:5173,http://localhost:3000`。新来源加在 `app.security.cors-allowed-origins`。
- **SpaConfig**：非 API 路由转发 `index.html`（支持 history 模式）。不要删除。
- **Vite 仅代理 /api**：开发模式下 `/h2-console`、`/swagger-ui.html` 无法经 :5173 访问，需直连 :8080（README 中的代理清单与实际配置不符）。
- **Docker 端口映射**：应用监听 :8080，Dockerfile `EXPOSE 18080`。README 用 `-p 18080:8080`。

## 架构

```
backend/src/main/java/com/example/mynewwork/
  config/           # SecurityConfig、DataInitializer、SpaConfig、JpaAuditingConfig
  controller/       # REST 控制器，全部位于 /api/* 下
  service/          # 业务逻辑、ActivityLogger
  repository/       # Spring Data JPA 仓库
  model/entity/     # 15 个 JPA 实体
  model/dto/        # ApiResponse、DashboardStats、HealthCheckResult
  security/         # JWT 过滤器、TokenProvider、UserPrincipal、CustomUserDetailsService
  exception/        # GlobalExceptionHandler、自定义异常

frontend/src/
  api/              # 各模块的 Axios 封装
  views/            # 每个页面一个 .vue 文件（扁平结构，无子目录）
  components/       # 可复用组件：PasswordDisplay、ConnectionStringDialog、ColumnSelector、UrlLink
  composables/      # useColumnVisibility、useKeyboardShortcuts
  layout/           # MainLayout.vue（侧边栏 + 内容区）
  stores/           # Pinia 状态管理
  types/            # TypeScript 类型定义
  utils/            # connectionStrings.ts 等

运行时目录：`data/`（H2 数据库，已 gitignore）、`logs/`（已 gitignore）、`uploads/`（文件上传目录，**未** gitignore）、`backend/target/`、`backend/src/main/resources/static/`（前端构建产物，已 gitignore）。
```

### 核心实体（15 个）

Environment、TechnicalComponent、BusinessProcess、Project、CodeSnippet、User、UserMenuPermission、SysDictType、SysDictData、SystemConfig、ActivityLog、SoCreationLog、Iteration、IterationSyncHistory、IterationImportConfig

README / CLAUDE.md 未覆盖的较新模块：迭代管理（Iteration 系列）、菜单权限（UserMenuPermission）、文件上传（FileController + `uploads/`）、业务工具（BusinessToolController）、MQ 发送（RocketMQ）。

## 新增模块

后端：实体 → 仓库 → 服务（注入 `ActivityLogger`）→ 控制器（`/api/xxx`）。  
前端：类型 → API 封装 → 视图组件 → `router/index.ts` 加路由 → `layout/MainLayout.vue` 加菜单项。

## 关键 URL（本地运行时）

| URL | 说明 |
|-----|------|
| `http://localhost:8080/` | 完整应用（一体化） |
| `http://localhost:5173/` | 前端开发服务器 |
| `http://localhost:8080/h2-console` | H2 控制台（JDBC URL 必须完全一致） |
| `http://localhost:8080/swagger-ui.html` | API 文档 |

默认账号：`admin` / `admin123`（另有 `testuser` / `123456`）

## 文档同步说明

`CLAUDE.md` 已与代码库同步。`docs/DEPLOYMENT.md` 仍含过时内容（`application-prod.yml`、Nginx、certbot 等实际不存在）。以本文件、`CLAUDE.md` 和 `README.md` 为准。
