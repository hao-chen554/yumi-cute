/**
 * 基础组件统一出口。
 *
 * 页面里这样用：
 *   import { BaseButton, BaseCard, toast } from '@/components/base'
 */
export { default as BaseButton } from './BaseButton.vue'
export { default as BaseCard } from './BaseCard.vue'
export { default as BaseInput } from './BaseInput.vue'
export { default as BaseBadge } from './BaseBadge.vue'
export { default as BaseModal } from './BaseModal.vue'
export { default as ConfirmDialog } from './ConfirmDialog.vue'
export { default as BaseSkeleton } from './BaseSkeleton.vue'
export { default as EmptyState } from './EmptyState.vue'
export { default as ToastHost } from './ToastHost.vue'
export { toast, dismissToast, toasts } from './useToast'
