<template>
  <div class="iteration-detail" v-if="iteration">
    <!-- 同步提示 -->
    <div v-if="syncNotification" class="sync-notification" @click="applySyncNotification">
      <el-icon><Refresh /></el-icon>
      <span>{{ syncNotification }}</span>
      <el-button text size="small" @click.stop="applySyncNotification">加载最新</el-button>
      <el-button text size="small" @click.stop="syncNotification = ''">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <!-- 顶部标题栏 -->
    <div class="detail-header">
      <div class="header-left">
        <span class="issue-badge">{{ iteration.issueNumber }}</span>
        <span class="project-codes">{{ iteration.projectCode }}</span>
      </div>
      <div class="header-right">
        <span class="select-label">任务状态</span>
        <el-select
          v-model="localIteration.status"
          size="small"
          :style="{ width: '100px' }"
          @change="handleStatusChange"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <span class="select-label">优先级</span>
        <el-select
          v-model="localIteration.priority"
          size="small"
          :style="{ width: '70px' }"
          @change="handlePriorityChange"
        >
          <el-option
            v-for="item in priorityOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-tooltip content="编辑" placement="top">
          <el-button text size="small" @click="$emit('edit', iteration)">
            <el-icon :size="16"><Edit /></el-icon>
          </el-button>
        </el-tooltip>
        <el-tooltip content="删除" placement="top">
          <el-button text size="small" @click="$emit('delete', iteration.id)">
            <el-icon :size="16"><Delete /></el-icon>
          </el-button>
        </el-tooltip>
      </div>
    </div>

    <!-- 需求标题 -->
    <div class="detail-title">{{ iteration.title }}</div>

    <!-- 状态时间信息 -->
    <div class="detail-meta">
      <div class="meta-item" v-if="iteration.statusChangedAt">
        <el-icon :size="12"><Clock /></el-icon>
        <span>状态变更: {{ formatDateTime(iteration.statusChangedAt) }}</span>
      </div>
      <div class="meta-item completed" v-if="iteration.completedAt">
        <el-icon :size="12"><CircleCheck /></el-icon>
        <span>完成时间: {{ formatDateTime(iteration.completedAt) }}</span>
      </div>
      <div class="meta-item" v-if="iteration.localDirPath">
        <el-icon :size="12"><FolderOpened /></el-icon>
        <span class="dir-path clickable" @click="handleOpenFile(iteration.localDirPath)" :title="iteration.localDirPath">{{ iteration.localDirPath }}</span>
      </div>
    </div>

    <!-- Tab 导航 -->
    <div class="detail-tabs">
      <div class="tabs-left">
        <div
          v-for="tab in tabs"
          :key="tab.key"
          class="tab-item"
          :class="{ active: activeTab === tab.key }"
          @click="activeTab = tab.key"
        >
          <el-icon :size="14"><component :is="tab.icon" /></el-icon>
          {{ tab.label }}
          <span v-if="tab.key === 'files' && allFiles.length > 0" class="tab-badge">{{ allFiles.length }}</span>
        </div>
      </div>
      <div class="tabs-right">
        <el-button text size="small" @click="isEditing = !isEditing" v-if="activeTab !== 'files' && activeTab !== 'history'">
          <el-icon :size="14"><Edit v-if="!isEditing" /><View v-else /></el-icon>
          {{ isEditing ? '预览' : '编辑' }}
          <span class="shortcut-hint">Ctrl+E</span>
        </el-button>
      </div>
    </div>

    <!-- 内容区域 -->
    <div class="detail-content">
      <!-- 开发笔记(txt) -->
      <div v-if="activeTab === 'notes'" class="content-section">
        <div v-if="isEditing" class="edit-area">
          <div class="editor-with-lines">
            <div class="line-numbers" v-html="getLineNumbers(localIteration.developmentNotes)"></div>
            <textarea
              ref="notesTextarea"
              v-model="localIteration.developmentNotes"
              class="notes-textarea"
              placeholder="记录后端改动点、接口URL、API文档地址、核心业务逻辑、改动范围和影响等助记信息..."
              @input="triggerAutoSave"
              spellcheck="false"
            ></textarea>
          </div>
          <span class="auto-save-hint" v-if="autoSaveStatus">
            <el-icon><Clock /></el-icon>
            {{ autoSaveStatus }}
          </span>
        </div>
        <div v-else class="preview-area">
          <div class="notes-content-with-lines">
            <div class="line-numbers" v-html="getLineNumbers(localIteration.developmentNotes)"></div>
            <div class="notes-content" v-html="renderedNotesWithLineBreaks" @scroll="handleScroll"></div>
          </div>
          <el-empty v-if="!localIteration.developmentNotes" description="暂无开发笔记" />
        </div>
      </div>

      <!-- 发布文档(md) -->
      <div v-if="activeTab === 'release'" class="content-section">
        <div v-if="isEditing" class="edit-area">
          <div class="editor-with-lines">
            <div class="line-numbers" v-html="getLineNumbers(localIteration.releaseNotes)"></div>
            <textarea
              ref="releaseTextarea"
              v-model="localIteration.releaseNotes"
              class="notes-textarea release-textarea"
              placeholder="输入发布文档内容（支持 Markdown）..."
              @input="triggerAutoSave"
              spellcheck="false"
            ></textarea>
          </div>
          <span class="auto-save-hint" v-if="autoSaveStatus">
            <el-icon><Clock /></el-icon>
            {{ autoSaveStatus }}
          </span>
        </div>
        <div v-else class="preview-area">
          <div class="notes-content-with-lines">
            <div class="line-numbers" v-html="getLineNumbers(localIteration.releaseNotes)"></div>
            <div class="markdown-body" v-html="renderedRelease" @scroll="handleScroll"></div>
          </div>
          <el-empty v-if="!localIteration.releaseNotes" description="暂无发布文档" />
        </div>
      </div>

      <!-- 流程图 -->
      <div v-if="activeTab === 'flowchart'" class="content-section">
        <div v-if="isEditing" class="edit-area">
          <el-form-item label="流程图路径" class="flowchart-form-item">
            <el-input
              v-model="localIteration.flowchartPath"
              placeholder="Excalidraw 文件绝对路径"
              @input="triggerAutoSave"
            >
              <template #append>
                <el-button type="text" @click="copyPath(localIteration.flowchartPath)">
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </template>
            </el-input>
          </el-form-item>
        </div>
        <div v-else class="preview-area">
          <div v-if="localIteration.flowchartPath" class="flowchart-info">
            <el-icon :size="24"><Link /></el-icon>
            <span class="flowchart-path">{{ localIteration.flowchartPath }}</span>
            <el-button type="primary" link size="small" @click="copyPath(localIteration.flowchartPath)">
              复制路径
            </el-button>
          </div>
          <el-empty v-else description="暂无流程图" />
        </div>
      </div>

      <!-- 全部文件 -->
      <div v-if="activeTab === 'files'" class="content-section">
        <div v-if="allFiles.length > 0" class="file-list">
          <div v-for="(file, index) in allFiles" :key="index" class="file-item">
            <div class="file-icon">
              <el-icon :size="20" :color="getFileColor(file.name)">
                <component :is="getFileIcon(file.name)" />
              </el-icon>
            </div>
            <div class="file-info">
              <div class="file-name">{{ file.name }}</div>
              <div class="file-path">{{ file.path }}</div>
            </div>
            <div class="file-actions">
              <el-button text size="small" @click="copyToClipboard(file.name, '文件名')">
                <el-icon><CopyDocument /></el-icon>
              </el-button>
              <el-button text size="small" @click="copyToClipboard(file.path, '路径')">
                <el-icon><Location /></el-icon>
              </el-button>
              <el-button text size="small" @click="handleOpenFile(file.path)">
                <el-icon><FolderOpened /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无文件" />
      </div>

      <!-- 同步历史 -->
      <div v-if="activeTab === 'history'" class="content-section">
        <div v-if="syncHistory.length > 0" class="history-list">
          <div v-for="record in syncHistory" :key="record.id" class="history-item">
            <div class="history-header">
              <el-tag :type="record.syncType === 'PAGE_TO_LOCAL' ? 'success' : 'primary'" size="small">
                {{ record.syncType === 'PAGE_TO_LOCAL' ? '页面→本地' : '本地→页面' }}
              </el-tag>
              <span class="history-field">{{ getFieldLabel(record.fieldName) }}</span>
              <span class="history-time">{{ formatDateTime(record.syncedAt) }}</span>
            </div>
            <div class="history-content">
              <div class="history-diff">
                <div class="diff-old">
                  <div class="diff-label">旧值</div>
                  <div class="diff-body">
                    <template v-if="record.oldValue">
                      <div v-for="(line, li) in getDiffLines(record.oldValue, record.newValue, 'old')" :key="'o'+li"
                        :class="['diff-line', line.type]">
                        <span class="diff-ln">{{ line.lineNum }}</span>
                        <span class="diff-prefix">{{ line.type === 'removed' ? '-' : ' ' }}</span>
                        <span class="diff-text">{{ line.text || '\u00A0' }}</span>
                      </div>
                    </template>
                    <span v-else class="diff-empty">（空）</span>
                  </div>
                </div>
                <div class="diff-new">
                  <div class="diff-label">新值</div>
                  <div class="diff-body">
                    <template v-if="record.newValue">
                      <div v-for="(line, li) in getDiffLines(record.oldValue, record.newValue, 'new')" :key="'n'+li"
                        :class="['diff-line', line.type]">
                        <span class="diff-ln">{{ line.lineNum }}</span>
                        <span class="diff-prefix">{{ line.type === 'added' ? '+' : ' ' }}</span>
                        <span class="diff-text">{{ line.text || '\u00A0' }}</span>
                      </div>
                    </template>
                    <span v-else class="diff-empty">（空）</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂无同步历史" />
      </div>
    </div>
  </div>

  <!-- 空状态 -->
  <div class="detail-empty" v-else>
    <el-empty description="选择左侧需求查看详情">
      <template #image>
        <el-icon :size="64" color="#c0c4cc"><Document /></el-icon>
      </template>
    </el-empty>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Edit, Delete, FolderOpened, Clock, CopyDocument, Link, View, Document, CircleCheck, Location,
  Notebook, Picture, Refresh, Close
} from '@element-plus/icons-vue'
import type { Iteration, IterationSyncHistory, OtherFile } from '@/types/iteration'
import { IterationStatusMap, IterationPriorityMap } from '@/types/iteration'
import { getSyncHistory } from '@/api/iteration'
import { readFile, checkFileUpdate, openFile } from '@/api/file'
import markdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import 'highlight.js/styles/github.css'

