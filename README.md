# 个人助记单

## 谐音梗：筑基丹，或一本正经叫：企业技术栈管理系统，都随你 ~

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-green" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.4-blue" alt="Vue">
  <img src="https://img.shields.io/badge/TypeScript-5.3-blue" alt="TypeScript">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

> 说明：本项目除了这一行文字之外，代码和文档均由 AI 生成。

## 📖 项目简介

助记单系统是一个基于 **Spring Boot 3** + **Vue 3** 的前后端一体化助记单平台，旨在帮助企业集中管理技术环境配置、业务流程文档、代码片段等知识资产。

系统采用 **H2 本地文件数据库**，开箱即用，无需额外安装 MySQL 等数据库服务，更无需担心密码泄露问题，非常适合个人开发者或团队内部局域网使用。

## ✨ 特性

- 🚀 **开箱即用**：采用 H2 本地数据库，无需安装数据库服务
- 🔐 **安全可靠**：JWT 认证、BCrypt 密码加密、Jasypt 敏感字段加密、角色权限控制
- 📱 **响应式设计**：Element Plus UI，支持桌面端访问，界面美观现代
- 🔍 **全局搜索**：快速检索环境、组件、流程、项目等所有模块
- 📝 **代码片段管理**：支持 Java / Vue / JavaScript / SQL / 其他等语言
- 📊 **数据可视化**：首页统计图表，直观展示各模块数据量与最近活动
- 🔄 **实时生效**：系统配置（名称、欢迎语等）修改无需重启，实时更新
- 🌐 **一体化部署**：前端资源内置于后端 Jar，单命令启动
- 🗂️ **字典管理**：组件分类、流程分类、环境类型、项目状态等可配置
- 👤 **用户管理**：管理员可管理用户、重置密码、分配角色
- 📋 **连接信息生成**：根据组件配置一键生成各类连接字符串
- 🔑 **密码显隐复制**：所有密码字段支持显示/隐藏、一键复制
- 📜 **活动日志**：记录所有 CRUD 操作与登录行为，审计可追溯
- 🔧 **自定义列显示**：列表页支持列选择与快捷键操作
- 🐳 **Docker 支持**：提供 Dockerfile，支持容器化部署
- ⚙️ **CI/CD**：GitHub Actions 自动构建前后端，微信 Webhook 通知

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Data JPA | - | ORM 框架 |
| Spring Security | - | 安全框架 |
| JWT | jjwt | Token 认证 |
| H2 Database | - | 本地文件数据库 |
| Redis (可选) | - | 缓存 |
| Jasypt | 3.0.5 | 敏感字段加密 |
| Lombok | 1.18.30 | 代码简化 |
| SpringDoc OpenAPI | 2.3.0 | API 文档 |
| Validation | - | 参数校验 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.x | 核心框架 |
| TypeScript | 5.3.x | 类型安全 |
| Vite | 5.0.x | 构建工具 |
| Element Plus | 2.5.x | UI 组件库 |
| Pinia | 2.1.x | 状态管理 |
| Vue Router | 4.2.x | 路由管理 |
| Axios | 1.6.x | HTTP 请求 |
| @vueuse/core | 10.7.x | 组合式工具集 |
| dayjs | - | 日期处理 |
| highlight.js | 11.x | 代码高亮 |
| pnpm | 8+ | 包管理器 |

## 📁 项目结构

