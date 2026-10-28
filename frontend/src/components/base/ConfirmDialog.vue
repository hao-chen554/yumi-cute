<script setup>
import BaseModal from './BaseModal.vue'
import BaseButton from './BaseButton.vue'

defineProps({
  modelValue: Boolean,
  title: { type: String, default: '确认操作' },
  /** 危险操作的说明文字，支持 \n 换行 */
  message: String,
  confirmText: { type: String, default: '确定' },
  cancelText: { type: String, default: '取消' },
  /** true 时确认按钮为红色 —— 删除、撤销这类不可逆操作必须用它 */
  danger: Boolean,
  loading: Boolean
})

const emit = defineEmits(['update:modelValue', 'confirm', 'cancel'])

function cancel() {
  emit('update:modelValue', false)
  emit('cancel')
}
</script>

<template>
  <BaseModal
    :model-value="modelValue"
    :title="title"
    :closable="!loading"
    :mask-closable="!loading"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <p class="text-sm leading-relaxed text-ink-600 whitespace-pre-line">{{ message }}</p>

    <template #footer>
      <div class="flex gap-3">
        <BaseButton variant="ghost" block :disabled="loading" @click="cancel">
          {{ cancelText }}
        </BaseButton>
        <BaseButton
          :variant="danger ? 'danger' : 'primary'"
          block
          :loading="loading"
          @click="emit('confirm')"
        >
          {{ confirmText }}
        </BaseButton>
      </div>
    </template>
  </BaseModal>
</template>
