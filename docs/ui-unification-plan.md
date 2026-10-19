# 前端页面展示统一方案

> 状态：待审阅。审阅通过后方可动代码。
> 依据：`GLOSSARY.md`（术语）、`docs/adr/0001-element-plus-native-over-admin-template.md`（方案取舍）。

## 1. 目标与非目标

### 目标

1. 建立单一的设计令牌层，页面与组件取用视觉值时不再出现裸值。
2. 建立三个展示层组件，把页头、容器、分页三处重复收敛为唯一实现。
3. 按三批把 21 个业务视图迁移到统一骨架，使同一类页面的结构、间距、字号、卡片层次一致。
4. 暗色模式作为令牌层的自然产物，已迁移页面必须通过暗色检查。

### 非目标

1. 不改动 `backend/` 任何文件、API 契约、H2 数据文件、store、router、类型定义。
2. 不引入新依赖，不改 `element-plus` 版本（现为 2.13.7）。
3. 不做逻辑搬迁：`copyToClipboard` 的 7 处、`formatDate` 的 8 处重复保持原样，需单独授权。
4. 不清理死资产：`TestStats.vue`、`MarkdownEditor.vue`、`src/styles/markdown-preview.{css,scss,min.css}` 本次不动，另记为独立清理项。
5. 不重做业务交互：按钮位置变化不改变点击后的行为，弹窗宽度变化不改变字段与校验。

## 2. 已定决策

| 编号 | 决策 | 结论 |
| --- | --- | --- |
| Q1 | 统一母版 | Element Plus 原生能力加 theme-chalk 令牌，自建薄封装 |
| Q2 | 改造边界 | 锁死 script 业务逻辑；允许改 template 结构、class、style、令牌 |
| Q3 | 覆盖范围 | 三批推进，第一批 5 个同构 CRUD 页 |
| Q4 | 验收口径 | build 通过、lint 通过、逐页冒烟、你本地自测；不新增测试框架 |
| Q5 | 暗色范围 | 令牌层定义亮暗双套；批内页面必须通过暗色检查 |
| Q6 | 组件粒度 | 三个薄组件；搜索栏不抽组件 |
| Q7 | 表格容器 | 统一包 `el-card` |
| Q8 | 弹窗策略 | 宽度三档 480 / 640 / 800，统一高度约束 |
| Q9 | 死资产 | 本次不动 |
| Q10 | ADR | 已落 `docs/adr/0001` |

## 3. 边界澄清

逻辑锁定（见 `GLOSSARY.md`）需要两处精确刻度，这是方案能落地的前提，请你确认：

1. **允许新增 import 语句**。在模板中使用新建组件，必须在 `script setup` 顶部增加 `import PageContainer from '@/components/PageContainer.vue'`。这属于声明性引用，不涉及业务逻辑。
2. **允许删除 scoped 样式中被全局规则取代的重复规则**。例如删除 5 个 CRUD 页各自声明的 `.page-header`、`.pagination-wrapper`、`.page-title` 规则。这属于样式收敛，不涉及 script。

不在允许范围内、需要单独授权的动作：新增或修改任何函数、ref、computed、watch、生命周期钩子；修改任何 API 调用与类型定义。

## 4. 令牌层

### 4.1 载体

新建 `frontend/src/styles/tokens.css`，在 `main.ts` 中于 `element-plus/dist/index.css` 之后、`./style.css` 之前引入。

不在 `style.css` 内扩展，原因是该文件现有 252 行混合了 reset、容器、Element 覆盖与响应式四类职责，令牌混入后无法区分【令牌定义】与【令牌消费】。

### 4.2 命名约定

- `--el-*`：仅用于覆盖 Element Plus 既有变量，不新增此命名空间。
- `--app-*`：本仓库自定义令牌。
- 语义颜色一律指向 Element 变量，不写字面值。这是暗色模式免费生效的关键。

### 4.3 令牌清单

