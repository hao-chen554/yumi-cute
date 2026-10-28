<script setup>
import { ref, computed, onMounted } from 'vue'
import { getStyleList } from '@/api/style'
import { createOrder } from '@/api/order'
import { getMyInfo } from '@/api/user'
import { useUserStore } from '@/stores/user'
import PhotoUploader from '@/components/PhotoUploader.vue'
import { BaseButton, BaseCard, BaseSkeleton, EmptyState } from '@/components/base'

const userStore = useUserStore()

const styles = ref([])
const styleLoading = ref(true)
const styleError = ref('')
const loading = ref(false)
const errorMsg = ref('')
const orderNo = ref('')
const clientToken = crypto.randomUUID()

const form = ref({
  clientToken,
  styleId: null,
  personPhotoUrl: '',
  petPhotoUrl: '',
  orderType: 2 // 2 = 整套，1 = 试看
})

const selectedStyle = computed(() => styles.value.find((s) => s.id === form.value.styleId))

// ⚠️ 注意：这是"前端预估价"，真正扣多少以后端为准。
// 业务规则在前端后端各写一遍是技术债，后面会重构掉。
const cost = computed(() => {
  const s = selectedStyle.value
  if (!s) return 0
  return form.value.orderType === 1 ? Math.max(1, Math.floor(s.pointsCost / 4)) : s.pointsCost
})

const photoCount = computed(() => (form.value.orderType === 1 ? 1 : 6))

const canSubmit = computed(
  () =>
    !!form.value.styleId &&
    !!form.value.personPhotoUrl &&
    !!form.value.petPhotoUrl &&
    !loading.value
)

async function loadStyles() {
  styleLoading.value = true
  styleError.value = ''
  try {
    styles.value = await getStyleList()
  } catch (e) {
    styleError.value = e.message || '风格加载失败'
  } finally {
    styleLoading.value = false
  }
}

