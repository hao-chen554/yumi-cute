<script setup>
import { ref, watch } from 'vue'
import { RouterLink, RouterView, useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout as logoutApi } from '@/api/user'
import { BaseButton, ToastHost, toast } from '@/components/base'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const menuOpen = ref(false)

// 路由一变就收起移动端菜单，否则点完链接菜单还挂着
watch(() => route.fullPath, () => {
  menuOpen.value = false
})

async function handleLogout() {
  try {
    await logoutApi()
  } catch (e) {
    // 服务端拉黑失败也要让本地登出，否则用户被困在已登录状态
  } finally {
    userStore.logout()
    menuOpen.value = false
    toast.success('已退出登录')
    router.push('/login')
  }
}
</script>

<template>
  <div class="flex min-h-screen flex-col">
    <!-- ============ 顶部导航 ============ -->
    <header class="sticky top-0 z-40 border-b border-ink-200 bg-surface/85 backdrop-blur">
      <nav
        class="mx-auto flex h-16 max-w-[1200px] items-center justify-between gap-4 px-4 sm:px-6 lg:px-8"
      >
        <RouterLink to="/" class="flex shrink-0 items-center gap-2">
          <span class="text-xl leading-none">🐾</span>
          <span class="text-lg font-bold tracking-tight text-brand-600">YuMi-cute</span>
        </RouterLink>

        <!-- 桌面端导航 -->
        <div class="hidden items-center gap-1 md:flex">
          <RouterLink to="/" class="nav-link" exact-active-class="nav-link-current">
            首页
          </RouterLink>
          <template v-if="userStore.isLogin">
            <RouterLink to="/create" class="nav-link" active-class="nav-link-current">
              开始创作
            </RouterLink>
            <RouterLink to="/orders" class="nav-link" active-class="nav-link-current">
              我的作品
            </RouterLink>
            <RouterLink to="/recharge" class="nav-link" active-class="nav-link-current">
              充值
            </RouterLink>
            <RouterLink to="/profile" class="nav-link" active-class="nav-link-current">
              我的
            </RouterLink>
          </template>
        </div>

        <!-- 桌面端右侧 -->
        <div class="hidden items-center gap-3 md:flex">
          <template v-if="userStore.isLogin">
            <RouterLink to="/recharge" class="points-pill">
              💎 <span class="tabular-nums">{{ userStore.userInfo?.points ?? 0 }}</span>
            </RouterLink>
            <span class="max-w-[8rem] truncate text-sm text-ink-600">
              {{ userStore.userInfo?.nickname }}
            </span>
            <BaseButton variant="ghost" size="sm" @click="handleLogout">退出</BaseButton>
          </template>
          <template v-else>
            <BaseButton variant="ghost" size="sm" @click="router.push('/login')">登录</BaseButton>
            <BaseButton size="sm" @click="router.push('/register')">注册</BaseButton>
          </template>
        </div>

        <!-- 移动端右侧 -->
        <div class="flex items-center gap-2 md:hidden">
          <RouterLink v-if="userStore.isLogin" to="/recharge" class="points-pill">
            💎 <span class="tabular-nums">{{ userStore.userInfo?.points ?? 0 }}</span>
          </RouterLink>
          <button
            type="button"
            class="grid size-10 place-items-center rounded-xl text-ink-600 transition-colors hover:bg-ink-100"
            :aria-expanded="menuOpen"
            aria-label="菜单"
            @click="menuOpen = !menuOpen"
          >
            <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path v-if="!menuOpen" d="M4 7h16M4 12h16M4 17h16" stroke-linecap="round" />
              <path v-else d="M6 6l12 12M18 6L6 18" stroke-linecap="round" />
            </svg>
          </button>
        </div>
      </nav>

      <!-- 移动端下拉菜单 -->
      <Transition name="pop">
        <div v-if="menuOpen" class="border-t border-ink-200 bg-surface px-4 py-3 md:hidden">
          <div class="flex flex-col gap-1">
            <RouterLink to="/" class="nav-link" exact-active-class="nav-link-current">
              首页
            </RouterLink>
            <template v-if="userStore.isLogin">
              <RouterLink to="/create" class="nav-link" active-class="nav-link-current">
                开始创作
              </RouterLink>
              <RouterLink to="/orders" class="nav-link" active-class="nav-link-current">
                我的作品
              </RouterLink>
              <RouterLink to="/recharge" class="nav-link" active-class="nav-link-current">
                充值
              </RouterLink>
              <RouterLink to="/profile" class="nav-link" active-class="nav-link-current">
                我的
              </RouterLink>
              <button
                type="button"
                class="nav-link text-left text-danger hover:bg-danger-soft hover:text-danger"
                @click="handleLogout"
              >
                退出登录
              </button>
            </template>
            <template v-else>
              <RouterLink to="/login" class="nav-link">登录</RouterLink>
              <RouterLink to="/register" class="nav-link">注册</RouterLink>
            </template>
          </div>
        </div>
      </Transition>
    </header>

    <!-- ============ 主内容 ============ -->
    <main class="mx-auto w-full max-w-[1200px] flex-1 px-4 py-8 sm:px-6 sm:py-10 lg:px-8">
      <RouterView v-slot="{ Component }">
        <Transition name="fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>

    <!-- ============ 页脚 ============ -->
    <footer class="border-t border-ink-200 py-6">
      <p class="text-center text-xs text-ink-400">
        🐾 YuMi-cute · 为每一个把宠物当家人的你
      </p>
    </footer>

    <ToastHost />
  </div>
</template>
