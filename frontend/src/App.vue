<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const router = useRouter()

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app">
    <nav class="navbar">
      <RouterLink to="/" class="logo">🐾 YuMi-cute</RouterLink>

      <div class="links">
        <RouterLink to="/">首页</RouterLink>

        <template v-if="userStore.isLogin">
          <RouterLink to="/create">开始创作</RouterLink>
          <RouterLink to="/orders">我的作品</RouterLink>
          <RouterLink to="/profile">我的</RouterLink>
          <span class="points">💎 {{ userStore.userInfo?.points ?? 0 }}</span>
          <span class="nickname">{{ userStore.userInfo?.nickname }}</span>
          <a class="logout" @click="handleLogout">退出</a>
        </template>

        <template v-else>
          <RouterLink to="/register">注册</RouterLink>
          <RouterLink to="/login">登录</RouterLink>
        </template>
      </div>
    </nav>

    <main class="main">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.app { min-height: 100vh; }

.navbar {
  display: flex; align-items: center; justify-content: space-between;
  height: 64px; padding: 0 32px; background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  position: sticky; top: 0; z-index: 10;
}

.logo { font-size: 20px; font-weight: 700; color: #5b7fff; }

.links { display: flex; align-items: center; gap: 24px; }

.links a { font-size: 15px; color: #666; cursor: pointer; }
.links a:hover { color: #5b7fff; }
.links a.router-link-active { color: #5b7fff; font-weight: 600; }

.points {
  font-size: 14px; color: #ff7849; font-weight: 600;
  background: #fff5f0; padding: 4px 12px; border-radius: 20px;
}

.nickname { font-size: 15px; color: #333; }

.logout { color: #999 !important; }

.main { max-width: 1200px; margin: 0 auto; padding: 40px 32px; }
</style>