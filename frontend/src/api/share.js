import request from './request'

/** 查当前分享状态；没有有效分享时返回 null */
export function getShare(orderId) {
    return request.get(`/order/${orderId}/share`)
}

/** 生成（或重新生成）分享链接 —— 旧链接会被立即覆盖失效 */
export function createShare(orderId) {
    return request.post(`/order/${orderId}/share`)
}

/** 撤销分享（幂等） */
export function cancelShare(orderId) {
    return request.delete(`/order/${orderId}/share`)
}

/**
 * 公开访问分享页 —— 不需要登录。
 * 用的是同一个 request 实例：没登录时它不会加 Authorization 头，有登录也只是多带一个头，
 * 后端的 /share/ 前缀白名单会在校验 token 之前就放行。
 */
export function viewShare(token) {
    return request.get(`/share/${encodeURIComponent(token)}`)
}
