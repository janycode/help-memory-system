# CLAUDE.md

本文件为 Claude Code（claude.ai/code）在本仓库工作时提供指导。

## 项目概述

助记单（Help-Memory-System）—— 企业技术栈管理系统，用于集中管理环境配置、技术组件、业务流程、代码片段等知识资产。

- **单人项目**，代码由 AI 生成
- **一体化部署**：前端 `pnpm build` 产物输出到 `backend/src/main/resources/static`，仅需运行 Spring Boot JAR
- **H2 本地文件数据库**：开箱即用，无需安装 MySQL

## 技术栈

| 层级 | 技术栈 |
|------|--------|
| 后端 | Java 21、Spring Boot 3.2.0、Spring Data JPA、Spring Security + JWT、H2 Database、Jasypt、Lombok、SpringDoc OpenAPI |
| 前端 | Vue 3.4、TypeScript 5.3、Vite 5、Element Plus、Pinia、Vue Router、Axios、pnpm |
| 构建 | Maven（后端）、pnpm（前端） |

## 常用命令

### 后端（`backend/`）

```bash
mvn spring-boot:run              # 在 :8080 启动
mvn clean package -DskipTests    # 打包 JAR
mvn test                         # 无测试用例 —— 空跑通过
```

### 前端（`frontend/`）

```bash
pnpm install
pnpm dev         # 开发服务器 :5173，/api 代理到 :8080
pnpm build       # 产物输出到 ../backend/src/main/resources/static
pnpm lint        # eslint --fix
pnpm format      # prettier --write src/
```

### 全栈

```bash
cd frontend && pnpm install && pnpm build && cd ../backend && mvn spring-boot:run
```

### Docker

```bash
# 必须先构建前端，然后：
docker build -t help-memory-system .
docker run -d --name help-memory -p 18080:8080 -v $(pwd)/data:/app/data help-memory-system
```

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
  utils/            # connectionStrings.ts
```

### 核心实体（15 个 JPA 实体）

Environment、TechnicalComponent、BusinessProcess、Project、CodeSnippet、User、UserMenuPermission、SysDictType、SysDictData、SystemConfig、ActivityLog、SoCreationLog、Iteration、IterationSyncHistory、IterationImportConfig

### 新增模块

**后端：** 实体 → 仓库 → 服务（注入 `ActivityLogger`）→ 控制器（`/api/xxx`）

**前端：** 类型 → API 封装 → 视图组件 → 在 `router/index.ts` 添加路由 → 在 `layout/MainLayout.vue` 添加菜单项

## 关键 URL

| URL | 用途 |
|-----|------|
| `http://localhost:8080/` | 完整应用（一体化） |
| `http://localhost:5173/` | 前端开发服务器 |
| `http://localhost:8080/h2-console` | H2 控制台（JDBC URL 必须完全一致） |
| `http://localhost:8080/swagger-ui.html` | API 文档 |

默认登录账号：`admin` / `admin123`

## 注意事项（Gotchas）

- **前端构建进后端**：`pnpm build` 直接写入 `backend/src/main/resources/static/`。打包后端 JAR 前必须先构建前端。Dockerfile 假设前端已构建完成。
- **H2 相对路径**：JDBC URL 为 `jdbc:h2:file:../data/mynewwork`（相对 `backend/` 目录），因此数据库文件位于项目根目录 `data/` 下。`data/` 目录已被 gitignore。
- **无测试套件**：前后端均无测试。`mvn test` 运行 0 个测试。
- **Redis 可选但已配置**：`spring-boot-starter-data-redis` 是 pom.xml 中的硬依赖。若 Redis 未运行，启动时会出现警告。
- **Schema 自动迁移**：`spring.jpa.hibernate.ddl-auto=update` —— Hibernate 启动时自动修改表结构。无 Flyway/Liquibase。
- **首次启动初始化**：`DataInitializer.java` 在首次启动时创建管理员账号（`admin`/`admin123`）并初始化字典数据。`init-dict-data.sql` 文件存在但不会自动执行 —— 仅供参考。
- **Jasypt 加密**：password/username/url 字段通过 Jasypt 加密存储。密钥来自 `JASYPT_ENCRYPTOR_PASSWORD` 环境变量（默认 `mySecretKey`）。不要手动向这些列插入明文。
- **CORS**：在 `application.yml` 中硬编码为 `localhost:5173,localhost:3000`。新来源请添加到 `app.security.cors-allowed-origins`。
- **SpaConfig**：`SpaConfig.java` 将非 API 路由转发到 `index.html`，以支持 Vue Router history 模式。不要删除它。
- **操作日志**：所有 CRUD 控制器都应注入 `ActivityLogger` 记录操作。

## 配置

仅存在 `application.yml`（无 dev/prod 环境配置）。关键配置：

```yaml
server.port: 8080
spring.datasource.url: jdbc:h2:file:../data/mynewwork;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
jasypt.encryptor.password: ${JASYPT_ENCRYPTOR_PASSWORD:mySecretKey}
jwt.secret: ${JWT_SECRET:mySecretKeyForJWTTokenGeneration}
jwt.expiration: 86400000  # 24 小时
```

生产环境可通过环境变量覆盖：

```bash
export JASYPT_ENCRYPTOR_PASSWORD="your-strong-secret-key"
export JWT_SECRET="your-strong-jwt-secret-at-least-32-bytes"
```

## 编码原则

1. **不臆测** —— 明确说明假设；有歧义时给出取舍
2. **最小代码** —— 用最少的代码解决问题；不添加未要求的功能或抽象
3. **精确修改** —— 只改动必要部分；与现有风格保持一致；只清理自己遗留的孤立代码
4. **目标驱动** —— 定义成功标准；验证通过后再宣告完成
