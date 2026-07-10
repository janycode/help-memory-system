# AGENTS.md - 代理开发指南

## 项目概述
助记单系统：Spring Boot 3.2.0 + Vue 3 前后端一体化平台，管理环境配置、技术组件、业务流程等。
- 后端：Java 21、Spring Data JPA、H2 本地文件数据库（开箱即用）、Jasypt 加密敏感字段
- 前端：Vue 3、TypeScript、Element Plus、Vite、pnpm
- 一体化部署：前端构建产物自动复制到后端 `src/main/resources/static`

## 开发命令
### 后端（在 backend/ 目录）
- 启动：`mvn spring-boot:run`
- 构建：`mvn clean package -DskipTests`
- 测试：`mvn test`（目前无测试用例）
- 加密值：`mvn jasypt:encrypt-value -Djasypt.encryptor.password=secret -Djasypt.plugin.value="password"`

### 前端（在 frontend/ 目录）
- 安装依赖：`pnpm install`
- 开发服务器：`pnpm dev`（端口 5173，自动代理到后端 8080）
- 构建：`pnpm build`（输出到 `../backend/src/main/resources/static`）
- 代码检查：`pnpm lint`
- 格式化：`pnpm format`

### 一体化构建（根目录）
```bash
cd frontend && pnpm install && pnpm build && cd ../backend && mvn spring-boot:run
```

## 架构要点
- 数据库：H2 文件存储在 `data/mynewwork.mv.db`，JDBC URL：`jdbc:h2:file:../data/mynewwork;MODE=MySQL`
- 配置文件：`backend/src/main/resources/application.yml`
- 安全：JWT 认证（密钥通过 `JWT_SECRET` 环境变量配置），CORS 允许 `localhost:5173`
- 加密：Jasypt 加密 password/username/url 字段，密钥通过 `JASYPT_ENCRYPTOR_PASSWORD` 环境变量配置
- 前端代理：Vite 配置代理 `/api` 到后端（仅此路径，H2 控制台和 Swagger UI 需直接访问后端端口）

## 测试
- 后端：目前无测试用例，使用 `mvn test` 运行
- 前端：无测试配置，`package.json` 中未定义 test 脚本
- 集成测试：需手动测试，启动前后端服务后访问 http://localhost:8080

## 部署
- Docker：`docker build -t help-memory-system .`（需先构建前端）
- Docker 运行：`docker run -d -p 18080:18080 -v $(pwd)/data:/app/data --name help-memory-system help-memory-system:latest`
- 生产环境：推荐使用 Docker 或直接运行 JAR，数据库文件需持久化到 `data/` 目录
- 环境变量：生产环境必须设置 `JASYPT_ENCRYPTOR_PASSWORD` 和 `JWT_SECRET`

## 注意事项
- 首次启动自动创建数据库和初始化管理员账号（admin/admin123）
- 修改系统名称等配置无需重启，实时生效
- H2 控制台：http://localhost:8080/h2-console（JDBC URL 需与配置一致）
- API 文档：http://localhost:8080/swagger-ui.html
- 前端开发时，H2 控制台和 Swagger UI 需直接访问 http://localhost:8080（不经过 Vite 代理）
