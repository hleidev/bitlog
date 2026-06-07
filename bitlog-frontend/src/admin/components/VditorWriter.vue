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

const props = withDefaults(defineProps<{
  content: string
  editable?: boolean
  uploadImage?: (file: File) => Promise<string>
  minHeight?: number
}>(), { editable: true, minHeight: 600 })

const emit = defineEmits<{ change: []; error: [message: string] }>()

const containerRef = ref<HTMLDivElement | null>(null)
let vditor: Vditor | null = null
let themeObserver: MutationObserver | null = null

function isDarkTheme(): boolean {
  return document.documentElement.dataset.theme === 'dark'
}

function syncEditorTheme() {
  if (!vditor) return
  // setTheme(theme, contentTheme): theme 控编辑器本体, contentTheme 控 preview 区域
  // 内置 contentTheme: 'light' | 'dark'
  const contentTheme = isDarkTheme() ? 'dark' : 'light'
  vditor.setTheme(contentTheme === 'dark' ? 'dark' : 'classic', contentTheme)
}

onMounted(() => {
  if (!containerRef.value) return

  vditor = new Vditor(containerRef.value, {
    mode: 'ir',
    height: props.minHeight,
    placeholder: '开始写吧…',
    // 主题：跟随项目 [data-theme='dark']，由 syncEditorTheme 在 after 时设置
    theme: isDarkTheme() ? 'dark' : 'classic',
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
    // 工具栏：保留常用项，去掉与项目无关的微信/知乎导出
    toolbar: [
      'emoji', 'headings', 'bold', 'italic', 'strike', '|',
      'line', 'quote', 'list', 'ordered-list', 'check', '|',
      'code', 'inline-code', 'link', 'table', 'upload', '|',
      'undo', 'redo', 'fullscreen',
    ],
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

<style scoped>
.vditor-writer {
  width: 100%;
}

/* 套上项目 warm editorial + Lora 衬线 —— 用 CSS 变量,暗色模式自动切换 */
:deep(.vditor-ir) {
  font-family: 'Lora', 'Noto Serif SC', Georgia, serif;
  font-size: 16px;
  line-height: 1.75;
  color: var(--color-text-primary, #2a2520);
  background: var(--color-bg, #faf9f7);
}

:deep(.vditor-ir__preview) {
  font-family: inherit;
}

/* 主色与项目 --color-accent 对齐 */
:deep(.vditor-ir__node--expand),
:deep(.vditor-ir__link) {
  color: var(--color-accent, #b85c38);
}

:deep(.vditor-ir__blockquote) {
  border-left: 3px solid var(--color-accent, #b85c38);
  color: var(--color-text-secondary, #5a5248);
}

:deep(.vditor-ir__marker--link) {
  color: var(--color-accent, #b85c38);
}

:deep(.vditor-ir a) {
  color: var(--color-accent, #b85c38);
  text-decoration: none;
  border-bottom: 1px solid var(--color-accent-light, #e07b4f);
}

/* 工具栏：去掉默认阴影,套项目字体 + 暗色 */
:deep(.vditor-toolbar) {
  font-family: 'Inter', -apple-system, 'PingFang SC', sans-serif;
  background: var(--color-bg, #faf9f7);
  border-bottom: 1px solid var(--color-border, #e8e4de);
  box-shadow: none;
  color: var(--color-text-primary, #1a1610);
}

/* 工具栏激活按钮用 accent */
:deep(.vditor-menu--current) {
  color: var(--color-accent, #b85c38) !important;
  background: rgba(184, 92, 56, 0.08) !important;
}

/* 去掉 Vditor 默认的 hover 阴影（项目 design system：--shadow-card: none） */
:deep(.vditor-ir__node) {
  box-shadow: none !important;
  border-radius: 0 !important;
}

/* Vditor 自带分隔线/暗背景硬编码,这里覆盖避免暗色下反白 */
:deep(.vditor-ir__node--expand),
:deep(.vditor-ir__node--code) {
  background: var(--color-bg-card, #f4f2ef) !important;
  color: var(--color-text-primary, #1a1610) !important;
}
</style>
