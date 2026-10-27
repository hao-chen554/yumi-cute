<script setup>
import {ref, computed, onMounted, onUnmounted} from 'vue'
import {useRoute} from 'vue-router'
import {getOrderDetail,deleteOrder} from '@/api/order'
import {downloadOrderZip, downloadOneImage, filenameFrom, saveBlob} from '@/api/download'
import {getShare, createShare, cancelShare} from '@/api/share'

const route = useRoute()

const order = ref(null)
const loading = ref(true)
const errorMsg = ref('')
const downloading = ref(false)

/* ---------- 分享 ---------- */
const shareInfo = ref(null)      // { token, expireAt } 或 null
const shareOpen = ref(false)
const shareBusy = ref(false)

/** 分享链接由前端拼：后端不该知道前端部署在哪个域名/端口 */
const shareUrl = computed(() =>
    shareInfo.value ? `${window.location.origin}/share/${shareInfo.value.token}` : ''
)

const STATUS_TEXT = {
  0: '待生成', 1: '生成中', 2: '已完成', 3: '部分失败', 4: '生成失败', 5: '已取消'
}
const STATUS_CLASS = {
  0: 'pending', 1: 'running', 2: 'done', 3: 'partial', 4: 'failed', 5: 'canceled'
}
const TASK_TEXT = {0: '排队中', 1: '生成中', 2: '已完成', 3: '失败'}

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

/** 整套下载：后端把成功的那些图打成一个 zip 流回来 */
async function handleDownloadAll() {
  if (downloading.value) return
  downloading.value = true
  try {
    const resp = await downloadOrderZip(order.value.id)
    const fallback = `${order.value.styleName || '写真'}-${order.value.orderNo}.zip`
    saveBlob(resp.data, filenameFrom(resp, fallback))
  } catch (e) {
    alert(e.message || '下载失败')
  } finally {
    downloading.value = false
  }
}

/** 单张下载 */
async function handleDownloadOne(task) {
  try {
    const resp = await downloadOneImage(order.value.id, task.id)
    const fallback = `${order.value.styleName || '写真'}-${task.seqNo}.jpg`
    saveBlob(resp.data, filenameFrom(resp, fallback))
  } catch (e) {
    alert(e.message || '下载失败')
  }
}

/** 打开分享面板时先查现状：有链接就展示，没有才让用户生成 */
async function openSharePanel() {
  shareOpen.value = true
  shareBusy.value = true
  try {
    shareInfo.value = await getShare(order.value.id)
  } catch (e) {
    alert(e.message || '查询分享状态失败')
  } finally {
    shareBusy.value = false
  }
}

async function handleCreateShare() {
  if (shareBusy.value) return
  shareBusy.value = true
  try {
    shareInfo.value = await createShare(order.value.id)
    await copyShareUrl(true)     // 生成后顺手复制，省一步操作
  } catch (e) {
    alert(e.message || '生成分享链接失败')
  } finally {
    shareBusy.value = false
  }
}

async function copyShareUrl(silent) {
  const url = shareUrl.value
  if (!url) return
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url)
    } else {
      // 兜底：非安全上下文（http 且非 localhost）下 clipboard API 不可用
      const ta = document.createElement('textarea')
      ta.value = url
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.select()
      document.execCommand('copy')
      ta.remove()
    }
    if (!silent) alert('链接已复制')
  } catch (e) {
    alert('复制失败，请手动选中链接复制')
  }
}

async function handleCancelShare() {
  if (shareBusy.value) return
  if (!confirm('取消后，已经发出去的链接会立即失效，确定吗？')) return
  shareBusy.value = true
  try {
    await cancelShare(order.value.id)
    shareInfo.value = null
  } catch (e) {
    alert(e.message || '取消失败')
  } finally {
    shareBusy.value = false
  }
}

async function handleDelete(){
  const ok = confirm(
      '删除后这套作品会从【我的作品】里移除，已分享的链接也会立即失效。\n\n' +
      '注意：算力消费记录会保留，不会返还算力。\n\n确定删除吗？'
  )
  if(!ok) return

  try{
    await deleteOrder(order.value.id)
    alert('已删除')
    router.push('/orders')
  }catch(e){
    alert(e.message || '删除失败')
  }
}

