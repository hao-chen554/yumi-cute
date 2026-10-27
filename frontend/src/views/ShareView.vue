<script setup>
import {ref, onMounted} from 'vue'
import {useRoute} from 'vue-router'
import {viewShare} from '@/api/share'

const route = useRoute()

const data = ref(null)
const loading = ref(true)
const error = ref('')
const previewUrl = ref(null)

onMounted(async () => {
  try {
    data.value = await viewShare(route.params.token)
  } catch (e) {
    error.value = e.message || '分享链接不存在或已失效'
  } finally {
    loading.value = false
  }
})

/** 后端返回的是 ISO 时间（2026-10-29T08:39:05），显示成 2026-10-29 08:39 */
function fmtTime(s) {
  return s ? s.replace('T', ' ').slice(0, 16) : ''
}

function preview(url) {
  previewUrl.value = url
}
</script>

<template>
  <div class="share-page">

    <div v-if="loading" class="state">加载中…</div>

    <div v-else-if="error" class="state">
      <div class="icon">🔗</div>
      <p class="err">{{ error }}</p>
      <p class="hint">链接可能已被作者取消分享，或者已经过期。</p>
      <RouterLink to="/" class="cta">去看看 YuMi</RouterLink>
    </div>

    <template v-else>
      <header class="share-header">
        <div class="brand">YuMi</div>
        <h1>{{ data.styleName }}</h1>
        <p class="meta">
          {{ data.images.length }} 张 · 有效期至 {{ fmtTime(data.expireAt) }}
        </p>
      </header>

      <div class="grid">
        <img
            v-for="(u, i) in data.images"
            :key="i"
            :src="u"
            :alt="`第 ${i + 1} 张`"
            @click="preview(u)"
        />
      </div>

      <footer class="share-footer">
        <p>这是别人分享给你的人宠写真</p>
        <RouterLink to="/" class="cta">我也想为自己和它做一套 →</RouterLink>
      </footer>

      <div v-if="previewUrl" class="preview" @click="previewUrl = null">
        <img :src="previewUrl" alt=""/>
      </div>
    </template>

  </div>
</template>

<style scoped>
.share-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 40px 20px 60px;
}

.state {
  text-align: center;
  padding: 100px 20px;
  color: #999;
}

.state .icon {
  font-size: 44px;
  margin-bottom: 16px;
}

.state .err {
  color: #ff4d4f;
  font-size: 16px;
  margin-bottom: 10px;
}

.state .hint {
  font-size: 13px;
  color: #bbb;
  margin-bottom: 26px;
}

.share-header {
  text-align: center;
  margin-bottom: 32px;
}

.brand {
  display: inline-block;
  font-size: 13px;
  letter-spacing: 2px;
  color: #ff7849;
  font-weight: 700;
  margin-bottom: 14px;
}

.share-header h1 {
  font-size: 26px;
  margin-bottom: 10px;
}

.share-header .meta {
  font-size: 13px;
  color: #bbb;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.grid img {
  width: 100%;
  height: 260px;
  object-fit: cover;
  border-radius: 12px;
  cursor: zoom-in;
  background: #f7f8fa;
  transition: transform .18s;
}

.grid img:hover {
  transform: scale(1.015);
}

.share-footer {
  margin-top: 48px;
  text-align: center;
}

.share-footer p {
  font-size: 13px;
  color: #bbb;
  margin-bottom: 14px;
}

.cta {
  display: inline-block;
  padding: 10px 24px;
  border-radius: 22px;
  background: #ff7849;
  color: #fff;
  font-size: 14px;
  text-decoration: none;
}

.cta:hover {
  background: #f06a3c;
}

.preview {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .82);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
  cursor: zoom-out;
  padding: 40px;
}

.preview img {
  max-width: 100%;
  max-height: 100%;
  border-radius: 8px;
}

@media (max-width: 720px) {
  .grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .grid img {
    height: 180px;
  }
}
</style>
