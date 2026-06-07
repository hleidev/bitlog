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
import '@/components/prose.css'

const props = withDefaults(defineProps<{
  content: string
  editable?: boolean
  uploadImage?: (file: File) => Promise<string>
}>(), { editable: true })

const emit = defineEmits<{ change: []; error: [message: string] }>()

const containerRef = ref<HTMLDivElement | null>(null)
let vditor: Vditor | null = null
let themeObserver: MutationObserver | null = null

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
    // 主题：永远 classic + path='' 避免 Vditor 注入 dark.css link。
    // 否则 unpkg.com/.../content-theme/dark.css 作为 <body> 末尾的 <link>
    // 会赢过 prose.css 的 cascade,导致 blockquote / inline code 与详情页不一致。
    // 我们自己已经在 VditorWriter <style> 里处理 IR preview 的暗色配色。
    theme: { current: 'classic', path: '' },
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
      hljs: {
        enable: true,
        style: 'github',
        lineNumber: false,
      },
      // 关闭数学公式的 MathJax 引擎（节省 6.4MB）—— 后续如果需要再开 KaTeX
      math: { enable: false },
      // Vditor 内置 mermaid / flowchart / graphviz 渲染（CDN 加载,无需 enable 开关）
      // 写作者在 IR 模式下输入 ```mermaid 代码块 → 立即看到图表,
      // 与发布后详情页的 Lute + 客户端 mermaid.render() 路径视觉一致。
      mermaid: {
        theme: isDarkTheme() ? 'dark' : 'default',
      },
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
      // PoC 调试：暴露到 window 便于 DevTools 验证双向 I/O
      if (import.meta.env.DEV) {
        (window as unknown as { __vditorWriter: Vditor }).__vditorWriter = vditor!
      }
    },
  })
})

onBeforeUnmount(() => {
  themeObserver?.disconnect()
  themeObserver = null
  vditor?.destroy()
  vditor = null
})

// 外部 content 变化时回灌（防循环：仅在 vditor 内部值与外部不一致时）
watch(() => props.content, (newContent) => {
  if (!vditor) return
  const current = vditor.getValue()
  if (current === newContent) return
  vditor.setValue(newContent || '', true)
})

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
  color: var(--write-placeholder, var(--color-text-faint, #ccc5bc)) !important;
}

/* Accent color for the live-edit caret markers and link hint. */
.vditor-writer .vditor-ir__node--expand,
.vditor-writer .vditor-ir__link,
.vditor-writer .vditor-ir__marker--link {
  color: var(--color-accent, #b85c38);
}

/* 工具栏已禁用（toolbar: []），保留 IR 渲染与节点样式即可 */

/* 去掉 Vditor 默认的 hover 阴影（项目 design system：--shadow-card: none） */
.vditor-writer .vditor-ir__node {
  box-shadow: none !important;
  border-radius: 0 !important;
}

/* Vditor's default blockquote style is a thin grey bar. Reset to match
   prose.css — done in prose.css via .vditor-ir blockquote, so no override
   needed here. Vditor's default pre block IS white, however; force it to
   transparent so prose.css .code-block-wrapper (when wrapped by
   ArticleContent) shows its dark background. The plain IR view does not
   have a wrapper, so we explicitly theme its <pre> here. */
.vditor-writer pre.vditor-reset {
  background: #282c34 !important;
  color: #abb2bf;
  padding: 16px 22px;
  border-radius: 4px;
  font-family: var(--font-mono);
  font-size: 13.5px;
  line-height: 1.65;
  margin: 26px 0;
}
</style>
