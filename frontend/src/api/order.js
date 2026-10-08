import request from './request'

export function createOrder(data) {
    return request.post('/order/create', data)
}

export function getMyOrders(){
    return request.get('/order/list')
}

export function getOrderDetail(id){
    return request.get(`/order/detail/${id}`)
}