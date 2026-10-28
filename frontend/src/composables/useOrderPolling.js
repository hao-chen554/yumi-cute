import { onMounted, onUnmounted, ref, unref } from 'vue'
import { getOrderDetail } from '@/api/order'

/** 终态：到了这些状态就不用再轮询了 */
export const FINAL_STATUS = [2, 3, 4, 5]

/**
 * 订单详情加载 + 轮询。
 *
 * 非终态时每 2 秒刷新一次；组件销毁时一定清掉定时器。
 *
 * @param {string | import('vue').Ref<string>} orderId 订单 id（也接受 ref）
 * @param {{ interval?: number }} options
 */
export function useOrderPolling(orderId, { interval = 2000 } = {}) {
  const order = ref(null)
  const loading = ref(true)
  const errorMsg = ref('')

  let timer = null

  async function load() {
    try {
      order.value = await getOrderDetail(unref(orderId))
      errorMsg.value = ''
    } catch (e) {
      errorMsg.value = e.message || '加载失败'
      stopPolling()
    } finally {
      loading.value = false
    }
  }

  function startPolling() {
    stopPolling()
    timer = setInterval(async () => {
      await load()
      if (order.value && FINAL_STATUS.includes(order.value.status)) {
        stopPolling()
      }
    }, interval)
  }

  function stopPolling() {
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  async function init() {
    await load()
    if (order.value && !FINAL_STATUS.includes(order.value.status)) {
      startPolling()
    }
  }

  /** 加载失败后的「重新加载」：先停掉旧轮询，再重新走一遍初始化 */
  async function reload() {
    loading.value = true
    errorMsg.value = ''
    stopPolling()
    await init()
  }

  onMounted(init)
  onUnmounted(stopPolling)

  return {
    order,
    loading,
    errorMsg,
    load,
    init,
    reload,
    startPolling,
    stopPolling
  }
}
