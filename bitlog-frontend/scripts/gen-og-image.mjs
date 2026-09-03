import sharp from 'sharp'
import { fileURLToPath } from 'url'
import { join, dirname } from 'path'

const __dirname = dirname(fileURLToPath(import.meta.url))
const OUT = join(__dirname, '../public/og-image.png')

const W = 1200
const H = 630

// Design tokens — mirrors variables.css
const COLOR_BG      = '#faf9f7'
const COLOR_ACCENT  = '#b85c38'
const COLOR_PRIMARY = '#1a1a18'
const COLOR_MUTED   = '#8a8a82'
const COLOR_FAINT   = '#b8b8b0'

const svg = /* xml */`
<svg width="${W}" height="${H}" xmlns="http://www.w3.org/2000/svg">

  <!-- Background -->
  <rect width="${W}" height="${H}" fill="${COLOR_BG}"/>

  <!-- Top accent stripe -->
  <rect width="${W}" height="4" fill="${COLOR_ACCENT}"/>

  <!-- Vertical accent bar next to content -->
  <rect x="80" y="216" width="3" height="132" fill="${COLOR_ACCENT}"/>

  <!-- Label -->
  <text
    x="108" y="248"
    font-family="Georgia, serif"
    font-size="13"
    font-weight="400"
    letter-spacing="5"
    fill="${COLOR_ACCENT}"
    text-anchor="start"
  >PERSONAL TECH BLOG</text>

  <!-- Site name -->
  <text
    x="108" y="332"
    font-family="Georgia, 'Noto Serif SC', serif"
    font-size="88"
    font-weight="400"
    letter-spacing="2"
    fill="${COLOR_PRIMARY}"
    text-anchor="start"
  >BitLog</text>

  <!-- Separator line -->
  <rect x="108" y="354" width="56" height="2" fill="${COLOR_ACCENT}"/>

  <!-- Tagline -->
  <text
    x="108" y="396"
    font-family="-apple-system, 'PingFang SC', sans-serif"
    font-size="22"
    font-weight="400"
    letter-spacing="1"
    fill="${COLOR_MUTED}"
    text-anchor="start"
  >Harry Lei · 个人技术博客</text>

  <!-- URL — bottom right -->
  <text
    x="${W - 80}" y="${H - 52}"
    font-family="Georgia, monospace"
    font-size="16"
    font-weight="400"
    letter-spacing="2"
    fill="${COLOR_FAINT}"
    text-anchor="end"
  >bitlog.harrylei.top</text>

</svg>
`

await sharp(Buffer.from(svg))
  .png({ compressionLevel: 9 })
  .toFile(OUT)

console.log(`✓ og-image.png → ${OUT}`)