function closeSharePanel() {
  shareOpen.value = false
}

/** 后端返回 ISO 时间，显示成 YYYY-MM-DD HH:mm */
function fmtTime(s) {
  return s ? s.replace('T', ' ').slice(0, 16) : ''
}

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

      <h3>原图 <span class="privacy-tag">🔒 订单完成后 7 天自动清除</span></h3>
      <div class="originals">
        <div class="original">
          <img v-if="order.personPhotoUrl" :src="order.personPhotoUrl" alt="本人照片" />
          <div v-else class="gone">
            <span class="lock">🔒</span>
            <span>已按隐私承诺清除</span>
          </div>
          <span>你的照片</span>
        </div>
        <div class="original">
          <img v-if="order.petPhotoUrl" :src="order.petPhotoUrl" alt="宠物照片" />
          <div v-else class="gone">
            <span class="lock">🔒</span>
            <span>已按隐私承诺清除</span>
          </div>
          <span>宠物的照片</span>
        </div>
      </div>

      <div class="result-head">
        <h3>生成结果</h3>
        <div v-if="order.successCount > 0" class="result-actions">
          <button class="btn-share" @click="openSharePanel">🔗 分享</button>
          <button
              class="btn-download-all"
              :disabled="downloading"
              @click="handleDownloadAll"
          >
            {{ downloading ? '打包中…' : '⬇ 下载整套' }}
          </button>
        </div>
      </div>
      <div class="tasks">
        <div v-for="t in order.tasks" :key="t.id || t.seqNo" class="task">
          <img v-if="t.imageUrl" :src="t.imageUrl" alt=""/>
          <div v-else class="placeholder">
            <span>{{ TASK_TEXT[t.status] }}</span>
          </div>
          <div class="seq">第 {{ t.seqNo }} 张</div>
          <button
              v-if="t.imageUrl && t.id"
              class="btn-download-one"
              @click="handleDownloadOne(t)"
          >
            下载
          </button>
        </div>
      </div>

      <p class="note">
        页面每 2 秒自动刷新一次进度。
        当前为模拟生成（ai.provider=mock），接上真实 AI 后这里会换成真图。
      </p>

      <div class="danger-zone">
        <button class="btn-delete" @click="handleDelete">删除这套作品</button>
      </div>

      <!-- 分享面板 -->
      <div v-if="shareOpen" class="mask" @click.self="closeSharePanel">
        <div class="share-panel">
          <h3>分享这套写真</h3>

          <p v-if="shareBusy" class="share-tip">处理中…</p>

          <template v-else>
            <template v-if="shareInfo">
              <p class="share-desc">
                任何拿到链接的人都能查看，无需登录。<br/>
                链接将在 <b>{{ fmtTime(shareInfo.expireAt) }}</b> 自动失效。
              </p>
              <input
                  class="share-input"
                  :value="shareUrl"
                  readonly
                  @focus="$event.target.select()"
              />
              <div class="share-actions">
                <button class="ghost" @click="handleCancelShare">取消分享</button>
                <button class="ghost" @click="handleCreateShare">重新生成</button>
                <button class="primary" @click="copyShareUrl(false)">复制链接</button>
              </div>
              <p class="share-warn">重新生成后，之前发出去的链接会立即失效。</p>
            </template>

            <template v-else>
              <p class="share-desc">
                生成一条链接，发给家人朋友看这套写真。<br/>
                只有作品会被分享，<b>你上传的原图不会被展示</b>。
              </p>
              <div class="share-actions">
                <button class="ghost" @click="closeSharePanel">取消</button>
                <button class="primary" @click="handleCreateShare">生成分享链接</button>
              </div>
            </template>
          </template>
        </div>
      </div>
    </template>

  </div>
</template>

<style scoped>
.detail {
  max-width: 900px;
  margin: 0 auto;
}

.back {
  display: inline-block;
  font-size: 14px;
  color: #999;
  margin-bottom: 24px;
}

