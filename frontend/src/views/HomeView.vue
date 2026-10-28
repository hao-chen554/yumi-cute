<script setup>
import { ref, onMounted } from 'vue'
import { getStyleList } from '@/api/style'
import { useUserStore } from '@/stores/user'
import { BaseButton, BaseCard, BaseSkeleton, EmptyState, toast } from '@/components/base'

const userStore = useUserStore()

const styles = ref([])
const loading = ref(true)

// 没有真实封面图，先用渐变色块占位 —— 后端 cover_url 目前都是空
const GRADIENTS = [
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
  'linear-gradient(135deg, #d4d4d4 0%, #f5f5f5 100%)',
  'linear-gradient(135deg, #dce35b 0%, #45b649 100%)',
  'linear-gradient(135deg, #5b7fff 0%, #b06ab3 100%)',
  'linear-gradient(135deg, #f6d365 0%, #fda085 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #c1dfc4 0%, #deecdd 100%)'
]

const FEATURES = [
  { icon: '🎨', title: '多种风格模板', desc: '日系清新、复古胶片、国风工笔……总有一款适合你们' },
  { icon: '💎', title: '先试后买', desc: '先出一张试看，满意再生成整套，不花冤枉钱' },
  { icon: '🔒', title: '隐私优先', desc: '作品默认私密，原图用完即删，只有你能看到' }
]

onMounted(async () => {
  try {
    styles.value = await getStyleList()
  } catch (e) {
    toast.error(e.message || '风格列表加载失败')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <!-- ==================== Hero ==================== -->
    <section class="relative overflow-hidden rounded-3xl bg-surface px-6 py-14 text-center sm:py-20">
      <div
        class="pointer-events-none absolute -top-32 left-1/2 size-[28rem] -translate-x-1/2 rounded-full bg-brand-100/70 blur-3xl"
        aria-hidden="true"
      />

      <div class="relative">
        <span
          class="mb-5 inline-flex items-center gap-1.5 rounded-full bg-brand-50 px-3 py-1 text-xs font-semibold text-brand-600"
        >
          ✨ AI 人宠写真
        </span>

        <h1 class="text-3xl font-bold leading-tight tracking-tight text-ink-900 sm:text-5xl">
          为你和它，<br class="sm:hidden" />留下一套写真
        </h1>

        <p class="mx-auto mt-5 max-w-xl text-sm leading-relaxed text-ink-500 sm:text-base">
          用一顿饭的钱，把你和毛孩子的样子，变成一整套值得收藏的照片。
          先出一张看看，满意再生成整套。
        </p>

        <div class="mt-8 flex justify-center">
          <BaseButton
            size="lg"
            class="!h-14 !px-10 !text-base"
            @click="$router.push(userStore.isLogin ? '/create' : '/register')"
          >
            {{ userStore.isLogin ? '开始创作' : '免费开始' }}
          </BaseButton>
        </div>
      </div>
    </section>

    <!-- ==================== 风格列表 ==================== -->
    <section class="mt-16 sm:mt-20">
      <div class="mb-6 flex items-end justify-between gap-4">
        <div>
          <h2 class="text-xl font-bold tracking-tight text-ink-900 sm:text-2xl">选择你喜欢的风格</h2>
          <p class="mt-1.5 text-sm text-ink-400">
            <template v-if="loading">正在加载…</template>
            <template v-else-if="styles.length">共 {{ styles.length }} 种风格，每一种都不同</template>
          </p>
        </div>
      </div>

      <!-- 加载中：骨架屏 -->
      <div v-if="loading" class="grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-4">
        <div v-for="i in 4" :key="i" class="overflow-hidden rounded-2xl bg-surface shadow-card">
          <div class="skeleton h-40 w-full rounded-none" />
          <div class="space-y-2 p-4">
            <BaseSkeleton size="h-4 w-1/2" />
            <BaseSkeleton size="h-3 w-full" />
            <BaseSkeleton size="h-3 w-2/3" />
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <BaseCard v-else-if="!styles.length" padding="none">
        <EmptyState
          title="还没有可用的风格"
          description="风格模板由运营配置，请稍后再来，或者联系管理员初始化数据。"
          emoji="🎨"
        />
      </BaseCard>

      <!-- 正常 -->
      <div v-else class="grid grid-cols-2 gap-4 sm:gap-5 lg:grid-cols-4">
        <article
          v-for="(s, index) in styles"
          :key="s.id"
          class="group overflow-hidden rounded-2xl bg-surface shadow-card transition-all duration-200 hover:-translate-y-1 hover:shadow-hover"
        >
          <div
            class="relative flex h-36 items-center justify-center overflow-hidden sm:h-40"
            :style="{ background: GRADIENTS[index % GRADIENTS.length] }"
          >
            <!-- 底部压暗一层，保证浅色渐变上的白字也读得清 -->
            <div
              class="absolute inset-0 bg-gradient-to-t from-ink-900/35 via-ink-900/5 to-transparent"
              aria-hidden="true"
            />
            <span
              class="relative text-lg font-bold text-white drop-shadow-sm sm:text-xl"
              style="text-shadow: 0 2px 8px rgb(0 0 0 / 0.35)"
            >
              {{ s.name }}
            </span>
          </div>

          <div class="p-4">
            <h3 class="text-sm font-semibold text-ink-800 sm:text-base">{{ s.name }}</h3>
            <p class="mt-1.5 line-clamp-2 min-h-[2.5rem] text-xs leading-relaxed text-ink-400 sm:text-[13px]">
              {{ s.description }}
            </p>
            <div class="mt-3 flex items-center gap-1 text-xs font-semibold text-brand-600 sm:text-[13px]">
              💎 <span class="tabular-nums">{{ s.pointsCost }}</span> 算力
            </div>
          </div>
        </article>
      </div>
    </section>

    <!-- ==================== 特性 ==================== -->
    <section class="mt-16 grid gap-4 sm:mt-20 sm:grid-cols-3 sm:gap-5">
      <BaseCard v-for="f in FEATURES" :key="f.title" padding="lg" hoverable>
        <div class="text-3xl select-none">{{ f.icon }}</div>
        <h3 class="mt-3 text-base font-semibold text-ink-800">{{ f.title }}</h3>
        <p class="mt-2 text-sm leading-relaxed text-ink-400">{{ f.desc }}</p>
      </BaseCard>
    </section>
  </div>
</template>
