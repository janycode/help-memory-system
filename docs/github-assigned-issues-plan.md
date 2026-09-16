# GitHub 指派任务工作台 方案计划

> **修订记录**
>
> - **2026-09-16（实现收敛，已落地）**
>   1. Token 类型改为 **Classic PAT**（原方案写的 Fine-grained PAT 不再适用）。
>   2. 提示词模板精简：去掉「项目上下文」「已确认规则」「图片」三块；正文与评论中的 `<img />` 标签在生成时剥离。
>   3. 随之移除配置项 `github.project-labels`、`github.prompt.tech-context`、`github.prompt.extra-rules`，以及提示词弹窗里的「技术背景」覆盖输入。
>   4. 图片仍会下载，但仅用于详情抽屉预览，不再进入提示词；图片类警告改由响应字段 `imageWarnings` 单独返回，提示词只带正文/评论截断类警告。
>   5. 下文凡标注「已废弃 / 已调整」的段落属历史设计，保留备查，以本记录与当前代码为准。

## 1. 需求目标（已确认调整版）

1. 通过 GitHub API 获取**指派给当前 GitHub 账号**、**指定仓库**下的 issue 列表与 bug 列表；
2. 列表展示到 help-memory-system 页面上（本地部署）；
3. 每条任务支持查看详情（标题、正文、评论、图片）；
4. 每条任务可**一键生成提示词并复制**，供用户在外部 AI Agent 中对话，驱动 AI **直接修改项目代码**完成该 issue。

> 与上一版方案的本质差异：① 从「粘贴单条链接」升级为「拉取指派给"我"的完整列表」；② 提示词目标从「只产出计划文档」改为「驱动 AI Agent 修改项目代码」。

## 2. 需求理解与假设

| # | 理解 | 假设点（如有出入请指出） |
|---|------|------------------------|
| 1 | issue 列表与 bug 列表来自同一个仓库，**bug 用 label 区分**（默认 label 名 `bug`，可配置） | 依据：知识库中 pm 仓库既有需求号（#700/#775）也有 bug 号（#1241/bug-1411），同仓管理 |
| 2 | 目标仓库为 `LeaderrunTeam/pm` | 从现有代码 `IterationService` 硬编码地址与知识库推断，做成可配置 |
| 3 | 「指派给我」= issue 的 assignee 字段为当前 Token 对应账号 | 用 API `/user` 自动识别登录名，无需单独配置账号 |
| 4 | 列表默认只看 open 状态，页面可切换展示 closed | 可配置/可切换 |
| 5 | pm 仓库是**问题管理库，不是代码库**；真正改代码的位置在本地各 `-project` 目录（如 `D:\work\03_code\wm-project`） | ~~提示词中必须携带「目标代码库路径 + 技术背景」，生成时可在弹窗中临时修改~~ **已调整**：项目上下文（代码库路径/技术背景）已从提示词移除，改由 AI Agent 侧工作目录确定 |
| 6 | 图片依旧下载到本机目录，~~提示词引用本地路径~~ | **已调整**：图片仅用于详情抽屉预览，不再进入提示词 |

## 3. 认证与网络（不可绕过的约束）

- GitHub REST API 已禁用账号密码，**必须使用 Token**：使用 **Classic PAT**，需授权目标仓库的 Issues 和 Bugs 的只读权限（页面配置抽屉内已同步该提示语）。
- Token 经 jasypt `StringEncryptor` 加密存 `system_config` 表（复用 Jenkins 密码模式：加密存储、接口脱敏回显、日志不打印）。
- 国内网络访问 `api.github.com`：支持可选 HTTP 代理配置（JDK HttpClient ProxySelector 实现，复用现有 HttpClient 先例）。

## 4. 获取方案（后端拉取）

### 4.1 API 调用清单

| 用途 | GitHub API | 说明 |
|------|-----------|------|
| 识别当前账号 | `GET /user` | 返回 login，作为 assignee 过滤条件 |
| 指派给我的列表 | `GET /repos/{owner}/{repo}/issues?assignee={login}&state=open&per_page=100&page={n}` | **issues 接口混含 PR**，响应中 `pull_request` 字段非 null 的记录需过滤掉 |
| 单条详情 | `GET /repos/{owner}/{repo}/issues/{number}` | 标题/正文 Markdown/标签/状态/指派/时间 |
| 评论 | `GET /repos/{owner}/{repo}/issues/{number}/comments` | 按时间升序 |
| 图片 | 解析正文 `![alt](url)` 后带 `Authorization: Bearer` 头下载 | `github.com` / `user-images.githubusercontent.com` 域；其他域仅保留 URL 并警告 |

