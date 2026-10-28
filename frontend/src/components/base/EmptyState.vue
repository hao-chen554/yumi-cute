<script setup>
defineProps({
  title: { type: String, default: '这里还什么都没有' },
  description: String,
  /** 用 emoji 还是自定义图标 */
  emoji: { type: String, default: '🐾' }
})
</script>

<template>
  <div class="flex flex-col items-center justify-center px-6 py-16 text-center">
    <div class="mb-4 text-5xl select-none" aria-hidden="true">{{ emoji }}</div>
    <h3 class="text-base font-semibold text-ink-700">{{ title }}</h3>
    <p v-if="description" class="mt-2 max-w-sm text-sm leading-relaxed text-ink-400">
      {{ description }}
    </p>
    <div v-if="$slots.action" class="mt-6">
      <slot name="action" />
    </div>
  </div>
</template>
