<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** neutral | brand | success | warning | danger | info */
  tone: { type: String, default: 'neutral' },
  /** sm | md */
  size: { type: String, default: 'md' }
})

const TONES = {
  neutral: 'bg-ink-100 text-ink-500',
  brand: 'bg-brand-50 text-brand-600',
  success: 'bg-success-soft text-success',
  warning: 'bg-warning-soft text-warning',
  danger: 'bg-danger-soft text-danger',
  info: 'bg-info-soft text-info'
}

const SIZES = {
  sm: 'h-5 px-2 text-[11px]',
  md: 'h-6 px-2.5 text-xs'
}

const cls = computed(() => [
  'inline-flex items-center justify-center rounded-full font-semibold whitespace-nowrap',
  SIZES[props.size] || SIZES.md,
  TONES[props.tone] || TONES.neutral
])
</script>

<template>
  <span :class="cls"><slot /></span>
</template>
