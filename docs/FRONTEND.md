# 前端设计规范

> 这份文档是前端改动的唯一契约。新增页面、组件一律按它来，不允许自由发挥。
> 设计 token 定义在 `src/assets/main.css` 的 `@theme` 里。

---

## 1. 色彩

**规则：页面和组件里禁止出现硬编码色值（`#xxxxxx`），一律用 token。**

### 品牌色（暖橙）

| Token | 值 | 用途 |
|---|---|---|
| `brand-50` | `#fff5f0` | 极浅底、悬停背景、标签底 |
| `brand-100` | `#ffe6d9` | 浅底、装饰光晕 |
| `brand-200` | `#ffcdb5` | 装饰、浅边框 |
| `brand-300` | `#ffb08c` | 禁用态按钮底 |
| `brand-400` | `#ff9064` | — |
| **`brand-500`** | **`#ff7849`** | **主按钮、强调文字** |
| `brand-600` | `#f2622f` | 主按钮 hover、链接 |
| `brand-700` | `#d44d1d` | 主按钮 active |
| `brand-800/900` | 深色 | 极少用 |

### 中性色（文字与背景）

| Token | 用途 |
|---|---|
| `ink-900` | 大标题 |
| `ink-800` | 小标题 |
| `ink-700` | 正文（body 默认色） |
| `ink-600` | 次要正文、导航 |
| `ink-500` | 辅助说明 |
| `ink-400` | 弱提示、placeholder、页脚 |
| `ink-300` | 骨架块、滚动条 |
| `ink-200` | 分割线、输入框边框 |
| `ink-100` | 浅底、ghost 按钮底 |
| `surface` | 卡片/导航底色 `#ffffff` |
| `canvas` | 页面底色 `#f8f9fa` |

### 语义色

| Token | 用途 |
|---|---|
| `success` / `success-soft` | 成功提示、已完成徽章 |
| `warning` / `warning-soft` | 部分失败、等待中 |
| `danger` / `danger-soft` | 错误、删除、失败 |
| `info` / `info-soft` | 中性信息（原蓝色 `#5b7fff` 保留在这里） |

> **历史包袱**：老代码里蓝色 `#5b7fff` 被当成链接色和主按钮色用，新规范里它只作为 `info`。
> 主色统一为暖橙 —— 这是宠物/情感类产品该有的温度。

---

## 2. 间距

用 Tailwind 默认刻度（1 单位 = 4px）。**只用这几个**：

```
1 (4px)  2 (8px)  3 (12px)  4 (16px)  5 (20px)
6 (24px)  8 (32px)  10 (40px)  12 (48px)  16 (64px)
```

页面级纵向间距：区块之间 `mt-16 sm:mt-20`；卡片内部 `p-4`（小）/ `p-5`（中）/ `p-6 sm:p-8`（大）。

---

## 3. 圆角

| 类 | 值 | 用途 |
|---|---|---|
| `rounded-lg` | 8px | 小元素（导航 hover 底） |
| `rounded-xl` | 12px | 输入框、按钮 |
| `rounded-2xl` | 16px | 卡片、图片 |
| `rounded-3xl` | 24px | 大卡片、对话框、Hero 区 |
| `rounded-full` | — | 徽章、头像、胶囊按钮 |

---

## 4. 字号

| 类 | 用途 |
|---|---|
| `text-xs` (12px) | 标签、辅助、页脚 |
| `text-sm` (14px) | 正文默认、按钮 |
| `text-base` (16px) | 强调正文 |
| `text-lg` (18px) | 小标题、对话框标题 |
| `text-xl` (20px) | 区块标题 |
| `text-2xl`/`text-3xl` | 页面主标题 |
| `text-3xl`/`text-5xl` | Hero（响应式：`text-3xl sm:text-5xl`） |

字重只用三档：`font-normal`（正文）、`font-semibold`（强调）、`font-bold`（标题）。

---

## 5. 阴影

| 类 | 用途 |
|---|---|
| `shadow-card` | 卡片默认 |
| `shadow-hover` | 卡片悬停 |
| `shadow-pop` | 浮层、对话框、Toast |

---

## 6. 响应式断点

只在 `sm`（640px）、`md`（768px）、`lg`（1024px）三个档位做适配。

