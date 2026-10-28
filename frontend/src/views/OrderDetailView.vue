<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { deleteOrder } from '@/api/order'
import {
  BaseBadge,
  BaseButton,
  BaseCard,
  BaseModal,
  BaseSkeleton,
  ConfirmDialog,
  EmptyState,
  toast
} from '@/components/base'
import { useOrderPolling } from '@/composables/useOrderPolling'
import { useOrderDownload } from '@/composables/useOrderDownload'
import { useOrderShare } from '@/composables/useOrderShare'

const route = useRoute()
const router = useRouter()

/* ---------- 状态文案 / 颜色映射 ---------- */
const STATUS_TEXT = {
  0: '待生成',
  1: '生成中',
  2: '已完成',
  3: '部分失败',
  4: '生成失败',
  5: '已取消'
}

const STATUS_TONE = {
  0: 'neutral',
  1: 'info',
  2: 'success',
  3: 'warning',
  4: 'danger',
  5: 'neutral'
}

const TASK_TEXT = { 0: '排队中', 1: '生成中', 2: '已完成', 3: '失败' }

const DELETE_MESSAGE =
  '删除后这套作品会从【我的作品】里移除，已分享的链接也会立即失效。\n\n' +
  '注意：算力消费记录会保留，不会返还算力。\n\n' +
  '确定删除吗？'

/* ---------- 页面逻辑（都在 composables 里） ---------- */
const { order, loading, errorMsg, reload } = useOrderPolling(route.params.id)
const { downloading, downloadingTaskId, handleDownloadAll, handleDownloadOne } =
  useOrderDownload(order)
const {
  shareInfo,
  shareOpen,
  shareLoading,
  shareBusy,
  cancelShareOpen,
  shareUrl,
  openSharePanel,
  closeSharePanel,
  handleCreateShare,
  copyShareUrl,
  askCancelShare,
  handleCancelShare,
  fmtTime
} = useOrderShare(order)

/* ---------- 破图降级 ---------- */
/**
 * 作品图理论上永久保留，但有两种情况会加载失败：
 *  - mock 模式下结果图复用了「用户原图」的 URL，而原图满 7 天会被清理任务回收
 *  - 真实文件被误删 / 存储迁移
 * 失败时显示占位，而不是浏览器默认的破图图标。
 */
const brokenImages = ref(new Set())

function markBroken(key) {
  brokenImages.value = new Set(brokenImages.value).add(key)
}

/* ---------- 删除作品 ---------- */
const deleteOpen = ref(false)
const deleting = ref(false)

async function handleDelete() {
  if (deleting.value || !order.value) return

  // 只把「删除接口」放进 try：接口成功后的提示和导航不能被说成「删除失败」
  deleting.value = true
  try {
    await deleteOrder(order.value.id)
  } catch (e) {
    toast.error(e.message || '删除失败')
    deleting.value = false
    return
  }

  deleting.value = false
  deleteOpen.value = false
  toast.success('已删除')
  router.push('/orders')
}
</script>

