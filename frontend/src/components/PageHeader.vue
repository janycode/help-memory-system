<template>
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
</template>

<script setup lang="ts">
defineProps<{
  title: string
  description?: string
}>()
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: var(--app-space-4);
  margin-bottom: var(--app-space-5);
}

.page-header__main {
  display: flex;
  flex-direction: column;
  gap: var(--app-space-1);
  min-width: 0;
}

.page-header__title {
  margin: 0;
  font-size: var(--app-font-size-page-title);
  font-weight: 600;
  line-height: 1.4;
  color: var(--app-text-primary);
}

.page-header__desc {
  margin: 0;
  font-size: var(--app-font-size-secondary);
  color: var(--app-text-secondary);
}

.page-header__actions {
  display: flex;
  align-items: center;
  gap: var(--app-space-2);
  flex-shrink: 0;
}
</style>
