<script setup>
import { watch, onUnmounted } from 'vue'

const props = defineProps({
  modelValue: Boolean,
  title: String,
  /** 内容区最大宽度，如 max-w-md / max-w-lg */
  maxWidth: { type: String, default: 'max-w-md' },
  closable: { type: Boolean, default: true },
  /** 点遮罩是否关闭 */
  maskClosable: { type: Boolean, default: true }
})

const emit = defineEmits(['update:modelValue', 'close'])

function close() {
  if (!props.closable) return
  emit('update:modelValue', false)
  emit('close')
}

function onKeydown(e) {
  if (e.key === 'Escape') close()
}

/**
 * 打开时：锁 body 滚动 + 监听 ESC
 * 关闭/销毁时：一定要还原，否则页面会永久无法滚动
 */
watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      document.addEventListener('keydown', onKeydown)
      document.body.style.overflow = 'hidden'
    } else {
      document.removeEventListener('keydown', onKeydown)
      document.body.style.overflow = ''
    }
  },
  { immediate: true }
)

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  document.body.style.overflow = ''
})
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-center justify-center bg-ink-900/45 p-4"
        @click.self="maskClosable && close()"
      >
        <Transition name="pop" appear>
          <div
            :class="[
              'w-full bg-surface rounded-3xl shadow-pop overflow-hidden',
              maxWidth
            ]"
            role="dialog"
            aria-modal="true"
          >
            <header
              v-if="title || closable"
              class="flex items-start justify-between gap-4 px-6 pt-6 pb-2"
            >
              <h3 class="text-lg font-semibold text-ink-900">{{ title }}</h3>
              <button
                v-if="closable"
                type="button"
                class="-mr-1 -mt-1 grid place-items-center size-8 rounded-full text-ink-400 hover:bg-ink-100 hover:text-ink-600 transition-colors"
                aria-label="关闭"
                @click="close"
              >
                <svg class="size-4" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M6 6l12 12M18 6L6 18" stroke-linecap="round" />
                </svg>
              </button>
            </header>

            <div class="px-6 pt-2 pb-6">
              <slot />
            </div>

            <footer v-if="$slots.footer" class="px-6 pb-6 pt-0">
              <slot name="footer" />
            </footer>
          </div>
        </Transition>
      </div>
    </Transition>
  </Teleport>
</template>