### 4.2 拉取策略

- **列表**：页面加载/点击刷新时实时调用（GitHub 认证限流 5000 次/小时，量级不可能触达）；不做本地镜像落库，避免同步复杂度。
- **Bug 分类**：列表 DTO 直接携带 labels 数组，前端按可配置的 bug 标签名（`github.bug-labels`，默认 `bug`，逗号分隔支持多个）做 Tab 过滤，「需求」= 不含 bug 标签。
- **详情 + 图片**：仅当用户点开详情或生成提示词时按需拉取（列表不拉正文，省流量省时间）。
- **图片落地**：`uploads/github/{repo}/issue-{n}/img-{序号}.{ext}`；单张 ≥ 5MB 或超 10 张跳过下载、保留 URL 并返回 warnings；`.gitignore` 追加 `uploads/github/`（现有 uploads 未忽略，避免图片进 git）。
- 定时自动刷新（如每 5 分钟轮询新指派）**列为 v2 扩展**，v1 手动刷新即可。

## 5. 展示方案（前端页面）

新页面 `views/GitHubIssues.vue`，路由 `/github-issues`，菜单项「GitHub 指派任务」（置于「迭代管理」附近）。页面结构自上而下：

1. **工具条**：仓库名展示 + 「刷新」按钮 + 「配置」按钮（打开配置抽屉）+ 状态筛选（open/closed）；
2. **Tab 分类**：全部 / 需求 / Bug（前端按 label 过滤）；
3. **列表表格**（Element Plus el-table）：Issue 号（可点击跳 GitHub 原链）、标题、类型标签、状态、创建/更新日期、操作列（「详情」「生成提示词」）；
4. **详情抽屉**：正文 Markdown 渲染、标签、评论区（折叠列表）、图片缩略图预览（点开大图）；
5. **提示词弹窗**：只读 Markdown 预览 + 「一键复制」按钮（`navigator.clipboard.writeText`）+ 「下载 .md」。~~弹窗顶部提供「目标代码库路径」「技术背景」两个临时覆盖输入框~~ —— **已调整**：两个覆盖输入框已移除，弹窗只保留操作按钮与提示词正文。

## 6. 配置设计（存 system_config 表）

| Key | 默认值 | 说明 |
|-----|--------|------|
| `github.token` | 空 | Classic PAT（jasypt 加密，接口脱敏回显） |
| `github.owner` | `LeaderrunTeam` | 组织 |
| `github.repo` | `pm` | 仓库 |
| `github.base-url` | `https://api.github.com` | 支持 GHES 场景预留 |
| `github.proxy-host` / `github.proxy-port` | 空 | 可选代理 |
| `github.bug-labels` | `bug` | bug 标签名，逗号分隔 |
| ~~`github.prompt.codebase-path`~~ | — | **已调整**：未实现（提示词通过 AI Agent 侧工作目录确定项目） |
| ~~`github.prompt.tech-context`~~ | — | **已移除**：项目上下文已从提示词模板去掉，配置项随之删除 |
| ~~`github.project-labels`~~ / ~~`github.prompt.extra-rules`~~ | — | **已移除**：已确认规则/项目标签不再进入提示词 |

## 7. 提示词生成方案（核心）

### 7.1 生成流程

```
行内点「生成提示词」→ 后端实时拉详情（正文+评论）→ 图片下载仅供详情预览
→ GitHubPromptBuilder 按模板拼装（剥离 <img /> 标签）→ 返回提示词全文 + 截断类 warnings
→ 弹窗展示 → 一键复制 → 粘贴到 AI Agent 开始对话
```

> 已调整：原流程中的「图片路径清单进提示词」已取消；图片下载警告由 `imageWarnings` 单独返回，只在详情抽屉展示。

### 7.2 提示词模板

**当前实现（`service/GitHubPromptBuilder` 常量模板，占位符字面量替换）**

```markdown
# 任务：处理 GitHub Issue

## 任务目标
{title}
{issueUrl}
标签：{labels}｜状态：{state}

## 输入与验收
### 正文
{body}
### 评论时间线
{comments}

验收标准：以正文中「验收点」为准；无明确验收内容时，输出对应测试场景。

## 实现约束
1. 修改范围：只改与 Issue 相关的文件，保持最小改动，不重构无关代码、不引入新依赖。
2. 遵循该项目既有编码规范与目录约定。
3. 全程使用简体中文。
4. 先输出计划文档，不要做任何代码改动。

## 本轮交付要求（三步走）
1. 第一步：分析问题 —— 定位根因（bug）或梳理需求要点，说明影响范围。
2. 第二步：输出计划 Markdown 文档，需包含：
   - 技术方案与改动清单（涉及哪些文件、每个文件改什么）
   - 所有需要我确认的技术决策点，逐条列出：
     · 推荐方案（默认执行）
     · 可选方案（至少 1 个）
     · 每个方案的优劣说明（收益 / 代价 / 风险）
   - 风险评估与回滚策略
3. 第三步：等我确认计划文档后再开始代码实施。确认前不要动任何文件。
```

