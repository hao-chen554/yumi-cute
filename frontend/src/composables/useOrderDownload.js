import { ref } from 'vue'
import {
  downloadOrderZip,
  downloadOneImage,
  filenameFrom,
  saveBlob
} from '@/api/download'
import { toast } from '@/components/base'

/**
 * 订单下载逻辑：整套 zip / 单张图片。
 *
 * @param {import('vue').Ref<object | null>} order 订单详情 ref
 */
export function useOrderDownload(order) {
  const downloading = ref(false)
  const downloadingTaskId = ref(null)

  /** 整套下载：后端把成功的那些图打成一个 zip 流回来 */
  async function handleDownloadAll() {
    if (downloading.value || !order.value) return

    const current = order.value
    downloading.value = true
    try {
      const resp = await downloadOrderZip(current.id)
      const fallback = `${current.styleName || '写真'}-${current.orderNo}.zip`
      saveBlob(resp.data, filenameFrom(resp, fallback))
    } catch (e) {
      toast.error(e.message || '下载失败')
    } finally {
      downloading.value = false
    }
  }

  /** 单张下载 */
  async function handleDownloadOne(task) {
    if (downloadingTaskId.value || !order.value) return

    const current = order.value
    downloadingTaskId.value = task.id
    try {
      const resp = await downloadOneImage(current.id, task.id)
      const fallback = `${current.styleName || '写真'}-${task.seqNo}.jpg`
      saveBlob(resp.data, filenameFrom(resp, fallback))
    } catch (e) {
      toast.error(e.message || '下载失败')
    } finally {
      downloadingTaskId.value = null
    }
  }

  return {
    downloading,
    downloadingTaskId,
    handleDownloadAll,
    handleDownloadOne
  }
}
