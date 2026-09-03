<script setup lang="ts">
/**
 * VditorWriter — Vditor IR (Instant Rendering) 模式封装
 *
 * 与 ArticleEditor (Tiptap) 对齐的 API：
 *   props:  content / editable / uploadImage
 *   events: change / error
 *   expose: getMarkdown() / setMarkdown() / focus() / destroy()
 *
 * IR 模式原理（Vditor 内部实现）：每个块同时存在源与渲染两段 DOM，
 * 点击渲染区→光标移回源节点，输入/失焦→debounce 重渲染。
 * 这是市面上唯一原生实现 Typora 双形态的开源编辑器。
 */
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import Vditor from 'vditor'
import 'vditor/dist/index.css'
import '../theme/prose.css'
import './vditor-bridge.css'

const props = withDefaults(
  defineProps<{
    content: string
    editable?: boolean
    uploadImage?: (file: File) => Promise<string>
  }>(),
  { editable: true },
)

// ready：初始内容已灌入，此刻 getMarkdown() 才是 Lute 规范化后的真实值。宿主要拿它做
// 「有没有改动」的基线 —— autoSpace / fixTermTypo 会重写 markdown，直接拿播种的字符串
// 当基线会让文章一打开就显示成已修改。
const emit = defineEmits<{ change: []; error: [message: string]; ready: [] }>()

const containerRef = ref<HTMLDivElement | null>(null)
let vditor: Vditor | null = null
let themeObserver: MutationObserver | null = null
let codeLangObserver: MutationObserver | null = null

/**
 * 给每个 IR 代码块节点标注 data-lang,供 vditor-bridge.css 的
 * ::before { content: attr(data-lang) } 渲染 lang 标签
 * (无语言回退 'text',与 ArticleContent 的 wrapCodeBlock 一致)。
 * 只在值变化时写入 → 不触发 childList/characterData,无观察环。
 */
function annotateCodeBlocks(root: HTMLElement) {
  root.querySelectorAll<HTMLElement>('.vditor-ir__node[data-type="code-block"]').forEach((node) => {
    const info = node.querySelector('[data-type="code-block-info"]')?.textContent ?? ''
    const lang =
      info
        .replace(/\u200b/g, '')
        .trim()
        .toLowerCase() || 'text'
    if (node.dataset.lang !== lang) node.dataset.lang = lang
  })
}

function isDarkTheme(): boolean {
  return document.documentElement.dataset.theme === 'dark'
}

function syncEditorTheme() {
  if (!vditor) return
  // We use 'classic' with path='' so Vditor never injects the
  // dark.css <link> (which would win the cascade over prose.css).
  // The data-theme transition observer still re-runs this so any
  // background-color overrides in VditorWriter.vue stay in sync via
  // prose.css' dark-mode token mapping.
  vditor.setTheme('classic', 'classic')
}

onMounted(() => {
  if (!containerRef.value) return

  vditor = new Vditor(containerRef.value, {
    mode: 'ir',
    height: 'auto',
    placeholder: '开始写吧…',
    // 编辑器 UI 主题恒为 classic;暗色由 prose.css 的 token 映射负责,
    // 切到 Vditor 的 dark 会引入与详情页不一致的配色。
    theme: 'classic',
    icon: 'ant',
    cache: { enable: false },
    // 输入回调：只在 IR 模式触发
    input: () => emit('change'),
    // 聚焦/失焦也触发 change，便于保存状态机感知
    focus: () => emit('change'),
    blur: () => emit('change'),
    // 上传：Vditor handler 协议特殊 —— handler 返回 string 会被当成 error 弹 tip，
    // 返回 null/undefined Vditor 不自动插入图片。必须自己 insertValue 插入 Markdown。
    // 后端响应格式 {code, message, data: {fileUrl}} 也不符合 Vditor 的 succMap 协议，
    // 所以放弃 Vditor 的自动插入路径，改走全自管。
    upload: {
      accept: 'image/*',
      multiple: false,
      filename: (name: string) => name.replace(/[^\w.一-龥-]/g, '_'),
      validate: (files: File[]) => {
        if (!props.uploadImage) return '上传未配置'
        const file = files[0]
        if (!file) return '无文件'
        if (!file.type.startsWith('image/')) return '仅支持图片'
        if (file.size > 5 * 1024 * 1024) return '文件超出 5MB 限制'
        return true
      },
      // Vditor 把返回值类型写成 Promise<string> | Promise<null>,表达不了
      // 「成功返回 null、失败返回错误串」的 Promise<string | null>,是上游类型缺陷
      // @ts-expect-error -- upstream type is too narrow
      handler: async (files: File[]) => {
        const file = files[0]
        if (!file || !props.uploadImage) return '无文件'
        try {
          const url = await props.uploadImage(file)
          // 主动插入 Markdown 图片 —— Vditor 不会替我们做
          vditor?.insertValue(`\n![${file.name}](${url})\n`)
          return null
        } catch (e) {
          console.error('Image upload failed:', e)
          emit('error', `图片上传失败：${file.name}`)
          return `图片上传失败：${file.name}`
        }
      },
    },
    preview: {
      // Vditor 用内联 padding 把编辑列居中到该宽度,
      // 与阅读侧 --spacing-prose (800px) 保持一致
      maxWidth: 800,
      hljs: {
        enable: true,
        style: 'github',
        lineNumber: false,
      },
      // mermaid 主题不可配:Vditor 只在 options.theme === 'dark' 时切 dark,
      // 否则恒用 mermaid 默认主题,所以编辑侧图表配色与详情页的 neutral 有差异。
      theme: {
        current: isDarkTheme() ? 'dark' : 'light',
        list: { light: 'Light', dark: 'Dark' },
      },
      // markdown 选项：开启 GFM 全部特性，与项目 CommonMark + GFM 对齐
      markdown: {
        gfmAutoLink: true,
        footnotes: true,
        mark: true,
        toc: true,
        autoSpace: true,
        fixTermTypo: true,
        sanitize: true,
      },
    },
    // 工具栏：禁用。IR 模式 + Markdown 快捷键已覆盖所有操作，
    // 工具栏在两个端都是冗余 chrome（Web 后台有侧栏/顶栏/表单，桌面端是
    // 整窗即编辑器），留着反而把页面切成"工具栏→工具栏→编辑区"三段。
    toolbar: [],
    after: () => {
      vditor!.setValue(props.content || '')
      syncEditorTheme()
      // 监听项目暗色模式变化,跟随切换 Vditor 主题
      themeObserver = new MutationObserver(() => syncEditorTheme())
      themeObserver.observe(document.documentElement, {
        attributes: true,
        attributeFilter: ['data-theme'],
      })
      // 代码块 data-lang 标注:Lute spin 会整块重建 DOM,用 MutationObserver
      // 兜住所有重渲染路径(输入/粘贴/setValue/展开折叠)
      const irRoot = containerRef.value?.querySelector<HTMLElement>('.vditor-ir .vditor-reset')
      if (irRoot) {
        annotateCodeBlocks(irRoot)
        codeLangObserver = new MutationObserver(() => annotateCodeBlocks(irRoot))
        codeLangObserver.observe(irRoot, { childList: true, subtree: true, characterData: true })
      }
      // PoC 调试：暴露到 window 便于 DevTools 验证双向 I/O
      if (import.meta.env.DEV) {
        ;(window as unknown as { __vditorWriter: Vditor }).__vditorWriter = vditor!
      }
      emit('ready')
    },
  })
})