onMounted(loadStyles)

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
  <div class="mx-auto w-full max-w-4xl">
    <!-- 下单成功 -->
    <BaseCard
      v-if="orderNo"
      padding="lg"
      class="mx-auto mt-12 w-full max-w-xl text-center sm:mt-16"
    >
      <div class="mb-4 text-5xl select-none sm:text-6xl" aria-hidden="true">🎉</div>
      <h1 class="text-2xl font-bold tracking-tight text-ink-900">下单成功</h1>

      <p class="mt-5 break-all rounded-xl bg-brand-50 px-4 py-3 font-mono text-sm text-brand-600">
        订单号：{{ orderNo }}
      </p>

      <p class="mt-5 text-sm leading-relaxed text-ink-500 sm:text-base">
        已消耗
        <b class="font-semibold text-brand-600 tabular-nums">{{ cost }}</b> 算力，正在为你生成
        <b class="font-semibold text-ink-700 tabular-nums">{{ photoCount }}</b> 张写真
      </p>
      <p class="mt-3 text-xs text-ink-400">生成需要一点时间。订单进度页面我们下一轮做。</p>

      <div class="mt-8">
        <RouterLink to="/">
          <BaseButton size="lg">回到首页</BaseButton>
        </RouterLink>
      </div>
    </BaseCard>

    <!-- 下单表单 -->
    <div v-else>
      <div class="mb-8 sm:mb-10">
        <h1 class="text-2xl font-bold tracking-tight text-ink-900 sm:text-3xl">开始创作</h1>
        <p class="mt-2 text-sm text-ink-400 sm:text-base">三步：选风格 → 传照片 → 点生成</p>
      </div>

      <!-- 选风格 -->
      <section>
        <h2 class="mb-4 flex items-center gap-2 text-base font-semibold text-ink-800 sm:text-lg">
          <span
            class="inline-flex size-6 shrink-0 items-center justify-center rounded-full bg-brand-500 text-xs font-semibold text-white"
            aria-hidden="true"
            >1</span
          >
          选择风格
        </h2>

        <!-- 加载中：骨架屏 -->
        <div v-if="styleLoading" class="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
          <div v-for="i in 4" :key="i" class="rounded-2xl bg-surface p-4 shadow-card">
            <BaseSkeleton size="h-4 w-2/3" />
            <div class="mt-2 space-y-2">
              <BaseSkeleton size="h-3 w-full" />
              <BaseSkeleton size="h-3 w-1/2" />
            </div>
            <div class="mt-4">
              <BaseSkeleton size="h-3 w-12" />
            </div>
          </div>
        </div>

        <!-- 加载失败 -->
        <BaseCard v-else-if="styleError" padding="none">
          <EmptyState title="风格加载失败" :description="styleError" emoji="⚠️">
            <template #action>
              <BaseButton @click="loadStyles">重新加载</BaseButton>
            </template>
          </EmptyState>
        </BaseCard>

        <!-- 空状态 -->
        <BaseCard v-else-if="!styles.length" padding="none">
          <EmptyState
            title="还没有可用的风格"
            description="风格模板由运营配置，请稍后再来，或者联系管理员初始化数据。"
            emoji="🎨"
          >
            <template #action>
              <BaseButton @click="loadStyles">重新加载</BaseButton>
            </template>
          </EmptyState>
        </BaseCard>

        <!-- 正常 -->
        <div v-else class="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4">
          <button
            v-for="s in styles"
            :key="s.id"
            type="button"
            class="flex w-full cursor-pointer flex-col rounded-2xl border-2 p-4 text-left transition-all duration-200"
            :class="
              form.styleId === s.id
                ? 'border-brand-500 bg-brand-50 shadow-card'
                : 'border-ink-200 bg-surface hover:border-brand-300 hover:shadow-card'
            "
            :aria-pressed="form.styleId === s.id"
            @click="selectStyle(s.id)"
          >
            <span class="text-sm font-semibold text-ink-800">{{ s.name }}</span>
            <span class="mt-2 line-clamp-2 text-xs leading-relaxed text-ink-400">
              {{ s.description }}
            </span>
            <span class="mt-auto pt-3 text-xs font-semibold text-brand-600 tabular-nums">
              <span aria-hidden="true">💎</span> {{ s.pointsCost }}
            </span>
          </button>
        </div>
      </section>

      <!-- 传照片 -->
      <section class="mt-10">
        <h2 class="mb-4 flex items-center gap-2 text-base font-semibold text-ink-800 sm:text-lg">
          <span
            class="inline-flex size-6 shrink-0 items-center justify-center rounded-full bg-brand-500 text-xs font-semibold text-white"
            aria-hidden="true"
            >2</span
          >
          上传照片
        </h2>

        <div class="flex flex-col gap-5 sm:flex-row">
          <PhotoUploader
            v-model="form.personPhotoUrl"
            class="min-w-0 flex-1"
            label="你的照片"
            tip="清晰的正脸照效果最好"
          />
          <PhotoUploader
            v-model="form.petPhotoUrl"
            class="min-w-0 flex-1"
            label="宠物的照片"
            tip="尽量正面、光线充足"
          />
        </div>

        <p class="mt-3 text-center text-xs leading-relaxed text-ink-400">
          <span aria-hidden="true">🔒</span> 照片仅用于本次生成，不作他用；订单完成后
          <b class="font-semibold text-ink-500">7 天</b> 自动删除
        </p>
      </section>

      <!-- 选类型 -->
      <section class="mt-10">
        <h2 class="mb-4 flex items-center gap-2 text-base font-semibold text-ink-800 sm:text-lg">
          <span
            class="inline-flex size-6 shrink-0 items-center justify-center rounded-full bg-brand-500 text-xs font-semibold text-white"
            aria-hidden="true"
            >3</span
          >
          选择生成方式
        </h2>

        <div class="grid gap-4 sm:grid-cols-2">
          <button
            type="button"
            class="w-full cursor-pointer rounded-2xl border-2 p-4 text-left transition-all duration-200 sm:p-5"
            :class="
              form.orderType === 2
                ? 'border-brand-500 bg-brand-50'
                : 'border-ink-200 bg-surface hover:border-brand-300 hover:shadow-card'
            "
            :aria-pressed="form.orderType === 2"
            @click="form.orderType = 2"
          >
            <span class="block text-sm font-semibold text-ink-800">整套写真</span>
            <span class="mt-2 block text-xs leading-relaxed text-ink-400">生成 6 张，风格统一</span>
          </button>

          <button
            type="button"
            class="w-full cursor-pointer rounded-2xl border-2 p-4 text-left transition-all duration-200 sm:p-5"
            :class="
              form.orderType === 1
                ? 'border-brand-500 bg-brand-50'
                : 'border-ink-200 bg-surface hover:border-brand-300 hover:shadow-card'
            "
            :aria-pressed="form.orderType === 1"
            @click="form.orderType = 1"
          >
            <span class="block text-sm font-semibold text-ink-800">先试看一张</span>
            <span class="mt-2 block text-xs leading-relaxed text-ink-400"
              >只花 1/4 的算力，满意再出整套</span
            >
          </button>
        </div>
      </section>

      <!-- 提交 -->
      <BaseCard padding="lg" class="mt-10">
        <div class="mb-4 text-sm leading-relaxed text-ink-500">
          <template v-if="selectedStyle">
            已选「<span class="font-semibold text-ink-700">{{ selectedStyle.name }}</span
            >」 · 生成
            <span class="font-semibold text-ink-700 tabular-nums">{{ photoCount }}</span> 张 ·
            将消耗
            <b class="text-base font-semibold text-brand-600 tabular-nums">{{ cost }}</b> 算力
          </template>
          <template v-else>请先选择一个风格</template>
        </div>

        <div
          v-if="errorMsg"
          class="mb-3 flex items-start gap-2 rounded-xl bg-danger-soft px-4 py-3"
          role="alert"
        >
          <span class="mt-0.5 text-danger">!</span>
          <p class="text-sm leading-snug text-danger">{{ errorMsg }}</p>
        </div>

        <BaseButton size="lg" block :loading="loading" :disabled="!canSubmit" @click="handleSubmit">
          {{ loading ? '提交中…' : '开始生成' }}
        </BaseButton>
      </BaseCard>
    </div>
  </div>
</template>
