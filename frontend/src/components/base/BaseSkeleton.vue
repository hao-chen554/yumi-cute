<script setup>
import { computed } from 'vue'

const props = defineProps({
  /** 重复几个 */
  count: { type: Number, default: 1 },
  /** 圆角：rounded-lg | rounded-xl | rounded-2xl | rounded-full */
  rounded: { type: String, default: 'rounded-xl' },
  /** 自定义高度/宽度类，如 h-40 w-full */
  size: { type: String, default: 'h-4 w-full' }
})

const cls = computed(() => ['skeleton', props.rounded, props.size])
</script>

<template>
  <div v-for="i in count" :key="i" :class="cls" aria-hidden="true" />
</template>
