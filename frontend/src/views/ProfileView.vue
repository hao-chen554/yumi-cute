<script setup>
import { ref, onMounted } from 'vue'
import { getMyInfo, updateAvatar } from '@/api/user'
import { uploadFile } from '@/api/file'
import { useUserStore } from '@/stores/user'
import {logout as logoutApi} from '@/api/user'
import router from "@/router/index.js";

const userStore = useUserStore()

const user = ref(null)
const loading = ref(true)
const errorMsg = ref('')
const uploading = ref(false)
const fileInput = ref(null)

onMounted(async () => {
  try {
    user.value = await getMyInfo()
  } catch (e) {
    errorMsg.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function chooseFile() {
  fileInput.value.click()
}

async function handleFileChange(event) {
  const file = event.target.files[0]
  if (!file) return

  uploading.value = true
  try {
    const url = await uploadFile(file)
    await updateAvatar(url)

    user.value.avatarUrl = url
    if (userStore.userInfo) {
      userStore.userInfo.avatarUrl = url
    }
  } catch (e) {
    alert(e.message || '上传失败')
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

async function handleLogout() {
  try {
    await logoutApi()          // 先把 token 拉黑
  } catch (e) {
    // 网络失败也要让本地登出，否则用户被困住
  } finally {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
  }
}
</script>

<template>
  <div class="profile">
    <h2>个人中心</h2>

    <p v-if="loading" class="tip">加载中...</p>
    <p v-else-if="errorMsg" class="error">{{ errorMsg }}</p>

    <div v-else class="info">
      <div class="avatar-row">
        <img
            v-if="user.avatarUrl"
            :src="user.avatarUrl"
            class="avatar"
            alt="头像"
        />
        <div v-else class="avatar avatar-empty">🐾</div>

        <button class="upload-btn" :disabled="uploading" @click="chooseFile">
          {{ uploading ? '上传中...' : '更换头像' }}
        </button>

        <input
            ref="fileInput"
            type="file"
            accept="image/*"
            class="hidden-input"
            @change="handleFileChange"
        />
      </div>

      <div class="row"><span>用户名</span><b>{{ user.username }}</b></div>
      <div class="row"><span>昵称</span><b>{{ user.nickname }}</b></div>
      <div class="row">
        <span>算力</span>
        <span class="points-wrap">
          <b class="points">💎 {{ user.points }}</b>
          <router-link to="/recharge" class="recharge-link">充值</router-link>
        </span>
      </div>
      <div class="row"><span>注册时间</span><b>{{ user.createTime }}</b></div>
    </div>
  </div>
</template>

<style scoped>
.profile {
  max-width: 560px;
  margin: 40px auto;
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

h2 { font-size: 22px; margin-bottom: 28px; }

.avatar-row {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
}

.avatar {
  width: 96px;
  height: 96px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #f0f2f8;
}

.avatar-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f8;
  font-size: 36px;
}

.upload-btn {
  padding: 8px 20px;
  border: 1px solid #5b7fff;
  border-radius: 8px;
  background: #fff;
  color: #5b7fff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.upload-btn:hover:not(:disabled) { background: #f0f5ff; }
.upload-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.hidden-input { display: none; }

.row {
  display: flex;
  justify-content: space-between;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 15px;
}

.row span { color: #888; }
.points { color: #ff7849; }

.tip { color: #999; text-align: center; padding: 40px 0; }
.error { color: #ff4d4f; text-align: center; padding: 40px 0; }

.points-wrap { display: inline-flex; align-items: center; gap: 10px; }
.recharge-link {
  font-size: 12px; color: #ff7849; text-decoration: none;
  border: 1px solid #ffd7c2; border-radius: 20px; padding: 2px 10px;
}
.recharge-link:hover { background: #fff3ec; }
</style>