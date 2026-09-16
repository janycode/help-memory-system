# Jenkins 构建成功 → 企业微信通知 实现计划

## 1. 功能概述

系统每 30 秒轮询 Jenkins REST API，检测已配置的 job 是否出现**新的成功构建**；检测到后，通过企业微信 webhook 机器人向群推送构建成功通知。

- 监控方式：定时轮询 Jenkins API（已与用户确认）
- 监控范围：页面可配置多个 job（已与用户确认）
- 轮询频率：固定 30 秒（用户指定）
- 实现原则：最简单，不做过度设计

## 2. 技术方案

| 事项 | 方案 | 说明 |
|------|------|------|
| 获取构建状态 | `GET {jenkinsUrl}/job/{jobName}/lastBuild/api/json` | Basic Auth（账号密码） |
| HTTP 客户端 | JDK 21 内置 `java.net.http.HttpClient` | 零新依赖 |
| 定时任务 | 主类加 `@EnableScheduling` + `@Scheduled(fixedDelay = 30000)` | 当前项目未启用，需新增 |
| 密码存储 | 复用已有 jasypt `StringEncryptor` 加密 | 与项目加密机制一致 |
| 企业微信通知 | POST webhook，markdown 消息 | 群机器人标准协议 |

## 3. 数据模型（单表）

新增实体 `JenkinsJobConfig`，表 `jenkins_job_config`：

| 字段 | 列 | 说明 |
|------|-----|------|
| id | id | 主键自增 |
| jobName | job_name | Jenkins job 名称 |
| jenkinsUrl | jenkins_url | Jenkins 地址（如 http://192.168.x.x:8080） |
| username | username | Jenkins 账号 |
| password | password | Jenkins 密码（jasypt 加密存储） |
| webhookUrl | webhook_url | 企业微信机器人 webhook 地址 |
| atMobiles | at_mobiles | 需 @ 的手机号（逗号分隔，可空） |
| enabled | enabled | 是否启用监控 |
| lastNotifiedBuild | last_notified_build | 已通知的最大构建号 |
| remark | remark | 备注 |
| createdAt / updatedAt | created_at / updated_at | 审计时间（沿用 JpaAuditing） |

> 单表方案：每个 job 一条完整配置（含连接信息、webhook）。优点是最简单直观，后续可支持「不同 job 发不同群」，重复存储的少量连接信息对单人项目可接受。

## 4. 后端改动（新增 4 个文件 + 1 处注解）

1. `model/entity/JenkinsJobConfig.java` — 实体，风格对齐现有 `Environment` 实体
2. `repository/JenkinsJobConfigRepository.java` — 仓库接口
3. `service/JenkinsMonitorService.java` — 核心逻辑
4. `controller/JenkinsMonitorController.java` — `@RequestMapping("/api/jenkins-monitor")` CRUD + 测试连接
5. 主应用类加 `@EnableScheduling` 注解

### 轮询逻辑（JenkinsMonitorService）

```
每 30s 执行 checkBuilds()：
1. 查询所有 enabled 的 job 配置
2. 逐个调用 lastBuild/api/json（Basic Auth）
3. 条件判断：result == "SUCCESS" 且 number > lastNotifiedBuild
   → 发送企业微信通知 → 更新 lastNotifiedBuild
4. 边界处理：
   - 首次启用：记录当前构建号为基线，不发送（避免启动即刷屏）
   - 构建中（building=true）：跳过
   - 网络/认证异常：打 error 日志，不阻断其他 job
```

### 企业微信消息格式

```
**【Jenkins 构建成功】{jobName}**
> 构建号：#12
> 结果：SUCCESS
> 构建地址：[点击查看]({buildUrl})
```

## 5. 前端改动（新增 3 个文件 + 2 处修改）

1. `types/jenkinsMonitor.ts` — TS 类型
2. `api/jenkinsMonitor.ts` — Axios 封装
3. `views/JenkinsMonitor.vue` — 页面：
   - 表格列出所有 job 配置（job 名称 / Jenkins 地址 / 是否启用开关 / 最后已通知构建号 / 操作）
   - 新增 / 编辑弹窗：jobName、jenkinsUrl、username、password、webhookUrl、atMobiles、enabled、remark
   - 密码编辑时留空表示不修改（后端相应处理）
   - 「测试连接」按钮：后端调一次 Jenkins API 验证账号
4. `router/index.ts` 加路由 `/tools/jenkins-monitor`
5. `layout/MainLayout.vue` 业务工具分组加菜单项「Jenkins 监控」（沿用 `isMenuAllowed` 菜单权限机制）

## 6. 验证标准

1. 按项目约定：先 `cd frontend && pnpm build` 再 `cd backend && mvn clean package -DskipTests`，jar 打包成功即说明编译通过
2. 运行时验证：配置真实 Jenkins 地址/账号/密码 + 企业微信 webhook，手动触发一次构建，确认：
   - 前端页面能保存配置、能测试连接成功
   - 构建成功后企业微信群收到 markdown 通知
   - 日志无异常，二次轮询不重复发送同构建号

## 7. 待用户补充的信息（运行配置时填写，无需写死代码）

- Jenkins 地址、账号、密码
- 企业微信机器人 webhook 地址（群设置 → 群机器人添加）
- 需监控的 job 名称列表

## 8. 明确不做的内容（防过度设计）

- 不做失败/取消构建通知（只通知成功）
- 不做历史通知记录表与统计
- 不做消息重试队列（发送失败仅打日志）
- 不做 Jenkins 侧回调、不做 WebSocket 实时推送