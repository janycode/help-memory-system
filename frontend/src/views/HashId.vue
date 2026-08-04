<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { hashidApi } from '@/api/hashid'

const mode = ref<'encode' | 'decode'>('decode')
const input = ref('')
const salt = ref('(*%!~%^ynU-0882++=')
const loading = ref(false)
const result = ref<string | number[] | null>(null)

const resultDisplay = computed(() => {
  if (result.value === null) return ''
  if (Array.isArray(result.value)) {
    return String(result.value[0] ?? '')
  }
  return String(result.value)
})

const handleExecute = async () => {
  if (!input.value.trim()) {
    ElMessage.warning('请输入要处理的内容')
    return
  }

  loading.value = true
  result.value = null

  try {
    if (mode.value === 'encode') {
      const num = Number(input.value)
      if (!Number.isFinite(num)) {
        ElMessage.error('编码模式下，输入必须是整数')
        return
      }
      const res = await hashidApi.encode(num, salt.value, 8)
      result.value = res.data.result
    } else {
      const res = await hashidApi.decode(input.value.trim(), salt.value, 8)
      result.value = res.data.result
    }
  } catch (error: any) {
    ElMessage.error(error.message || '执行失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  mode.value = 'decode'
  input.value = ''
  salt.value = '(*%!~%^ynU-0882++='
  result.value = null
}
</script>

<template>
  <div class="hashid-page">
    <el-card shadow="never">
      <template #header>
        <span class="page-title">HashId 加解密工具</span>
      </template>

      <el-form label-position="top">
        <el-form-item label="模式">
          <el-radio-group v-model="mode">
            <el-radio value="decode">解密 (HashId → 数字)</el-radio>
            <el-radio value="encode">加密 (数字 → HashId)</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="输入内容">
          <el-input
            v-model="input"
            :placeholder="mode === 'decode' ? '请输入 HashId 字符串' : '请输入整数'"
            clearable
          />
        </el-form-item>

        <el-form-item label="Salt">
          <el-input v-model="salt" placeholder="请输入盐值" />
        </el-form-item>

        <el-form-item label="最小输出长度">
          <el-input-number :model-value="8" :min="1" :max="64" disabled />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleExecute">
            执行
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-if="result !== null" shadow="never" class="result-card">
      <template #header>
        <span>结果</span>
      </template>
      <div class="result-content">{{ resultDisplay }}</div>
    </el-card>
  </div>
</template>

<style scoped>
.hashid-page {
  max-width: 600px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
}

.result-card {
  margin-top: 16px;
}

.result-content {
  font-size: 20px;
  font-weight: 600;
  color: var(--el-color-primary);
  word-break: break-all;
  padding: 12px;
  background: var(--el-fill-color-light);
  border-radius: 4px;
}
</style>
