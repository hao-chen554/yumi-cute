<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** primary | secondary | ghost | danger | dangerGhost */
  variant: { type: String, default: 'primary' },
  /** sm | md | lg */
  size: { type: String, default: 'md' },
  loading: Boolean,
  disabled: Boolean,
  block: Boolean,
  type: { type: String, default: 'button' }
})

const VARIANTS = {
  primary: 'bg-brand-500 text-white hover:bg-brand-600 active:bg-brand-700 disabled:bg-brand-300',
  secondary:
    'bg-surface text-ink-700 border border-ink-200 hover:border-ink-300 hover:bg-ink-100 disabled:text-ink-400',
  ghost: 'bg-ink-100 text-ink-600 hover:bg-ink-200 disabled:text-ink-400',
  danger: 'bg-danger text-white hover:opacity-90 active:opacity-95 disabled:opacity-50',
  dangerGhost:
    'bg-surface text-danger border border-danger/30 hover:bg-danger-soft disabled:opacity-50'
}

const SIZES = {
  sm: 'h-8 px-3 text-xs rounded-lg gap-1',
  md: 'h-10 px-4 text-sm rounded-xl gap-1.5',
  lg: 'h-12 px-6 text-base rounded-xl gap-2'
}

const cls = computed(() => [
  'inline-flex items-center justify-center font-medium whitespace-nowrap',
  'transition-colors duration-150 select-none cursor-pointer',
  'focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500',
  'disabled:cursor-not-allowed',
  SIZES[props.size] || SIZES.md,
  VARIANTS[props.variant] || VARIANTS.primary,
  props.block ? 'w-full' : ''
])

const isDisabled = computed(() => props.disabled || props.loading)
</script>

<template>
  <button :type="type" :class="cls" :disabled="isDisabled">
    <!-- loading 时用转圈替换掉前置图标的位置 -->
    <svg
      v-if="loading"
      class="size-3.5 animate-spin"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-opacity="0.25" stroke-width="4" />
      <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="4" stroke-linecap="round" />
    </svg>
    <slot v-else name="icon" />
    <slot />
  </button>
</template>
