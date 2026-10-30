/**
 * 第二轮实验：验证「换画风」这件事到底行不行。
 *
 * 上一轮结论：风格参考图能转移「色调/光线」，但画风没变（还是照片）。
 * 这一轮专门测「主动要求画风」的两条路：
 *   S1 靠文字要求画风（只给人物 + 宠物两张图）
 *   S2 靠风格参考图要求画风（给三张图，并明确"模仿它的画风"）
 *
 * 关注点：画风是否真的变了，以及变了之后人脸 / 宠物还像不像 ——
 * 「像不像」是写真产品的命根子，画风是加分项，不能反过来。
 *
 * 用法：node scripts/style-transfer-experiment.mjs
 */

import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { resolve, join } from 'node:path'

const API_KEY = process.env.DASHSCOPE_API_KEY || ''
const BASE_URL = (process.env.AI_BASE_URL || '').replace(/\/+$/, '')
const MODEL = process.env.AI_MODEL || 'qwen-image-3.0-pro'

const OUT_DIR = resolve(process.cwd(), 'scripts/experiment-out')

const PERSON = 'C:/Projects/_files/upload/2026-10-21/e5a668b1-6cba-41aa-8985-490e5c4e6e5d.jpg'
const PET = resolve(OUT_DIR, '00-pet-ref.png')
const STYLE = resolve(OUT_DIR, '00-style-ref.png')

const STYLE_PROMPT =
  'soft natural window light, pastel tones, gentle mood, cozy japanese interior, ' +
  'person with pet, 85mm portrait lens, high detail'

const HARD =
  'Preserve the exact facial features of the person in image 1 and the exact fur color ' +
  'and markings of the pet in image 2. Generate a single image, not a collage or grid.'

/** 按真实扩展名给正确的 MIME —— 服务端对 MIME 与字节不一致是宽容的，但不该依赖它 */
function dataUri(path) {
  const buf = readFileSync(path)
  const mime = path.toLowerCase().endsWith('.png') ? 'png' : 'jpeg'
  return `data:image/${mime};base64,` + buf.toString('base64')
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
  if (images.length) body.image = images

  const res = await fetch(`${BASE_URL}/images/generations`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${API_KEY}` },
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
  if (!res.ok) throw new Error(`HTTP ${res.status}：${JSON.stringify(json).slice(0, 400)}`)

  const url =
    json?.data?.[0]?.url || json?.output?.results?.[0]?.url || json?.output?.results?.[0]?.b64_image
  if (!url) throw new Error(`没取到图片地址：${JSON.stringify(json).slice(0, 400)}`)

  let buf
  let ext = 'png'
  if (url.startsWith('http')) {
    const imgRes = await fetch(url, { signal: AbortSignal.timeout(120000) })
    buf = Buffer.from(await imgRes.arrayBuffer())
    // 按魔数定扩展名，上一轮就因为当成 jpg 存而读不出来
    if (buf[0] === 0xff && buf[1] === 0xd8) ext = 'jpg'
  } else {
    buf = Buffer.from(url, 'base64')
  }

  const file = join(OUT_DIR, `${label}.${ext}`)
  writeFileSync(file, buf)
  console.log(
    `✅ ${label.padEnd(30)} ${((Date.now() - started) / 1000).toFixed(1)}s → ${file}`
  )
  return file
}

async function main() {
  if (!API_KEY || !BASE_URL) {
    console.error('缺少 DASHSCOPE_API_KEY 或 AI_BASE_URL')
    process.exit(1)
  }
  mkdirSync(OUT_DIR, { recursive: true })
  console.log(`模型：${MODEL}\n`)

  /* ---- S1：只靠文字要求水彩画风 ---- */
  await generate(
    'S1-watercolor-by-text',
    `${STYLE_PROMPT}. ` +
      'Render the whole scene as a watercolor painting: loose brush strokes, ' +
      'soft washes, visible watercolor paper texture, painterly edges. ' +
      `${HARD}`,
    [dataUri(PERSON), dataUri(PET)]
  )
  await sleep(2000)

  /* ---- S2：靠风格参考图要求模仿画风 ---- */
  await generate(
    'S2-watercolor-by-ref',
    `${STYLE_PROMPT}. ` +
      'Image 3 is a STYLE REFERENCE. Reproduce its watercolor painting technique: ' +
      'loose brush strokes, soft washes, visible paper texture. ' +
      'Do NOT include anything that appears in image 3 (no sunflowers, no field, no landscape). ' +
      'Person is in image 1, pet is in image 2. ' +
      `${HARD}`,
    [dataUri(PERSON), dataUri(PET), dataUri(STYLE)]
  )

  console.log('\n完成。')
}

main().catch((e) => {
  console.error('\n❌ 失败：', e.message)
  process.exit(1)
})