```
help-memory-system/
├── backend/                          # 后端项目 (Spring Boot)
│   ├── src/main/
│   │   ├── java/com/example/mynewwork/
│   │   │   ├── config/              # 配置类
│   │   │   │   ├── DataInitializer.java       # 启动数据初始化（用户、字典、系统配置）
│   │   │   │   ├── JpaAuditingConfig.java     # JPA 审计配置
│   │   │   │   ├── SecurityConfig.java        # 安全配置（JWT + CORS）
│   │   │   │   └── SpaConfig.java             # SPA 路由回退
│   │   │   ├── controller/          # REST API 控制器
│   │   │   │   ├── AuthController.java        # 认证（登录/登出）
│   │   │   │   ├── DashboardController.java   # 仪表盘统计与最近活动
│   │   │   │   ├── EnvironmentController.java # 环境管理
│   │   │   │   ├── TechnicalComponentController.java # 技术组件
│   │   │   │   ├── BusinessProcessController.java    # 业务流程
│   │   │   │   ├── ProjectController.java     # 代码仓库（项目）
│   │   │   │   ├── CodeSnippetController.java # 代码片段
│   │   │   │   ├── SearchController.java      # 全局搜索
│   │   │   │   ├── SysDictController.java     # 字典管理
│   │   │   │   ├── SystemConfigController.java # 系统配置
│   │   │   │   ├── UserManageController.java  # 用户管理
│   │   │   │   ├── ActivityLogController.java # 活动日志
│   │   │   │   └── DebugController.java       # 调试接口
│   │   │   ├── service/             # 业务服务层
│   │   │   │   ├── ActivityLogger.java        # 操作日志记录器
│   │   │   │   └── ...
│   │   │   ├── repository/          # JPA Repository 数据访问层
│   │   │   ├── model/               # 数据模型
│   │   │   │   ├── entity/          # JPA 实体（10 张表）
│   │   │   │   └── dto/             # 响应封装（ApiResponse 等）
│   │   │   ├── security/            # JWT 过滤器 / TokenProvider / UserPrincipal
│   │   │   └── exception/           # 全局异常处理
│   │   └── resources/
│   │       ├── application.yml      # 应用配置（H2 / JWT / Jasypt / Redis 等）
│   │       └── static/              # 前端构建产物（一体化部署）
│   ├── data/                        # H2 数据库文件（运行后生成）
│   └── pom.xml                      # Maven 配置
│
├── frontend/                        # 前端项目 (Vue 3 + Vite)
│   ├── src/
│   │   ├── api/                     # API 接口封装
│   │   ├── components/              # 公共组件
│   │   │   ├── PasswordDisplay.vue           # 密码显示/隐藏/复制
│   │   │   ├── ConnectionStringDialog.vue    # 组件连接信息弹窗
│   │   │   ├── ColumnSelector.vue            # 列显示选择器
│   │   │   └── UrlLink.vue                   # URL 快捷链接
│   │   ├── composables/             # 组合式函数
│   │   │   ├── useColumnVisibility.ts         # 列显隐
│   │   │   └── useKeyboardShortcuts.ts        # 键盘快捷键
│   │   ├── layout/                # 布局组件
│   │   │   └── MainLayout.vue               # 主布局（侧边栏菜单 + 内容区）
│   │   ├── router/                # 路由配置
│   │   ├── stores/                # Pinia 状态管理
│   │   ├── types/                 # TypeScript 类型定义
│   │   ├── utils/                 # 工具函数
│   │   │   └── connectionStrings.ts           # 连接字符串生成器
│   │   └── views/                 # 页面视图
│   │       ├── Login.vue                     # 登录
│   │       ├── Dashboard.vue                 # 首页仪表盘
│   │       ├── EnvironmentList.vue           # 环境管理
│   │       ├── ComponentList.vue             # 技术组件
│   │       ├── ProcessList.vue               # 业务流程
│   │       ├── ProjectList.vue               # 代码仓库
│   │       ├── SnippetList.vue               # 代码片段
│   │       ├── DictManagement.vue            # 字典管理
│   │       ├── UserManagement.vue            # 用户管理（管理员）
│   │       ├── SystemConfig.vue              # 系统配置（管理员）
│   │       ├── Profile.vue                   # 个人中心
│   │       ├── Search.vue                    # 全局搜索
│   │       ├── Database.vue                  # H2 数据库控制台入口
│   │       └── NotFound.vue                  # 404
│   ├── package.json
│   └── vite.config.ts
│
├── .github/workflows/auto-build.yml  # GitHub Actions 自动构建
├── Dockerfile                        # 后端容器化
├── init-dict-data.sql                # 字典数据初始化脚本（可选）
├── README.md                         # 本文档
├── CLAUDE.md                         # 开发者指南
└── .gitignore
```

## 🚀 快速开始

### 环境要求

| 环境 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 21+ | Java 开发环境 |
| Node.js | 18+ | 前端开发环境 |
| pnpm | 8+ | 推荐使用 pnpm 作为包管理器 |
| Maven | 3.9+ | 后端构建工具 |

### 一体化部署（推荐）

只需启动后端即可访问完整应用：

```bash
# 1. 进入项目目录
cd help-memory-system

# 2. 构建前端（产物自动复制到 backend/src/main/resources/static）
cd frontend
pnpm install
pnpm build

# 3. 启动后端
cd ../backend
mvn spring-boot:run

# 4. 访问应用
http://localhost:8080/
```

