<template>
  <div
    class="page-container"
    :class="{ 'page-container--padded': padded, 'page-container--scroll': scrollable }"
  >
    <div class="page-container__inner" :style="innerStyle">
      <slot />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    maxWidth?: number | string
    padded?: boolean
    scrollable?: boolean
  }>(),
  {
    maxWidth: 1600,
    padded: true,
    scrollable: true
  }
)

const innerStyle = computed(() => ({
  maxWidth: typeof props.maxWidth === 'number' ? `${props.maxWidth}px` : props.maxWidth
}))
</script>

<style scoped>
.page-container {
  height: 100%;
}

.page-container--padded {
  padding: var(--app-page-padding-y) var(--app-page-padding-x);
}

.page-container--scroll {
  overflow-y: auto;
}

.page-container__inner {
  width: 100%;
  margin: 0 auto;
}
</style>
