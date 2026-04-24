# 个人助记单

## 谐音梗：筑基丹，或一本正经叫：企业技术栈管理系统，都随你~

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-blue" alt="Java">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.2.0-green" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Vue-3.4-blue" alt="Vue">
  <img src="https://img.shields.io/badge/TypeScript-5.3-blue" alt="TypeScript">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>
简体中文

> 说明：本项目除了这一行文字之外，代码和文档均由AI生成。

## 📖 项目简介

助记单系统是一个基于 **Spring Boot 3** + **Vue 3** 的前后端一体化助记单平台，旨在帮助企业集中管理技术环境配置、业务流程文档、代码片段等知识资产。

系统采用 H2 本地文件数据库，开箱即用，无需额外安装 MySQL 等数据库服务，更无需担心密码泄露问题，非常适合个人开发者或团队内部局域网使用。

## ✨ 特性

- **🚀 开箱即用**：采用 H2 本地数据库，无需安装数据库服务
- **🔐 安全可靠**：JWT 认证、密码加密存储、角色权限控制
- **📱 响应式设计**：支持桌面端访问，界面美观现代
- **🔍 全局搜索**：快速检索环境、组件、流程、代码片段
- **📝 代码片段管理**：支持 Java、Vue、JavaScript、SQL 等语言
- **📊 数据可视化**：首页统计图表，直观展示系统数据
- **🔄 实时生效**：系统配置修改无需重启，实时更新
- **🌐 一体化部署**：前端资源内置于后端，单命令启动

## 🛠️ 技术栈

### 后端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Java | 21 | 开发语言 |
| Spring Boot | 3.2.0 | 核心框架 |
| Spring Data JPA | - | ORM 框架 |
| Spring Security | - | 安全框架 |
| JWT | jjwt 0.11.5 | Token 认证 |
| H2 Database | - | 本地文件数据库 |
| Lombok | 1.18.30 | 代码简化 |
| SpringDoc OpenAPI | 2.3.0 | API 文档 |

### 前端技术

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4.0 | 核心框架 |
| TypeScript | 5.3.0 | 类型安全 |
| Vite | 5.0.0 | 构建工具 |
| Element Plus | 2.5.0 | UI 组件库 |
| Pinia | 2.1.0 | 状态管理 |
| Vue Router | 4.2.0 | 路由管理 |
| Axios | 1.6.0 | HTTP 请求 |
| dayjs | - | 日期处理 |
| pnpm | 8+ | 包管理器 |

## 📁 项目结构

```
my-new-work/
├── backend/                          # 后端项目
│   ├── src/main/
│   │   ├── java/com/example/mynewwork/
│   │   │   ├── config/              # 配置类
│   │   │   │   ├── DataInitializer.java      # 数据初始化
│   │   │   │   ├── SecurityConfig.java       # 安全配置
│   │   │   │   ├── CorsConfig.java           # 跨域配置
│   │   │   │   └── SpaConfig.java            # SPA路由配置
│   │   │   ├── controller/         # 控制器
│   │   │   │   ├── AuthController.java       # 认证
│   │   │   │   ├── EnvironmentController.java # 环境管理
│   │   │   │   ├── ComponentController.java  # 组件管理
│   │   │   │   ├── ProcessController.java    # 流程管理
│   │   │   │   ├── RepositoryController.java  # 仓库管理
│   │   │   │   ├── UserController.java       # 用户管理
│   │   │   │   ├── DictController.java       # 字典管理
│   │   │   │   ├── SystemConfigController.java # 系统配置
│   │   │   │   └── CodeSnippetController.java  # 代码片段
│   │   │   ├── service/             # 服务层
│   │   │   ├── repository/           # 数据访问层
│   │   │   ├── model/               # 实体和DTO
│   │   │   │   ├── entity/          # JPA实体
│   │   │   │   ├── dto/             # 数据传输对象
│   │   │   │   └── response/        # 响应封装
│   │   │   └── exception/           # 异常处理
│   │   └── resources/
│   │       ├── application.yml       # 配置文件
│   │       └── static/               # 前端静态资源
│   ├── data/                        # H2数据库文件
│   └── pom.xml                     # Maven配置
│
├── frontend/                        # 前端项目
│   ├── src/
│   │   ├── api/                    # API接口封装
│   │   │   ├── request.ts          # 请求拦截器
│   │   │   ├── auth.ts             # 认证接口
│   │   │   ├── environment.ts      # 环境接口
│   │   │   ├── component.ts        # 组件接口
│   │   │   ├── process.ts          # 流程接口
│   │   │   ├── repository.ts       # 仓库接口
│   │   │   ├── user.ts             # 用户接口
│   │   │   ├── system.ts           # 系统接口
│   │   │   └── snippet.ts          # 代码片段接口
│   │   ├── components/             # 公共组件
│   │   ├── layout/                 # 布局组件
│   │   │   └── MainLayout.vue      # 主布局
│   │   ├── router/                 # 路由配置
│   │   │   └── index.ts            # 路由定义
│   │   ├── stores/                 # 状态管理
│   │   │   └── user.ts             # 用户状态
│   │   ├── types/                  # 类型定义
│   │   ├── views/                  # 页面组件
│   │   │   ├── Login.vue           # 登录页
│   │   │   ├── Dashboard.vue       # 首页
│   │   │   ├── EnvironmentList.vue # 环境管理
│   │   │   ├── ComponentList.vue   # 组件管理
│   │   │   ├── ProcessList.vue     # 流程管理
│   │   │   ├── ProjectList.vue     # 代码仓库
│   │   │   ├── SnippetList.vue     # 代码片段
│   │   │   ├── UserManagement.vue  # 用户管理
│   │   │   ├── DictManagement.vue  # 字典管理
│   │   │   ├── SystemConfig.vue    # 系统配置
│   │   │   ├── Profile.vue         # 个人中心
│   │   │   ├── Search.vue          # 全局搜索
│   │   │   └── Database.vue        # 本地数据库
│   │   ├── App.vue                 # 根组件
│   │   └── main.ts                 # 入口文件
│   ├── package.json                # 依赖配置
│   ├── vite.config.ts              # Vite配置
│   └── tsconfig.json               # TS配置
│
├── README.md                       # 本文档
└── .gitignore                      # Git忽略配置
```