// 注册常用语言
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import sql from 'highlight.js/lib/languages/sql'
import xml from 'highlight.js/lib/languages/xml'
import css from 'highlight.js/lib/languages/css'
import json from 'highlight.js/lib/languages/json'
import bash from 'highlight.js/lib/languages/bash'
import yaml from 'highlight.js/lib/languages/yaml'

hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('java', java)
hljs.registerLanguage('python', python)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('css', css)
hljs.registerLanguage('json', json)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('yaml', yaml)

const props = defineProps<{
  iteration: Iteration | null
}>()

const emit = defineEmits<{
  edit: [iteration: Iteration]
  delete: [id: number]
  'update-status': [id: number, status: string]
  'update-priority': [id: number, priority: string]
  'auto-save': [data: Partial<Iteration>]
}>()

const activeTab = ref('notes')
const isEditing = ref(false)
const autoSaveStatus = ref('')
const syncHistory = ref<IterationSyncHistory[]>([])
const syncNotification = ref('')

const notesFilePath = ref('')
const releaseFilePath = ref('')
const notesLastModified = ref(0)
const releaseLastModified = ref(0)

// 性能优化：请求取消 + 切换标记
let abortController: AbortController | null = null
let currentIterationId: number | null = null
let switchDebounceTimer: ReturnType<typeof setTimeout> | null = null

