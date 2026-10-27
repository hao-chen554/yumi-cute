import request from './request'

export function downloadOrderZip(orderId) {
    return request.get(`/order/${orderId}/download`, { responseType: 'blob' })
        .then(unwrapBlob)
}

export function downloadOneImage(orderId, taskId) {
    return request.get(`/order/${orderId}/download/${taskId}`, { responseType: 'blob' })
        .then(unwrapBlob)
}

/**
 * 陷阱：业务错误时后端返回的是 HTTP 200 + JSON，
 * 但因为 responseType=blob，这个 JSON 会被包成一个 Blob 交给我们。
 * 不解开就会把一段 JSON 存成 .zip / .jpg。
 */
async function unwrapBlob(response) {
    const blob = response.data
    if (blob.type && blob.type.includes('application/json')) {
        const text = await blob.text()
        let msg = '下载失败'
        try {
            msg = JSON.parse(text).message || msg
        } catch (e) {
            /* 保持默认提示 */
        }
        throw new Error(msg)
    }
    return response
}

/** 从 Content-Disposition 里还原文件名（优先 filename*） */
export function filenameFrom(response, fallback) {
    const cd = response.headers['content-disposition'] || ''

    const star = /filename\*=UTF-8''([^;]+)/i.exec(cd)
    if (star) {
        try {
            return decodeURIComponent(star[1])
        } catch (e) {
            /* 落到下面的兜底 */
        }
    }

    const plain = /filename="([^"]+)"/i.exec(cd)
    return plain ? plain[1] : fallback
}

/** 把 Blob 存成文件 */
export function saveBlob(blob, filename) {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    // 稍等再释放：立即 revoke 会让部分浏览器的下载中断
    setTimeout(() => URL.revokeObjectURL(url), 1000)
}