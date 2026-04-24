# CLAUDE.md

本文档为 Claude Code (claude.ai/code) 在 My-New-Work 项目中工作时提供指导。

## 项目概述

My-New-Work 是一个助记单和环境信息导航系统，专为 Java 后端开发人员设计。该系统管理环境配置、技术组件、业务流程和项目导航，帮助员工快速了解工作环境和业务流程。

## 技术栈

### 后端技术栈
- **框架**: Spring Boot 3.2.0
- **安全**: Spring Security + JWT
- **数据库**: MySQL 8.0 + Spring Data JPA
- **缓存**: Redis
- **加密**: Jasypt 用于敏感数据加密
- **API文档**: SpringDoc OpenAPI
- **构建工具**: Maven
- **Java版本**: 21

### 前端技术栈
- **框架**: Vue 3 + TypeScript
- **构建工具**: Vite
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP客户端**: Axios
- **包管理**: pnpm

## 项目结构

```
my-new-work/
├── backend/                           # Spring Boot 后端
│   ├── src/main/java/
│   │   └── com.example.mynewwork/
│   │       ├── config/               # 配置类
│   │       ├── controller/           # REST API 控制器
│   │       ├── service/              # 业务逻辑服务
│   │       ├── repository/           # 数据访问层 (JPA Repository)
│   │       ├── model/entity/         # JPA 实体类
│   │       ├── model/dto/            # 数据传输对象
│   │       ├── security/             # 安全相关配置
│   │       ├── exception/            # 异常处理
│   │       └── util/                 # 工具类
│   ├── src/main/resources/
│   │   ├── application.yml           # 主配置文件
│   │   ├── application-dev.yml       # 开发环境配置
│   │   └── application-prod.yml      # 生产环境配置
│   └── pom.xml                      # Maven 依赖管理
│
├── frontend/                         # Vue 3 前端
│   ├── src/
│   │   ├── api/                     # API 接口封装
│   │   ├── assets/                  # 静态资源
│   │   ├── components/              # 通用组件
│   │   ├── layout/                  # 布局组件
│   │   ├── router/                  # 路由配置
│   │   ├── stores/                  # Pinia 状态管理
│   │   ├── types/                   # TypeScript 类型定义
│   │   ├── utils/                   # 工具函数
│   │   └── views/                   # 页面组件
│   │       ├── environments/        # 环境管理页面
│   │       ├── components/          # 技术组件页面
│   │       ├── processes/           # 业务流程页面
│   │       └── projects/            # 项目管理页面
│   ├── public/                      # 公共资源
│   └── package.json                 # pnpm 依赖管理
│
└── docs/                            # 项目文档
    ├── DEPLOYMENT.md                # 部署指南
    └── API.md                       # API 文档
```

## 核心模块

### 1. 用户认证模块
- **位置**: `backend/src/main/java/com/example/mynewwork/security/`
- **主要类**: `JwtTokenProvider`, `CustomUserDetailsService`, `UserPrincipal`
- **API路径**: `/api/auth/*`
- **功能**: JWT 认证、用户注册登录、权限验证

### 2. 环境管理模块
- **实体**: `Environment`
- **Repository**: `EnvironmentRepository`
- **Service**: `EnvironmentService`
- **Controller**: `EnvironmentController`
- **API路径**: `/api/environments/*`
- **功能**: 管理开发、测试、生产环境配置

### 3. 技术组件模块
- **实体**: `TechnicalComponent`
- **API路径**: `/api/components/*`
- **功能**: 管理数据库、Redis、MQ 等技术组件配置

### 4. 业务流程模块
- **实体**: `BusinessProcess`
- **API路径**: `/api/processes/*`
- **功能**: 管理发布流程、业务流程文档和检查清单

### 5. 项目管理模块
- **实体**: `Project`
- **API路径**: `/api/projects/*`
- **功能**: 管理项目代码仓库、文档位置等

## 常用开发命令

### 后端开发命令
```bash
# 清理并编译
mvn clean compile

# 运行测试
mvn test

# 打包应用
mvn package

# 运行应用
mvn spring-boot:run

# 加密配置值
mvn jasypt:encrypt-value -Djasypt.encryptor.password=secret -Djasypt.plugin.value="password"

# 运行单个测试
mvn test -Dtest=EnvironmentServiceTest

# 生成 API 文档
mvn springdoc-openapi:generate
```

### 前端开发命令
```bash
# 安装依赖
pnpm install

# 开发模式
pnpm dev

# 构建生产版本
pnpm build

# 预览构建结果
pnpm preview

# 代码检查
pnpm lint

# 代码格式化
pnpm format

# 运行单个测试
pnpm test -- --run src/components/EnvironmentList.spec.ts
```

### 数据库命令
```bash
# 创建数据库
CREATE DATABASE my_new_work CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 运行数据库迁移 (如果使用 Flyway/Liquibase)
mvn flyway:migrate

# 备份数据库
mysqldump -u username -p my_new_work > backup.sql
```

### Docker 命令
```bash
# 构建镜像
docker-compose build

# 启动服务
docker-compose up -d

# 查看日志
docker-compose logs -f

# 停止服务
docker-compose down
```

## 配置管理

### 环境配置优先级
1. 命令行参数
2. 环境变量
3. application-prod.yml
4. application.yml
5. 默认值

### 敏感配置示例
```yaml
# application.yml
spring:
  datasource:
    password: ENC(encrypted_password)

jasypt:
  encryptor:
    password: ${JASYPT_ENCRYPTOR_PASSWORD:default_secret}
```

