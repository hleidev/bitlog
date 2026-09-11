import sharp from 'sharp'
import { readFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'

// 分享图与 favicon 都由可编辑的矢量源生成；不在普通构建时依赖本机字体。
const output = (name) => fileURLToPath(new URL(`../public/${name}`, import.meta.url))
const design = await readFile(new URL('../src/assets/styles/design.css', import.meta.url), 'utf8')
function color(name) {
  const value = design.match(new RegExp(`--${name}:\\s*(#[0-9a-fA-F]+);`))?.[1]
  if (!value) throw new Error(`Missing design color: ${name}`)
  return value
}

const blue = color('journal-blue')
const primary = color('color-text-primary')
const muted = color('color-text-muted')
const svg = `
<svg width="1200" height="630" xmlns="http://www.w3.org/2000/svg">
  <rect width="1200" height="630" fill="${color('color-bg')}"/>
  <rect x="80" y="112" width="9" height="9" fill="${blue}"/>
  <text x="108" y="123" font-family="monospace" font-size="15" letter-spacing="3" fill="${muted}">HARRY'S PERSONAL JOURNAL</text>
  <text x="76" y="294" font-family="Georgia, serif" font-size="120" fill="${primary}">BitLog<tspan fill="${blue}">.</tspan></text>
  <text x="80" y="378" font-family="'Songti SC', STSong, 'Noto Serif SC', serif" font-size="38" fill="${primary}">文字有尽，折腾不止。</text>
  <text x="82" y="434" font-family="'PingFang SC', sans-serif" font-size="22" fill="${muted}">Harry / 随写随记</text>
  <rect x="824" y="80" width="296" height="390" fill="${blue}"/>
  <rect x="840" y="96" width="264" height="358" fill="none" stroke="#ffffff" stroke-opacity="0.25"/>
  <text x="856" y="132" font-family="monospace" font-size="12" letter-spacing="2" fill="#ffffff">BITLOG / THE JOURNAL</text>
  <text x="868" y="386" font-family="Georgia, serif" font-size="300" font-style="italic" fill="#ffffff">b</text>
  <circle cx="1052" cy="374" r="14" fill="#c7ff8b"/>
  <text x="856" y="430" font-family="monospace" font-size="12" fill="#ffffff">Bits of code. Notes on life.</text>
  <path d="M80 526H1120" stroke="${color('color-border-strong')}"/>
  <text x="80" y="570" font-family="monospace" font-size="16" fill="${muted}">bitlog.harrylei.top</text>
  <text x="1120" y="570" text-anchor="end" font-family="monospace" font-size="13" letter-spacing="2" fill="${muted}">UNTIL THE NEXT NOTE</text>
</svg>`

await sharp(Buffer.from(svg)).png({ compressionLevel: 9 }).toFile(output('og-image.png'))
await sharp(await readFile(output('favicon.svg')))
  .resize(64, 64)
  .png()
  .toFile(output('favicon.png'))
console.log('Generated og-image.png (1200×630) and favicon.png (64×64)')