### 开发模式（前后端分离）

适合开发调试，前端支持热更新：

```bash
# 终端 1：启动后端
cd backend
mvn spring-boot:run
# 后端 API: http://localhost:8080/api/

# 终端 2：启动前端开发服务器
cd frontend
pnpm install
pnpm dev
# 前端页面: http://localhost:5173/
```

### Docker 部署

```bash
# 1. 先构建前端（产物需放入 backend/src/main/resources/static）
cd frontend && pnpm install && pnpm build && cd ..

# 2. 构建镜像
docker build -t help-memory-system .

# 3. 运行容器
docker run -d \
  --name help-memory \
  -p 18080:8080 \
  -v $(pwd)/data:/app/data \
  help-memory-system

# 4. 访问
http://localhost:18080/
```

## 📋 访问地址

启动后端服务后，可通过以下地址访问：

| 页面 | 地址 | 说明 |
|------|------|------|
| **首页** | http://localhost:8080/ | 系统首页 |
| **登录页** | http://localhost:8080/login | 用户登录 |
| **环境管理** | http://localhost:8080/environments | 环境列表/新增/编辑 |
| **技术组件** | http://localhost:8080/components | 技术组件管理 |
| **业务流程** | http://localhost:8080/processes | 业务流程文档 |
| **代码仓库** | http://localhost:8080/repositories | 项目/仓库管理 |
| **代码片段** | http://localhost:8080/snippets | 代码片段管理 |
| **字典管理** | http://localhost:8080/dict | 字典类型与字典数据 |
| **用户管理** | http://localhost:8080/users | 用户管理（仅管理员） |
| **系统配置** | http://localhost:8080/system | 系统配置（仅管理员） |
| **个人中心** | http://localhost:8080/profile | 个人信息与密码修改 |
| **全局搜索** | http://localhost:8080/search | 跨模块搜索 |
| **H2 控制台** | http://localhost:8080/h2-console | 数据库管理 |
| **API 文档** | http://localhost:8080/swagger-ui.html | Swagger 文档 |

### 默认登录账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |

> ⚠️ **安全提示**：首次登录后请及时修改默认密码！
>
> 敏感字段（密码、URL 等）在后端使用 **Jasypt** 加密存储，默认加密密钥通过环境变量 `JASYPT_ENCRYPTOR_PASSWORD` 控制。

## 📖 功能模块

### 1. 首页仪表盘
- 📊 系统概览统计（环境数、组件数、流程数、项目数）
- 📝 最近活动记录（最近的 CRUD / 登录操作）
- 🔍 全局搜索入口
- 💬 可配置的系统名称与欢迎语

### 2. 环境管理
- 🌍 多环境配置（开发 / 测试 / 预发布 / 生产 / 演示）
- 🔑 数据库 / Redis / 消息队列连接信息
- 🔒 所有密码字段支持显示/隐藏与一键复制
- 📋 环境级备注说明

### 3. 技术组件
- 🛠️ 组件分类（数据库 / 缓存 / 消息队列 / API 服务 / 存储 / 监控 / 认证 / 其他）
- 📦 版本、描述、连接地址、用户名、密码、访问地址
- 🔗 一键生成连接字符串（JDBC、Redis、SSH、cURL 等）
- 🏷️ 标签与备注

### 4. 业务流程
- 📑 流程分类（发布 / 业务 / 运维 / 故障 / 维护 / 安全 / 其他）
- 📝 流程步骤、注意事项、检查清单
- ⚠️ 排序与启停控制

### 5. 代码仓库（项目管理）
- 📁 项目全称、描述、负责人
- 🔗 Git 仓库地址、文档位置、部署路径
- 🏷️ 项目状态（规划中 / 开发中 / 测试中 / 已发布 / 维护中 / 已废弃）

### 6. 代码片段
- 💻 多语言支持（Java / Vue / JavaScript / SQL / 其他）
- 📝 代码标题和描述
- 🏷️ 标签管理
- 📋 一键复制
- 🎨 代码高亮显示

### 7. 字典管理
- 📚 字典类型：组件分类、流程分类、环境类型、项目状态
- 💾 字典数据维护（增删改、排序、启停）

### 8. 用户管理（管理员）
- 👤 用户增删改查
- 🔐 角色权限分配（ADMIN / USER）
- 🔁 密码重置
- 🔌 启停账号

