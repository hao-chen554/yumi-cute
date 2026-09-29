<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'

const router = useRouter()

const form = ref({
  username: '',
  password: '',
  nickname: ''
})

const loading = ref(false)
const errorMsg = ref('')

async function handleRegister() {
  errorMsg.value = ''
  loading.value = true
  try {
    await register(form.value)
    alert('注册成功！接下来去登录吧')
    router.push('/login')
  } catch (e) {
    errorMsg.value = e.message || '注册失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register">
    <h2>注册 YuMi-cute</h2>
    <p class="subtitle">开始你和它的写真之旅</p>

    <form @submit.prevent="handleRegister">
      <div class="field">
        <label>用户名</label>
        <input v-model="form.username" type="text" placeholder="4-20 位字母、数字或下划线" />
      </div>

      <div class="field">
        <label>昵称</label>
        <input v-model="form.nickname" type="text" placeholder="给自己起个名字" />
      </div>

      <div class="field">
        <label>密码</label>
        <input v-model="form.password" type="password" placeholder="6-20 位" />
      </div>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <button type="submit" :disabled="loading">
        {{ loading ? '注册中...' : '立即注册' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.register {
  max-width: 420px;
  margin: 40px auto;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

h2 {
  font-size: 24px;
  text-align: center;
  margin-bottom: 8px;
}

.subtitle {
  text-align: center;
  font-size: 14px;
  color: #999;
  margin-bottom: 32px;
}

.field {
  margin-bottom: 20px;
}

.field label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.field input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 15px;
  outline: none;
  transition: border-color 0.2s;
}

.field input:focus {
  border-color: #5b7fff;
}

.error {
  color: #ff4d4f;
  font-size: 14px;
  margin-bottom: 16px;
}

button {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 8px;
  background: linear-gradient(135deg, #5b7fff 0%, #7b9fff 100%);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
}

button:hover:not(:disabled) {
  opacity: 0.9;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>