## 🚀 快速开始

### 环境要求

| 环境 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 21+ | Java开发环境 |
| Node.js | 18+ | 前端开发环境 |
| pnpm | 8+ | 推荐使用pnpm |

### 一体化部署（推荐）

只需启动后端即可访问完整应用：

```bash
# 1. 进入项目目录
cd my-new-work

# 2. 构建前端
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
# 终端1：启动后端
cd backend
mvn spring-boot:run

# 终端2：启动前端开发服务器
cd frontend
pnpm install
pnpm dev

# 访问前端：http://localhost:5173/
# 后端API：http://localhost:8080/api/
```

## 📋 访问地址

启动后端服务后，可通过以下地址访问：

| 页面 | 地址 | 说明 |
|------|------|------|
| **首页** | http://localhost:8080/ | 系统首页 |
| **登录页** | http://localhost:8080/login | 用户登录 |
| **H2控制台** | http://localhost:8080/h2-console | 数据库管理 |
| **API文档** | http://localhost:8080/swagger-ui.html | Swagger文档 |
| **代码片段** | http://localhost:8080/snippets | 代码片段管理 |

<img width="1281" height="941" alt="image" src="https://github.com/user-attachments/assets/c40465bc-9689-4220-a395-533775c2ce5e" />
<img width="1281" height="941" alt="image" src="https://github.com/user-attachments/assets/d74ccdaa-2b33-4561-bd78-dc7052880592" />
<img width="1281" height="941" alt="image" src="https://github.com/user-attachments/assets/319afd3d-fe7f-422d-bda5-655fc30ee913" />


### 默认登录账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| admin | admin123 | 管理员 |

> ⚠️ **安全提示**：首次登录后请及时修改默认密码！

## 📖 功能模块

### 1. 首页
- 📊 系统概览统计（环境数、组件数、流程数、项目数）
- 📝 最近活动记录
- 🔍 全局搜索功能
- 💬 欢迎消息配置

### 2. 环境管理
- 🌍 多环境配置（开发/测试/预发布/生产/演示）
- 🔑 数据库连接信息（地址、端口、账号、密码）
- 💾 Redis/MQ 配置信息
- 📋 文本复制功能

### 3. 技术组件
- 🛠️ 组件分类（数据库/缓存/消息队列/存储/监控/认证）
- 📦 版本管理
- 🔗 配置信息存储

### 4. 业务流程
- 📑 流程分类（发布/业务/运维/故障/维护/安全）
- 📝 流程步骤记录
- ⚠️ 注意事项
- ✅ 检查清单

### 5. 代码仓库
- 📁 项目全称和描述
- 🔗 代码仓库地址
- 👥 项目负责人
- 📂 文档位置

### 6. 代码片段
- 💻 多语言支持（Java/Vue/JavaScript/SQL）
- 📝 代码标题和描述
- 🏷️ 标签管理
- 📋 一键复制