### 9. 系统配置（管理员）
- ⚙️ 系统名称配置（实时生效，用于登录页、首页、欢迎语）
- 🔧 其他可扩展的运行时配置项

### 10. 本地数据库
- 🗄️ H2 控制台入口
- 📍 数据库文件路径显示（`../data/mynewwork`）

### 11. 全局搜索
- 🔎 跨 4 大模块（环境 / 组件 / 流程 / 项目）联合搜索
- 📊 分 Tab 展示结果，点击可跳转到对应记录

### 12. 活动日志（后端能力）
- 📜 记录所有模块的 CREATE / UPDATE / DELETE / VIEW / LOGIN 操作
- 🧾 包含操作人、IP、时间、描述
- 🔗 对应接口：`/api/dashboard/recent-activities`（前端仪表盘已接入）

## 🔧 API 文档

启动后端后访问 Swagger UI 查看完整 API 文档：

```
http://localhost:8080/swagger-ui.html
```

### 主要 API 端点

| 模块 | 端点 | 方法 | 说明 |
|------|------|------|------|
| 认证 | `/api/auth/login` | POST | 用户登录 |
| 认证 | `/api/auth/logout` | POST | 用户登出 |
| 认证 | `/api/auth/me` | GET | 获取当前用户信息 |
| 仪表盘 | `/api/dashboard/stats` | GET | 获取各模块统计数 |
| 仪表盘 | `/api/dashboard/recent-activities` | GET | 获取最近活动 |
| 环境 | `/api/environments` | GET/POST | 环境列表 / 创建 |
| 环境 | `/api/environments/{id}` | GET/PUT/DELETE | 环境详情 / 更新 / 删除 |
| 组件 | `/api/components` | GET/POST | 组件列表 / 创建 |
| 组件 | `/api/components/{id}` | GET/PUT/DELETE | 组件详情 / 更新 / 删除 |
| 流程 | `/api/processes` | GET/POST | 流程列表 / 创建 |
| 项目 | `/api/projects` | GET/POST | 项目列表 / 创建 |
| 片段 | `/api/snippets` | GET/POST | 片段列表 / 创建 |
| 用户 | `/api/users` | GET | 用户列表（管理员） |
| 用户 | `/api/users/{id}/password` | PUT | 重置用户密码（管理员） |
| 字典 | `/api/dict/types` | GET | 字典类型列表 |
| 字典 | `/api/dict/data/{typeCode}` | GET | 字典数据列表 |
| 搜索 | `/api/search?q=xxx` | GET | 全局搜索 |
| 配置 | `/api/system/config` | GET/PUT | 系统配置查询 / 修改 |
| 配置 | `/api/system/title` | GET | 获取系统标题（匿名访问） |
| 健康检查 | `/actuator/health` | GET | 应用健康状态 |

## 💾 数据库

### H2 数据库配置

