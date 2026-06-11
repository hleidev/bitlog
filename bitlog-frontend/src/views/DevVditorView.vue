<script setup lang="ts">
/**
 * DEV-only:Vditor 编辑器 vs ArticleContent 阅读渲染的并排对照页。
 * 仅在 import.meta.env.DEV 下注册路由(/dev/vditor),不进生产构建。
 * 用于校准 vditor-bridge.css 与 prose.css 的渲染一致性,验证完可删。
 */
import { useRoute } from 'vue-router'
import VditorWriter from '@/admin/components/VditorWriter.vue'
import ArticleContent from '@/components/ArticleContent.vue'

// ?theme=dark 便于 headless 截图校验暗色渲染
if (useRoute().query.theme === 'dark') {
  document.documentElement.dataset.theme = 'dark'
}

const content = `# 一级标题 H1

对照段落:这是一段普通正文,用于核对字号、行高与颜色。包含**粗体**、*斜体*、~~删除线~~、\`行内代码\`和[链接文本](https://example.com)。

## 二级标题 H2

> 引用块:用于核对左边框、背景与斜体文字。

### 三级标题 H3

- 无序列表项一
- 无序列表项二

1. 有序列表项一
2. 有序列表项二

#### 四级标题 H4

| 表头一 | 表头二 |
| --- | --- |
| 单元格 | 单元格 |

---

![测试图片](https://picsum.photos/seed/bitlog/600/300)

\`\`\`typescript
const x: number = 1
function greet(name: string): string {
  return \`hello \${name}\`
}
\`\`\`

\`\`\`
skills/hl-diagram/
├── SKILL.md
└── references/
\`\`\`

\`\`\`mermaid
graph TD
  A[开始] --> B{判断}
\`\`\`

结尾段落,用于核对代码块下方间距。`
</script>

<template>
  <div class="dev-compare">
    <section>
      <h2 class="dev-label">VditorWriter(编辑)</h2>
      <VditorWriter :content="content" :editable="true" />
    </section>
    <section>
      <h2 class="dev-label">ArticleContent(阅读)</h2>
      <ArticleContent :content="content" />
    </section>
  </div>
</template>

<style scoped>
.dev-compare {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  padding: 40px;
  background: var(--color-bg);
  min-height: 100vh;
}
.dev-compare > section {
  min-width: 0;
  border: 1px dashed var(--color-border);
}
.dev-label {
  font-size: 12px;
  color: var(--color-text-muted);
  padding: 8px 12px;
  border-bottom: 1px dashed var(--color-border);
}
</style>
