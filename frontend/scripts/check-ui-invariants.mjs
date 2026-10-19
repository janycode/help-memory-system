#!/usr/bin/env node
// 前端展示统一的架构不变量检查。零依赖，供每批迁移做验收闸门。
// 判定原则：已迁移视图与共享展示层不满足即失败；未迁移页面只计待办，不阻断。
import { readFileSync, readdirSync, statSync } from 'node:fs'
import { join, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = fileURLToPath(new URL('..', import.meta.url))
const SRC = join(ROOT, 'src')

// 已迁移到统一骨架的视图，每批完成后在此追加
const MIGRATED_VIEWS = [
  'views/EnvironmentList.vue',
  'views/ComponentList.vue',
  'views/ProcessList.vue',
  'views/ProjectList.vue',
  'views/SnippetList.vue',
  'views/DictManagement.vue',
  'views/UserManagement.vue',
  'views/MenuPermissionManagement.vue',
  'views/SystemConfig.vue',
  'views/JenkinsMonitor.vue',
  'views/Profile.vue',
  'views/HashId.vue',
  'views/Database.vue',
  'views/IterationList.vue',
  'views/RocketMQ.vue',
  'views/MqSend.vue'
]

// 共享展示层，逐令牌化
const SHARED_LAYER_FILES = [
  'style.css',
  'components/PageContainer.vue',
  'components/PageHeader.vue',
  'components/PaginationBar.vue'
]

// 弹窗宽度允许值，三档令牌或等值字面量
const ALLOWED_DIALOG_WIDTHS = new Set([
  '480px',
  '640px',
  '800px',
  'var(--app-dialog-width-sm)',
  'var(--app-dialog-width-md)',
  'var(--app-dialog-width-lg)'
])

// 定高或宽幅特例白名单，格式 相对 src 的路径:宽度值
// MQ 工具页的请求体与代码预览需要超出 800 档的宽度，保留百分比写法
const SPECIAL_DIALOG_WIDTHS = new Set([
  'views/GitHubIssues.vue:min(1180px, 94vw)',
  'views/RocketMQ.vue:90%',
  'views/RocketMQ.vue:80%',
  'views/MqSend.vue:90%'
])

// 已被全局规则取代的局部选择器
const LEGACY_SELECTORS = [
  '.page-header',
  '.page-title',
  '.pagination-wrapper',
  '.search-bar',
  '.search-inputs',
  '.search-input',
  '.filter-select',
  '.search-button'
]

const failures = []
const pending = []
const warnings = []

const lineAt = (text, index) => text.slice(0, index).split('\n').length
const rel = (file) => relative(SRC, file).replace(/\\/g, '/')
const isMigrated = (file) => MIGRATED_VIEWS.includes(rel(file))

function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const full = join(dir, name)
    if (statSync(full).isDirectory()) walk(full, out)
    else if (name.endsWith('.vue')) out.push(full)
  }
  return out
}

function templateRoot(text) {
  const start = text.indexOf('<template>')
  if (start < 0) return null
  const body = text.slice(start + '<template>'.length)
  const matched = body.match(/^\s*(?:<!--[\s\S]*?-->\s*)*<([a-zA-Z][\w-]*)/)
  return matched ? matched[1] : null
}

function styleBlocks(text) {
  const blocks = []
  const re = /<style[^>]*>([\s\S]*?)<\/style>/g
  let matched
  while ((matched = re.exec(text))) blocks.push({ content: matched[1], index: matched.index })
  return blocks
}

const vueFiles = walk(SRC)

// 不变量 1：已迁移视图的根节点为页面容器
for (const file of vueFiles) {
  if (!isMigrated(file)) continue
  const text = readFileSync(file, 'utf8')
  const root = templateRoot(text)
  if (root !== 'PageContainer') {
    failures.push({ rule: 1, file, line: 1, message: `根节点应为 PageContainer，实际为 ${root ?? '无法识别'}` })
  }
}

