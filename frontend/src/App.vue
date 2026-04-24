<template>
  <el-config-provider :locale="locale">
    <router-view />
  </el-config-provider>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { zhCn } from 'element-plus/es/locale/index'
import { useThemeStore } from '@/stores/theme'

const locale = zhCn
const themeStore = useThemeStore()

onMounted(() => {
  themeStore.init()
})
</script>