| 场景 | 桌面 | 移动 |
|---|---|---|
| 风格网格 | `lg:grid-cols-4` | `grid-cols-2` |
| 作品网格 | `sm:grid-cols-2 lg:grid-cols-3` | `grid-cols-2` |
| 导航链接 | 横排 | 折叠进汉堡菜单 |
| 页面内边距 | `px-6 lg:px-8` | `px-4` |

**硬性要求：375px 宽度下不允许出现横向滚动条。**

---

## 7. 基础组件

统一从 `@/components/base` 导入。

### BaseButton

```
variant: primary | secondary | ghost | danger | dangerGhost   （默认 primary）
size:    sm | md | lg                                          （默认 md）
loading / disabled / block / type
```

- 主操作 → `primary`
- 次要 → `secondary`（带边框）
- 弱操作（退出、取消）→ `ghost`
- **不可逆操作（删除、撤销）→ `danger` 或 `dangerGhost`**
- 异步按钮**必须**传 `loading`，它会自动禁用防重复提交

### BaseCard

```
padding: none | sm | md | lg   （默认 md）
hoverable / flat
```

### BaseInput

```
v-model / label / placeholder / type / error / hint / disabled / maxlength
@enter 事件（回车提交表单）
```

`error` 和 `hint` 互斥，`error` 优先。

### BaseModal

```
v-model / title / maxWidth / closable / maskClosable
```

自带：ESC 关闭、锁 body 滚动、淡入 + 弹出动画。**离开页面时会自动还原 body 滚动**。

### ConfirmDialog

```
v-model / title / message / confirmText / cancelText / danger / loading
@confirm
```

**所有 `confirm()` 都必须换成它。** 删除类操作传 `danger`，`message` 里要写清后果。

### BaseBadge

```
tone: neutral | brand | success | warning | danger | info
size: sm | md
```

### BaseSkeleton

```
count / rounded / size   ← size 传高度宽度类，如 "h-4 w-1/2"
```

或用 `.skeleton` 类自己拼（如整块封面占位）。

### EmptyState

```
title / description / emoji
#action 插槽放按钮
```

### toast（函数式）

```js
import { toast } from '@/components/base'

toast.success('已删除')
toast.error(e.message)
toast.warn('...')
toast.info('...')
```

**所有 `alert()` 都必须换成它。**

---

## 8. 页面状态（硬性要求）

每个有数据请求的页面，必须处理四种状态：

| 状态 | 做法 |
|---|---|
| loading | 骨架屏（`BaseSkeleton` / `.skeleton`），**不要用"加载中…"文字** |
| error | 错误提示 + 重试按钮，或 `toast.error` + `EmptyState` |
| empty | `EmptyState` + 引导按钮 |
| ready | 正常内容 |

---

## 9. 错误处理规范

**一次 `catch` 只对应一件事。** 反例（真实踩过的坑）：

```js
try {
  await deleteOrder(id)
  alert('已删除')
  router.push('/orders')     // router 未定义 → 抛错
} catch (e) {
  alert(e.message)           // 提示"删除失败"，但删除其实成功了
}
```

正确写法：

```js
try {
  await deleteOrder(order.value.id)
} catch (e) {
  toast.error(e.message || '删除失败')
  return                      // 失败才返回
}
toast.success('已删除')
router.push('/orders')        // 导航失败不该说成"删除失败"
```

---

## 10. 可访问性

- 图标按钮必须有 `aria-label`
- 对话框用 `role="dialog"` + `aria-modal="true"`
- Toast 容器 `role="status" aria-live="polite"`
- 只用 emoji 做装饰时加 `aria-hidden="true"`

---

## 11. 图标

暂不引入图标库，用内联 SVG（`viewBox="0 0 24 24"`，`stroke-width="2"`，`stroke-linecap="round"`）。

尺寸：小图标 `size-3.5` / `size-4`，按钮旁 `size-5`。

---

## 12. 文件组织

```
src/components/base/     ← 无业务的基础组件（本规范覆盖）
src/components/          ← 业务组件（PhotoUploader 等）
src/composables/         ← 可复用的组合式逻辑（轮询、下载、分享）
src/views/               ← 页面，只负责组装，样式尽量用工具类，避免大段 <style>
```

>`<style scoped>` 默认不再新增。确实需要（如复杂动画）才写，并且只能用 token 变量。