<template>
  <div class="mx-auto w-full max-w-3xl">
    <!-- ============ 骨架屏 ============ -->
    <div v-if="loading" class="space-y-6" aria-busy="true">
      <div class="flex items-center gap-3">
        <BaseSkeleton size="h-8 w-48 max-w-full" />
        <BaseSkeleton size="h-6 w-16" rounded="rounded-full" />
      </div>

      <BaseCard>
        <div class="space-y-4">
          <div v-for="i in 4" :key="i" class="flex items-center justify-between gap-4">
            <BaseSkeleton size="h-3 w-16" />
            <BaseSkeleton size="h-3 w-28" />
          </div>
        </div>
      </BaseCard>

      <div class="grid grid-cols-2 gap-3 sm:gap-4">
        <BaseCard v-for="i in 2" :key="i" padding="sm">
          <BaseSkeleton size="h-40 w-full sm:h-48" rounded="rounded-xl" />
          <BaseSkeleton size="mt-2 h-3 w-20 mx-auto" />
        </BaseCard>
      </div>

      <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4">
        <BaseCard v-for="i in 3" :key="i" padding="sm">
          <BaseSkeleton size="h-40 w-full sm:h-48" rounded="rounded-xl" />
          <BaseSkeleton size="mt-2 h-3 w-14 mx-auto" />
        </BaseCard>
      </div>
    </div>

    <!-- ============ 加载失败 ============ -->
    <BaseCard v-else-if="errorMsg" padding="none">
      <EmptyState title="作品加载失败" :description="errorMsg" emoji="⚠️">
        <template #action>
          <BaseButton @click="reload">重新加载</BaseButton>
        </template>
      </EmptyState>
    </BaseCard>

    <!-- ============ 正常内容 ============ -->
    <template v-else-if="order">
      <RouterLink
        to="/orders"
        class="mb-6 inline-flex items-center gap-1 text-sm text-ink-400 transition-colors hover:text-brand-600"
      >
        <svg
          class="size-4"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="2"
          aria-hidden="true"
        >
          <path d="M15 18l-6-6 6-6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        返回我的作品
      </RouterLink>

      <div class="mb-6 flex flex-wrap items-center gap-3">
        <h1
          class="min-w-0 flex-1 break-words text-2xl font-bold tracking-tight text-ink-900 sm:text-3xl"
        >
          {{ order.styleName }}
        </h1>
        <BaseBadge :tone="STATUS_TONE[order.status] || 'neutral'">
          {{ STATUS_TEXT[order.status] || '未知' }}
        </BaseBadge>
      </div>

      <BaseCard padding="none">
        <dl class="divide-y divide-ink-100 px-5">
          <div class="flex items-center justify-between gap-4 py-3">
            <dt class="shrink-0 text-sm text-ink-400">订单号</dt>
            <dd class="min-w-0 truncate font-mono text-sm text-ink-700">{{ order.orderNo }}</dd>
          </div>
          <div class="flex items-center justify-between gap-4 py-3">
            <dt class="shrink-0 text-sm text-ink-400">类型</dt>
            <dd class="text-sm text-ink-700">{{ order.orderType === 1 ? '试看单' : '整套单' }}</dd>
          </div>
          <div class="flex items-center justify-between gap-4 py-3">
            <dt class="shrink-0 text-sm text-ink-400">进度</dt>
            <dd class="text-sm text-ink-700 tabular-nums">
              {{ order.successCount }} / {{ order.totalCount }} 张
            </dd>
          </div>
          <div class="flex items-center justify-between gap-4 py-3">
            <dt class="shrink-0 text-sm text-ink-400">消耗算力</dt>
            <dd class="text-sm font-semibold text-brand-600 tabular-nums">
              <span aria-hidden="true">💎</span> {{ order.pointsCost }}
            </dd>
          </div>
        </dl>
      </BaseCard>

      <!-- ---------- 原图 ---------- -->
      <section class="mt-8">
        <div class="mb-3 flex flex-wrap items-baseline gap-x-2 gap-y-1">
          <h2 class="text-lg font-semibold text-ink-800">原图</h2>
          <span class="text-xs text-ink-400">
            <span aria-hidden="true">🔒</span> 订单完成后 7 天自动清除
          </span>
        </div>

        <div class="grid grid-cols-2 gap-3 sm:gap-4">
          <BaseCard padding="sm" class="min-w-0 text-center">
            <img
              v-if="order.personPhotoUrl"
              :src="order.personPhotoUrl"
              alt="本人照片"
              class="mb-2 h-40 w-full rounded-xl object-cover sm:h-48"
            />
            <div
              v-else
              class="mb-2 flex h-40 w-full flex-col items-center justify-center gap-2 rounded-xl bg-ink-100 px-2 text-center text-xs text-ink-400 sm:h-48"
            >
              <span class="text-2xl" aria-hidden="true">🔒</span>
              <span>已按隐私承诺清除</span>
            </div>
            <span class="text-xs text-ink-400">你的照片</span>
          </BaseCard>

          <BaseCard padding="sm" class="min-w-0 text-center">
            <img
              v-if="order.petPhotoUrl"
              :src="order.petPhotoUrl"
              alt="宠物照片"
              class="mb-2 h-40 w-full rounded-xl object-cover sm:h-48"
            />
            <div
              v-else
              class="mb-2 flex h-40 w-full flex-col items-center justify-center gap-2 rounded-xl bg-ink-100 px-2 text-center text-xs text-ink-400 sm:h-48"
            >
              <span class="text-2xl" aria-hidden="true">🔒</span>
              <span>已按隐私承诺清除</span>
            </div>
            <span class="text-xs text-ink-400">宠物的照片</span>
          </BaseCard>
        </div>
      </section>

      <!-- ---------- 生成结果 ---------- -->
      <div class="mt-10 mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 class="text-lg font-semibold text-ink-800">生成结果</h2>

        <div v-if="order.successCount > 0" class="flex flex-wrap items-center gap-2">
          <BaseButton variant="secondary" size="sm" @click="openSharePanel">
            <template #icon>
              <svg
                class="size-4"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <path
                  d="M10 13a5 5 0 0 0 7 0l2-2a5 5 0 0 0-7-7l-1 1M14 11a5 5 0 0 0-7 0l-2 2a5 5 0 0 0 7 7l1-1"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </template>
            分享
          </BaseButton>

          <BaseButton
            size="sm"
            :loading="downloading"
            :disabled="downloading"
            @click="handleDownloadAll"
          >
            <template #icon>
              <svg
                class="size-4"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="2"
                aria-hidden="true"
              >
                <path
                  d="M12 4v10m0 0l-3.5-3.5M12 14l3.5-3.5M5 19h14"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </template>
            {{ downloading ? '打包中…' : '下载整套' }}
          </BaseButton>
        </div>
      </div>

      <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4">
        <BaseCard
          v-for="t in order.tasks"
          :key="t.id || t.seqNo"
          padding="sm"
          class="min-w-0"
        >
          <img
            v-if="t.imageUrl && !brokenImages.has(t.id || t.seqNo)"
            :src="t.imageUrl"
            :alt="`第 ${t.seqNo} 张生成结果`"
            class="h-40 w-full rounded-xl bg-ink-100 object-cover sm:h-48"
            loading="lazy"
            @error="markBroken(t.id || t.seqNo)"
          />
          <div
            v-else
            class="flex h-40 w-full flex-col items-center justify-center gap-1.5 rounded-xl bg-ink-100 px-2 text-center text-xs text-ink-400 sm:h-48"
          >
            <template v-if="t.imageUrl">
              <span class="text-lg" aria-hidden="true">🖼️</span>
              <span>图片已不可用</span>
            </template>
            <template v-else>
              {{ TASK_TEXT[t.status] || '处理中' }}
            </template>
          </div>

          <p class="mt-2 text-center text-xs text-ink-400">第 {{ t.seqNo }} 张</p>

          <BaseButton
            v-if="t.imageUrl && t.id"
            variant="secondary"
            size="sm"
            block
            class="mt-2"
            :loading="downloadingTaskId === t.id"
            :aria-label="`下载第 ${t.seqNo} 张`"
            @click="handleDownloadOne(t)"
          >
            下载
          </BaseButton>
        </BaseCard>
      </div>

      <p class="mt-8 text-center text-xs leading-relaxed text-ink-400">
        页面每 2 秒自动刷新一次进度。 当前为模拟生成（ai.provider=mock），接上真实 AI 后这里会换成真图。
      </p>

      <div class="mt-10 text-center">
        <BaseButton variant="dangerGhost" size="sm" @click="deleteOpen = true">
          删除这套作品
        </BaseButton>
      </div>
    </template>

    <!-- ============ 删除确认 ============ -->
    <ConfirmDialog
      v-model="deleteOpen"
      title="删除这套作品？"
      :message="DELETE_MESSAGE"
      confirm-text="删除"
      danger
      :loading="deleting"
      @confirm="handleDelete"
    />

    <!-- ============ 分享面板 ============ -->
    <BaseModal v-model="shareOpen" title="分享这套写真">
      <div v-if="shareLoading" class="space-y-3 py-1" aria-busy="true">
        <BaseSkeleton size="h-3 w-full" />
        <BaseSkeleton size="h-3 w-4/5" />
        <BaseSkeleton size="h-10 w-full" rounded="rounded-xl" />
      </div>

      <template v-else-if="shareInfo">
        <p class="text-sm leading-relaxed text-ink-500">
          任何拿到链接的人都能查看，无需登录。<br />
          链接将在
          <b class="font-semibold text-brand-600">{{ fmtTime(shareInfo.expireAt) }}</b>
          自动失效。
        </p>

        <input
          :value="shareUrl"
          readonly
          aria-label="分享链接"
          class="mt-4 h-10 w-full min-w-0 rounded-xl border border-ink-200 bg-ink-100 px-3 text-xs text-ink-600 focus:border-brand-500 focus:outline-none focus:ring-2 focus:ring-brand-500/25"
          @focus="$event.target.select()"
        />

        <div class="mt-4 flex flex-col gap-2 sm:flex-row">
          <BaseButton variant="ghost" size="sm" block @click="askCancelShare">取消分享</BaseButton>
          <BaseButton
            variant="ghost"
            size="sm"
            block
            :loading="shareBusy"
            @click="handleCreateShare"
          >
            重新生成
          </BaseButton>
          <BaseButton size="sm" block :disabled="shareBusy" @click="copyShareUrl(false)">
            复制链接
          </BaseButton>
        </div>

        <p class="mt-3 text-xs text-ink-400">重新生成后，之前发出去的链接会立即失效。</p>
      </template>

      <template v-else>
        <p class="text-sm leading-relaxed text-ink-500">
          生成一条链接，发给家人朋友看这套写真。<br />
          只有作品会被分享，<b class="font-semibold text-brand-600">你上传的原图不会被展示</b>。
        </p>

        <div class="mt-5 flex flex-col gap-2 sm:flex-row">
          <BaseButton variant="ghost" size="sm" block @click="closeSharePanel">取消</BaseButton>
          <BaseButton size="sm" block :loading="shareBusy" @click="handleCreateShare">
            生成分享链接
          </BaseButton>
        </div>
      </template>
    </BaseModal>

    <!-- ============ 取消分享确认 ============ -->
    <ConfirmDialog
      v-model="cancelShareOpen"
      title="取消分享？"
      message="取消后，已经发出去的链接会立即失效，确定吗？"
      confirm-text="确认取消"
      danger
      :loading="shareBusy"
      @confirm="handleCancelShare"
    />
  </div>
</template>
