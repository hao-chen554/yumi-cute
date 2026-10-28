import { reactive } from 'vue'

/**
 * 全局 Toast。
 *
 * 为什么不用 alert：
 *  - alert 会阻塞整个线程，动画和请求全部暂停
 *  - 样式不可控、不可关闭、移动端体验差
 *  - 一次只能弹一个，连续操作会叠加一堆弹窗
 *
 * 用法：
 *   import { toast } from '@/components/base/useToast'
 *   toast.success('已删除')
 *   toast.error(e.message)
 */

let seq = 0
const DURATION = 2600

export const toasts = reactive([])

export function dismissToast(id) {
  const i = toasts.findIndex((t) => t.id === id)
  if (i > -1) toasts.splice(i, 1)
}

function push(message, type, duration) {
  const id = ++seq
  toasts.push({ id, message, type })
  if (duration > 0) {
    setTimeout(() => dismissToast(id), duration)
  }
  return id
}

export const toast = Object.assign(
  (message, type = 'info', duration = DURATION) => push(message, type, duration),
  {
    success: (m, d = DURATION) => push(m, 'success', d),
    error: (m, d = DURATION) => push(m, 'error', d),
    warn: (m, d = DURATION) => push(m, 'warn', d),
    info: (m, d = DURATION) => push(m, 'info', d)
  }
)
