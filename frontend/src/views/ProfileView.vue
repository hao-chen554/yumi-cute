<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyInfo, updateAvatar, logout as logoutApi } from '@/api/user'
import { uploadFile } from '@/api/file'
import { useUserStore } from '@/stores/user'
import {
  BaseBadge,
  BaseButton,
  BaseCard,
  BaseSkeleton,
  EmptyState,
  toast
} from '@/components/base'

const router = useRouter()
const userStore = useUserStore()

const user = ref(null)
const loading = ref(true)
const errorMsg = ref('')
const uploading = ref(false)
const fileInput = ref(null)

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    user.value = await getMyInfo()
  } catch (e) {
    errorMsg.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

function chooseFile() {
  fileInput.value?.click()
}

async function handleFileChange(event) {
  const file = event.target.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const url = await uploadFile(file)
    await updateAvatar(url)

    user.value.avatarUrl = url
    if (userStore.userInfo) userStore.userInfo.avatarUrl = url
    toast.success('头像已更新')
  } catch (e) {
    toast.error(e.message || '上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

async function handleLogout() {
  try {
    await logoutApi()
  } catch (e) {
    // 服务端拉黑失败也要让本地登出，否则用户被困在已登录状态
  } finally {
    userStore.logout()
    toast.success('已退出登录')
    router.push('/login')
  }
}

function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : '—'
}
</script>

<template>
  <div class="mx-auto w-full max-w-2xl">
    <h1 class="mb-1 text-2xl font-bold tracking-tight text-ink-900">个人中心</h1>
    <p class="mb-8 text-sm text-ink-400">管理你的资料、算力与登录状态</p>

    <!-- 加载中：骨架屏 -->
    <BaseCard v-if="loading" padding="lg">
      <div class="flex flex-col items-center gap-4">
        <div class="skeleton size-24 rounded-full" />
        <BaseSkeleton size="h-4 w-24" />
      </div>
      <div class="mt-8 space-y-4">
        <BaseSkeleton v-for="i in 4" :key="i" size="h-10 w-full" />
      </div>
    </BaseCard>

    <!-- 加载失败 -->
    <BaseCard v-else-if="errorMsg" padding="none">
      <EmptyState title="资料加载失败" :description="errorMsg" emoji="⚠️">
        <template #action>
          <BaseButton @click="load">重新加载</BaseButton>
        </template>
      </EmptyState>
    </BaseCard>

    <template v-else>
      <!-- 头像卡 -->
      <BaseCard padding="lg">
        <div class="flex flex-col items-center gap-4">
          <div class="relative">
            <img
              v-if="user.avatarUrl"
              :src="user.avatarUrl"
              alt="头像"
              class="size-24 rounded-full border-4 border-ink-100 object-cover"
            />
            <div
              v-else
              class="grid size-24 place-items-center rounded-full border-4 border-ink-100 bg-ink-100 text-4xl select-none"
            >
              🐾
            </div>
          </div>

          <div class="text-center">
            <p class="text-lg font-semibold text-ink-900">{{ user.nickname }}</p>
            <p class="mt-0.5 text-sm text-ink-400">@{{ user.username }}</p>
          </div>

          <BaseButton
            variant="secondary"
            size="sm"
            :loading="uploading"
            @click="chooseFile"
          >
            {{ uploading ? '上传中…' : '更换头像' }}
          </BaseButton>

          <input
            ref="fileInput"
            type="file"
            accept="image/jpeg,image/png,image/webp"
            class="hidden"
            @change="handleFileChange"
          />
        </div>
      </BaseCard>

      <!-- 资料卡 -->
      <BaseCard padding="lg" class="mt-5">
        <dl class="divide-y divide-ink-200">
          <div class="flex items-center justify-between gap-4 py-3.5">
            <dt class="text-sm text-ink-500">用户名</dt>
            <dd class="text-sm font-medium text-ink-800">{{ user.username }}</dd>
          </div>

          <div class="flex items-center justify-between gap-4 py-3.5">
            <dt class="text-sm text-ink-500">昵称</dt>
            <dd class="text-sm font-medium text-ink-800">{{ user.nickname }}</dd>
          </div>

          <div class="flex items-center justify-between gap-4 py-3.5">
            <dt class="text-sm text-ink-500">算力</dt>
            <dd class="flex items-center gap-3">
              <BaseBadge tone="brand" size="md">
                💎 <span class="ml-1 tabular-nums">{{ user.points }}</span>
              </BaseBadge>
              <RouterLink to="/recharge">
                <BaseButton variant="secondary" size="sm">充值</BaseButton>
              </RouterLink>
            </dd>
          </div>

          <div class="flex items-center justify-between gap-4 py-3.5">
            <dt class="text-sm text-ink-500">注册时间</dt>
            <dd class="text-sm text-ink-600 tabular-nums">{{ fmtTime(user.createTime) }}</dd>
          </div>
        </dl>
      </BaseCard>

      <!-- 危险区 -->
      <div class="mt-8 text-center">
        <BaseButton variant="dangerGhost" size="sm" @click="handleLogout">
          退出登录
        </BaseButton>
      </div>
    </template>
  </div>
</template>
