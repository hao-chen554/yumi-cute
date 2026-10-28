import { computed, ref } from 'vue'
import { getShare, createShare, cancelShare } from '@/api/share'
import { toast } from '@/components/base'

/** 后端返回 ISO 时间，显示成 YYYY-MM-DD HH:mm */
export function fmtTime(s) {
  return s ? String(s).replace('T', ' ').slice(0, 16) : ''
}

/**
 * 分享面板的状态与操作。
 *
 * @param {import('vue').Ref<object | null>} order 订单详情 ref
 */
export function useOrderShare(order) {
  const shareInfo = ref(null) // { token, expireAt } 或 null
  const shareOpen = ref(false)
  const shareLoading = ref(false) // 打开面板时查询分享状态
  const shareBusy = ref(false) // 生成 / 重新生成 / 取消分享
  const cancelShareOpen = ref(false) // 「取消分享」二次确认

  /** 分享链接由前端拼：后端不该知道前端部署在哪个域名/端口 */
  const shareUrl = computed(() =>
    shareInfo.value ? `${window.location.origin}/share/${shareInfo.value.token}` : ''
  )

  /** 打开分享面板时先查现状：有链接就展示，没有才让用户生成 */
  async function openSharePanel() {
    shareOpen.value = true
    if (!order.value) return

    shareLoading.value = true
    try {
      shareInfo.value = await getShare(order.value.id)
    } catch (e) {
      toast.error(e.message || '查询分享状态失败')
    } finally {
      shareLoading.value = false
    }
  }

  function closeSharePanel() {
    shareOpen.value = false
    cancelShareOpen.value = false
  }

  async function handleCreateShare() {
    if (shareBusy.value || !order.value) return

    shareBusy.value = true
    try {
      shareInfo.value = await createShare(order.value.id)
      await copyShareUrl(true) // 生成后顺手复制，省一步操作
    } catch (e) {
      toast.error(e.message || '生成分享链接失败')
    } finally {
      shareBusy.value = false
    }
  }

  async function copyShareUrl(silent = false) {
    const url = shareUrl.value
    if (!url) return

    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(url)
      } else {
        // 兜底：非安全上下文（http 且非 localhost）下 clipboard API 不可用
        const ta = document.createElement('textarea')
        ta.value = url
        ta.style.position = 'fixed'
        ta.style.opacity = '0'
        document.body.appendChild(ta)
        ta.select()
        document.execCommand('copy')
        ta.remove()
      }
      if (!silent) toast.success('链接已复制')
    } catch (e) {
      toast.error('复制失败，请手动选中链接复制')
    }
  }

  function askCancelShare() {
    cancelShareOpen.value = true
  }

  async function handleCancelShare() {
    if (shareBusy.value || !order.value) return

    shareBusy.value = true
    try {
      await cancelShare(order.value.id)
    } catch (e) {
      toast.error(e.message || '取消失败')
      return
    } finally {
      shareBusy.value = false
    }

    shareInfo.value = null
    cancelShareOpen.value = false
    toast.success('已取消分享')
  }

  return {
    shareInfo,
    shareOpen,
    shareLoading,
    shareBusy,
    cancelShareOpen,
    shareUrl,
    openSharePanel,
    closeSharePanel,
    handleCreateShare,
    copyShareUrl,
    askCancelShare,
    handleCancelShare,
    fmtTime
  }
}
