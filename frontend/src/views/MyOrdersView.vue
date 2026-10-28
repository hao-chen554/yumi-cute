<script setup>
import { ref, onMounted } from 'vue'
import { getMyOrders } from '@/api/order'
import {
  BaseBadge,
  BaseButton,
  BaseCard,
  BaseSkeleton,
  EmptyState
} from '@/components/base'

const orders = ref([])
const loading = ref(true)
const errorMsg = ref('')

const STATUS = {
  0: { text: '待生成', tone: 'neutral' },
  1: { text: '生成中', tone: 'info' },
  2: { text: '已完成', tone: 'success' },
  3: { text: '部分失败', tone: 'warning' },
  4: { text: '生成失败', tone: 'danger' },
  5: { text: '已取消', tone: 'neutral' }
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    orders.value = await getMyOrders()
  } catch (e) {
    errorMsg.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)

function fmtTime(t) {
  return t ? String(t).replace('T', ' ').slice(0, 16) : ''
}
</script>

<template>
  <div class="mx-auto w-full max-w-3xl">
    <div class="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div>
        <h1 class="text-2xl font-bold tracking-tight text-ink-900">我的作品</h1>
        <p class="mt-1 text-sm text-ink-400">
          <template v-if="loading">正在加载…</template>
          <template v-else-if="orders.length">你和它的每一套写真，共 {{ orders.length }} 套</template>
          <template v-else>你和它的每一套写真</template>
        </p>
      </div>

      <RouterLink to="/create">
        <BaseButton size="sm">+ 新建一套</BaseButton>
      </RouterLink>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="space-y-3.5">
      <BaseCard v-for="i in 3" :key="i">
        <div class="flex items-center justify-between gap-4">
          <div class="flex-1 space-y-2.5">
            <BaseSkeleton size="h-5 w-24" />
            <BaseSkeleton size="h-3 w-40" />
            <BaseSkeleton size="h-3 w-32" />
          </div>
          <BaseSkeleton size="h-6 w-16" rounded="rounded-full" />
        </div>
      </BaseCard>
    </div>

    <!-- 加载失败 -->
    <BaseCard v-else-if="errorMsg" padding="none">
      <EmptyState title="作品加载失败" :description="errorMsg" emoji="⚠️">
        <template #action>
          <BaseButton @click="load">重新加载</BaseButton>
        </template>
      </EmptyState>
    </BaseCard>

    <!-- 空状态 -->
    <BaseCard v-else-if="!orders.length" padding="none">
      <EmptyState
        title="还没有作品"
        description="上传一张你和毛孩子的合照，选一个风格，先出一张试看。"
        emoji="🐾"
      >
        <template #action>
          <BaseButton @click="$router.push('/create')">去创作第一套</BaseButton>
        </template>
      </EmptyState>
    </BaseCard>

    <!-- 列表 -->
    <div v-else class="space-y-3.5">
      <RouterLink
        v-for="o in orders"
        :key="o.id"
        :to="`/orders/${o.id}`"
        class="block rounded-2xl bg-surface p-5 shadow-card transition-all duration-200 hover:-translate-y-0.5 hover:shadow-hover"
      >
        <div class="flex items-start justify-between gap-4">
          <div class="min-w-0 flex-1">
            <h2 class="truncate text-base font-semibold text-ink-900 sm:text-lg">
              {{ o.styleName }}
            </h2>

            <div class="mt-2 flex flex-wrap items-center gap-x-2 gap-y-1 text-[13px] text-ink-500">
              <span>{{ o.orderType === 1 ? '试看单' : '整套单' }}</span>
              <span class="text-ink-300">·</span>
              <span class="tabular-nums">{{ o.successCount }} / {{ o.totalCount }} 张</span>
              <span class="text-ink-300">·</span>
              <span class="font-semibold text-brand-600 tabular-nums">💎 {{ o.pointsCost }}</span>
            </div>

            <p class="mt-1.5 truncate font-mono text-xs text-ink-300">{{ o.orderNo }}</p>
          </div>

          <div class="flex shrink-0 flex-col items-end gap-2">
            <BaseBadge :tone="STATUS[o.status]?.tone || 'neutral'">
              {{ STATUS[o.status]?.text || '未知' }}
            </BaseBadge>
            <span class="text-xs text-ink-300 tabular-nums">{{ fmtTime(o.createTime) }}</span>
          </div>
        </div>
      </RouterLink>
    </div>
  </div>
</template>
