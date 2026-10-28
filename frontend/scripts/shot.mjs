import { chromium } from 'playwright'
import { mkdirSync } from 'node:fs'
import { resolve } from 'node:path'

/**
 * 自动截图脚本 —— 让 AI（或你）能"看见"页面。
 *
 * 用法：
 *   node scripts/shot.mjs                     # 用默认页面清单，三种宽度全截
 *   node scripts/shot.mjs /orders /recharge   # 只截指定路由
 *   node scripts/shot.mjs /orders --w=375     # 只截一种宽度
 *
 * 产物落在 frontend/.screenshots/ 下，文件名形如
 *   orders__375.png / orders__768.png / orders__1440.png
 *
 * 前置：先在另一个终端跑着 `npm run dev`（默认 http://localhost:5173）
 */

const BASE = process.env.SHOT_BASE || 'http://localhost:5173'
const OUT_DIR = resolve(process.cwd(), '.screenshots')
const WIDTHS = [375, 768, 1440]

/**
 * 有的页面需要登录态（/orders、/profile、/create、/recharge）。
 * 设了 SHOT_TOKEN 就会在每次导航前注入 localStorage，绕过登录页。
 *
 * 拿 token 的方式：调用 POST /user/login，把返回的 data.token 塞进环境变量。
 */
const TOKEN = process.env.SHOT_TOKEN || ''
const USER_INFO = process.env.SHOT_USER || ''

/** 默认要截的页面。带 :id 的会在登录后自动取真实 id 替换。 */
const DEFAULT_PAGES = [
  '/',
  '/login',
  '/register',
  '/create',
  '/orders',
  '/recharge',
  '/profile'
]

function parseArgs(argv) {
  const pages = []
  let widths = WIDTHS

  for (const a of argv) {
    if (a.startsWith('--w=')) {
      widths = a
        .slice(4)
        .split(',')
        .map((n) => Number(n))
        .filter(Boolean)
    } else if (a.startsWith('/')) {
      pages.push(a)
    }
  }

  return { pages: pages.length ? pages : DEFAULT_PAGES, widths }
}

/** 路由路径 → 安全文件名 */
function toFileName(route) {
  const clean = route.replace(/\//g, '_').replace(/^_/, '') || 'home'
  return clean.replace(/[^a-zA-Z0-9_-]/g, '-')
}

async function main() {
  const { pages, widths } = parseArgs(process.argv.slice(2))
  mkdirSync(OUT_DIR, { recursive: true })

  const browser = await chromium.launch()
  let ok = 0
  let fail = 0

  for (const width of widths) {
    const context = await browser.newContext({
      viewport: { width, height: 900 },
      deviceScaleFactor: 1
    })
    const page = await context.newPage()

    // 注入登录态（如果提供了 token）
    if (TOKEN) {
      await page.addInitScript(
        ([t, info]) => {
          localStorage.setItem('token', t)
          if (info) localStorage.setItem('userInfo', info)
        },
        [TOKEN, USER_INFO]
      )
    }

    for (const route of pages) {
      const url = BASE + route
      try {
        await page.goto(url, { waitUntil: 'networkidle', timeout: 20000 })

        // 隐藏 Vue DevTools 的悬浮面板 —— 它固定在屏幕中间，会挡住内容
        await page
          .addStyleTag({
            content: `
              [class*="vue-devtools"], [id*="vue-devtools"],
              [class*="__vue-devtools"], [id*="__vue-devtools"],
              vue-devtools, .vite-plugin-vue-devtools {
                display: none !important;
              }
            `
          })
          .catch(() => {})

        // 等动画和图片稳定
        await page.waitForTimeout(600)

        const file = resolve(OUT_DIR, `${toFileName(route)}__${width}.png`)
        await page.screenshot({ path: file, fullPage: true })
        console.log(`✅ ${route} @${width}px  →  .screenshots/${toFileName(route)}__${width}.png`)
        ok++
      } catch (e) {
        console.log(`❌ ${route} @${width}px  ${e.message.split('\n')[0]}`)
        fail++
      }
    }

    await context.close()
  }

  await browser.close()
  console.log(`\n完成：成功 ${ok} 张，失败 ${fail} 张`)
  console.log(`输出目录：${OUT_DIR}`)
}

main().catch((e) => {
  console.error(e)
  process.exit(1)
})
