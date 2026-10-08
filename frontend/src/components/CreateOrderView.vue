<script setup>
import { ref, computed, onMounted } from 'vue'
import { getStyleList } from '@/api/style'
import { createOrder } from '@/api/order'
import { getMyInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'
import PhotoUploader from '@/components/PhotoUploader.vue'

const userStore = useUserStore()

const styles = ref([])
const loading = ref(false)
const errorMsg = ref('')
const orderNo = ref('')

const form = ref({
  styleId: null,
  personPhotoUrl: '',
  petPhotoUrl: '',
  orderType: 2        // 2 = 整套，1 = 试看
})

const selectedStyle = computed(() =>
    styles.value.find(s => s.id === form.value.styleId)
)

// ⚠️ 注意：这是"前端预估价"，真正扣多少以后端为准。
// 业务规则在前端后端各写一遍是技术债，后面会重构掉。
const cost = computed(() => {
  const s = selectedStyle.value
  if (!s) return 0
  return form.value.orderType === 1
      ? Math.max(1, Math.floor(s.pointsCost / 4))
      : s.pointsCost
})

const photoCount = computed(() => (form.value.orderType === 1 ? 1 : 6))

const canSubmit = computed(() =>
    !!form.value.styleId &&
    !!form.value.personPhotoUrl &&
    !!form.value.petPhotoUrl &&
    !loading.value
)

onMounted(async () => {
  try {
    styles.value = await getStyleList()
  } catch (e) {
    errorMsg.value = e.message || '风格加载失败'
  }
})

function selectStyle(id) {
  form.value.styleId = id
}

async function handleSubmit() {
  errorMsg.value = ''
  loading.value = true
  try {
    orderNo.value = await createOrder({ ...form.value })

    // 下单后算力变了，重新拉一次刷到导航栏
    const info = await getMyInfo()
    if (userStore.userInfo) {
      userStore.userInfo.points = info.points
    }
  } catch (e) {
    errorMsg.value = e.message || '下单失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="create">

    <!-- 下单成功 -->
    <div v-if="orderNo" class="success">
      <div class="icon">🎉</div>
      <h2>下单成功</h2>
      <p class="order-no">订单号：{{ orderNo }}</p>
      <p class="desc">
        已消耗 <b>{{ cost }}</b> 算力，正在为你生成 <b>{{ photoCount }}</b> 张写真
      </p>
      <p class="hint">生成需要一点时间。订单进度页面我们下一轮做。</p>
      <RouterLink to="/" class="btn">回到首页</RouterLink>
    </div>

    <!-- 下单表单 -->
    <div v-else class="form-wrap">
      <h2>开始创作</h2>
      <p class="subtitle">三步：选风格 → 传照片 → 点生成</p>

      <!-- 选风格 -->
      <section>
        <div class="section-title"><span class="step">1</span>选择风格</div>
        <div class="style-grid">
          <div
              v-for="s in styles"
              :key="s.id"
              class="style-card"
              :class="{ active: form.styleId === s.id }"
              @click="selectStyle(s.id)"
          >
            <div class="name">{{ s.name }}</div>
            <div class="desc">{{ s.description }}</div>
            <div class="price">💎 {{ s.pointsCost }}</div>
          </div>
        </div>
      </section>

      <!-- 传照片 -->
      <section>
        <div class="section-title"><span class="step">2</span>上传照片</div>
        <div class="uploaders">
          <PhotoUploader
              v-model="form.personPhotoUrl"
              label="你的照片"
              tip="清晰的正脸照效果最好"
          />
          <PhotoUploader
              v-model="form.petPhotoUrl"
              label="宠物的照片"
              tip="尽量正面、光线充足"
          />
        </div>
      </section>

      <!-- 选类型 -->
      <section>
        <div class="section-title"><span class="step">3</span>选择生成方式</div>
        <div class="type-row">
          <div
              class="type-card"
              :class="{ active: form.orderType === 2 }"
              @click="form.orderType = 2"
          >
            <div class="name">整套写真</div>
            <div class="desc">生成 6 张，风格统一</div>
          </div>
          <div
              class="type-card"
              :class="{ active: form.orderType === 1 }"
              @click="form.orderType = 1"
          >
            <div class="name">先试看一张</div>
            <div class="desc">只花 1/4 的算力，满意再出整套</div>
          </div>
        </div>
      </section>

      <!-- 提交 -->
      <div class="submit-bar">
        <div class="summary">
          <template v-if="selectedStyle">
            已选「{{ selectedStyle.name }}」 · 生成 {{ photoCount }} 张
            · 将消耗 <b>{{ cost }}</b> 算力
          </template>
          <template v-else>请先选择一个风格</template>
        </div>

        <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

        <button :disabled="!canSubmit" @click="handleSubmit">
          {{ loading ? '提交中...' : '开始生成' }}
        </button>
      </div>
    </div>

  </div>
</template>

<style scoped>
.create {
  max-width: 860px;
  margin: 0 auto;
}

h2 { font-size: 26px; margin-bottom: 8px; }
.subtitle { font-size: 14px; color: #999; margin-bottom: 36px; }

section { margin-bottom: 40px; }

.section-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 17px;
  font-weight: 600;
  margin-bottom: 18px;
}

.step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #5b7fff;
  color: #fff;
  font-size: 13px;
}

/* 风格卡片 */
.style-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
}

.style-card {
  background: #fff;
  border: 2px solid #eef0f4;
  border-radius: 12px;
  padding: 16px;
  cursor: pointer;
  transition: all 0.2s;
}

.style-card:hover { border-color: #b8c9ff; }

.style-card.active {
  border-color: #5b7fff;
  background: #f5f8ff;
  box-shadow: 0 4px 14px rgba(91, 127, 255, 0.15);
}

.style-card .name { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.style-card .desc {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
  height: 36px;
  overflow: hidden;
}
.style-card .price { font-size: 13px; color: #ff7849; font-weight: 600; }

/* 上传区 */
.uploaders { display: flex; gap: 20px; }

/* 类型选择 */
.type-row { display: flex; gap: 16px; }

.type-card {
  flex: 1;
  background: #fff;
  border: 2px solid #eef0f4;
  border-radius: 12px;
  padding: 18px 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.type-card:hover { border-color: #b8c9ff; }

.type-card.active {
  border-color: #5b7fff;
  background: #f5f8ff;
}

.type-card .name { font-size: 15px; font-weight: 600; margin-bottom: 6px; }
.type-card .desc { font-size: 12px; color: #999; }

/* 提交栏 */
.submit-bar {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.summary { font-size: 14px; color: #666; margin-bottom: 16px; }
.summary b { color: #ff7849; font-size: 16px; }

.error { color: #ff4d4f; font-size: 14px; margin-bottom: 12px; }

button {
  width: 100%;
  height: 48px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #5b7fff 0%, #7b9fff 100%);
  color: #fff;
  font-size: 16px;
  cursor: pointer;
  transition: all 0.3s;
}

button:hover:not(:disabled) { opacity: 0.92; }
button:disabled { opacity: 0.45; cursor: not-allowed; }

/* 成功页 */
.success {
  max-width: 520px;
  margin: 60px auto;
  background: #fff;
  border-radius: 16px;
  padding: 48px 32px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.success .icon { font-size: 56px; margin-bottom: 16px; }
.success h2 { margin-bottom: 20px; }

.order-no {
  font-family: monospace;
  font-size: 15px;
  color: #5b7fff;
  background: #f5f8ff;
  padding: 10px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.success .desc { font-size: 15px; color: #666; margin-bottom: 10px; }
.success .desc b { color: #ff7849; }
.success .hint { font-size: 13px; color: #bbb; margin-bottom: 28px; }

.btn {
  display: inline-block;
  padding: 12px 40px;
  border-radius: 8px;
  background: linear-gradient(135deg, #5b7fff 0%, #7b9fff 100%);
  color: #fff;
  font-size: 15px;
}
</style>