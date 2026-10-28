<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { viewShare } from '@/api/share'
import { BaseButton, BaseCard, BaseSkeleton, EmptyState } from '@/components/base'

const route = useRoute()

const data = ref(null)
const loading = ref(true)
const error = ref('')
const previewUrl = ref(null)

onMounted(async () => {
  try {
    data.value = await viewShare(route.params.token)
  } catch (e) {
    error.value = e.message || '链接不存在或已失效'
  } finally {
    loading.value = false
  }
})

/** 后端返回的是 ISO 时间（2026-10-29T08:39:05），显示成 2026-10-29 08:39 */
function fmtTime(s) {
  return s ? s.replace('T', ' ').slice(0, 16) : ''
}

function preview(url) {
  previewUrl.value = url
}

/** 失效页文案：接口给的错误信息 + 固定提示 */
const errorDescription = computed(() =>
  [error.value, '链接可能已被作者取消分享，或者已经过期。'].filter(Boolean).join(' ')
)
</script>

<template>
  <div class="mx-auto w-full max-w-4xl">
    <!-- 加载中：骨架屏 -->
    <div v-if="loading" class="animate-fade-in">
      <div class="mb-8 flex flex-col items-center gap-3 sm:mb-10">
        <BaseSkeleton size="h-3 w-16" />
        <BaseSkeleton size="h-7 w-56 max-w-full" />
        <BaseSkeleton size="h-3 w-40 max-w-full" />
      </div>
      <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4">
        <BaseSkeleton :count="6" size="aspect-square w-full" rounded="rounded-2xl" />
      </div>
    </div>

    <!-- 链接失效 -->
    <BaseCard v-else-if="error" padding="none" class="animate-fade-in">
      <EmptyState
        title="链接不存在或已失效"
        :description="errorDescription"
        emoji="🔗"
      >
        <template #action>
          <RouterLink to="/">
            <BaseButton>去看看 YuMi</BaseButton>
          </RouterLink>
        </template>
      </EmptyState>
    </BaseCard>

    <!-- 分享内容 -->
    <template v-else>
      <header class="mb-8 text-center sm:mb-10">
        <p class="text-xs font-bold tracking-widest text-brand-500">YuMi</p>
        <h1 class="mt-3 break-words text-2xl font-bold tracking-tight text-ink-900 sm:text-3xl">
          {{ data.styleName }}
        </h1>
        <p class="mt-2 text-sm text-ink-400 tabular-nums">
          {{ data.images.length }} 张 · 有效期至 {{ fmtTime(data.expireAt) }}
        </p>
      </header>

      <div class="grid grid-cols-2 gap-3 sm:grid-cols-3 sm:gap-4">
        <button
          v-for="(u, i) in data.images"
          :key="i"
          type="button"
          class="group block aspect-square w-full cursor-zoom-in overflow-hidden rounded-2xl bg-ink-100 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-brand-500"
          :aria-label="`查看第 ${i + 1} 张`"
          @click="preview(u)"
        >
          <img
            :src="u"
            :alt="`第 ${i + 1} 张`"
            loading="lazy"
            decoding="async"
            class="size-full object-cover transition-transform duration-200 group-hover:scale-[1.03]"
          />
        </button>
      </div>

      <footer class="mt-12 text-center">
        <p class="text-sm text-ink-400">这是别人分享给你的人宠写真</p>
        <RouterLink to="/" class="mt-4 inline-block">
          <BaseButton size="lg">我也想为自己和它做一套 →</BaseButton>
        </RouterLink>
      </footer>

      <!-- 全屏预览：点空白（遮罩）关闭，点图片不关闭 -->
      <div
        v-if="previewUrl"
        class="fixed inset-0 z-50 flex cursor-zoom-out items-center justify-center bg-ink-900/80 p-4 animate-fade-in sm:p-8"
        role="dialog"
        aria-modal="true"
        aria-label="图片预览"
        @click="previewUrl = null"
      >
        <img
          :src="previewUrl"
          alt=""
          class="max-h-full max-w-full rounded-xl object-contain"
          @click.stop
        />
      </div>
    </template>
  </div>
</template>