const localIteration = reactive({
  developmentNotes: '',
  releaseNotes: '',
  flowchartPath: '',
  status: '',
  priority: ''
})

const tabs = [
  { key: 'notes', label: '开发笔记(txt)', icon: Edit },
  { key: 'release', label: '发布文档(md)', icon: Document },
  { key: 'flowchart', label: '流程图', icon: Link },
  { key: 'files', label: '全部文件', icon: FolderOpened },
  { key: 'history', label: '同步历史', icon: Clock }
]

const statusOptions = Object.values(IterationStatusMap).map(item => ({
  value: item.value,
  label: item.label
}))

const getStatusColor = (status: string) => {
  const map: Record<string, string> = {
    TODO: '#909399',
    IN_PROGRESS: '#409EFF',
    TESTING: '#E6A23C',
    DONE: '#67C23A'
  }
  return map[status] || '#909399'
}

const getStatusLabel = (status: string) => {
  return IterationStatusMap[status as keyof typeof IterationStatusMap]?.label || status || '未设置'
}

const getPriorityColor = (priority: string) => {
  const map: Record<string, string> = {
    HIGH: '#F56C6C',
    MEDIUM: '#E6A23C',
    LOW: '#67C23A'
  }
  return map[priority] || '#909399'
}

const getPriorityLabel = (priority: string) => {
  return IterationPriorityMap[priority as keyof typeof IterationPriorityMap]?.label || priority || '未设置'
}

const priorityOptions = Object.values(IterationPriorityMap).map(item => ({
  value: item.value,
  label: item.label
}))

const allFiles = computed<OtherFile[]>(() => {
  if (!props.iteration?.impactScope) return []
  return props.iteration.impactScope
    .split('\n')
    .filter(line => line.includes('|'))
    .map(line => {
      const [name, path] = line.split('|', 2)
      return { name: name.trim(), path: path.trim() }
    })
})

