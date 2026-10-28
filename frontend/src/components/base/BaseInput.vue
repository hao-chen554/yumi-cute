<script setup>
import { computed, useId } from 'vue'

const props = defineProps({
  modelValue: [String, Number],
  label: String,
  placeholder: String,
  type: { type: String, default: 'text' },
  error: String,
  hint: String,
  disabled: Boolean,
  autocomplete: String,
  maxlength: [String, Number]
})

const emit = defineEmits(['update:modelValue', 'enter'])

const id = useId()
const cls = computed(() => [
  'w-full h-11 px-4 rounded-xl text-sm bg-surface transition-colors',
  'placeholder:text-ink-400',
  'focus:outline-none focus:ring-2',
  props.error
    ? 'border border-danger focus:ring-danger/25 focus:border-danger'
    : 'border border-ink-200 focus:ring-brand-500/25 focus:border-brand-500',
  props.disabled ? 'bg-ink-100 text-ink-400 cursor-not-allowed' : ''
])
</script>

<template>
  <div class="w-full">
    <label v-if="label" :for="id" class="block mb-1.5 text-sm font-medium text-ink-700">
      {{ label }}
    </label>

    <input
      :id="id"
      :class="cls"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :disabled="disabled"
      :autocomplete="autocomplete"
      :maxlength="maxlength"
      @input="emit('update:modelValue', $event.target.value)"
      @keyup.enter="emit('enter')"
    />

    <p v-if="error" class="mt-1.5 text-xs text-danger">{{ error }}</p>
    <p v-else-if="hint" class="mt-1.5 text-xs text-ink-400">{{ hint }}</p>
  </div>
</template>
