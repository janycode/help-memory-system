<template>
  <div class="jenkins-monitor">
    <div class="page-header">
      <div>
        <h2 class="page-title">Jenkins 监控</h2>
        <p class="page-desc">每 30 秒轮询一次 Jenkins 构建状态，检测到新的成功构建时自动推送企业微信机器人通知</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新增配置
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="configs" v-loading="loading" stripe>
        <el-table-column prop="jobName" label="Job 名称" min-width="120">
          <template #default="{ row }">
            <span class="job-name">{{ row.jobName }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="jenkinsUrl" label="Jenkins 地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="environment" label="环境" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.environment" size="small" effect="plain">{{ row.environment }}</el-tag>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="lastNotifiedBuild" label="已通知构建号" width="120" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.lastNotifiedBuild" type="info" size="small">#{{ row.lastNotifiedBuild }}</el-tag>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="90" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              @change="(val: boolean) => handleToggle(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button
              link
              type="success"
              size="small"
              :loading="testingRowId === row.id"
              @click="handleTest(row)"
            >
              测试
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑监控配置' : '新增监控配置'"
      width="560px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="Job 名称" prop="jobName">
          <el-select
            v-model="form.jobName"
            filterable
            placeholder="先填地址/账号/密码，点「测试连接」后从下拉选择"
            style="width: 100%"
          >
            <el-option v-for="name in jobOptions" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item label="Jenkins 地址" prop="jenkinsUrl">
          <el-input v-model="form.jenkinsUrl" placeholder="如 https://devops.leaderrun.com/jenkins" />
        </el-form-item>
        <el-form-item label="账号" prop="username">
          <el-input v-model="form.username" placeholder="Jenkins 登录账号" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="Jenkins 登录密码"
          />
        </el-form-item>
        <el-form-item label="企业微信Webhook" prop="webhookUrl">
          <el-input v-model="form.webhookUrl" placeholder="群机器人 webhook 地址" />
        </el-form-item>
        <el-form-item label="环境标识">
          <el-input v-model="form.environment" placeholder="如 DEV / TEST / PROD，通知消息中展示" />
        </el-form-item>
        <el-form-item label="本地Git目录">
          <el-input v-model="form.localDir" placeholder="如 D:\work\03_code\tm-project\tm（含 .git 的仓库根目录），留空不显示最近提交" />
        </el-form-item>
        <el-form-item label="@手机号">
          <el-input v-model="form.atMobiles" placeholder="需 @ 的手机号，多个用英文逗号分隔（可空）" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button
          :loading="testing"
          :disabled="!form.jenkinsUrl || !form.username"
          @click="handleTestDialog"
        >
          测试连接
        </el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 测试结果弹窗 -->
    <el-dialog v-model="testResultVisible" title="连接测试结果" width="500px">
      <el-alert
        v-if="testResult.ok"
        type="success"
        :title="`连接成功，当前账号可见 ${testResult.jobs.length} 个 job`"
        :closable="false"
      />
      <el-alert v-else type="error" :title="testResult.message || '连接失败'" :closable="false" />
      <div v-if="testResult.ok && testResult.jobs.length" class="test-jobs">
        <el-tag v-for="job in testResult.jobs" :key="job" size="small" class="test-job-tag">
          {{ job }}
        </el-tag>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getJenkinsMonitorConfigs,
  createJenkinsMonitorConfig,
  updateJenkinsMonitorConfig,
  deleteJenkinsMonitorConfig,
  testJenkinsConnection
} from '@/api/jenkinsMonitor'
import type { JenkinsJobConfig, JenkinsTestResult } from '@/types/jenkinsMonitor'

const configs = ref<JenkinsJobConfig[]>([])
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
/** 当前进行连接测试的行 id（用于行内按钮 loading） */
const testingRowId = ref<number | null>(null)
/** 测试连接成功后返回的可用 job 列表，用于下拉单选 */
const jobOptions = ref<string[]>([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<JenkinsJobConfig>({
  jobName: '',
  jenkinsUrl: '',
  username: '',
  password: '',
  environment: '',
  localDir: '',
  webhookUrl: '',
  atMobiles: '',
  enabled: true,
  remark: ''
})

const rules: FormRules = {
  jobName: [{ required: true, message: '请选择 job 名称', trigger: 'change' }],
  jenkinsUrl: [{ required: true, message: '请输入 Jenkins 地址', trigger: 'blur' }],
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  webhookUrl: [{ required: true, message: '请输入企业微信 webhook 地址', trigger: 'blur' }]
}

const testResultVisible = ref(false)
const testResult = reactive<JenkinsTestResult>({ ok: false, jobs: [] })

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res = await getJenkinsMonitorConfigs()
    configs.value = res.data || []
  } catch {
    configs.value = []
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  isEdit.value = false
  jobOptions.value = []
  dialogVisible.value = true
}

const openEdit = (row: JenkinsJobConfig) => {
  isEdit.value = true
  jobOptions.value = []
  Object.assign(form, row)
  dialogVisible.value = true
}

const resetForm = () => {
  formRef.value?.clearValidate()
  jobOptions.value = []
  Object.assign(form, {
    id: undefined,
    jobName: '',
    jenkinsUrl: '',
    username: '',
    password: '',
    environment: '',
    localDir: '',
    webhookUrl: '',
    atMobiles: '',
    enabled: true,
    remark: ''
  })
}

const handleSubmit = async () => {
  if (!isEdit.value && !form.password) {
    ElMessage.warning('新增配置请填写 Jenkins 密码')
    return
  }
  await formRef.value?.validate()
  saving.value = true
  try {
    if (isEdit.value && form.id) {
      await updateJenkinsMonitorConfig(form.id, { ...form })
      ElMessage.success('配置更新成功')
    } else {
      await createJenkinsMonitorConfig({ ...form })
      ElMessage.success('配置创建成功')
    }
    dialogVisible.value = false
    fetchConfigs()
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleToggle = async (row: JenkinsJobConfig, enabled: boolean) => {
  try {
    await updateJenkinsMonitorConfig(row.id!, { jobName: row.jobName, enabled })
    ElMessage.success(enabled ? '已启用监控' : '已停用监控')
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  }
}

const handleDelete = (row: JenkinsJobConfig) => {
  ElMessageBox.confirm(`确定删除 ${row.jobName} 的监控配置吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deleteJenkinsMonitorConfig(row.id!)
    ElMessage.success('删除成功')
    fetchConfigs()
  })
}

// 统一连接测试：成功弹出结果窗，失败提示原因，列表与弹窗入口共用同一效果
const runConnectionTest = async (params: {
  id?: number
  jenkinsUrl: string
  username: string
  password: string
  jobName?: string
}) => {
  testing.value = true
  try {
    const res = await testJenkinsConnection(params)
    Object.assign(testResult, res.data)
    jobOptions.value = res.data?.jobs || []
    testResultVisible.value = true
  } catch (error: any) {
    ElMessage.error(error.message || '测试失败')
  } finally {
    testing.value = false
  }
}

// 列表行内测试（使用已保存配置，密码留空由后端按 id 兜底）
const handleTest = async (row: JenkinsJobConfig) => {
  testingRowId.value = row.id ?? null
  await runConnectionTest({
    id: row.id,
    jenkinsUrl: row.jenkinsUrl,
    username: row.username,
    password: '',
    jobName: row.jobName
  })
  testingRowId.value = null
}

// 弹窗内测试（使用表单当前值，密码留空时复用已保存密码）
const handleTestDialog = async () => {
  if (!form.jenkinsUrl || !form.username) {
    ElMessage.warning('请先填写 Jenkins 地址和账号')
    return
  }
  if (!isEdit.value && !form.password) {
    ElMessage.warning('新增配置请先填写 Jenkins 密码再测试连接')
    return
  }
  await runConnectionTest({
    id: isEdit.value ? form.id : undefined,
    jenkinsUrl: form.jenkinsUrl,
    username: form.username,
    password: form.password,
    jobName: form.jobName
  })
}

onMounted(fetchConfigs)
</script>

<style scoped>
.jenkins-monitor {
  height: 100%;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.page-desc {
  margin: 6px 0 0;
  font-size: 13px;
  color: #86909c;
}

.table-card {
  flex: 1;
  overflow: hidden;
}

.job-name {
  font-weight: 600;
}

.text-muted {
  color: #c0c4cc;
}

.test-jobs {
  margin-top: 12px;
  max-height: 260px;
  overflow-y: auto;
}

.test-job-tag {
  margin: 0 8px 8px 0;
}
</style>