<script setup>
import { toasts, dismissToast } from './useToast'

const ICONS = {
  success: 'M20 6L9 17l-5-5',
  error: 'M6 6l12 12M18 6L6 18',
  warn: 'M12 9v4m0 4h.01',
  info: 'M12 8h.01M11 12h1v4h1'
}

const TONES = {
  success: 'bg-success-soft text-success',
  error: 'bg-danger-soft text-danger',
  warn: 'bg-warning-soft text-warning',
  info: 'bg-info-soft text-info'
}
</script>

<template>
  <Teleport to="body">
    <div
      class="fixed top-4 left-1/2 -translate-x-1/2 z-[60] flex flex-col items-center gap-2 pointer-events-none"
      role="status"
      aria-live="polite"
    >
      <TransitionGroup name="toast">
        <div
          v-for="t in toasts"
          :key="t.id"
          class="pointer-events-auto flex items-center gap-3 max-w-[92vw] sm:max-w-md px-4 py-3 rounded-2xl bg-surface shadow-pop animate-slide-down"
        >
          <span
            :class="[
              'grid place-items-center size-6 shrink-0 rounded-full',
              TONES[t.type] || TONES.info
            ]"
          >
            <svg
              class="size-3.5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            >
              <path :d="ICONS[t.type] || ICONS.info" />
            </svg>
          </span>
          <p class="text-sm text-ink-700 leading-snug">{{ t.message }}</p>
          <button
            class="ml-1 shrink-0 text-ink-400 hover:text-ink-600 transition-colors"
            aria-label="关闭"
            @click="dismissToast(t.id)"
          >
            <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M6 6l12 12M18 6L6 18" stroke-linecap="round" />
            </svg>
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>
