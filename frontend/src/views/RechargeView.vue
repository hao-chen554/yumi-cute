<script setup>
import { ref, computed, onMounted } from 'vue'
import { getPackages, createRecharge, payRecharge } from '@/api/recharge'
import { getMyInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'
import {
  BaseBadge,
  BaseButton,
  BaseCard,
  BaseModal,
  BaseSkeleton,
  EmptyState,
  toast
} from '@/components/base'

const userStore = useUserStore()

const packages = ref([])
const points = ref(0)
const loading = ref(true)
const errorMsg = ref('')
/** 正在创建充值单的套餐 id（只让被点的那张卡转圈） */
const creatingId = ref(null)
/** 收银台正在提交支付 */
const paying = ref(false)
const cashier = ref(null)

/** 任何一笔请求在飞，就锁住重复提交 */
const busy = computed(() => creatingId.value !== null || paying.value)

// 幂等键绑定「一次购买意图」：
//   同一套餐重复点 → 返回同一单（不会产生多个待支付单）
//   换套餐         → 必须换新 key，否则会拿到上一个套餐的订单，
//                    导致「收银台显示 550 算力、实际只到账 100」
//   支付成功       → 换新 key（这次购买已经结束）
let clientToken = crypto.randomUUID()
let tokenPkgId = null

function tokenFor(packageId) {
  if (tokenPkgId !== packageId) {
    clientToken = crypto.randomUUID()
    tokenPkgId = packageId
  }
  return clientToken
}

/** 收银台的开关：cashier 有值即打开，关闭时清空 */
const cashierOpen = computed({
  get: () => cashier.value !== null,
  set: (open) => {
    if (!open) closeCashier()
  }
})

function fmtAmount(cent) {
  return ((cent || 0) / 100).toFixed(2)
}

async function loadPoints() {
  const info = await getMyInfo()
  points.value = info.points
  if (userStore.userInfo) {
    userStore.userInfo.points = info.points
  }
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const [pkgs] = await Promise.all([getPackages(), loadPoints()])
    packages.value = pkgs || []
  } catch (e) {
    errorMsg.value = e.message || '加载失败，请刷新重试'
    toast.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)

async function handleBuy(p) {
  if (busy.value) return
  creatingId.value = p.id
  try {
    const rechargeNo = await createRecharge({
      clientToken: tokenFor(p.id),
      packageId: p.id
    })
    cashier.value = {
      rechargeNo,
      pkgName: p.name,
      points: p.points + p.bonusPoints,
      amountCent: p.amountCent
    }
  } catch (e) {
    toast.error(e.message || '创建充值单失败')
  } finally {
    creatingId.value = null
  }
}

async function handlePay() {
  if (busy.value || !cashier.value) return

  paying.value = true
  let credited
  try {
    credited = await payRecharge(cashier.value.rechargeNo)
  } catch (e) {
    // 只包住支付请求：失败才是「支付失败」
    toast.error(e.message || '支付失败')
    return
  } finally {
    paying.value = false
  }

  // ★ 支付已成功：关收银台 + 本次购买意图结束，下一单必须是新订单
  closeCashier()
  clientToken = crypto.randomUUID()
  tokenPkgId = null

  // credited 是「本次到账算力」。0 表示这单之前已经到过账了，不重复加钱。
  // 这句话必须说出口 —— 钱已经到账了，不能因为后面的刷新失败就不告诉用户
  if (credited > 0) {
    toast.success(`充值成功，到账 ${credited} 算力`)
  } else {
    toast.info('该充值单已到账，未重复增加算力')
  }

  // 刷新余额是附随动作：失败不改变「支付成功」这个结论，更不能提示「支付失败」
  loadPoints().catch(() => {
    console.warn('余额刷新失败，刷新页面即可看到最新余额')
  })
}

function closeCashier() {
  cashier.value = null
}
</script>

<template>
  <div class="mx-auto w-full max-w-4xl">
    <!-- ============ 页头 ============ -->
    <div class="mb-8 flex flex-wrap items-end justify-between gap-4">
      <div class="min-w-0">
        <h1 class="text-2xl font-bold tracking-tight text-ink-900">充值算力</h1>
        <p class="mt-1 text-sm text-ink-400">算力用于生成写真，充值后立即到账</p>
      </div>

      <BaseSkeleton
        v-if="loading"
        size="h-8 w-28"
        rounded="rounded-full"
      />
      <span
        v-else
        class="inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1.5 text-sm font-semibold text-brand-600 tabular-nums"
      >
        当前算力
        <span aria-hidden="true">💎</span>
        {{ points }}
      </span>
    </div>

    <!-- ============ 加载中：套餐卡片骨架 ============ -->
    <div v-if="loading" class="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
      <BaseCard
        v-for="i in 4"
        :key="i"
        padding="none"
        class="min-w-0 p-3 sm:p-5"
      >
        <div class="space-y-3">
          <BaseSkeleton size="h-3 w-14 mx-auto" />
          <BaseSkeleton size="h-7 w-20 mx-auto" />
          <BaseSkeleton size="h-3 w-16 mx-auto" />
          <BaseSkeleton size="h-5 w-12 mx-auto" />
          <BaseSkeleton size="h-10 w-full" />
        </div>
      </BaseCard>
    </div>

    <!-- ============ 加载失败 ============ -->
    <BaseCard v-else-if="errorMsg" padding="none">
      <EmptyState title="套餐加载失败" :description="errorMsg" emoji="⚠️">
        <template #action>
          <BaseButton @click="load">重新加载</BaseButton>
        </template>
      </EmptyState>
    </BaseCard>

    <!-- ============ 空套餐 ============ -->
    <BaseCard v-else-if="!packages.length" padding="none">
      <EmptyState
        title="暂时没有可用的充值套餐"
        description="稍后再来看看，或者先去做一套试看作品。"
        emoji="💎"
      >
        <template #action>
          <BaseButton variant="secondary" @click="load">刷新试试</BaseButton>
        </template>
      </EmptyState>
    </BaseCard>

    <!-- ============ 套餐列表 ============ -->
    <div v-else class="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
      <BaseCard
        v-for="p in packages"
        :key="p.id"
        padding="none"
        class="flex min-w-0 flex-col p-3 text-center transition-all duration-200 hover:-translate-y-0.5 hover:shadow-hover sm:p-5"
        :class="p.bonusPoints > 0 ? 'ring-2 ring-brand-200' : 'ring-1 ring-ink-200'"
      >
        <p class="truncate text-xs text-ink-500 sm:text-sm">{{ p.name }}</p>

        <p class="mt-2 leading-none">
          <b class="text-2xl font-bold text-brand-500 tabular-nums sm:text-3xl">
            {{ p.points + p.bonusPoints }}
          </b>
          <span class="ml-1 text-xs text-ink-500">算力</span>
        </p>

        <div class="mt-2 flex h-5 items-center justify-center">
          <BaseBadge v-if="p.bonusPoints > 0" tone="brand" size="sm">
            赠 {{ p.bonusPoints }}
          </BaseBadge>
          <span v-else class="text-xs text-ink-400" aria-hidden="true">—</span>
        </div>

        <p class="mt-2 text-base font-semibold text-ink-900 tabular-nums sm:text-lg">
          ¥{{ fmtAmount(p.amountCent) }}
        </p>

        <BaseButton
          size="md"
          block
          class="mt-3"
          :loading="creatingId === p.id"
          :disabled="busy"
          @click="handleBuy(p)"
        >
          立即充值
        </BaseButton>
      </BaseCard>
    </div>

    <!-- ============ 模拟收银台 ============ -->
    <BaseModal
      v-model="cashierOpen"
      title="模拟收银台"
      max-width="max-w-md"
      :closable="!paying"
      :mask-closable="!paying"
    >
      <dl class="space-y-1 text-sm">
        <div class="flex items-baseline justify-between gap-4 py-1.5">
          <dt class="shrink-0 text-ink-500">套餐</dt>
          <dd class="min-w-0 truncate text-right font-semibold text-ink-900">
            {{ cashier?.pkgName }}
          </dd>
        </div>
        <div class="flex items-baseline justify-between gap-4 py-1.5">
          <dt class="shrink-0 text-ink-500">到账</dt>
          <dd class="min-w-0 text-right font-semibold text-ink-900 tabular-nums">
            {{ cashier?.points }} 算力
          </dd>
        </div>
        <div class="flex items-baseline justify-between gap-4 py-1.5">
          <dt class="shrink-0 text-ink-500">应付</dt>
          <dd class="text-right text-xl font-bold text-brand-500 tabular-nums">
            ¥{{ fmtAmount(cashier?.amountCent) }}
          </dd>
        </div>
        <div class="flex items-baseline justify-between gap-4 py-1.5">
          <dt class="shrink-0 text-ink-500">单号</dt>
          <dd class="min-w-0 break-all text-right font-mono text-xs text-ink-400">
            {{ cashier?.rechargeNo }}
          </dd>
        </div>
      </dl>

      <p class="mt-4 rounded-xl bg-brand-50 px-4 py-3 text-xs leading-relaxed text-ink-500">
        这是模拟支付，不会真的扣款。点「我已支付」即视为支付成功。
      </p>

      <template #footer>
        <div class="flex gap-3">
          <BaseButton variant="ghost" block :disabled="paying" @click="closeCashier">
            取消
          </BaseButton>
          <BaseButton block :loading="paying" @click="handlePay">
            {{ paying ? '处理中…' : '我已支付' }}
          </BaseButton>
        </div>
      </template>
    </BaseModal>
  </div>
</template>