```css
:root {
  /* 1. 间距刻度，4px 基数 */
  --app-space-1: 4px;
  --app-space-2: 8px;
  --app-space-3: 12px;
  --app-space-4: 16px;
  --app-space-5: 20px;
  --app-space-6: 24px;
  --app-space-8: 32px;

  /* 2. 页面容器 */
  --app-page-max-width: 1600px;
  --app-page-narrow-max-width: 1200px;
  --app-page-padding-y: var(--app-space-5);
  --app-page-padding-x: var(--app-space-6);

  /* 3. 字号 */
  --app-font-size-page-title: 20px;
  --app-font-size-section-title: 15px;
  --app-font-size-body: 14px;
  --app-font-size-secondary: 13px;

  /* 4. 圆角与阴影 */
  --app-radius-card: 8px;
  --app-radius-control: 6px;
  --app-shadow-card: 0 2px 12px rgba(0, 0, 0, 0.04);

  /* 5. 语义颜色，全部指向 Element Plus 变量 */
  --app-bg-page: var(--el-bg-color-page);
  --app-bg-surface: var(--el-bg-color);
  --app-text-primary: var(--el-text-color-primary);
  --app-text-regular: var(--el-text-color-regular);
  --app-text-secondary: var(--el-text-color-secondary);
  --app-text-placeholder: var(--el-text-color-placeholder);
  --app-border-base: var(--el-border-color-lighter);
  --app-border-light: var(--el-border-color-extra-light);
  --app-fill-light: var(--el-fill-color-light);

  /* 6. 搜索栏 */
  --app-search-input-width: 320px;
  --app-search-select-width: 160px;
  --app-search-gap: var(--app-space-2);

  /* 7. 弹窗档位 */
  --app-dialog-width-sm: 480px;
  --app-dialog-width-md: 640px;
  --app-dialog-width-lg: 800px;
  --app-dialog-max-height: 85vh;
}

html.dark {
  --app-shadow-card: 0 2px 12px rgba(0, 0, 0, 0.3);
}
```

已核实：`--el-bg-color-page`、`--el-bg-color`、`--el-text-color-primary/regular/secondary/placeholder`、`--el-border-color-lighter/extra-light`、`--el-fill-color-light` 在 2.13.7 的 `theme-chalk/index.css` 中均存在，且暗色版本由 `dark/css-vars.css` 在 `html.dark` 下重新赋值，因此第 5 组令牌无需写暗色副本。

### 4.4 弹窗高度的实现位置

`.el-dialog` 自身声明了 `--el-dialog-margin-top: 15vh` 并使用 `margin: var(--el-dialog-margin-top, 15vh) auto 50px`。因此该变量必须覆盖在 `.el-dialog` 而非 `:root`，写在 `:root` 会被元素自身声明遮蔽。落地写在 `style.css` 的 Element 覆盖区：

```css
.el-dialog {
  --el-dialog-margin-top: 6vh;
  display: flex;
  flex-direction: column;
  max-height: var(--app-dialog-max-height);
}

.el-dialog__body {
  overflow-y: auto;
  min-height: 0;
}
```

`.el-dialog:not(.prompt-dialog)` 之外无需特例，`GitHubIssues` 的 `92vh` 定高弹窗已有 `.prompt-dialog` 类且写在非 scoped 块中，通过 `max-height: none` 覆盖即可。

## 5. 三个展示层组件

三个组件的共同设计约束：**纯透传，接入时不改既有 script 的任何函数、状态与事件签名**。这是逻辑锁定能成立的前提。

### 5.1 PageContainer

`src/components/PageContainer.vue`，承担页面根容器与滚动契约。当前 `el-main.content` 为 `overflow: hidden`，滚动必须由页面自己承担，但 23 个视图中只有 3 个显式写了 `overflow-y: auto`，其余依赖内容溢出的默认表现，这是布局脆弱的根因。

| 项 | 定义 |
| --- | --- |
| props | `maxWidth`（Number/String，默认 1600）；`padded`（Boolean，默认 true）；`scrollable`（Boolean，默认 true） |
| slots | `default` |
| emits | 无 |

```html
<div class="page-container" :class="...">
  <div class="page-container__inner" :style="{ maxWidth: ... }">
    <slot />
  </div>
</div>
```

窄版页面（SystemConfig、UserManagement、DictManagement、MenuPermissionManagement）传 `:max-width="1200"`，取代当前的 4 份局部 `.container { max-width: 1200px }` 重定义。

### 5.2 PageHeader

`src/components/PageHeader.vue`，取代 15 处 `.page-header` 定义。

| 项 | 定义 |
| --- | --- |
| props | `title`（String，必填）；`description`（String，可选） |
| slots | `default`（标题下方自定义区）；`actions`（右侧操作区） |
| emits | 无 |

```html
<header class="page-header">
  <div class="page-header__main">
    <h1 class="page-header__title">{{ title }}</h1>
    <p v-if="description" class="page-header__desc">{{ description }}</p>
    <slot />
  </div>
  <div class="page-header__actions">
    <slot name="actions" />
  </div>
</header>
```

