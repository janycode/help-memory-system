# 部署指南

## 环境准备

### 服务器要求
- **操作系统**: Linux (推荐 Ubuntu 20.04+ 或 CentOS 8+)
- **内存**: 最低 4GB，推荐 8GB+
- **磁盘**: 最低 20GB 可用空间
- **网络**: 稳定的网络连接

### 软件依赖
- JDK 17
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+
- Nginx (生产环境)
- Docker (可选，推荐用于生产环境)

## 后端部署

### 1. 数据库配置

#### 创建数据库
```sql
CREATE DATABASE my_new_work 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

-- 创建专用用户
CREATE USER 'mynewwork'@'%' IDENTIFIED BY 'your_secure_password';
GRANT ALL PRIVILEGES ON my_new_work.* TO 'mynewwork'@'%';
FLUSH PRIVILEGES;
```

#### 配置数据库连接
编辑 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/my_new_work?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: mynewwork
    password: ENC(encrypted_password)  # 使用 Jasypt 加密
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### 2. Redis 配置

#### 安装 Redis
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install redis-server

# CentOS/RHEL
sudo yum install redis
```

#### 配置 Redis
编辑 `/etc/redis/redis.conf`：
```conf
bind 127.0.0.1
port 6379
daemonize yes
requirepass your_redis_password
databases 16
maxmemory 2gb
maxmemory-policy allkeys-lru
```

重启 Redis：
```bash
sudo systemctl restart redis
```

### 3. 应用配置

#### 敏感信息加密
使用 Jasypt 加密敏感配置：

```bash
# 加密数据库密码
./mvnw jasypt:encrypt-value -Djasypt.encryptor.password=your_secret_key -Djasypt.plugin.value="your_db_password"

# 加密 Redis 密码
./mvnw jasypt:encrypt-value -Djasypt.encryptor.password=your_secret_key -Djasypt.plugin.value="your_redis_password"
```

#### 生产环境配置
创建 `application-prod.yml`：

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:${DB_PORT:3306}/my_new_work?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:mynewwork}
    password: ${DB_PASSWORD:ENC(encrypted_password)}
    
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:ENC(encrypted_redis_password)}
      database: 0
      timeout: 3000ms

# JWT 配置
jwt:
  secret: ${JWT_SECRET:your_jwt_secret_key}
  expiration: 86400000

# 文件存储
app:
  file-storage:
    upload-dir: /var/www/my-new-work/uploads

# 日志配置
logging:
  file:
    name: /var/log/my-new-work/application.log
  level:
    com.example.mynewwork: INFO
    org.springframework.security: WARN
```

### 4. 构建和运行

#### 构建应用
```bash
cd backend
./mvnw clean package -DskipTests
```

#### 运行应用
```bash
# 设置环境变量
export JASYPT_ENCRYPTOR_PASSWORD="your_secret_key"
export DB_PASSWORD="ENC(encrypted_password)"
export REDIS_PASSWORD="ENC(encrypted_redis_password)"
export JWT_SECRET="your_jwt_secret_key"

# 启动应用
java -jar target/my-new-work-1.0.0.jar \
  --spring.profiles.active=prod \
  --spring.config.location=classpath:/application.yml,classpath:/application-prod.yml
```

#### 使用 systemd 管理
创建服务文件 `/etc/systemd/system/my-new-work.service`：

```ini
[Unit]
Description=My New Work Application
After=syslog.target
After=network.target

[Service]
Type=application
User=mynewwork
Group=mynewwork
WorkingDirectory=/opt/my-new-work
ExecStart=/usr/bin/java -jar /opt/my-new-work/my-new-work-1.0.0.jar
SuccessExitStatus=143
Restart=always
RestartSec=10

# 环境变量
Environment=JASYPT_ENCRYPTOR_PASSWORD=your_secret_key
Environment=DB_PASSWORD=ENC(encrypted_password)
Environment=REDIS_PASSWORD=ENC(encrypted_redis_password)
Environment=JWT_SECRET=your_jwt_secret_key

[Install]
WantedBy=multi-user.target
```

启用并启动服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable my-new-work
sudo systemctl start my-new-work
sudo systemctl status my-new-work
```

## 前端部署

### 1. 构建生产版本
```bash
cd frontend
pnpm install
pnpm build
```

构建输出位于 `frontend/dist/` 目录。

### 2. Nginx 配置

#### 安装 Nginx
```bash
# Ubuntu/Debian
sudo apt install nginx

# CentOS/RHEL
sudo yum install nginx
```

#### 配置 Nginx
创建配置文件 `/etc/nginx/sites-available/my-new-work`：

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    # 前端静态文件
    root /var/www/my-new-work/frontend;
    index index.html index.htm;
    
    # 前端路由支持
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket 支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    
    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }
    
    # 错误页面
    error_page 404 /index.html;
    error_page 500 502 503 504 /50x.html;
    location = /50x.html {
        root /usr/share/nginx/html;
    }
}
```

