import request from './request'

export function getPackages() {
    return request.get('/recharge/packages')
}

export function createRecharge(data) {
    return request.post('/recharge/create', data)
}

/** 返回本次到账算力；0 表示这单之前已经到过账 */
export function payRecharge(rechargeNo) {
    return request.post(`/recharge/${rechargeNo}/pay`)
}

export function getMyRecharges() {
    return request.get('/recharge/orders')
}