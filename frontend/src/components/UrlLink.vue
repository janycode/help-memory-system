<template>
  <div class="url-link" @click="openUrl">
    <el-icon class="link-icon"><Link /></el-icon>
    <span class="url-text" :title="url">{{ displayUrl }}</span>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Link } from '@element-plus/icons-vue'

const props = defineProps<{
  url: string
}>()

const displayUrl = computed(() => {
  if (!props.url) return ''

  try {
    const urlObj = new URL(props.url)
    // 如果URL太长，显示域名+前20个字符
    const fullUrl = props.url
    if (fullUrl.length > 50) {
      return urlObj.hostname + fullUrl.substring(urlObj.origin.length, 30) + '...'
    }
    return fullUrl
  } catch {
    // 如果URL格式不正确，直接返回原文本
    return props.url.length > 50 ? props.url.substring(0, 47) + '...' : props.url
  }
})

const openUrl = () => {
  if (!props.url) return

  try {
    // 确保URL有协议前缀
    let finalUrl = props.url
    if (!finalUrl.startsWith('http://') && !finalUrl.startsWith('https://')) {
      finalUrl = 'https://' + finalUrl
    }
    window.open(finalUrl, '_blank')
  } catch (error) {
    console.error('打开URL失败:', error)
  }
}
</script>

<style scoped>
.url-link {
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #409eff;
  transition: color 0.3s;
}

.url-link:hover {
  color: #66b1ff;
  text-decoration: underline;
}

.link-icon {
  font-size: 14px;
}

.url-text {
  font-size: 14px;
  word-break: break-all;
}
</style>