.back:hover {
  color: #5b7fff;
}

.head {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}

h2 {
  font-size: 26px;
}

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

.row:last-child {
  border-bottom: none;
}

.row span {
  color: #999;
}

.points {
  color: #ff7849;
}

.originals {
  display: flex;
  gap: 16px;
}

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

.original span {
  font-size: 13px;
  color: #999;
}

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

.badge.pending {
  background: #f0f2f8;
  color: #888;
}

.badge.running {
  background: #eef4ff;
  color: #5b7fff;
}

.badge.done {
  background: #eaf8ee;
  color: #2fa85a;
}

.badge.partial {
  background: #fff7e6;
  color: #d48806;
}

.badge.failed {
  background: #fff1f0;
  color: #ff4d4f;
}

.badge.canceled {
  background: #f5f5f5;
  color: #aaa;
}

.tip {
  color: #999;
  text-align: center;
  padding: 60px 0;
}

.error {
  color: #ff4d4f;
  text-align: center;
  padding: 60px 0;
}

.privacy-tag {
  font-size: 12px;
  font-weight: 400;
  color: #999;
  margin-left: 8px;
}

.gone {
  height: 220px;              /* 和 .original img 的高度保持一致 */
  border-radius: 8px;
  background: #f7f8fa;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #bbb;
  font-size: 13px;
  margin-bottom: 10px;
}

.gone .lock { font-size: 28px; }

/* ---------- 下载 ---------- */

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 36px 0 16px;
}

/* 覆盖上面 h3 的 margin，避免它把按钮挤下去 */
.result-head h3 {
  margin: 0;
}

.btn-download-all {
  padding: 8px 18px;
  border: none;
  border-radius: 20px;
  background: #ff7849;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.btn-download-all:disabled {
  background: #f3b8a1;
  cursor: not-allowed;
}

.btn-download-one {
  display: block;
  width: 100%;
  margin-top: 8px;
  padding: 6px 0;
  border: 1px solid #ffd7c2;
  border-radius: 8px;
  background: #fff;
  color: #ff7849;
  font-size: 12px;
  cursor: pointer;
}

.btn-download-one:hover {
  background: #fff3ec;
}

/* ---------- 分享 ---------- */

.result-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.btn-share {
  padding: 8px 18px;
  border: 1px solid #ffd7c2;
  border-radius: 20px;
  background: #fff;
  color: #ff7849;
  font-size: 13px;
  cursor: pointer;
}

.btn-share:hover {
  background: #fff3ec;
}

.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, .45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 50;
}

.share-panel {
  width: 440px;
  background: #fff;
  border-radius: 16px;
  padding: 26px 24px;
}

.share-panel h3 {
  font-size: 17px;
  margin: 0 0 16px;
}

.share-tip {
  color: #999;
  font-size: 14px;
  padding: 20px 0;
  text-align: center;
}

.share-desc {
  font-size: 13px;
  color: #777;
  line-height: 1.7;
  margin-bottom: 14px;
}

.share-desc b {
  color: #ff7849;
}

.share-input {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 12px;
  border: 1px solid #ececec;
  border-radius: 10px;
  background: #fafafa;
  font-size: 13px;
  color: #555;
  margin-bottom: 16px;
}

.share-actions {
  display: flex;
  gap: 10px;
}

.share-actions button {
  flex: 1;
  padding: 10px 0;
  border-radius: 10px;
  font-size: 13px;
  cursor: pointer;
  border: none;
}

.share-actions .ghost {
  background: #f5f5f5;
  color: #666;
}

.share-actions .primary {
  background: #ff7849;
  color: #fff;
}

.share-warn {
  margin-top: 12px;
  font-size: 12px;
  color: #bbb;
}

.danger-zone { margin-top: 40px; text-align: center; }
.btn-delete {
  padding: 8px 20px;
  border: 1px solid #ffd7d7;
  border-radius: 20px;
  background: #fff;
  color: #ff4d4f;
  font-size: 13px;
  cursor: pointer;
}
.btn-delete:hover { background: #fff1f0; }
</style>