onBeforeUnmount(() => {
  themeObserver?.disconnect()
  themeObserver = null
  codeLangObserver?.disconnect()
  codeLangObserver = null
  vditor?.destroy()
  vditor = null
})

// 外部 content 变化时回灌（防循环：仅在 vditor 内部值与外部不一致时）
watch(
  () => props.content,
  (newContent) => {
    if (!vditor) return
    const current = vditor.getValue()
    if (current === newContent) return
    vditor.setValue(newContent || '', true)
  },
)

function getMarkdown(): string {
  return vditor?.getValue() ?? ''
}

function setMarkdown(md: string) {
  vditor?.setValue(md, true)
}

function focus() {
  vditor?.focus()
}

defineExpose({ getMarkdown, setMarkdown, focus })
</script>

<template>
  <div ref="containerRef" class="vditor-writer" />
</template>

<style>
/* unscoped：Vditor 接管容器后会把 class "vditor" 加在 .vditor-writer 上,scoped
   data-v 不再挂在 .vditor 元素上,:deep() 编译出的 [data-v-xxx] .vditor
   匹配不上。改用 .vditor-writer 作命名空间前缀,无需 scoped 也能定位。 */

/* Typora 模式：去掉 Vditor 自带容器边框/圆角/背景/阴影/outline,让编辑区"成为页面本身" */
.vditor-writer {
  border: none;
  border-radius: 0;
  background: transparent;
  box-shadow: none;
  outline: none;
}
.vditor-writer .vditor-toolbar {
  display: none;
}
.vditor-writer .vditor-content {
  min-height: 0;
  background: transparent;
  border: none;
  box-shadow: none;
}
.vditor-writer .vditor-reset,
.vditor-writer pre.vditor-reset {
  background: transparent !important;
}
.vditor-writer .vditor-reset:focus,
.vditor-writer pre.vditor-reset:focus {
  background: transparent !important;
}

/* Most typography/blockquote/code colors now come from prose.css
   (imported above) which targets .vditor-ir directly so the IR container
   matches the read-side ArticleContent pixel-for-pixel. */

/* Placeholder tone: warmer than the default cool grey in dark mode */
.vditor-writer .vditor-ir__node:empty::before,
.vditor-writer pre.vditor-reset[placeholder]:empty::before {
  color: var(--write-placeholder, var(--color-text-faint)) !important;
}

/* Accent color for the live-edit caret markers and link hint. */
.vditor-writer .vditor-ir__node--expand,
.vditor-writer .vditor-ir__link,
.vditor-writer .vditor-ir__marker--link {
  color: var(--color-accent);
}

/* 工具栏已禁用（toolbar: []），保留 IR 渲染与节点样式即可 */

/* 去掉 Vditor 默认的 hover 阴影（项目 design system：--shadow-card: none） */
.vditor-writer .vditor-ir__node {
  box-shadow: none !important;
  border-radius: 0 !important;
}
</style>
