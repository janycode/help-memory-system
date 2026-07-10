<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { Edit, Document, List, Link, Minus, Paperclip, More } from '@element-plus/icons-vue'
import { ElButton, ElTooltip } from 'element-plus'
import markdownIt from 'markdown-it'
import hljs from 'highlight.js'

const props = withDefaults(defineProps<{
  modelValue: string
  height?: string
  showToolbar?: boolean
}>(), {
  height: '400px',
  showToolbar: true
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  autoSave: [data: { success: boolean; message: string }]
}>()

const isPreviewMode = ref(false)
const content = ref(props.modelValue)
const isSaving = ref(false)
const saveStatus = ref('')
let debounceTimer: ReturnType<typeof setTimeout> | null = null

const md = markdownIt({
  highlight: (str, lang) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(str, { language: lang }).value
      } catch (__) {}
    }
    return hljs.highlightAuto(str).value
  }
})

const renderedContent = computed(() => {
  return md.render(content.value)
})

const handleInput = (event: Event) => {
  const target = event.target as HTMLTextAreaElement
  content.value = target.value
  emit('update:modelValue', content.value)
  triggerAutoSave()
}

const triggerAutoSave = () => {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    performAutoSave()
  }, 3000)
}

const performAutoSave = async () => {
  isSaving.value = true
  saveStatus.value = '保存中...'
  try {
    emit('autoSave', { success: true, message: '已保存' })
    saveStatus.value = '已保存'
  } catch {
    emit('autoSave', { success: false, message: '保存失败' })
    saveStatus.value = '保存失败'
  } finally {
    isSaving.value = false
    setTimeout(() => {
      saveStatus.value = ''
    }, 3000)
  }
}

const insertMarkdown = (prefix: string, suffix: string = '') => {
  const textarea = document.querySelector('.markdown-textarea') as HTMLTextAreaElement
  if (!textarea) return
  
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = content.value.substring(start, end)
  const before = content.value.substring(0, start)
  const after = content.value.substring(end)
  
  const newText = before + prefix + selectedText + suffix + after
  content.value = newText
  emit('update:modelValue', content.value)
  
  setTimeout(() => {
    textarea.focus()
    const newCursorPos = start + prefix.length + selectedText.length
    textarea.setSelectionRange(newCursorPos, newCursorPos)
  }, 0)
  
  triggerAutoSave()
}

const toolbarButtons = [
  { icon: Document, action: () => insertMarkdown('# '), tooltip: '标题' },
  { icon: List, action: () => insertMarkdown('- '), tooltip: '列表' },
  { icon: Link, action: () => insertMarkdown('[', '](url)'), tooltip: '链接' },
  { icon: Minus, action: () => insertMarkdown('\n---\n'), tooltip: '分隔线' }
]

watch(() => props.modelValue, (newVal) => {
  content.value = newVal
})

onMounted(() => {
  content.value = props.modelValue
})
</script>

<template>
  <div class="markdown-editor-container">
    <div class="editor-header">
      <div class="toolbar" v-if="showToolbar">
        <ElButton
          v-for="btn in toolbarButtons"
          :key="btn.tooltip"
          type="text"
          size="small"
          @click="btn.action"
        >
          <component :is="btn.icon" />
        </ElButton>
      </div>
      <div class="mode-switch">
        <span class="save-status" :class="{ saving: isSaving }">{{ saveStatus }}</span>
        <ElButton
          type="text"
          size="small"
          @click="isPreviewMode = !isPreviewMode"
        >
          <Edit v-if="isPreviewMode" />
          <Document v-else />
          {{ isPreviewMode ? '编辑' : '预览' }}
        </ElButton>
      </div>
    </div>
    <div class="editor-body" :style="{ height: height }">
      <textarea
        class="markdown-textarea"
        :value="content"
        @input="handleInput"
        placeholder="请输入Markdown内容..."
        v-show="!isPreviewMode"
      ></textarea>
      <div
        class="markdown-preview"
        v-show="isPreviewMode"
        v-html="renderedContent"
      ></div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.markdown-editor-container {
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.editor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
}

.toolbar {
  display: flex;
  gap: 4px;
}

.mode-switch {
  display: flex;
  align-items: center;
  gap: 12px;
}

.save-status {
  font-size: 12px;
  color: #67c23a;
  white-space: nowrap;
  
  &.saving {
    color: #e6a23c;
  }
}

.editor-body {
  flex: 1;
  overflow: hidden;
}

.markdown-textarea {
  width: 100%;
  height: 100%;
  padding: 12px;
  border: none;
  outline: none;
  resize: none;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', 'Consolas', monospace;
  font-size: 14px;
  line-height: 1.6;
  background: #fff;
  box-sizing: border-box;
}

.markdown-preview {
  width: 100%;
  height: 100%;
  padding: 12px;
  overflow-y: auto;
  background: #fff;
  box-sizing: border-box;
}
</style>