页面骨架统一为【标题与描述在左、操作在右】。四个系统管理页当前是【新建按钮换行左对齐】，迁移后并入此范式。这是可见变化之一，见第 10 节。

### 5.3 PaginationBar

`src/components/PaginationBar.vue`，取代 6 份逐字重复的分页块。

| 项 | 定义 |
| --- | --- |
| props | `currentPage`（Number）；`pageSize`（Number）；`total`（Number）；`pageSizes`（Array，默认 `[10, 20, 50, 100]`）；`layout`（String，默认 `total, sizes, prev, pager, next, jumper`）；`small`（Boolean） |
| emits | `current-change`、`size-change` |
| slots | 无 |

关键点：事件名与 `el-pagination` 保持一致，因此页面现有的

```html
@current-change="handlePageChange" @size-change="handleSizeChange"
```

原样保留即可，不需要改 handler 签名。这是选择【透传两个原生事件】而非【合并为单个 change 事件】的原因。

## 6. 标准页面骨架

迁移后的列表页结构固定为以下四段，顺序不可调整：

```html
<PageContainer>
  <PageHeader title="环境管理" description="...">
    <template #actions>
      <ColumnSelector ... />
      <el-button type="primary" @click="handleAdd">新增</el-button>
    </template>
  </PageHeader>

  <div class="search-bar">
    <div class="search-inputs">
      <el-input class="search-input" ... />
      <el-select class="filter-select" ... />
    </div>
    <el-button class="search-button" @click="...">搜索</el-button>
  </div>

  <el-card class="table-card" shadow="never">
    <el-table ... />
  </el-card>

  <PaginationBar
    :current-page="currentPage"
    :page-size="pageSize"
    :total="total"
    @current-change="handlePageChange"
    @size-change="handleSizeChange"
  />
</PageContainer>
```

强制约定：

1. 页头直接放在 `PageContainer` 内，不再包额外 div。
2. 搜索栏使用全局类 `.search-bar` / `.search-inputs` / `.search-input` / `.filter-select` / `.search-button`，页面内不再重复声明这些选择器。
3. 表格唯一入口是 `el-card.table-card`，`shadow` 统一写 `"never"` 字符串，禁止 `:'never'` 绑定写法与不传。
4. 分页是唯一入口，禁止直接写 `el-pagination`。
5. 根节点不再设置 `padding`、`min-height: 100vh`。

反例（当前存在，迁移时删除）：根节点 `padding: 20px` 加借用全局 `.container` 造成双重内边距；`.page-header` 在 15 个文件各写一遍；`<div style="display: flex; gap: 8px; align-items: center;">` 内联包裹操作按钮。

## 7. 搜索栏规范

保留现有全局类名，不新增命名，减少改动面。三条收敛：

1. 删除 5 个 CRUD 页 scoped 中的 `.search-bar`、`.search-inputs`、`.search-input`、`.filter-select`、`.search-button` 重复定义，统一由 `style.css` 的全局规则承担。
2. 修正冲突：全局 `.search-inputs .search-input { max-width: 320px }` 与局内 `.search-input { width: 600px }` 同时命中，按层叠实际由 320px 上限约束，但写法上互相矛盾。收敛为 `width: var(--app-search-input-width)`，去掉 `max-width`。
3. 下拉框统一 `width: var(--app-search-select-width)`，取消 `min-width: 120px` 与 `width: 150px` 两种写法并存。

`ProcessList` 与 `ComponentList` 同文件内 `.search-bar` 连续定义两次（后者覆盖前者），直接删除前者。

## 8. 弹窗规范

### 8.1 宽度档位

| 档位 | 令牌 | 适用 |
| --- | --- | --- |
| 小 | 480px | 简单表单，字段不超过 4 个 |
| 中 | 640px | 常规表单，字段 5 到 8 个 |
| 大 | 800px | 详情、多列、代码或长文本、内嵌表格 |

### 8.2 映射

| 现值 | 数量 | 去向 |
| --- | --- | --- |
| 400px | 1 | 480 |
| 500px | 6 | 480 |
| 560px | 1 | 640 |
| 600px | 6 | 640 |
| 650px | 1 | 640 |
| 700px | 7 | 表单类 640，详情类 800 |
| 800px | 6 | 800 |
| 90% / 80% | 4 | 800，`RocketMQ` 长文本预览需单独确认可读性 |
| `min(1180px, 94vw)` | 1 | 特例保留，`GitHubIssues` 定高弹窗 |

