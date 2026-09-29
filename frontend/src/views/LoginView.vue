<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = ref({
  username: '',
  password: ''
})

const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  errorMsg.value = ''
  loading.value = true
  try {
    await userStore.login(form.value)
    // 如果是从别的页面被拦过来的，就跳回那里；否则去首页
    router.push(route.query.redirect || '/')
  } catch (e) {
    errorMsg.value = e.message || '登录失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login">
    <h2>登录 YuMi-cute</h2>
    <p class="subtitle">欢迎回来</p>

    <form @submit.prevent="handleLogin">
      <div class="field">
        <label>用户名</label>
        <input v-model="form.username" type="text" placeholder="请输入用户名" />
      </div>

      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="请输入密码" />
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <button type="submit" :disabled="loading">
        {{ loading ? '登录中...' : '登录' }}
      </button>
    </form>

    <p class="tip">
      还没有账号？<RouterLink to="/register">去注册</RouterLink>
    </p>
  </div>
</template>

<style scoped>
.login {
  max-width: 420px;
  margin: 40px auto;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

h2 { font-size: 24px; text-align: center; margin-bottom: 8px; }
.subtitle { text-align: center; font-size: 14px; color: #999; margin-bottom: 32px; }

.field { margin-bottom: 20px; }
.field label { display: block; font-size: 14px; color: #666; margin-bottom: 8px; }
.field input {
  width: 100%; height: 44px; padding: 0 14px;
  border: 1px solid #e5e7eb; border-radius: 8px;
  font-size: 15px; outline: none; transition: border-color 0.2s;
}
.field input:focus { border-color: #5b7fff; }

.error { color: #ff4d4f; font-size: 14px; margin-bottom: 16px; }

button {
  width: 100%; height: 46px; border: none; border-radius: 8px;
  background: linear-gradient(135deg, #5b7fff 0%, #7b9fff 100%);
  color: #fff; font-size: 16px; cursor: pointer; transition: all 0.3s ease;
}
button:hover:not(:disabled) { opacity: 0.9; }
button:disabled { opacity: 0.6; cursor: not-allowed; }

.tip { text-align: center; font-size: 14px; color: #999; margin-top: 20px; }
.tip a { color: #5b7fff; }
</style>