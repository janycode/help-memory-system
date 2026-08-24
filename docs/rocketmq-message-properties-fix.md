# /rocketmq 页面支持 RocketMQ 消息属性（Content-Type）修复方案

> 依据：`D:\work\03_code\om-project\docs\om-tm-container-truck-order-status-solutionB.md`（OM 消费 TM `container-truck-order-status` 消息 ClassCastException 的方案 B 结论）
> 状态：方案 B 已确认不可行；路径 B（放弃工具侧属性）已选定（2026-08-19）。本页保持原始发送，OM 修复交生产者侧。
> 日期：2026-08-19

## 一、背景与根因

OM 消费 tag=`container-truck-order-status`（TM 来源）时报 `ClassCastException: class [B cannot be cast to BaseMessagePayload`。

根因（方案 B 结论）：OM 的 RocketMQ 消费链路依赖消息的 **Content-Type 消息属性**（Properties）判断是否用 Jackson 反序列化：

- 消息携带 `Content-Type: application/json` 属性 → 反序列化为 `ContainerTruckOrderDTO`；
- 消息无该属性 → payload 保持 `byte[]` → 强转失败。

关键区分：**HTTP 请求头 Content-Type ≠ RocketMQ 消息属性 Content-Type**。当前 `/rocketmq` 页面通过控制台 `sendTopicMessage.do` 转发发送，HTTP 层带 `Content-Type: application/json` 头，但消息落库时**不带 Content-Type 属性**，故 OM 消费失败。

## 二、目标

让 `/rocketmq` 页面发送的消息携带 `Content-Type: application/json` **消息属性**（且支持自定义其他属性），使 OM 消费链路正常反序列化。

## 三、前置验证（关键前提，必须先行）

方案 B 生效的前提是控制台 `sendTopicMessage.do` 接受并写入**消息属性**参数。当前 `/rocketmq` 页面后端转发 payload 仅含 `{topic, key, tag, messageBody, traceEnabled}`，从未传过属性参数。**必须确认接口的属性参数名与格式**：

| 方式 | 操作 | 产出 |
|---|---|---|
| A（推荐） | 在 devops 控制台发送页填入 `Content-Type=application/json` 属性并发送，浏览器 F12 抓包，查看请求体里属性对应的**参数名**（候选：`properties` / `userProperties` / `msgProperties`）与格式（`key=value` 字符串 / JSON Map） | 确切的参数名与格式 |
| B（后端探测） | 用有效 JSESSIONID 调 `/api/tools/mq-send`，在 payload 中带候选属性参数名，发送后到控制台「消息查询」看消息详情是否含 `Content-Type` 属性 | 验证参数是否被写入 |

结论决定后端转发字段名。**在确认前，不建议直接改代码。**

## 四、改造方案

### 4.1 前端 `frontend/src/views/RocketMQ.vue`

| 项 | 内容 |
|---|---|
| 新增「消息属性」配置区 | 表单区新增 key-value 列表编辑器：默认预置一行 `Content-Type = application/json`，支持增删多行 |
| `buildPayloadForDisplay` / `buildPayload` | 增加 `properties: Record<string, string>`（合法行转对象，空值/非法行过滤） |
| 请求预览与弹窗预览 | 同步展示 properties（复用既有 json-box 展示与 JSON 着色） |
| 发送前校验 | 属性行必须是 `key=value` 格式；key 非空 |

### 4.2 后端 `BusinessToolController.sendMqMessage`

| 项 | 内容 |
|---|---|
| 读取 | 从 request 读取 `properties`（`Map<String,String>`，容错：缺失/非 Map 时置空） |
| 转发 | 转发 `sendTopicMessage.do` 时，将 properties 作为请求体字段（参数名 = 前置验证结论，默认 `properties`；格式按验证结论，如 `Content-Type=application/json` 串或 JSON） |
| 日志 | 打印 `properties`，便于排查 |

### 4.3 （可选）MqSend.vue 同步

MQ自动称重页面同步增加属性发送能力，保持两页一致。

## 五、风险与备选

| 风险 | 应对 |
|---|---|
| 控制台 `sendTopicMessage.do` 不支持属性参数（方案 B 前提不成立） | 工具层无法实现，退回文档方案 B 原意（人工在官方控制台发送）。工具侧彻底方案见备选① |
| 属性 key/value 与 OM 判断不一致（大小写/值） | 以 OM `BaseConsumer` 判断逻辑为准核对（`Content-Type=application/json`） |
| 属性 key 与 RocketMQ 系统属性冲突 | 自定义属性避免系统保留 key（如 `KEYS`/`TAGS`/`DELAY` 等） |

**备选①（彻底方案，改动大，仅当前提不成立时考虑）**：后端不经过控制台，改用 RocketMQ `DefaultMQProducer` 直连 broker 发送，`Message.putUserProperty("Content-Type", "application/json")` 完全可控属性。需要 namesrv 地址、broker 网络可达、客户端权限，且绕过控制台审计体系，风险较高，需单独评估。

## 六、验证方式

1. 发送后到控制台「消息查询」查看消息详情：确认含 `Content-Type=application/json` **属性**（非 HTTP 头）。
2. OM 日志出现 `收到 TM 订单状态变更事件参数: {...}`，不再打印 `ClassCastException`。
3. 历史消息/响应结果展示正常（无回归）。

## 七、已确认结论与待决策

### 7.1 已确认（2026-08-19）
- 控制台（devops.leaderrun.com/rocketmq）发送页**无「消息属性」输入框**（用户确认）。
- 抓包请求体仅 5 字段：`{topic, key, tag, messageBody, traceEnabled}`。
- Web 源码核实：老版 `rocketmq-console-ng` 的 `TopicMessageRequest` 不含 properties 字段，且 `sendTopicMessage` 内部构造 `Message` 时不调用 `putUserProperty`。
- **结论：方案 B（工具经控制台接口转发消息属性）彻底不可行。**

### 7.2 决策结果（2026-08-19）
- **选定：路径 B — 放弃工具侧属性**。用户确认：`/rocketmq` 页面保持原始发送（无属性），OM 消费修复依赖生产者侧（TM 真实发送代码 `putUserProperty` 设 `Content-Type`），本页面仅作无属性手动测试。
- 路径 A（备选① 后端直连 broker）不实施：需 namesrv/ACL/后端到 broker 可达性等前置，且属生产者侧职责范畴，不在本工具范围。
- 本草案不再进入实施阶段。

> 关键区分：OM 消费链路真正的修复点是**生产者侧**（TM 业务发送代码设 `Content-Type` 属性）。控制台/本工具只是手动测试入口，受限于控制台能力。