// 不变量 2：已迁移视图不再声明被全局取代的局部样式
for (const file of vueFiles) {
  if (!isMigrated(file)) continue
  const text = readFileSync(file, 'utf8')
  for (const block of styleBlocks(text)) {
    for (const selector of LEGACY_SELECTORS) {
      const matched = new RegExp(`\\${selector}\\s*[{,]`).exec(block.content)
      if (matched) {
        failures.push({ rule: 2, file, line: lineAt(text, block.index + matched.index), message: `仍声明局部样式 ${selector}` })
      }
    }
  }
}

// 不变量 3：除分页条自身外不使用原生分页，已迁移视图必须清零
for (const file of vueFiles) {
  if (rel(file) === 'components/PaginationBar.vue') continue
  const text = readFileSync(file, 'utf8')
  const index = text.indexOf('<el-pagination')
  if (index < 0) continue
  const item = { rule: 3, file, line: lineAt(text, index), message: '直接使用 el-pagination，应改用 PaginationBar' }
  if (isMigrated(file)) failures.push(item)
  else pending.push(item)
}

// 不变量 4：弹窗宽度落在三档之内或属于已登记特例，已迁移视图必须满足
for (const file of vueFiles) {
  const text = readFileSync(file, 'utf8')
  const re = /<el-dialog\b[^>]*>/g
  let matched
  while ((matched = re.exec(text))) {
    const widthMatch = matched[0].match(/\bwidth="([^"]*)"/)
    const line = lineAt(text, matched.index)
    if (!widthMatch) {
      const item = { rule: 4, file, line, message: '弹窗未声明宽度' }
      if (isMigrated(file)) failures.push(item)
      else pending.push(item)
      continue
    }
    const width = widthMatch[1].trim()
    if (ALLOWED_DIALOG_WIDTHS.has(width)) continue
    if (SPECIAL_DIALOG_WIDTHS.has(`${rel(file)}:${width}`)) continue
    const item = { rule: 4, file, line, message: `弹窗宽度 ${width} 不在三档之内且未登记特例` }
    if (isMigrated(file)) failures.push(item)
    else pending.push(item)
  }
}

// 不变量 5：共享展示层不出现裸颜色值
for (const path of SHARED_LAYER_FILES) {
  const file = join(SRC, path)
  const text = readFileSync(file, 'utf8')
  const hexRe = /#[0-9a-fA-F]{3,8}\b/g
  let matched
  while ((matched = hexRe.exec(text))) {
    failures.push({ rule: 5, file, line: lineAt(text, matched.index), message: `裸颜色值 ${matched[0]}，应改用令牌` })
  }
}

// 趋势指标：已迁移视图内的裸颜色值数量，不阻断
for (const file of vueFiles) {
  if (!isMigrated(file)) continue
  const text = readFileSync(file, 'utf8')
  const count = (text.match(/#[0-9a-fA-F]{3,8}\b/g) ?? []).length
  if (count > 0) warnings.push({ rule: 5, file, line: 1, message: `仍有 ${count} 处裸颜色值` })
}

const label = (v) => `  [不变量${v.rule}] ${rel(v.file)}:${v.line}  ${v.message}`

if (pending.length > 0) {
  const byRule = new Map()
  pending.forEach((p) => byRule.set(p.rule, (byRule.get(p.rule) ?? 0) + 1))
  const summary = [...byRule.entries()].map(([rule, count]) => `不变量${rule} ${count} 处`).join('，')
  console.log(`待办 ${pending.length} 处（未迁移页面，不阻断）：${summary}`)
}

if (warnings.length > 0) {
  console.log(`提示 ${warnings.length} 项：`)
  warnings.forEach((w) => console.log(label(w)))
}

if (failures.length > 0) {
  if (pending.length > 0 || warnings.length > 0) console.log('')
  console.log(`违规 ${failures.length} 项：`)
  failures.forEach((f) => console.log(label(f)))
  process.exitCode = 1
} else {
  console.log(`\n通过：${MIGRATED_VIEWS.length} 个已迁移视图，5 条不变量全部满足。`)
}