const md = markdownIt({
  html: true,
  linkify: true,
  typographer: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
               hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
               '</code></pre>'
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

const renderedNotes = computed(() => {
  if (!localIteration.developmentNotes) return ''
  return md.render(localIteration.developmentNotes)
})

// 单个字符是否为中文/中文标点/空白
const isCJKOrSpace = (ch: string): boolean => {
  const code = ch.charCodeAt(0)
  return (code >= 0x4E00 && code <= 0x9FFF)   // CJK统一汉字
    || (code >= 0x3000 && code <= 0x303F)      // CJK标点
    || (code >= 0xFF00 && code <= 0xFFEF)      // 全角符号
    || (code >= 0x2000 && code <= 0x206F)      // 通用标点
    || ch === ' ' || ch === '\t'
}

// HTML 转义单个字符
const escHtml = (ch: string): string =>
  ch === '&' ? '&amp;' : ch === '<' ? '&lt;' : ch === '>' ? '&gt;' : ch

// 扫描一段纯文本，输出 HTML（转义 + 英文/数字着色）
// 生成行号 HTML
const getLineNumbers = (text: string | null): string => {
  if (!text) return '<div class="ln">1</div>'
  const lines = text.split('\n')
  return lines.map((_, i) => `<div class="ln">${i + 1}</div>`).join('')
}

const scanPlain = (text: string, start: number, end: number): string => {
  let html = ''
  let i = start
  while (i < end) {
    const ch = text[i]
    if (ch === '&' || ch === '<' || ch === '>') {
      html += escHtml(ch); i++
    } else if (/[0-9]/.test(ch)) {
      let num = ''
      while (i < end && /[0-9]/.test(text[i])) { num += text[i]; i++ }
      html += `<span class="num-highlight">${num}</span>`
    } else if (!isCJKOrSpace(ch)) {
      let en = ''
      while (i < end && !isCJKOrSpace(text[i]) && !/[0-9]/.test(text[i])
        && text[i] !== '&' && text[i] !== '<' && text[i] !== '>') {
        en += text[i]; i++
      }
      html += `<span class="en-highlight">${en}</span>`
    } else {
      html += ch; i++
    }
  }
  return html
}

// 开发笔记需要保留换行符显示
const renderedNotesWithLineBreaks = computed(() => {
  if (!localIteration.developmentNotes) return ''
  const text = localIteration.developmentNotes
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
  // 逐行做单次遍历扫描，输出 HTML
  return text.split('\n').map(line => {
    let html = ''
    let i = 0
    const len = line.length
    while (i < len) {
      // ""包裹 → 红色（最高优先级，内部不做其他着色）
      if (line[i] === '\u201C' || line[i] === '"') {
        const q = line[i]
        const closeChar = q === '\u201C' ? '\u201D' : '"'
        const close = line.indexOf(closeChar, i + 1)
        if (close !== -1) {
          const inner = line.substring(i + 1, close)
          // 内容只做 HTML 转义，不做英文/数字着色
          let innerHtml = ''
          for (let j = 0; j < inner.length; j++) {
            innerHtml += escHtml(inner[j])
          }
          html += `<span class="quote-highlight">${q}${innerHtml}${closeChar}</span>`
          i = close + 1
          continue
        }
      }
      // 【】包裹 → 加粗加大橙色
      if (line[i] === '【') {
        const close = line.indexOf('】', i + 1)
        if (close !== -1) {
          const inner = line.substring(i + 1, close)
          html += `<span class="bracket-highlight">【${scanPlain(inner, 0, inner.length)}】</span>`
          i = close + 1
          continue
        }
      }
      // "待办" 关键词
      if (line.substring(i, i + 2) === '待办') {
        html += '<span class="todo-highlight">待办</span>'
        i += 2
        continue
      }
      // 普通字符：转义 + 英文/数字着色（逐段扫描到下一个特殊字符）
      const segStart = i
      while (i < len && line[i] !== '\u201C' && line[i] !== '"'
        && line[i] !== '【' && line.substring(i, i + 2) !== '待办') { i++ }
      html += scanPlain(line, segStart, i)
    }
    return html
  }).join('<br>')
})

const renderedRelease = computed(() => {
  if (!localIteration.releaseNotes) return ''
  return md.render(localIteration.releaseNotes)
})

watch(
  () => props.iteration,
  async (newVal) => {
    if (!newVal) return

    const iterationId = newVal.id
    const isSameIteration = currentIterationId === iterationId

    // 同一迭代的数据更新（auto-save 后端返回）：只更新标量字段，不重新加载文件
    if (isSameIteration) {
      localIteration.flowchartPath = newVal.flowchartPath || ''
      localIteration.status = newVal.status
      localIteration.priority = newVal.priority
      return
    }

    // 切换到不同迭代：完整加载流程
    // 取消上一次未完成的请求
    if (abortController) {
      abortController.abort()
    }
    // 清除上一次的防抖定时器
    if (switchDebounceTimer) {
      clearTimeout(switchDebounceTimer)
    }

    // 防抖：快速切换时只处理最后一次（100ms）
    await new Promise<void>(resolve => {
      switchDebounceTimer = setTimeout(() => {
        switchDebounceTimer = null
        resolve()
      }, 100)
    })

    // 防抖后如果又切换了，放弃本次
    if (iterationId !== props.iteration?.id) return

    // 创建新的 AbortController
    abortController = new AbortController()
    currentIterationId = iterationId

    localIteration.flowchartPath = newVal.flowchartPath || ''
    localIteration.status = newVal.status
    localIteration.priority = newVal.priority

    // 自动关联逻辑：当 localDirPath 不为空时，自动设置文件路径
    if (newVal.localDirPath && newVal.localDirPath.trim()) {
      notesFilePath.value = newVal.localDirPath + '/' + newVal.issueNumber + '.txt'
      releaseFilePath.value = newVal.localDirPath + '/' + newVal.issueNumber + '.md'
      // 编辑模式下不从文件加载，避免覆盖用户正在编辑的内容
      if (!isEditing.value) {
        await loadFileContent('notes')
        await loadFileContent('release')
      }
    } else {
      notesFilePath.value = ''
      releaseFilePath.value = ''
      localIteration.developmentNotes = newVal.developmentNotes || ''
      localIteration.releaseNotes = newVal.releaseNotes || ''
    }

    // 如果已切换到其他迭代，停止后续操作
    if (currentIterationId !== iterationId) return

    if (currentIterationId !== iterationId) return
    loadSyncHistory()
  },
  { immediate: true }
)

const notesTextarea = ref<HTMLTextAreaElement | null>(null)
const releaseTextarea = ref<HTMLTextAreaElement | null>(null)

const syncLineNumbers = (textarea: HTMLTextAreaElement) => {
  const lineNumbers = textarea.parentElement?.querySelector('.line-numbers') as HTMLElement
  if (lineNumbers) {
    lineNumbers.scrollTop = textarea.scrollTop
  }
}

const onNotesScroll = () => {
  if (notesTextarea.value) syncLineNumbers(notesTextarea.value)
}
const onReleaseScroll = () => {
  if (releaseTextarea.value) syncLineNumbers(releaseTextarea.value)
}

let debounceTimer: ReturnType<typeof setTimeout> | null = null
let fileCheckInterval: ReturnType<typeof setInterval> | null = null

const triggerAutoSave = () => {
  if (debounceTimer) clearTimeout(debounceTimer)
  autoSaveStatus.value = '输入中...'
  debounceTimer = setTimeout(() => {
    performAutoSave()
  }, 2000)
}

const performAutoSave = async () => {
  if (!props.iteration?.id) return
  autoSaveStatus.value = '保存中...'

  try {
    // 发送内容给后端，由后端统一处理文件写入和同步历史
    emit('auto-save', {
      id: props.iteration.id,
      developmentNotes: localIteration.developmentNotes,
      releaseNotes: localIteration.releaseNotes,
      flowchartPath: localIteration.flowchartPath
    } as Partial<Iteration>)

    autoSaveStatus.value = '已保存'
  } catch {
    autoSaveStatus.value = '保存失败'
  }
  setTimeout(() => {
    autoSaveStatus.value = ''
  }, 3000)
}

const loadFileContent = async (type: 'notes' | 'release') => {
  const filePath = type === 'notes' ? notesFilePath.value : releaseFilePath.value
  if (!filePath) return

  try {
    const response = await readFile(filePath)
    if (response.data) {
      // 统一换行符为 \n
      let content = response.data.content || ''
      content = content.replace(/\r\n/g, '\n').replace(/\r/g, '\n')

      if (type === 'notes') {
        localIteration.developmentNotes = content
        notesLastModified.value = Date.now()
      } else {
        localIteration.releaseNotes = content
        releaseLastModified.value = Date.now()
      }
    }
  } catch (error) {
    console.error('读取文件失败:', error)
  }
}

const checkFileUpdates = async () => {
  // 只有在非编辑状态下才检查文件更新，避免干扰用户编辑
  if (isEditing.value) return
  // 如果已切换到其他迭代，跳过检查
  if (props.iteration?.id !== currentIterationId) return
  // 没有关联文件则跳过
  if (!notesFilePath.value && !releaseFilePath.value) return

  let hasUpdate = false

  if (notesFilePath.value) {
    try {
      const response = await checkFileUpdate(notesFilePath.value, notesLastModified.value)
      if (response.data?.updated) {
        syncNotification.value = '本地开发笔记(txt)文件已更新，点击加载最新内容'
        hasUpdate = true
      }
    } catch (error) {
      console.error('检查文件更新失败:', error)
    }
  }

  if (releaseFilePath.value) {
    try {
      const response = await checkFileUpdate(releaseFilePath.value, releaseLastModified.value)
      if (response.data?.updated) {
        syncNotification.value = '本地发布文档(md)文件已更新，点击加载最新内容'
        hasUpdate = true
      }
    } catch (error) {
      console.error('检查文件更新失败:', error)
    }
  }
}

const applySyncNotification = async () => {
  if (notesFilePath.value) {
    await loadFileContent('notes')
  }
  if (releaseFilePath.value) {
    await loadFileContent('release')
  }
  syncNotification.value = ''
  // 发送内容给后端，由后端统一处理文件写入和同步历史
  if (props.iteration?.id) {
    emit('auto-save', {
      id: props.iteration.id,
      developmentNotes: localIteration.developmentNotes,
      releaseNotes: localIteration.releaseNotes,
      flowchartPath: localIteration.flowchartPath
    } as Partial<Iteration>)
  }
  ElMessage.success('已加载最新内容')
}

const handleStatusChange = (val: string) => {
  if (props.iteration?.id) {
    emit('update-status', props.iteration.id, val)
  }
}

const handlePriorityChange = (val: string) => {
  if (props.iteration?.id) {
    emit('update-priority', props.iteration.id, val)
  }
}

const copyPath = async (path: string) => {
  if (!path) {
    ElMessage.warning('路径为空')
    return
  }
  try {
    await navigator.clipboard.writeText(path)
    ElMessage.success('路径已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败')
  }
}

const copyToClipboard = async (text: string, label: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已复制${label}`)
  } catch {
    ElMessage.error('复制失败')
  }
}

const handleOpenFile = async (path: string) => {
  try {
    await openFile(path)
  } catch (error: any) {
    ElMessage.error(error.message || '打开失败')
  }
}

const formatDateTime = (dateStr: string | null) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 逐行 diff：返回指定侧的差异行（含行号）
type DiffLine = { text: string; type: 'same' | 'added' | 'removed'; lineNum: number }
const getDiffLines = (oldVal: string | null, newVal: string | null, side: 'old' | 'new'): DiffLine[] => {
  const oldLines = (oldVal || '').split('\n')
  const newLines = (newVal || '').split('\n')
  const result: DiffLine[] = []
  let oi = 0, ni = 0
  while (oi < oldLines.length || ni < newLines.length) {
    const o = oi < oldLines.length ? oldLines[oi] : undefined
    const n = ni < newLines.length ? newLines[ni] : undefined
    if (o === n) {
      if (o !== undefined) result.push({ text: o, type: 'same', lineNum: oi + 1 })
      oi++; ni++
    } else if (o !== undefined && newLines.indexOf(o, ni) === -1) {
      result.push({ text: o, type: 'removed', lineNum: oi + 1 })
      oi++
    } else if (n !== undefined && oldLines.indexOf(n, oi) === -1) {
      result.push({ text: n, type: 'added', lineNum: ni + 1 })
      ni++
    } else {
      if (o !== undefined) result.push({ text: o, type: 'removed', lineNum: oi + 1 })
      if (n !== undefined) result.push({ text: n, type: 'added', lineNum: ni + 1 })
      oi++; ni++
    }
  }
  // 只返回有差异的行（去掉 same），并保留行号上下文
  const diffIndices = new Set<number>()
  result.forEach((l, i) => { if (l.type !== 'same') { diffIndices.add(i); diffIndices.add(i - 1); diffIndices.add(i + 1) } })
  return result.filter((l, i) => diffIndices.has(i) && l.type !== 'same')
}

const getFieldLabel = (fieldName: string) => {
  const labels: Record<string, string> = {
    developmentNotes: '开发笔记(txt)',
    releaseNotes: '发布文档(md)',
    flowchart: '流程图',
    title: '标题',
    flowchartPath: '流程图路径'
  }
  return labels[fieldName] || fieldName
}

const truncateText = (text: string, maxLength: number) => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

const loadSyncHistory = async () => {
  if (!props.iteration?.id) return
  const id = props.iteration.id
  try {
    const response = await getSyncHistory(id)
    // 如果已切换到其他迭代，丢弃结果
    if (currentIterationId !== id) return
    syncHistory.value = response.data || []
  } catch (error) {
    console.error('加载同步历史失败:', error)
  }
}

const getFileIcon = (fileName: string) => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  if (ext === 'md') return Document
  if (ext === 'txt') return Notebook
  if (['jpg', 'jpeg', 'png', 'gif', 'svg', 'bmp'].includes(ext || '')) return Picture
  return Document
}

const getFileColor = (fileName: string) => {
  const ext = fileName.split('.').pop()?.toLowerCase()
  if (ext === 'md') return '#409eff'
  if (ext === 'txt') return '#67c23a'
  if (['jpg', 'jpeg', 'png', 'gif', 'svg', 'bmp'].includes(ext || '')) return '#e6a23c'
  return '#909399'
}

// Ctrl+E 快捷键切换编辑/预览模式
const handleKeydown = (e: KeyboardEvent) => {
  if (e.ctrlKey && e.key === 'e') {
    e.preventDefault()
    if (activeTab.value !== 'files' && activeTab.value !== 'history') {
      isEditing.value = !isEditing.value
    }
  }
}

onMounted(() => {
  if (props.iteration?.id) {
    loadSyncHistory()
  }
  // 每10秒检查文件更新
  fileCheckInterval = setInterval(checkFileUpdates, 10000)
  // 监听快捷键
  document.addEventListener('keydown', handleKeydown)
  // 行号跟随滚动
  nextTick(() => {
    notesTextarea.value?.addEventListener('scroll', onNotesScroll)
    releaseTextarea.value?.addEventListener('scroll', onReleaseScroll)
  })
})

watch(isEditing, () => {
  nextTick(() => {
    notesTextarea.value?.addEventListener('scroll', onNotesScroll)
    releaseTextarea.value?.addEventListener('scroll', onReleaseScroll)
  })
})

onUnmounted(() => {
  if (fileCheckInterval) {
    clearInterval(fileCheckInterval)
  }
  if (switchDebounceTimer) {
    clearTimeout(switchDebounceTimer)
  }
  if (abortController) {
    abortController.abort()
  }
  // 移除快捷键监听
  document.removeEventListener('keydown', handleKeydown)
  // 移除滚动监听
  notesTextarea.value?.removeEventListener('scroll', onNotesScroll)
  releaseTextarea.value?.removeEventListener('scroll', onReleaseScroll)
})
</script>

<style scoped>
.iteration-detail {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 0;
}

.sync-notification {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: #ecf5ff;
  border-bottom: 1px solid #b3d8ff;
  color: #409eff;
  font-size: 13px;
  cursor: pointer;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}

.sync-notification:hover {
  background: #d9ecff;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px 8px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.issue-badge {
  font-size: 18px;
  font-weight: 700;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #b3d8ff;
  border-radius: 6px;
  padding: 2px 10px;
  letter-spacing: 0.5px;
}

.project-codes {
  font-size: 13px;
  color: #86909c;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.select-label {
  font-size: 12px;
  color: #606266;
  margin-right: -8px;
}

.detail-title {
  padding: 8px 24px 0;
  font-size: 18px;
  font-weight: 600;
  color: #1d2129;
  line-height: 1.4;
}

.detail-meta {
  padding: 6px 24px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: #86909c;
}

.meta-item.completed {
  color: #67c23a;
}

.dir-path {
  word-break: break-all;
}

.dir-path.clickable {
  color: #409eff;
  cursor: pointer;
  text-decoration: none;
}

.dir-path.clickable:hover {
  text-decoration: underline;
}

.detail-tabs {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px 0;
  border-bottom: 1px solid #f0f0f0;
}

.tabs-left {
  display: flex;
  gap: 4px;
}

.tabs-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.tab-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #4e5969;
  cursor: pointer;
  border-bottom: 2px solid transparent;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
  position: relative;
}

.tab-item:hover {
  color: #409eff;
}

.tab-item.active {
  color: #409eff;
  border-bottom-color: #409eff;
  font-weight: 500;
}

.tab-badge {
  background: #f56c6c;
  color: white;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.shortcut-hint {
  font-size: 11px;
  color: #909399;
  background: #f5f7fa;
  padding: 1px 4px;
  border-radius: 3px;
  margin-left: 4px;
}

.detail-content {
  flex: 1;
  overflow: hidden;
  padding: 0 24px 12px;
  min-height: 0;
}

.content-section {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.edit-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  position: relative;
  min-height: 0;
}

.editor-with-lines {
  flex: 1;
  display: flex;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  min-height: 0;
  overflow: hidden;
}

.line-numbers {
  flex-shrink: 0;
  width: 32px;
  padding: 16px 4px 16px 2px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #c0c4cc;
  text-align: right;
  user-select: none;
  overflow: hidden;
}

.line-numbers :deep(.ln) {
  height: 19.6px;
}

.notes-textarea {
  width: 100%;
  flex: 1;
  min-height: 0;
  padding: 16px;
  border: none;
  border-radius: 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.4;
  color: #2b2b2b;
  resize: none;
  outline: none;
  overflow-y: auto;
}

.editor-with-lines .notes-textarea {
  border: none;
}

.notes-textarea:focus {
  border-color: transparent;
}

.notes-content-with-lines {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.notes-content-with-lines .line-numbers {
  padding-top: 0;
}

.notes-content-with-lines .notes-content,
.notes-content-with-lines .markdown-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.release-textarea {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.auto-save-hint {
  position: absolute;
  bottom: 8px;
  right: 8px;
  font-size: 12px;
  color: #67c23a;
  display: flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.9);
  padding: 4px 8px;
  border-radius: 4px;
}

.preview-area {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
}

/* 开发笔记内容样式 */
.notes-content {
  font-size: 13px;
  line-height: 1.3;
  color: #2b2b2b;
  word-break: break-word;
  white-space: pre-wrap;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
}

.notes-content :deep(br) {
  display: block;
  content: '';
  margin-top: 4px;
}

.notes-content :deep(.todo-highlight) {
  color: #e6a23c;
  font-weight: 600;
  background-color: #fdf6ec;
  padding: 1px 4px;
  border-radius: 3px;
}

.notes-content :deep(.en-highlight) {
  color: #409eff;
  font-weight: 700;
}

.notes-content :deep(.num-highlight) {
  color: #9b59b6;
  font-weight: 700;
}

.notes-content :deep(.bracket-highlight) {
  font-weight: 700;
  font-size: 1.15em;
  color: #e6a23c;
  background-color: #fdf6ec;
  padding: 1px 4px;
  border-radius: 3px;
}

.notes-content :deep(.quote-highlight) {
  color: #f56c6c;
  font-weight: 600;
}

/* Markdown 样式美化 */
.markdown-body {
  font-size: 14px;
  line-height: 1.2;
  color: #1d2129;
  word-break: break-word;
}

.markdown-body :deep(h1) {
  font-size: 24px;
  font-weight: 700;
  margin: 24px 0 16px;
  padding-bottom: 8px;
  border-bottom: 2px solid #e4e7ed;
  color: #1d2129;
}

.markdown-body :deep(h2) {
  font-size: 20px;
  font-weight: 600;
  margin: 20px 0 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid #f0f0f0;
  color: #1d2129;
}

.markdown-body :deep(h3) {
  font-size: 16px;
  font-weight: 600;
  margin: 16px 0 10px;
  color: #1d2129;
}

.markdown-body :deep(p) {
  margin: 10px 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 24px;
  margin: 10px 0;
}

.markdown-body :deep(li) {
  margin: 6px 0;
}

.markdown-body :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 13px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  color: #c7254e;
  border: 1px solid #e8e8e8;
}

.markdown-body :deep(pre) {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 16px;
  overflow-x: auto;
  margin: 16px 0;
  border: 1px solid #e9ecef;
}

.markdown-body :deep(pre code) {
  background: none;
  padding: 0;
  color: #383a42;
  border: none;
  font-size: 13px;
  line-height: 1.6;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

/* Highlight.js 浅色主题样式 */
.markdown-body :deep(.hljs) {
  background: #f8f9fa;
  color: #383a42;
}

.markdown-body :deep(.hljs-keyword),
.markdown-body :deep(.hljs-selector-tag),
.markdown-body :deep(.hljs-literal),
.markdown-body :deep(.hljs-section),
.markdown-body :deep(.hljs-link) {
  color: #a626a4;
}

.markdown-body :deep(.hljs-function .hljs-keyword) {
  color: #a626a4;
}

.markdown-body :deep(.hljs-string),
.markdown-body :deep(.hljs-title),
.markdown-body :deep(.hljs-name),
.markdown-body :deep(.hljs-type),
.markdown-body :deep(.hljs-attribute),
.markdown-body :deep(.hljs-symbol),
.markdown-body :deep(.hljs-bullet),
.markdown-body :deep(.hljs-addition),
.markdown-body :deep(.hljs-variable),
.markdown-body :deep(.hljs-template-tag),
.markdown-body :deep(.hljs-template-variable) {
  color: #50a14f;
}

.markdown-body :deep(.hljs-comment),
.markdown-body :deep(.hljs-quote),
.markdown-body :deep(.hljs-deletion),
.markdown-body :deep(.hljs-meta) {
  color: #a0a1a7;
}

.markdown-body :deep(.hljs-number),
.markdown-body :deep(.hljs-regexp),
.markdown-body :deep(.hljs-literal),
.markdown-body :deep(.hljs-params) {
  color: #986801;
}

.markdown-body :deep(.hljs-built_in) {
  color: #e45649;
}

.markdown-body :deep(.hljs-emphasis) {
  font-style: italic;
}

.markdown-body :deep(.hljs-strong) {
  font-weight: bold;
}

.markdown-body :deep(blockquote) {
  border-left: 4px solid #409eff;
  padding: 12px 16px;
  margin: 16px 0;
  color: #4e5969;
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9ff 100%);
  border-radius: 0 8px 8px 0;
}

.markdown-body :deep(a) {
  color: #409eff;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: all 0.2s;
}

.markdown-body :deep(a:hover) {
  color: #337ecc;
  border-bottom-color: #337ecc;
}

.markdown-body :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.markdown-body :deep(th) {
  background: linear-gradient(135deg, #f5f7fa 0%, #f0f2f5 100%);
  padding: 10px 14px;
  text-align: left;
  font-weight: 600;
  color: #1d2129;
  border-bottom: 2px solid #e4e7ed;
}

.markdown-body :deep(td) {
  padding: 10px 14px;
  border-bottom: 1px solid #f0f0f0;
}

.markdown-body :deep(tr:hover td) {
  background: #f5f7fa;
}

.markdown-body :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin: 8px 0;
}

.flowchart-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  background: #f7f8fa;
  border-radius: 8px;
}

.flowchart-path {
  font-size: 14px;
  color: #4e5969;
  word-break: break-all;
  flex: 1;
}

.flowchart-form-item {
  margin-bottom: 0;
}

/* 文件列表样式 */
.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow-y: auto;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f7f8fa;
  border-radius: 8px;
  transition: all 0.2s;
}

.file-item:hover {
  background: #ecf5ff;
}

.file-icon {
  flex-shrink: 0;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  font-weight: 500;
  color: #1d2129;
  margin-bottom: 4px;
}

.file-path {
  font-size: 12px;
  color: #86909c;
  word-break: break-all;
}

.file-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

/* 同步历史样式 */
.history-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.history-item {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
}

.history-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #f7f8fa;
  border-bottom: 1px solid #e4e7ed;
}

.history-field {
  font-weight: 500;
  color: #1d2129;
}

.history-time {
  margin-left: auto;
  font-size: 12px;
  color: #86909c;
}

.history-content {
  padding: 12px;
}

.history-diff {
  display: flex;
  gap: 1px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  overflow: hidden;
  font-size: 12px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
}

.diff-old,
.diff-new {
  flex: 1;
  min-width: 0;
  background: #fff;
}

.diff-label {
  font-size: 11px;
  color: #86909c;
  padding: 4px 8px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.diff-body {
  max-height: 200px;
  overflow-y: auto;
}

.diff-line {
  display: flex;
  padding: 0 8px;
  line-height: 20px;
  white-space: pre-wrap;
  word-break: break-all;
}

.diff-line.added {
  background: #e6ffec;
}

.diff-line.removed {
  background: #ffebe9;
}

.diff-line.same {
  color: #86909c;
}

.diff-ln {
  width: 32px;
  flex-shrink: 0;
  text-align: right;
  padding-right: 8px;
  color: #86909c;
  font-size: 11px;
  user-select: none;
}

.diff-prefix {
  width: 16px;
  flex-shrink: 0;
  text-align: center;
  color: #86909c;
  user-select: none;
}

.diff-line.added .diff-prefix {
  color: #1a7f37;
}

.diff-line.removed .diff-prefix {
  color: #cf222e;
}

.diff-text {
  flex: 1;
  min-width: 0;
}

.diff-empty {
  display: block;
  padding: 8px;
  color: #c0c4cc;
  font-style: italic;
}

.detail-empty {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