逐条归属在实施时按字段数判定，判定结果写入每批的提交说明。

### 8.3 高度

统一 `top: 6vh`（通过 `.el-dialog` 覆盖 `--el-dialog-margin-top`）与 `max-height: 85vh`，超出部分由 `.el-dialog__body` 内部滚动。`destroy-on-close` 缺失的 12 个弹窗补齐，避免残留状态。

## 9. 分批清单

### 第一批：同构 CRUD 页（5 个）

`EnvironmentList.vue`（454 行）、`ComponentList.vue`（559）、`ProcessList.vue`（530）、`ProjectList.vue`（421）、`SnippetList.vue`（523）。

这 5 个文件模板结构已逐字相同，是验证范式的最低成本样本。逐页改造点：

1. 根节点换成 `PageContainer`。
2. 页头换成 `PageHeader`，操作区放 `ColumnSelector` 与新增按钮。
3. 删除内联 `div style="display: flex; gap: 8px"` 包裹。
4. 表格包 `el-card shadow="never"`。
5. 分页块换成 `PaginationBar`。
6. 删除 scoped 中的 `.page-header`、`.page-title`、`.pagination-wrapper`、`.search-*` 重复规则。
7. 弹窗宽度按第 8 节改档，补 `destroy-on-close`。

风险：`SnippetList` 的 `total` 是局部 ref 而非 store 分页对象，传参时保持局部，不改数据来源。

### 第二批：低危展示页（8 个）

`DictManagement.vue`（362）、`UserManagement.vue`（353）、`MenuPermissionManagement.vue`（205）、`SystemConfig.vue`（232）、`JenkinsMonitor.vue`（384）、`Profile.vue`（244）、`HashId.vue`（132）、`Database.vue`（83）。

其中前 4 个需要处理双重内边距与 `min-height: 100vh` 遗留，`HashId` 与 `UserManagement` 的窄版用 `:max-width="1200"`。

### 第三批：高风险业务页（8 个）

`IterationList.vue`（718）与 `components/IterationDetail.vue`（1643）、`RocketMQ.vue`（1548）、`MqSend.vue`（672）、`Search.vue`（1141）、`Dashboard.vue`（1087）、`BatchSo.vue`（974）、`GitHubIssues.vue`（944）。

这一批只做骨架对齐与令牌替换，不改内部结构：`Dashboard` 的 6 个内联弹窗不合并、`Search` 的详情弹窗不抽组件、`BatchSo` 的左右分栏布局保持、`GitHubIssues` 的非 scoped 样式块保留。

### 批外（3 个，只对齐令牌不改骨架）

`Login.vue`、`NotFound.vue` 为全屏独立布局，不套用页面骨架；`TestStats.vue` 未挂路由，本次不动。

## 10. 三处可见变化（需你确认）

1. **页头结构**。四个系统管理页的【标题、描述、新建按钮纵向堆叠】变为【标题与描述在左，新建按钮在右】。
2. **搜索输入宽度**。由 600px 收为 320px，下拉框统一 160px，搜索区整体更紧凑。
3. **表格外层**。5 个 CRUD 页的裸表包上卡片，与其余页面一致；同时 33 个弹窗按档位调整宽度。

这三处会带来肉眼可见的差异，其余改动为间距微调与重复规则消除。

## 11. 每批验收

1. `pnpm build` 通过，产物输出到 `backend/src/main/resources/static`。
2. `pnpm lint` 通过。
3. 逐页冒烟：页面可加载、列表条数正确、搜索可用、翻页可用、打开弹窗后取消。
4. 冒烟不提交表单，避免写入 H2 数据。
5. 暗色模式下逐页检查批内页面，无白底黑字残留。
6. 每批一个独立提交，可按批回滚。

## 12. 遗留与后续

1. 死资产清理：`TestStats.vue`、`MarkdownEditor.vue`、三份 `markdown-preview.*`。建议作为独立任务归档到仓库外，另行授权。
2. 逻辑搬迁：`copyToClipboard` 7 处、`formatDate` 8 处、`getEnvironmentTypeTagType` 3 处重复，需单独授权。
3. `style.css` 的全局 `.container` 在全部批次完成后评估是否删除。
4. 响应式断点沿用现有 1200px 与 768px 两档，本次不调整。