启用配置：
```bash
sudo ln -s /etc/nginx/sites-available/my-new-work /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl restart nginx
```

### 3. 部署文件
```bash
# 创建部署目录
sudo mkdir -p /var/www/my-new-work/frontend
sudo chown -R $USER:$USER /var/www/my-new-work

# 复制前端文件
cp -r frontend/dist/* /var/www/my-new-work/frontend/

# 设置权限
sudo chown -R www-data:www-data /var/www/my-new-work
sudo chmod -R 755 /var/www/my-new-work
```

## Docker 部署（推荐）

### 1. 创建 Dockerfile

#### 后端 Dockerfile
`backend/Dockerfile`：
```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

# 复制应用 JAR 文件
COPY target/my-new-work-*.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口
EXPOSE 8080

# 运行应用
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
```

#### 前端 Dockerfile
`frontend/Dockerfile`：
```dockerfile
FROM nginx:alpine

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf
COPY nginx-site.conf /etc/nginx/conf.d/default.conf

# 复制构建输出
COPY dist /usr/share/nginx/html

# 暴露端口
EXPOSE 80

# 启动 Nginx
CMD ["nginx", "-g", "daemon off;"]
```

### 2. 创建 docker-compose.yml
```yaml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root_password
      MYSQL_DATABASE: my_new_work
      MYSQL_USER: mynewwork
      MYSQL_PASSWORD: db_password
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    restart: always

  redis:
    image: redis:7-alpine
    command: redis-server --requirepass redis_password
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    restart: always

  backend:
    build: ./backend
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - JASYPT_ENCRYPTOR_PASSWORD=your_secret_key
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_USERNAME=mynewwork
      - DB_PASSWORD=ENC(encrypted_password)
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - REDIS_PASSWORD=ENC(encrypted_redis_password)
      - JWT_SECRET=your_jwt_secret_key
    depends_on:
      - mysql
      - redis
    restart: always

  frontend:
    build: ./frontend
    ports:
      - "80:80"
    depends_on:
      - backend
    restart: always

volumes:
  mysql_data:
  redis_data:
```

### 3. 启动 Docker 容器
```bash
docker-compose up -d
```

## HTTPS 配置

### 使用 Let's Encrypt
```bash
# 安装 Certbot
sudo apt install certbot python3-certbot-nginx

# 获取 SSL 证书
sudo certbot --nginx -d your-domain.com

# 自动续期测试
sudo certbot renew --dry-run
```

### Nginx HTTPS 配置
```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;
    
    ssl_certificate /etc/letsencrypt/live/your-domain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/your-domain.com/privkey.pem;
    
    # SSL 配置
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES256-GCM-SHA512:DHE-RSA-AES256-GCM-SHA512:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers off;
    
    # 其余配置同上...
}

# HTTP 重定向到 HTTPS
server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$server_name$request_uri;
}
```

## 监控和维护

### 健康检查
```bash
# 检查应用状态
curl -f http://localhost:8080/api/actuator/health || exit 1

# 检查前端可访问性
curl -f http://localhost || exit 1
```

### 日志管理
```bash
# 查看应用日志
tail -f /var/log/my-new-work/application.log

# 查看 Nginx 访问日志
tail -f /var/log/nginx/access.log

# 查看 Nginx 错误日志
tail -f /var/log/nginx/error.log
```

### 备份策略
```bash
# 数据库备份
mysqldump -u mynewwork -p my_new_work > backup_$(date +%Y%m%d).sql

# 文件备份
tar -czf uploads_backup_$(date +%Y%m%d).tar.gz /var/www/my-new-work/uploads/
```

## 故障排除

### 常见问题

1. **数据库连接失败**
   - 检查 MySQL 服务状态
   - 验证数据库用户权限
   - 检查防火墙设置

2. **Redis 连接问题**
   - 确认 Redis 服务运行状态
   - 检查 Redis 密码配置
   - 验证网络连接

3. **前端路由问题**
   - 确保 Nginx 配置正确
   - 检查前端构建输出
   - 验证静态文件权限

4. **应用启动失败**
   - 检查日志文件
   - 验证环境变量
   - 确认端口未被占用

### 性能优化

1. **数据库优化**
   - 添加适当的索引
   - 优化查询语句
   - 定期清理历史数据

2. **应用优化**
   - 调整 JVM 参数
   - 配置连接池大小
   - 启用缓存策略

3. **前端优化**
   - 启用 Gzip 压缩
   - 配置浏览器缓存
   - 使用 CDN 加速静态资源

## 安全建议

1. **网络安全**
   - 配置防火墙规则
   - 使用 VPN 或专线访问
   - 定期更新系统补丁

2. **应用安全**
   - 定期更换密钥
   - 启用访问日志
   - 配置速率限制

3. **数据安全**
   - 定期备份数据
   - 加密敏感信息
   - 限制数据库访问权限
