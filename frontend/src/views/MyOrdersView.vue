<script setup>
import { ref, onMounted } from 'vue'
import { getMyOrders } from '@/api/order'

const orders = ref([])
const loading = ref(true)
const errorMsg = ref('')

const STATUS_TEXT = {
  0: '待生成',
  1: '生成中',
  2: '已完成',
  3: '部分失败',
  4: '生成失败',
  5: '已取消'
}

const STATUS_CLASS = {
  0: 'pending',
  1: 'running',
  2: 'done',
  3: 'partial',
  4: 'failed',
  5: 'canceled'
}

onMounted(async () => {
  try {
    orders.value = await getMyOrders()
  } catch (e) {
    errorMsg.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})

function fmtTime(t) {
  return t ? t.replace('T', ' ').slice(0, 16) : ''
}
</script>

<template>
  <div class="orders">
    <h2>我的作品</h2>
    <p class="subtitle">这里是你和它的每一套写真</p>

    <p v-if="loading" class="tip">加载中...</p>
    <p v-else-if="errorMsg" class="error">{{ errorMsg }}</p>

    <div v-else-if="orders.length === 0" class="empty">
      <div class="icon">🐾</div>
      <p>还没有作品</p>
      <RouterLink to="/create" class="btn">去创作第一套</RouterLink>
    </div>

    <div v-else class="list">
      <RouterLink
          v-for="o in orders"
          :key="o.id"
          :to="`/orders/${o.id}`"
          class="card"
      >
        <div class="left">
          <div class="style-name">{{ o.styleName }}</div>
          <div class="meta">
            <span>{{ o.orderType === 1 ? '试看单' : '整套单' }}</span>
            <span>·</span>
            <span>{{ o.successCount }} / {{ o.totalCount }} 张</span>
            <span>·</span>
            <span>💎 {{ o.pointsCost }}</span>
          </div>
          <div class="order-no">{{ o.orderNo }}</div>
        </div>

        <div class="right">
          <span class="badge" :class="STATUS_CLASS[o.status]">
            {{ STATUS_TEXT[o.status] }}
          </span>
          <div class="time">{{ fmtTime(o.createTime) }}</div>
        </div>
      </RouterLink>
    </div>
  </div>
</template>

<style scoped>
.orders { max-width: 860px; margin: 0 auto; }

h2 { font-size: 26px; margin-bottom: 8px; }
.subtitle { font-size: 14px; color: #999; margin-bottom: 32px; }

.list { display: flex; flex-direction: column; gap: 14px; }

.card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.25s;
}

.card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.09);
}

.style-name { font-size: 17px; font-weight: 600; margin-bottom: 8px; }

.meta {
  display: flex;
  gap: 8px;
  font-size: 13px;
  color: #888;
  margin-bottom: 6px;
}

.order-no {
  font-family: monospace;
  font-size: 12px;
  color: #bbb;
}

.right { text-align: right; }

.badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.badge.pending { background: #f0f2f8; color: #888; }
.badge.running { background: #eef4ff; color: #5b7fff; }
.badge.done { background: #eaf8ee; color: #2fa85a; }
.badge.partial { background: #fff7e6; color: #d48806; }
.badge.failed { background: #fff1f0; color: #ff4d4f; }
.badge.canceled { background: #f5f5f5; color: #aaa; }

.time { font-size: 12px; color: #bbb; margin-top: 8px; }

.empty {
  text-align: center;
  padding: 80px 0;
  background: #fff;
  border-radius: 16px;
}

.empty .icon { font-size: 48px; margin-bottom: 16px; }
.empty p { color: #999; margin-bottom: 24px; }

.btn {
  display: inline-block;
  padding: 12px 36px;
  border-radius: 8px;
  background: linear-gradient(135deg, #5b7fff 0%, #7b9fff 100%);
  color: #fff;
  font-size: 15px;
}

.tip { color: #999; text-align: center; padding: 60px 0; }
.error { color: #ff4d4f; text-align: center; padding: 60px 0; }
</style>