<template>
  <div class="recharge">
    <div class="head">
      <h2>充值算力</h2>
      <p class="balance">当前算力 <b>💎 {{ points }}</b></p>
    </div>

    <div v-if="loading" class="tip">加载中…</div>

    <div v-else class="packages">
      <div
          v-for="p in packages"
          :key="p.id"
          class="pkg"
          :class="{ hot: p.bonusPoints > 0 }"
      >
        <div class="pkg-name">{{ p.name }}</div>
        <div class="pkg-points"><b>{{ p.points + p.bonusPoints }}</b> 算力</div>
        <div class="pkg-bonus" :class="{ placeholder: !p.bonusPoints }">
          {{ p.bonusPoints > 0 ? `含赠送 ${p.bonusPoints}` : '—' }}
        </div>
        <div class="pkg-price">¥{{ (p.amountCent / 100).toFixed(2) }}</div>
        <button class="pkg-btn" :disabled="busy" @click="handleBuy(p)">
          立即充值
        </button>
      </div>
    </div>

    <!-- 模拟收银台 -->
    <div v-if="cashier" class="mask" @click.self="closeCashier">
      <div class="cashier">
        <h3>模拟收银台</h3>
        <div class="line"><span>套餐</span><b>{{ cashier.pkgName }}</b></div>
        <div class="line"><span>到账</span><b>{{ cashier.points }} 算力</b></div>
        <div class="line">
          <span>应付</span>
          <b class="amount">¥{{ (cashier.amountCent / 100).toFixed(2) }}</b>
        </div>
        <div class="line"><span>单号</span><code>{{ cashier.rechargeNo }}</code></div>
        <p class="hint">
          这是模拟支付，不会真的扣款。点「我已支付」即视为支付成功。
        </p>
        <div class="actions">
          <button class="ghost" @click="closeCashier">取消</button>
          <button class="primary" :disabled="busy" @click="handlePay">
            {{ busy ? '处理中…' : '我已支付' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPackages, createRecharge, payRecharge } from '@/api/recharge'
import { getMyInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const packages = ref([])
const points = ref(0)
const loading = ref(true)
const busy = ref(false)
const cashier = ref(null)

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

async function loadPoints() {
  const info = await getMyInfo()
  points.value = info.points
  if (userStore.userInfo) {
    userStore.userInfo.points = info.points
  }
}

onMounted(async () => {
  try {
    const [pkgs] = await Promise.all([getPackages(), loadPoints()])
    packages.value = pkgs
  } catch (e) {
    alert(e.message || '加载失败，请刷新重试')
  } finally {
    loading.value = false
  }
})

async function handleBuy(p) {
  if (busy.value) return
  busy.value = true
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
    alert(e.message || '创建充值单失败')
  } finally {
    busy.value = false
  }
}

async function handlePay() {
  if (busy.value || !cashier.value) return
  busy.value = true
  try {
    const credited = await payRecharge(cashier.value.rechargeNo)

    closeCashier()

    // ★ 本次购买已结束，下一单必须是新订单
    clientToken = crypto.randomUUID()
    tokenPkgId = null

    // credited 是「本次到账算力」。0 表示这单之前已经到过账了，不重复加钱。
    // 这句话必须说出口 —— 钱已经到账了，不能因为后面的刷新失败就不告诉用户
    alert(
        credited > 0
            ? `充值成功，到账 ${credited} 算力`
            : '该充值单已到账，未重复增加算力'
    )

    // 刷新余额是附随动作：失败不改变「支付成功」这个结论
    loadPoints().catch(() => {
        console.warn('余额刷新失败，刷新页面即可看到最新余额')
    })
  } catch (e) {
    alert(e.message || '支付失败')
  } finally {
    busy.value = false
  }
}

function closeCashier() {
  cashier.value = null
}
</script>

<style scoped>
.recharge { max-width: 900px; margin: 40px auto; padding: 0 20px; }
.head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 24px; }
.head h2 { font-size: 22px; }
.balance { color: #666; font-size: 14px; }
.balance b { color: #ff7849; font-size: 18px; }
.tip { text-align: center; color: #999; padding: 60px 0; }

.packages { display: grid; grid-template-columns: repeat(auto-fill, minmax(190px, 1fr)); gap: 16px; }

.pkg {
  background: #fff; border: 2px solid #f0f0f0; border-radius: 16px;
  padding: 22px 18px; text-align: center; transition: .2s;
}
.pkg.hot { border-color: #ffd7c2; }
.pkg:hover { transform: translateY(-3px); box-shadow: 0 8px 22px rgba(0,0,0,.07); }

.pkg-name { font-size: 14px; color: #888; }
.pkg-points { margin: 10px 0 4px; font-size: 14px; color: #444; }
.pkg-points b { font-size: 30px; color: #ff7849; margin-right: 2px; }
.pkg-bonus { font-size: 12px; color: #ff7849; height: 18px; }
.pkg-bonus.placeholder { color: transparent; }
.pkg-price { margin: 12px 0 16px; font-size: 18px; font-weight: 600; }

.pkg-btn {
  width: 100%; padding: 9px 0; border: none; border-radius: 10px;
  background: #ff7849; color: #fff; font-size: 14px; cursor: pointer;
}
.pkg-btn:disabled { background: #f3b8a1; cursor: not-allowed; }

.mask {
  position: fixed; inset: 0; background: rgba(0,0,0,.45);
  display: flex; align-items: center; justify-content: center; z-index: 50;
}
.cashier {
  width: 400px; background: #fff; border-radius: 16px; padding: 26px 24px;
}
.cashier h3 { font-size: 17px; margin-bottom: 18px; }
.line { display: flex; justify-content: space-between; padding: 7px 0; font-size: 14px; color: #666; }
.line b { color: #333; font-weight: 600; }
.line .amount { color: #ff7849; font-size: 18px; }
.line code { font-size: 12px; color: #999; }
.hint { font-size: 12px; color: #aaa; margin: 14px 0 20px; line-height: 1.6; }
.actions { display: flex; gap: 12px; }
.actions button { flex: 1; padding: 10px 0; border-radius: 10px; font-size: 14px; cursor: pointer; border: none; }
.ghost { background: #f5f5f5; color: #666; }
.primary { background: #ff7849; color: #fff; }
.primary:disabled { background: #f3b8a1; cursor: not-allowed; }
</style>