> 说明：`{body}` 与 `{comments}` 在替换前会剥离 HTML `<img />` 标签（独立成行的整行删除、行内的只删标签，并收敛多余空行），避免把图片标签带进提示词。项目上下文、已确认规则、图片三块已按 2026-09-16 修订移除。

### 7.3 初版模板评审稿（已废弃，保留备查）

```markdown
# 任务：解决 GitHub Issue（直接修改项目代码）

你是一名资深全栈工程师，正在用户的本地代码库中工作。请基于下方材料
完成任务，最终以代码改动交付，而不是只出方案。

## 代码库信息
- 目标代码库本地路径：{codebasePath}
- 技术背景：{techContext}

## Issue 材料
- 链接：{issueUrl}
- 标题：{title}
- 标签：{labels}
- 状态：{state}

### 正文
{body}

### 评论时间线（按时间顺序）
{comments}

### 图片（本地路径，需要看图信息时直接用工具读取对应文件）
{imagePaths}

## 执行要求（按顺序）

1. 通读目标代码库中与 Issue 相关的代码，明确现状与涉及的模块。
2. 分析问题：定位根因（bug）或梳理需求要点，说明影响范围。
3. 信息充分性判断：若关键信息缺失，先输出「二次确认清单」
   （逐条标注 阻塞/非阻塞），等用户补充后再继续，不要臆测硬做。
4. 直接实施：在目标代码库中修改代码完成 Issue，遵循该项目的
   既有编码规范、架构与目录约定，保持最小改动。
5. 自测验证：执行可用的编译/构建/测试手段，确认无误。
6. 收尾输出：给出文件级改动清单与验证结论。

## 约束
- 与用户全程使用简体中文交流。
- 不修改与 Issue 无关的文件，不引入需求之外的依赖与重构。
- 无法确认的点先说明风险再动手。

{extraRules}
```

> 说明（针对上方 7.3 初版评审稿）：第 3 步即「根据分析判断是否需要二次确认」的落地。`extraRules` 对应的配置项 `github.prompt.extra-rules` 已于 2026-09-16 移除，该占位符不再存在。

## 8. 接口设计（`controller/GitHubController`，`/api/github/*`）

| 方法 | 路径 | 功能 |
|------|------|------|
| GET | `/api/github/config` | 读配置（token 脱敏为 `ghp_***abc`） |
| POST | `/api/github/config` | 保存配置（token 为空串 = 不修改） |
| POST | `/api/github/connection-test` | 调 `/user` 验证 Token，返回登录名（对应「指派给我」的"我"） |
| GET | `/api/github/issues?state=open&page=1` | 指派给当前账号的列表（已过滤 PR，含 labels） |
| GET | `/api/github/issues/{number}` | 详情：正文+评论+图片（含下载），warnings（截断类）+ imageWarnings（图片类） |
| POST | `/api/github/issues/{number}/prompt` | 生成提示词：**无请求体**，返回 prompt/warnings（仅截断类） |

统一规范：所有接口注入 `ActivityLogger` 记录操作日志；配置错误/网络异常/404（issue 不存在）/401（Token 失效）均返回友好中文错误（复用 `GlobalExceptionHandler`）。

## 9. 改动文件清单

**后端（新增 7 个）**

1. `model/dto/GitHubConfigDTO.java` — 配置读写 DTO（token 脱敏处理）
2. `model/dto/GitHubIssueDTO.java` — 列表行 + 详情 DTO（详情报文复用，评论/图片列表字段）
3. `model/dto/GitHubPromptResultDTO.java` — prompt 全文 + issue 摘要 + 图片清单 + warnings
4. `service/GitHubService.java` — 配置存取、连接测试、列表、详情、图片下载
5. `service/GitHubPromptBuilder.java` — 提示词模板拼装
6. `controller/GitHubController.java` — 上述 6 个接口
7. `config/` 无新增（复用 SystemConfig + StringEncryptor，无定时任务）

**前端（新增 3 个 + 修改 2 个）**

