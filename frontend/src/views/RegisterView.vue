<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'
import { BaseButton, BaseCard, BaseInput, toast } from '@/components/base'

const router = useRouter()

const form = ref({ username: '', nickname: '', password: '' })
const loading = ref(false)
const errorMsg = ref('')

async function handleRegister() {
  if (loading.value) return

  errorMsg.value = ''
  loading.value = true
  try {
    await register(form.value)
    toast.success('注册成功，送你 100 算力，去登录吧')
    router.push('/login')
  } catch (e) {
    errorMsg.value = e.message || '注册失败，请稍后再试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="mx-auto flex w-full max-w-md items-center justify-center min-h-[calc(100vh-15rem)]">
    <div class="relative w-full">
      <div
        class="pointer-events-none absolute -top-24 left-1/2 size-72 -translate-x-1/2 rounded-full bg-brand-200/40 blur-3xl"
        aria-hidden="true"
      />

    <BaseCard padding="lg" class="relative">
      <div class="mb-8 text-center">
        <div class="mb-3 text-4xl select-none">🐾</div>
        <h1 class="text-2xl font-bold tracking-tight text-ink-900">创建账号</h1>
        <p class="mt-2 text-sm text-ink-400">注册即送 100 算力，够你先试几套</p>
      </div>

      <form class="space-y-5" @submit.prevent="handleRegister">
        <BaseInput
          v-model="form.username"
          label="用户名"
          placeholder="4-20 位字母、数字或下划线"
          hint="登录时使用，注册后不可修改"
          autocomplete="username"
        />

        <BaseInput
          v-model="form.nickname"
          label="昵称"
          placeholder="给自己起个名字"
          autocomplete="nickname"
        />

        <BaseInput
          v-model="form.password"
          type="password"
          label="密码"
          placeholder="6-20 位"
          autocomplete="new-password"
          @enter="handleRegister"
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
          {{ loading ? '注册中…' : '立即注册' }}
        </BaseButton>
      </form>

      <p class="mt-6 text-center text-sm text-ink-400">
        已经有账号？
        <RouterLink to="/login" class="font-semibold text-brand-600 hover:text-brand-700">
          直接登录
        </RouterLink>
      </p>
    </BaseCard>
    </div>
  </div>
</template>
