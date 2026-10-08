<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail } from '@/api/order'

const route = useRoute()

const order = ref(null)
const loading = ref(true)
const errorMsg = ref('')

const STATUS_TEXT = {
  0: '待生成', 1: '生成中', 2: '已完成', 3: '部分失败', 4: '生成失败', 5: '已取消'
}
const STATUS_CLASS = {
  0: 'pending', 1: 'running', 2: 'done', 3: 'partial', 4: 'failed', 5: 'canceled'
}
const TASK_TEXT = { 0: '排队中', 1: '生成中', 2: '已完成', 3: '失败' }

/** 终态：到了这些状态就不用再轮询了 */
const FINAL_STATUS = [2, 3, 4, 5]

let timer = null

async function load() {
  try {
    order.value = await getOrderDetail(route.params.id)
  } catch (e) {
    errorMsg.value = e.message || '加载失败'
    stopPolling()
  } finally {
    loading.value = false
  }
}

function startPolling() {
  timer = setInterval(() => {
    load()
    if (order.value && FINAL_STATUS.includes(order.value.status)) {
      stopPolling()
    }
  }, 2000)
}

function stopPolling() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(async () => {
  await load()
  if (order.value && !FINAL_STATUS.includes(order.value.status)) {
    startPolling()
  }
})

// 组件被销毁前，一定要把定时器清掉
onUnmounted(stopPolling)
</script>

<template>
  <div class="detail">

    <p v-if="loading" class="tip">加载中...</p>
    <p v-else-if="errorMsg" class="error">{{ errorMsg }}</p>

    <template v-else>
      <RouterLink to="/orders" class="back">← 返回我的作品</RouterLink>

      <div class="head">
        <h2>{{ order.styleName }}</h2>
        <span class="badge" :class="STATUS_CLASS[order.status]">
          {{ STATUS_TEXT[order.status] }}
        </span>
      </div>

      <div class="info">
        <div class="row"><span>订单号</span><b>{{ order.orderNo }}</b></div>
        <div class="row"><span>类型</span><b>{{ order.orderType === 1 ? '试看单' : '整套单' }}</b></div>
        <div class="row"><span>进度</span><b>{{ order.successCount }} / {{ order.totalCount }} 张</b></div>
        <div class="row"><span>消耗算力</span><b class="points">💎 {{ order.pointsCost }}</b></div>
      </div>

      <h3>原图</h3>
      <div class="originals">
        <div class="original">
          <img :src="order.personPhotoUrl" alt="本人照片" />
          <span>你的照片</span>
        </div>
        <div class="original">
          <img :src="order.petPhotoUrl" alt="宠物照片" />
          <span>宠物的照片</span>
        </div>
      </div>

      <h3>生成结果</h3>
      <div class="tasks">
        <div v-for="t in order.tasks" :key="t.seqNo" class="task">
          <img v-if="t.imageUrl" :src="t.imageUrl" alt="" />
          <div v-else class="placeholder">
            <span>{{ TASK_TEXT[t.status] }}</span>
          </div>
          <div class="seq">第 {{ t.seqNo }} 张</div>
        </div>
      </div>

      <p class="note">
        页面每 2 秒自动刷新一次进度。
        现在是模拟生成（每张约 2 秒，有 12% 概率失败），接上真实 AI 后这里会换成真图。
      </p>
    </template>

  </div>
</template>

<style scoped>
.detail { max-width: 900px; margin: 0 auto; }

.back {
  display: inline-block;
  font-size: 14px;
  color: #999;
  margin-bottom: 24px;
}

.back:hover { color: #5b7fff; }

.head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

h2 { font-size: 26px; }

h3 {
  font-size: 17px;
  margin: 36px 0 16px;
}

.info {
  background: #fff;
  border-radius: 14px;
  padding: 8px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.row {
  display: flex;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 14px;
}

.row:last-child { border-bottom: none; }
.row span { color: #999; }
.points { color: #ff7849; }

.originals { display: flex; gap: 16px; }

.original {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  text-align: center;
}

.original img {
  width: 100%;
  height: 220px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 10px;
}

.original span { font-size: 13px; color: #999; }

.tasks {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.task {
  background: #fff;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.task img {
  width: 100%;
  height: 200px;
  object-fit: cover;
  border-radius: 8px;
}

.placeholder {
  height: 200px;
  border-radius: 8px;
  background: #f7f8fa;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #bbb;
  font-size: 14px;
}

.seq {
  font-size: 12px;
  color: #bbb;
  text-align: center;
  margin-top: 10px;
}

.note {
  margin-top: 32px;
  font-size: 13px;
  color: #bbb;
  text-align: center;
}

.badge {
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

.tip { color: #999; text-align: center; padding: 60px 0; }
.error { color: #ff4d4f; text-align: center; padding: 60px 0; }
</style>