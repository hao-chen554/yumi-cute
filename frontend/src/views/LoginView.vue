<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { BaseButton, BaseCard, BaseInput, toast } from '@/components/base'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')

async function handleLogin() {
  if (loading.value) return

  errorMsg.value = ''
  loading.value = true
  try {
    await userStore.login(form.value)
    toast.success('欢迎回来')
    // 从别的页面被拦过来的，就跳回那里；否则去首页
    router.push(route.query.redirect || '/')
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto flex w-full max-w-md items-center justify-center min-h-[calc(100vh-15rem)]">
    <div class="relative w-full">
      <!-- 背景装饰：一团很淡的品牌色光晕 -->
      <div
        class="pointer-events-none absolute -top-24 left-1/2 size-72 -translate-x-1/2 rounded-full bg-brand-200/40 blur-3xl"
        aria-hidden="true"
      />

    <BaseCard padding="lg" class="relative">
      <div class="mb-8 text-center">
        <div class="mb-3 text-4xl select-none">🐾</div>
        <h1 class="text-2xl font-bold tracking-tight text-ink-900">欢迎回来</h1>
        <p class="mt-2 text-sm text-ink-400">登录 YuMi-cute，继续为你和它留下写真</p>
      </div>

      <form class="space-y-5" @submit.prevent="handleLogin">
        <BaseInput
          v-model="form.username"
          label="用户名"
          placeholder="请输入用户名"
          autocomplete="username"
        />

        <BaseInput
          v-model="form.password"
          type="password"
          label="密码"
          placeholder="请输入密码"
          autocomplete="current-password"
          @enter="handleLogin"
        />

        <div
          v-if="errorMsg"
          class="flex items-start gap-2 rounded-xl bg-danger-soft px-4 py-3"
          role="alert"
        >
          <span class="mt-0.5 text-danger">!</span>
          <p class="text-sm leading-snug text-danger">{{ errorMsg }}</p>
        </div>

        <BaseButton type="submit" size="lg" block :loading="loading">
          {{ loading ? '登录中…' : '登录' }}
        </BaseButton>
      </form>

      <p class="mt-6 text-center text-sm text-ink-400">
        还没有账号？
        <RouterLink to="/register" class="font-semibold text-brand-600 hover:text-brand-700">
          立即注册
        </RouterLink>
      </p>
    </BaseCard>
    </div>
  </div>
</template>
