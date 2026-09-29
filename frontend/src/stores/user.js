import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/user'

export const useUserStore = defineStore('user', () => {

    // 初始化时，先从 localStorage 把上次的数据读回来
    const token = ref(localStorage.getItem('token') || '')
    const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))

    // 计算属性：只要 token 有值，就算已登录
    const isLogin = computed(() => !!token.value)

    async function login(form) {
        const data = await loginApi(form)

        token.value = data.token
        userInfo.value = {
            userId: data.userId,
            username: data.username,
            nickname: data.nickname,
            points: data.points
        }

        // 同步到 localStorage，这样刷新页面也不会丢
        localStorage.setItem('token', data.token)
        localStorage.setItem('userInfo', JSON.stringify(userInfo.value))
    }

    function logout() {
        token.value = ''
        userInfo.value = null
        localStorage.removeItem('token')
        localStorage.removeItem('userInfo')
    }

    return { token, userInfo, isLogin, login, logout }
})