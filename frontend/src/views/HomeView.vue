<script setup>
import { ref, onMounted } from 'vue'
import { getStyleList } from '@/api/style'

const styles = ref([])

// 因为没有真实封面图，先用渐变色块占位
const gradients = [
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)',
  'linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)',
  'linear-gradient(135deg, #d4d4d4 0%, #f5f5f5 100%)',
  'linear-gradient(135deg, #dce35b 0%, #45b649 100%)',
  'linear-gradient(135deg, #5b7fff 0%, #b06ab3 100%)',
  'linear-gradient(135deg, #f6d365 0%, #fda085 100%)',
  'linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%)',
  'linear-gradient(135deg, #c1dfc4 0%, #deecdd 100%)'
]

onMounted(async () => {
  try {
    styles.value = await getStyleList()
  } catch (e) {
    console.error('加载风格列表失败', e)
  }
})
</script>

<template>
  <div class="home">
    <h1>为你和它，留下一套写真</h1>
    <p class="subtitle">
      YuMi-cute 是一个专注人宠写真的 AI 生成平台<br />
      用一顿饭的钱，留下属于你们的纪念
    </p>

    <div class="actions">
      <RouterLink to="/register" class="btn-primary">免费开始</RouterLink>
    </div>

    <section class="styles">
      <h2>选择你喜欢的风格</h2>
      <p class="section-tip">共 {{ styles.length }} 种风格，每一种都不同</p>

      <div class="style-grid">
        <div v-for="(s, index) in styles" :key="s.id" class="style-card">
          <div
              class="cover"
              :style="{ background: gradients[index % gradients.length] }"
          >
            <span>{{ s.name }}</span>
          </div>
          <div class="meta">
            <h4>{{ s.name }}</h4>
            <p>{{ s.description }}</p>
            <div class="cost">💎 {{ s.pointsCost }} 算力</div>
          </div>
        </div>
      </div>
    </section>

    <section class="features">
      <div class="card">
        <div class="icon">🎨</div>
        <h3>多种风格模板</h3>
        <p>日系清新、复古胶片、圣诞主题……总有一款适合你们</p>
      </div>
      <div class="card">
        <div class="icon">💎</div>
        <h3>先试后买</h3>
        <p>先出一张试看，满意再生成整套，不花冤枉钱</p>
      </div>
      <div class="card">
        <div class="icon">🔒</div>
        <h3>隐私优先</h3>
        <p>作品默认私密，只有你能看到</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  text-align: center;
  padding-top: 40px;
}

h1 {
  font-size: 40px;
  margin-bottom: 20px;
}

.subtitle {
  font-size: 17px;
  line-height: 1.8;
  color: #666;
  margin-bottom: 36px;
}

.btn-primary {
  display: inline-block;
  padding: 14px 48px;
  border-radius: 8px;
  background: linear-gradient(135deg, #5b7fff 0%, #7b9fff 100%);
  color: #fff;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.btn-primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(91, 127, 255, 0.35);
}

/* 风格区 */
.styles {
  margin-top: 90px;
}

.styles h2 {
  font-size: 26px;
  margin-bottom: 10px;
}

.section-tip {
  font-size: 14px;
  color: #999;
  margin-bottom: 36px;
}

.style-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}

.style-card {
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  text-align: left;
}

.style-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.cover {
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.95);
  text-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.meta {
  padding: 18px;
}

.meta h4 {
  font-size: 16px;
  margin-bottom: 8px;
}

.meta p {
  font-size: 13px;
  line-height: 1.6;
  color: #999;
  margin-bottom: 12px;
  min-height: 42px;
}

.cost {
  font-size: 13px;
  color: #ff7849;
  font-weight: 600;
}

/* 特性区 */
.features {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
  margin-top: 90px;
}

.features .card {
  background: #fff;
  border-radius: 16px;
  padding: 32px 24px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
}

.features .card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.icon {
  font-size: 36px;
  margin-bottom: 14px;
}

.features h3 {
  font-size: 17px;
  margin-bottom: 10px;
}

.features p {
  font-size: 14px;
  line-height: 1.7;
  color: #888;
}
</style>