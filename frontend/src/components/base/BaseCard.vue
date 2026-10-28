<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** none | sm | md | lg */
  padding: { type: String, default: 'md' },
  hoverable: Boolean,
  /** 去掉默认阴影，用于嵌套卡片 */
  flat: Boolean
})

const PADDING = {
  none: '',
  sm: 'p-3',
  md: 'p-5',
  lg: 'p-6 sm:p-8'
}

const cls = computed(() => [
  'bg-surface rounded-2xl',
  props.flat ? 'border border-ink-200' : 'shadow-card',
  props.hoverable
    ? 'transition-shadow duration-200 hover:shadow-hover cursor-pointer'
    : '',
  PADDING[props.padding] ?? PADDING.md
])
</script>

<template>
  <div :class="cls">
    <slot />
  </div>
</template>
