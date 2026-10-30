/**
 * 提示词实验：验证「模板 + 用户补充」与「模板 + 风格参考图」两条扩展路线。
 *
 * 回答四个问题：
 *   Q1 加了用户补充后，人脸保真度会不会下降
 *   Q2 三张参考图会不会混淆（把风格图里的人/宠物也画进来）
 *   Q3 中文补充模型能否正确理解
 *   Q4 失败率是否上升（观察即可）
 *
 * 用法（需要环境变量 DASHSCOPE_API_KEY、AI_BASE_URL）：
 *   node scripts/prompt-experiment.mjs
 *
 * 产物落在 scripts/experiment-out/ 下，只用于本地评估，已 gitignore。
 */

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { resolve, join } from 'node:path'

const API_KEY = process.env.DASHSCOPE_API_KEY || ''
const BASE_URL = (process.env.AI_BASE_URL || '').replace(/\/+$/, '')
const MODEL = process.env.AI_MODEL || 'qwen-image-3.0-pro'

const OUT_DIR = resolve(process.cwd(), 'scripts/experiment-out')

/* ---------------- 素材 ---------------- */
const FILES = 'C:/Projects/_files/upload'
// 两张都是人像，所以宠物图与风格图由模型先生成，作为"用户会提供的素材"替身
const PERSON = `${FILES}/2026-10-21/e5a668b1-6cba-41aa-8985-490e5c4e6e5d.jpg`

/* ---------------- 与后端保持一致的模板 prompt ---------------- */
const STYLE_PROMPT =
  'soft natural window light, pastel tones, gentle mood, film grain, ' +
  'cozy japanese interior, person with pet, 85mm portrait lens, high detail'

/** 后端目前的硬约束（从 AiPhotoGenerator 抄来，保证实验与线上一致） */
const HARD =
  'Preserve the exact facial features of the person in image 1 and the exact fur color ' +
  'and markings of the pet in image 2. Generate a single photograph, not a collage or grid.'

const USER_NOTE = '给人物戴一顶生日帽，背景里加一些彩色气球'

/* ---------------- 工具 ---------------- */

function dataUri(path) {
  return 'data:image/jpeg;base64,' + readFileSync(path).toString('base64')
}

const sleep = (ms) => new Promise((r) => setTimeout(r, ms))

async function generate(label, prompt, images) {
  const started = Date.now()
  const body = {
    model: MODEL,
    prompt,
    n: 1,
    size: '1024x1024',
    prompt_extend: false,
    watermark: false
  }

  // ★ 文生图必须完全省略 image 字段，传空数组会被服务端拒绝
  if (images && images.length > 0) {
    body.image = images
  }

  const res = await fetch(`${BASE_URL}/images/generations`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${API_KEY}`
    },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(600000)
  })

  const text = await res.text()
  let json
  try {
    json = JSON.parse(text)
  } catch {
    throw new Error(`响应不是 JSON（HTTP ${res.status}）：${text.slice(0, 300)}`)
  }

  if (!res.ok) {
    throw new Error(`HTTP ${res.status}：${JSON.stringify(json).slice(0, 400)}`)
  }

  // 兼容 OpenAI 兼容模式与 DashScope 原生两种返回结构
  const url =
    json?.data?.[0]?.url ||
    json?.output?.results?.[0]?.url ||
    json?.output?.results?.[0]?.b64_image

  if (!url) {
    throw new Error(`没取到图片地址：${JSON.stringify(json).slice(0, 400)}`)
  }

  let localPath
  if (url.startsWith('http')) {
    const imgRes = await fetch(url, { signal: AbortSignal.timeout(120000) })
    const buf = Buffer.from(await imgRes.arrayBuffer())
    localPath = join(OUT_DIR, `${label}.jpg`)
    writeFileSync(localPath, buf)
  } else {
    // base64
    localPath = join(OUT_DIR, `${label}.jpg`)
    writeFileSync(localPath, Buffer.from(url, 'base64'))
  }

  console.log(`✅ ${label.padEnd(24)} ${((Date.now() - started) / 1000).toFixed(1)}s → ${localPath}`)
  return localPath
}

async function main() {
  if (!API_KEY || !BASE_URL) {
    console.error('缺少 DASHSCOPE_API_KEY 或 AI_BASE_URL')
    process.exit(1)
  }

  mkdirSync(OUT_DIR, { recursive: true })
  console.log(`模型：${MODEL}\n输出：${OUT_DIR}\n`)

  /* ---- 0. 准备两张素材：宠物照、风格参考图 ---- */
  console.log('--- 准备素材 ---')

  const petPath = await generate(
    '00-pet-ref',
    'a golden retriever sitting calmly on a light grey sofa, looking at camera, ' +
      'natural daylight, full body visible, sharp focus, photograph, no people',
    []
  )
  await sleep(2000)

  const stylePath = await generate(
    '00-style-ref',
    'watercolor painting of a sunflower field at golden hour, warm amber and soft green ' +
      'palette, loose brush strokes, dreamy atmosphere, warm glowing light, no people, no animals',
    []
  )
  await sleep(2000)

  /* ---- 1. 基线：纯模板 ---- */
  console.log('\n--- 三组对比 ---')

  await generate('01-template-only', `${STYLE_PROMPT}. ${HARD}`, [
    dataUri(PERSON),
    dataUri(petPath)
  ])
  await sleep(2000)

  /* ---- 2. 模板 + 中文补充 ---- */
  await generate('02-template-plus-note', `${STYLE_PROMPT}. ${USER_NOTE}. ${HARD}`, [
    dataUri(PERSON),
    dataUri(petPath)
  ])
  await sleep(2000)

  /* ---- 3. 模板 + 风格参考图（第 3 张） ---- */
  const t3Prompt =
    `${STYLE_PROMPT}. ` +
    'Image 3 is a STYLE REFERENCE only: borrow its warm amber palette, glowing light and ' +
    'loose watercolor atmosphere. Do NOT include anything from image 3 itself. ' +
    `Person is in image 1, pet is in image 2. ${HARD}`

  await generate('03-template-plus-style-ref', t3Prompt, [
    dataUri(PERSON),
    dataUri(petPath),
    dataUri(stylePath)
  ])

  console.log('\n完成。三组结果都在 scripts/experiment-out/ 下。')
}

main().catch((e) => {
  console.error('\n❌ 失败：', e.message)
  process.exit(1)
})