8. `types/github.ts` / `api/github.ts` — 类型与 Axios 封装
9. `views/GitHubIssues.vue` — 页面（列表 + 详情抽屉 + 提示词弹窗）
10. `router/index.ts` 加路由 `/github-issues`；`layout/MainLayout.vue` 加菜单项

**其他**

11. `.gitignore` 追加 `uploads/github/`

零新依赖：后端复用 JDK HttpClient + `org.jsoup` 是否需要？——**不需要**，正文 Markdown 原样嵌入提示词，前端渲染用已装的 markdown 库（Element Plus 体系配套）。仅图片正则解析用 JDK 内置 `Pattern`。

## 10. 实施步骤

| 阶段 | 内容 | 验证 |
|------|------|------|
| 1 | 配置存取 + 加密 + connection-test | 真实 Token 测试连接返回登录名；无效 Token 报 401 友好提示 |
| 2 | 列表接口（assignee 过滤 + PR 过滤 + 分页） | 返回的每条均指派给当前账号；含 bug label 的记录标签正确 |
| 3 | 详情接口（正文 + 评论 + 图片下载 + 限制策略） | 带截图 issue 图片落盘 `uploads/github/...` 本地可打开 |
| 4 | PromptBuilder + 生成接口 | curl 返回完整提示词，占位符全部替换、warnings 正确 |
| 5 | 前端页面（Tab/表格/抽屉/弹窗/复制） | 全流程点选可用，复制按钮一次到位 |
| 6 | 端到端验证 + 构建 | 提示词粘贴到 AI Agent 能驱动其修改本地项目代码；`pnpm build` + `mvn clean package -DskipTests` 通过 |

## 11. 风险与注意点

| 风险 | 对策 |
|------|------|
| 访问 api.github.com 不稳定 | 代理可配 + connection-test 即时反馈 |
| Token 泄露面 | jasypt 加密、接口脱敏、日志不打印；建议 Classic PAT 限仓库、限期限 |
| issues 接口混入 PR | 响应过滤 `pull_request != null`，属易踩坑点（已在 4.1 标注） |
| bug 标签口径不一 | `github.bug-labels` 可配置多个标签名 |
| 图片数量/体积失控 | 10 张上限 + 单张 5MB 跳过并警告 |
| 超长 issue/评论撑爆提示词 | 正文与评论各自按配置截断（默认 20000 字符），截断写入 warnings |
| AI 在错误目录改代码 / 改错项目 | 已调整：不再由提示词携带代码库路径，改由 AI Agent 侧工作目录确定 |
| 私有图片路径跨机（系统与 AI Agent 不同机器） | 图片不再进提示词，该风险随 2026-09-16 精简一并消除 |
| pm 仓库 ≠ 代码仓库，issue 可能涉及多个系统 | 已调整：项目上下文不再进提示词 |

## 12. 待确认问题清单

1. **bug 分类口径**：确认 pm 仓库中 bug 以 `bug` label 区分？（还是其他标签名 / 独立仓库？）
2. **列表实时拉取**：v1 页面手动刷新，不做定时轮询；是否需要定时自动刷新 + 新指派提醒？
3. **closed 记录**：默认只展示 open，是否需要展示 closed（可选筛选）？
4. **评论默认包含**：生成提示词时默认带全部评论（bug 复现细节常在评论），确认可以？
5. **同机部署**：确认 help-memory-system 与 AI Agent 在同一台机器（图片绝对路径才可读）；跨机器则需要额外"打包下载"能力。
6. **代码库路径默认值**：~~`github.prompt.codebase-path`~~ 已取消该配置项，AI Agent 侧自行确定工作目录。

## 13. 验收标准

1. 配置有效 Classic PAT 后，connection-test 返回当前账号登录名；
2. 「刷新」后列表展示**指派给我的**指定仓库 open issues，需含 bug，且不含 PR；需求/Bug Tab 过滤正确；
3. 点「详情」可见正文渲染、评论、图片（本地已下载且可打开）；私有图片下载失败在详情抽屉有提示（imageWarnings）；
4. 点「生成提示词」→ 弹窗展示完整提示词（任务目标 + 正文 + 评论 + 约束 + 交付要求，不含项目上下文/规则/图片）→ 一键复制成功；
5. 把提示词粘贴到 AI Agent（指定本地项目路径），AI 完成代码修改且遵循"先分析、信息不足先确认"的流程；
6. Token 失效/仓库不存在/issue 不存在/网络不通，均返回友好中文错误；
7. 编译构建验证：先 `pnpm build` 再 `mvn clean package -DskipTests`，全部通过；
8. 每次拉取/生成在活动日志中可见。