### 7. 字典管理
- 📚 字典类型管理
- 💾 字典数据维护

### 8. 用户管理（管理员）
- 👤 用户增删改查
- 🔐 角色权限分配
- 🔁 密码重置

### 9. 系统配置
- ⚙️ 系统名称配置（实时生效）
- 🔧 登录页、首页标题定制

### 10. 本地数据库
- 🗄️ H2 控制台访问
- 📍 数据库文件路径显示

## 🔧 API 文档

启动后端后访问 Swagger UI 查看完整 API 文档：

```
http://localhost:8080/api/swagger-ui.html
```

### 主要 API 端点

| 模块 | 端点 | 方法 | 说明 |
|------|------|------|------|
| 认证 | /api/auth/login | POST | 用户登录 |
| 认证 | /api/auth/logout | POST | 用户登出 |
| 环境 | /api/environments | GET/POST | 环境列表/创建 |
| 组件 | /api/components | GET/POST | 组件列表/创建 |
| 流程 | /api/processes | GET/POST | 流程列表/创建 |
| 仓库 | /api/repositories | GET/POST | 仓库列表/创建 |
| 片段 | /api/snippets | GET/POST | 片段列表/创建 |
| 用户 | /api/users | GET | 用户列表 |
| 字典 | /api/dicts | GET | 字典列表 |
| 配置 | /api/system/config | GET/PUT | 系统配置 |

## 💾 数据库

### H2 数据库配置

| 配置项 | 值 |
|--------|-----|
| JDBC URL | `jdbc:h2:file:./data/mynewwork` |
| 用户名 | `sa` |
| 密码 | 空 |

### 访问 H2 控制台

```
JDBC URL: jdbc:h2:file:./data/mynewwork
用户名: sa
密码: (空)
```

### 数据文件位置

数据库文件存储在 `backend/data/` 目录下：
- `mynewwork.mv.db` - 数据文件
- `mynewwork.trace.db` - 日志文件

### 重置数据库

```bash
# 停止服务后，删除数据库文件
rm -rf backend/data/*
# 重启后端自动重建数据库
```

## ⚙️ 配置说明

### 后端配置 (application.yml)

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:h2:file:./data/mynewwork
    username: sa
    password:
  h2:
    console:
      enabled: true
      path: /h2-console

jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时
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
      }
    }
  }
})
```

## 🔒 安全特性

- 🔐 **JWT Token 认证**：无状态认证，支持自动刷新
- 🛡️ **密码加密**：BCrypt 加密存储
- 👥 **角色权限**：基于角色的访问控制（RBAC）
- 🔒 **敏感字段**：支持加密存储
- 🌐 **CORS 配置**：安全的跨域访问控制
- 📝 **操作日志**：记录用户操作历史

## 🧩 扩展开发

### 添加新功能模块

**后端：**
```java
// 1. 创建实体类
@Entity
@Table(name = "xxx")
public class XxxEntity {
    @Id
    @GeneratedValue
    private Long id;
}

// 2. 创建Repository
public interface XxxRepository extends JpaRepository<XxxEntity, Long> {
}

// 3. 创建Service
@Service
@RequiredArgsConstructor
public class XxxService {
    private final XxxRepository repository;
}

// 4. 创建Controller
@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
public class XxxController {
    private final XxxService service;
}
```

**前端：**
```typescript
// 1. 创建类型定义
export interface Xxx {
  id: number;
  name: string;
}

// 2. 创建API接口
export const xxxApi = {
  getList() {
    return request.get('/xxx');
  }
};

// 3. 创建页面组件
// views/XxxList.vue

// 4. 添加路由
// router/index.ts
```

## 📝 常见问题

### Q: 登录失败怎么办？

A: 请检查：
1. 后端是否正常启动
2. 数据库是否初始化
3. 用户名密码是否正确（默认：admin/admin123）

### Q: 前端页面空白？

A: 请检查：
1. 前端是否已构建
2. 静态资源是否在正确目录
3. 浏览器控制台是否有错误

### Q: 如何修改系统名称？

A: 登录管理员账号 → 系统配置 → 修改系统名称 → 实时生效

### Q: 如何备份数据？

A: 复制 `backend/data/` 目录下的 `.mv.db` 文件即可

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) - 强大的 Java 框架
- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Element Plus](https://element-plus.org/) - Vue 3 组件库
- [H2 Database](https://www.h2database.com/) - 纯 Java 数据库

---

<p align="center">
  Made with Jerry(姜源)❤️ by Java Developer
</p>
