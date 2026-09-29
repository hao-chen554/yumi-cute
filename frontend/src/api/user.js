import request from './request'

export function register(data) {
    return request.post('/user/register', data)
}

export function login(data) {
    return request.post('/user/login', data)
}

export function getMyInfo() {
    return request.get('/user/me')
}

export function updateAvatar(avatarUrl) {
    return request.put('/user/avatar', { avatarUrl })
}