import axios from 'axios'

const request = axios.create({
    baseURL: '/api',
    timeout: 10000
})

// interceptors.request是请求拦截器
request.interceptors.request.use(config => {
        const token = localStorage.getItem('token')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => Promise.reject(error))

request.interceptors.response.use(
    (response) => {
        const res = response.data

        // 业务码 401：登录态失效（token 过期或伪造）
        if (res.code === 401) {
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            window.location.href = '/login'
            return Promise.reject(new Error(res.message || '登录已过期'))
        }

        if (res.code !== 200) {
            return Promise.reject(new Error(res.message || '请求失败'))
        }

        return res.data
    },
    (error) => {
        return Promise.reject(error)
    }
)

export default request