## API 文档

### 访问地址
- **开发环境**: http://localhost:8080/api/swagger-ui.html
- **生产环境**: https://your-domain.com/api/swagger-ui.html

### 主要 API 分组
1. **认证 API**: `/api/auth/*`
2. **环境管理 API**: `/api/environments/*`
3. **技术组件 API**: `/api/components/*`
4. **业务流程 API**: `/api/processes/*`
5. **项目管理 API**: `/api/projects/*`
6. **搜索 API**: `/api/search/*`

## 安全配置

### JWT 配置
```yaml
jwt:
  secret: ${JWT_SECRET:your_secret_key}
  expiration: 86400000  # 24 小时
```

### CORS 配置
```java
@Configuration
public class CorsConfig {
    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:5173",
        "http://localhost:3000"
    };
}
```

## 开发工作流程

### 功能开发流程
1. **需求分析**: 明确功能需求和业务逻辑
2. **数据库设计**: 设计或修改表结构
3. **后端开发**:
   - 创建实体类
   - 实现 Repository
   - 编写 Service
   - 创建 Controller
4. **前端开发**:
   - 创建页面组件
   - 实现 API 调用
   - 添加路由配置
5. **测试验证**: 单元测试和集成测试
6. **代码审查**: 代码质量和规范检查

### Git 工作流程
1. 从 main 分支创建 feature 分支
2. 开发完成后提交 Pull Request
3. 代码审查通过后合并到 main
4. 定期发布版本标签

## 性能优化

### 数据库优化
- 合理使用索引
- 避免 N+1 查询问题
- 使用连接池
- 定期分析慢查询

### 缓存策略
- Redis 缓存热点数据
- HTTP 缓存静态资源
- 浏览器缓存策略

### 应用优化
- JVM 参数调优
- 异步处理耗时操作
- 合理配置线程池

## 监控和日志

### 健康检查
- `/api/actuator/health` - 应用健康状态
- `/api/actuator/info` - 应用信息
- `/api/actuator/metrics` - 性能指标

### 日志级别
- **DEBUG**: 开发环境详细日志
- **INFO**: 重要业务操作日志
- **WARN**: 警告信息
- **ERROR**: 错误信息

## 故障排查

### 常见问题
1. **数据库连接失败**: 检查 MySQL 服务状态和连接配置
2. **Redis 连接问题**: 验证 Redis 服务运行状态
3. **JWT 认证失败**: 检查令牌有效期和密钥配置
4. **文件上传失败**: 检查文件存储路径权限
5. **CORS 问题**: 验证 CORS 配置

### 调试技巧
1. 使用 Postman 测试 API
2. 查看应用日志定位问题
3. 使用浏览器开发者工具调试前端
4. 启用 SQL 日志查看数据库操作

## 代码质量工具

### 后端
- SonarQube 代码质量检测
- Checkstyle Java 代码规范检查
- JaCoCo 单元测试覆盖率

### 前端
- ESLint 代码检查
- Prettier 代码格式化
- TypeScript 严格模式
- Vue Test Utils 组件测试

## 部署注意事项

### 生产环境配置
1. 使用 HTTPS
2. 配置防火墙规则
3. 启用访问日志
4. 设置合适的 JVM 参数
5. 配置监控告警

### 备份策略
1. 定期备份数据库
2. 备份配置文件
3. 备份上传的文件
4. 测试备份恢复流程


---
### 1. 编码前思考
**不要假设。不要隐藏困惑。呈现权衡。**
LLM 经常默默选择一种解释然后执行。这个原则强制明确推理：
- **明确说明假设** — 如果不确定，询问而不是猜测
- **呈现多种解释** — 当存在歧义时，不要默默选择
- **适时提出异议** — 如果存在更简单的方法，说出来
- **困惑时停下来** — 指出不清楚的地方并要求澄清

### 2. 简洁优先
**用最少的代码解决问题。不要过度推测。**
对抗过度工程的倾向：
- 不要添加要求之外的功能
- 不要为一次性代码创建抽象
- 不要添加未要求的"灵活性"或"可配置性"
- 不要为不可能发生的场景做错误处理
- 如果 200 行代码可以写成 50 行，重写它
  **检验标准：** 资深工程师会觉得这过于复杂吗？如果是，简化。

### 3. 精准修改
**只碰必须碰的。只清理自己造成的混乱。**
编辑现有代码时：
- 不要"改进"相邻的代码、注释或格式
- 不要重构没坏的东西
- 匹配现有风格，即使你更倾向于不同的写法
- 如果注意到无关的死代码，提一下 —— 不要删除它
  当你的改动产生孤儿代码时：
- 删除因你的改动而变得无用的导入/变量/函数
- 不要删除预先存在的死代码，除非被要求
  **检验标准：** 每一行修改都应该能直接追溯到用户的请求。

### 4. 目标驱动执行
**定义成功标准。循环验证直到达成。**
将指令式任务转化为可验证的目标：
| 不要这样做... | 转化为...                            |
| ------------- | ------------------------------------ |
| "添加验证"    | "为无效输入编写测试，然后让它们通过" |
| "修复 bug"    | "编写重现 bug 的测试，然后让它通过"  |
| "重构 X"      | "确保重构前后测试都能通过"           |
对于多步骤任务，说明一个简短的计划：
```
1. [步骤] → 验证: [检查]
2. [步骤] → 验证: [检查]
3. [步骤] → 验证: [检查]
```