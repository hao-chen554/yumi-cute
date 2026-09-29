import request from './request'

export function getStyleList() {
    return request.get('/style/list')
}