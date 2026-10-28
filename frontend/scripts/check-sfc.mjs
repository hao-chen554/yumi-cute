import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { parse } from '@vue/compiler-sfc'

/**
 * 快速定位 .vue 文件的解析错误（比如「Element is missing end tag」）。
 *
 * 用法：
 *   node scripts/check-sfc.mjs src/views/OrderDetailView.vue
 *   node scripts/check-sfc.mjs            # 检查 src 下全部 .vue
 */
import { globSync } from 'node:fs'

const targets = process.argv.slice(2).length
  ? process.argv.slice(2)
  : globSync('src/**/*.vue')

let bad = 0

for (const file of targets) {
  const abs = resolve(process.cwd(), file)
  let src
  try {
    src = readFileSync(abs, 'utf8')
  } catch {
    console.log(`跳过（读不到）: ${file}`)
    continue
  }

  const { errors } = parse(src, { filename: abs })

  if (!errors.length) {
    console.log(`✅ ${file}`)
    continue
  }

  bad++
  console.log(`\n❌ ${file}`)
  const lines = src.split('\n')
  for (const e of errors) {
    console.log(`   ${e.message}`)
    if (!e.loc) continue
    const startLine = e.loc.start.line
    console.log(`   位置：第 ${startLine} 行，第 ${e.loc.start.column} 列`)
    const from = Math.max(0, startLine - 4)
    const to = Math.min(lines.length, startLine + 3)
    for (let i = from; i < to; i++) {
      const mark = i + 1 === startLine ? '>>' : '  '
      console.log(`   ${mark} ${String(i + 1).padStart(4)} | ${lines[i]}`)
    }
  }
}

console.log(`\n检查 ${targets.length} 个文件，${bad} 个有问题`)
process.exit(bad ? 1 : 0)