| 配置项 | 值 |
|--------|-----|
| 驱动 | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:file:../data/mynewwork;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE` |
| 用户名 | `sa` |
| 密码 | （空） |
| 模式 | MySQL 兼容模式 |

### 访问 H2 控制台

```
JDBC URL: jdbc:h2:file:../data/mynewwork;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
用户名: sa
密码: （留空）
```

### 数据文件位置

数据库文件默认存储在 `backend/../data/` 目录下（项目根目录的 data 子目录）：
- `mynewwork.mv.db` — 数据文件
- `mynewwork.trace.db` — 日志文件

### 重置数据库

```bash
# 停止服务后，删除数据库文件
rm -rf data/*
# 重启后端，系统将自动重建数据库并初始化管理员账号与字典数据
```

## ⚙️ 配置说明

### 后端配置 (application.yml)

```yaml
server:
  port: 8080

spring:
  # H2 本地文件数据库
  datasource:
    driver-class-name: org.h2.Driver
    url: jdbc:h2:file:../data/mynewwork;MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;AUTO_RECONNECT=TRUE
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect

  # Redis（可选，未配置时不影响主功能）
  data:
    redis:
      host: localhost
      port: 6379

# Jasypt 加密配置（敏感字段加密）
jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD:mySecretKey}
    algorithm: PBEWithMD5AndDES
    iv-generator-classname: org.jasypt.iv.NoIvGenerator

# JWT 配置
jwt:
  secret: ${JWT_SECRET:mySecretKeyForJWTTokenGeneration}
  expiration: 86400000  # 24 小时

# 应用特定配置
app:
  security:
    cors-allowed-origins: http://localhost:5173,http://localhost:3000
    password-min-length: 6
  encryption:
    sensitive-fields: password,username,url
  system:
    title: 助记单
    version: 1.0.0
```

### 生产环境安全建议

```bash
# 通过环境变量覆盖默认密钥
export JASYPT_ENCRYPTOR_PASSWORD="your-strong-secret-key"
export JWT_SECRET="your-strong-jwt-secret-at-least-32-bytes"
```

### 前端配置 (vite.config.ts)

```typescript
export default defineConfig({
  base: '/',
  build: {
    outDir: '../backend/src/main/resources/static'
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/h2-console': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/swagger-ui.html': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/v3/api-docs': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

## 🔒 安全特性

- 🔐 **JWT Token 认证**：无状态认证，支持自动刷新
- 🛡️ **BCrypt 密码加密**：用户密码使用 BCrypt 存储
- 👥 **角色权限**：基于角色的访问控制（ADMIN / USER）
- 🔒 **敏感字段加密**：所有模块的 password / url 字段使用 Jasypt 加密存储
- 🌐 **CORS 配置**：安全的跨域访问控制，允许本地开发端口
- 📝 **操作日志**：完整记录用户操作历史（CREATE / UPDATE / DELETE / VIEW / LOGIN）
- 🚫 **全局异常处理**：统一异常响应，避免堆栈信息泄露

## 🧩 扩展开发

### 添加新功能模块

**后端：**

1. 创建实体类 `model/entity/Xxx.java`
2. 创建 Repository `repository/XxxRepository.java`
3. 创建 Service `service/XxxService.java`（继承常用方法）
4. 创建 Controller `controller/XxxController.java`，并在其中注入 `ActivityLogger` 记录活动

```java
@Entity
@Table(name = "xxxs")
public class Xxx {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    // ...
}

@RestController
@RequestMapping("/api/xxx")
public class XxxController {
    private final XxxService service;
    private final ActivityLogger activityLogger;
    // CRUD + 活动日志记录
}
```

**前端：**

1. 在 `types/` 定义 TypeScript 类型
2. 在 `api/` 创建 API 调用封装
3. 在 `views/` 创建页面组件（可参考 `EnvironmentList.vue`）
4. 在 `router/index.ts` 添加路由
5. 在 `layout/MainLayout.vue` 添加侧边栏菜单项

### 添加字典类型

系统自带 4 类字典，可在 `DataInitializer.java` 中仿照已有结构扩展；也可通过字典管理页面在运行时动态新增。

## 📝 常见问题

### Q: 登录失败怎么办？

A: 请检查：
1. 后端是否正常启动（端口 8080）
2. `../data/mynewwork.mv.db` 是否存在（首次启动会自动创建）
3. 用户名密码是否正确（默认：`admin` / `admin123`）

### Q: 前端页面空白？

A: 请检查：
1. 前端是否已执行 `pnpm build`，静态资源是否生成在 `backend/src/main/resources/static/`
2. 浏览器开发者工具 Console 与 Network 是否有报错
3. 若使用开发模式，确认 Vite 代理是否正确

### Q: 如何修改系统名称？

A: 登录管理员账号 → 「系统配置」 → 修改系统名称 → 实时生效

### Q: 如何备份数据？

A: 复制 `data/` 目录下的 `mynewwork.mv.db` 文件即可；也可以通过 H2 控制台执行 `SCRIPT TO 'backup.sql'` 导出 SQL。

### Q: H2 控制台无法登录？

A: 确认 JDBC URL 与 `application.yml` 中完全一致，特别是 `file:` 后的相对路径是否正确（默认相对于 `backend/` 目录为 `../data/mynewwork`）。

### Q: 密码字段为空？

A: 后端对 password / url 等字段使用 Jasypt 加解密。如果直接在数据库中手动插入明文，读取时会解密失败显示空值，请通过页面或 API 新增记录。

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) — 强大的 Java 框架
- [Vue.js](https://vuejs.org/) — 渐进式 JavaScript 框架
- [Element Plus](https://element-plus.org/) — Vue 3 组件库
- [H2 Database](https://h2database.com/) — 纯 Java 数据库
- [Jasypt](https://github.com/ulisesbocchio/jasypt-spring-boot) — Spring Boot 敏感信息加密

---

<p align="center">
  Made with Jerry(姜源)❤️ by Java Developer
</p>
