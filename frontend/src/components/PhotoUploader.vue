<script setup>
import { ref } from 'vue'
import { uploadFile } from '@/api/file'
import { toast } from '@/components/base'

defineProps({
  label: { type: String, required: true },
  tip: { type: String, default: '' },
  modelValue: { type: String, default: '' }
})

const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)
const inputRef = ref(null)
const dragging = ref(false)

function choose() {
  if (uploading.value) return
  inputRef.value?.click()
}

async function upload(file) {
  if (!file) return
  if (!file.type.startsWith('image/')) {
    toast.error('只能上传图片')
    return
  }

  uploading.value = true
  try {
    const url = await uploadFile(file)
    // 关键：不直接改，而是"上报"给父组件
    emit('update:modelValue', url)
    toast.success('上传成功')
  } catch (e) {
    toast.error(e.message || '上传失败')
  } finally {
    uploading.value = false
  }
}

async function onChange(event) {
  const file = event.target.files?.[0]
  await upload(file)
  event.target.value = ''
}

function onDrop(event) {
  dragging.value = false
  const file = event.dataTransfer?.files?.[0]
  upload(file)
}
</script>

<template>
  <div class="min-w-0 flex-1">
    <p class="mb-2.5 text-sm text-ink-600">{{ label }}</p>

    <!-- 用 button 而不是 div：键盘也能操作 -->
    <button
      type="button"
      :class="[
        'relative block w-full overflow-hidden rounded-xl transition-colors',
        'aspect-[4/3] max-h-56',
        modelValue
          ? 'border-2 border-solid border-ink-200 bg-surface'
          : 'border-2 border-dashed border-ink-300 bg-ink-100/60 hover:border-brand-400 hover:bg-brand-50',
        dragging ? 'border-brand-500 bg-brand-50' : ''
      ]"
      :aria-label="modelValue ? '更换照片' : '上传照片'"
      @click="choose"
      @dragover.prevent="dragging = true"
      @dragleave.prevent="dragging = false"
      @drop.prevent="onDrop"
    >
      <img
        v-if="modelValue"
        :src="modelValue"
        alt="已上传"
        class="size-full object-cover"
      />

      <div
        v-else
        class="flex size-full flex-col items-center justify-center gap-2 text-ink-400"
      >
        <span class="text-3xl leading-none text-ink-300">+</span>
        <span class="text-sm">{{ uploading ? '上传中…' : '点击上传' }}</span>
        <span class="text-xs text-ink-300">也可拖拽进来</span>
      </div>

      <!-- 上传中的遮罩 -->
      <div
        v-if="uploading"
        class="absolute inset-0 grid place-items-center bg-ink-900/35 backdrop-blur-[1px]"
      >
        <svg class="size-7 animate-spin text-white" viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-opacity="0.3" stroke-width="4" />
          <path d="M12 2a10 10 0 0 1 10 10" stroke="currentColor" stroke-width="4" stroke-linecap="round" />
        </svg>
      </div>
    </button>

    <p v-if="tip" class="mt-2 text-xs text-ink-400">{{ tip }}</p>

    <input
      ref="inputRef"
      type="file"
      accept="image/jpeg,image/png,image/webp"
      class="hidden"
      @change="onChange"
    />
  </div>
</template>
