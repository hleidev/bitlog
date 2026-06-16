# 公开站点加载体验升级 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让公开站点（首页、文章列表、文章详情）在后端慢时不再出现"长时间空白/空骨架"，把感知拉到主流社区基准线：动画骨架屏 + hover 预取 + 内存缓存秒开 + 慢加载兜底提示。

**Architecture:** 不改动现有 vite-ssg 架构，只在客户端（CSR 取数那一层）做体验增强。新增一个零依赖的文章详情**内存缓存 + 在途去重 + 预取**模块；新增一个可复用的**列表骨架屏组件**；列表页/首页在"首屏无数据"时渲染骨架屏而非空白；详情页命中缓存时直接秒出内容；超过阈值显示"加载较慢"提示。

**为什么不引入 TanStack Vue Query：** 该库虽是社区标准，但与 vite-ssg 的 SSR/SSG hydration 集成繁琐、风险高；本站取数场景只有"按 id 取详情""按筛选取列表"两种，需求很窄。按 Simplicity First / 最少代码原则，手写一个约 30 行的缓存模块即可，且避免改动 `main.ts` 的 SSG 入口。若未来取数场景显著增多，可再迁移到 vue-query。

**Tech Stack:** Vue 3 `<script setup>` + TypeScript、vue-router、vite-ssg（不改）、scoped CSS（复用现有 `shimmer` keyframe 与设计 token）。

**关于验证：** 本项目无测试套件（见 CLAUDE.md "No test suite exists"）。每个任务的验证 = `npm run type-check` 通过 + `npm run lint` 无新增错误 + 浏览器手动观察指定现象。要稳定复现"后端慢"，在浏览器 DevTools → Network 面板把节流设为 "Slow 3G"，或临时在被调函数里 `await new Promise(r => setTimeout(r, 3000))`（验证后务必删除）。

---

### Task 1: 可复用的列表骨架屏组件

**Files:**
- Create: `src/components/common/ArticleListSkeleton.vue`

骨架行的布局要对齐 `ArticleListView.vue` 的 `.article-row`（date 列 + 标题 + 摘要两行），并复用详情页已有的 `shimmer` 流光动画，确保"在加载"而不是"空内容"的视觉信号一致。

- [ ] **Step 1: 创建骨架组件**

`src/components/common/ArticleListSkeleton.vue`:

```vue
<script setup lang="ts">
withDefaults(defineProps<{ rows?: number }>(), { rows: 6 })
</script>

<template>
  <div class="skeleton-list" aria-hidden="true">
    <div v-for="i in rows" :key="i" class="skeleton-row">
      <div class="sk sk-date" />
      <div class="sk-body">
        <div class="sk sk-title" />
        <div class="sk sk-excerpt" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-list {
  border-top: 1px solid var(--color-border);
}

.skeleton-row {
  display: flex;
  align-items: center;
  gap: 40px;
  padding: 28px 0;
  border-bottom: 1px solid var(--color-border);
}

.sk {
  border-radius: var(--admin-radius);
  background: linear-gradient(
    90deg,
    var(--color-bg-hover) 25%,
    var(--color-border) 50%,
    var(--color-bg-hover) 75%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s infinite;
}

.sk-date {
  width: 80px;
  height: 11px;
  flex-shrink: 0;
}

.sk-body {
  flex: 1;
  max-width: 680px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.sk-title {
  width: 60%;
  height: 18px;
}

.sk-excerpt {
  width: 90%;
  height: 13px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

@media (max-width: 640px) {
  .sk-date { display: none; }
}
</style>
```

- [ ] **Step 2: 类型检查**

Run: `npm run type-check`
Expected: PASS（无新增错误）

- [ ] **Step 3: 提交**

```bash
git add src/components/common/ArticleListSkeleton.vue
git commit -m "feat(public): 新增可复用列表骨架屏组件 ArticleListSkeleton"
```

---

### Task 2: 文章列表页首屏渲染骨架屏

**Files:**
- Modify: `src/views/ArticleListView.vue:284`（模板中的内容切换块）
- Modify: `src/views/ArticleListView.vue:2`（import 区）

当前逻辑：`loading && articles 为空` 时既不显示 empty（empty 条件是 `!loading`），也没有占位，只有顶部 2px 进度条 + 列表区空白。改为：首屏无数据时显示骨架屏；翻页/筛选（已有数据）时保留"列表变暗"的 `list--loading`。

- [ ] **Step 1: 引入骨架组件**

在 `src/views/ArticleListView.vue` 的 `<script setup>` 顶部 import 区（现有 import 之后，约第 7 行后）加入：

```ts
import ArticleListSkeleton from '@/components/common/ArticleListSkeleton.vue'
```

- [ ] **Step 2: 在内容切换块加入骨架分支**

把现有这段（`src/views/ArticleListView.vue` 约 274–283 行）：

```html
      <!-- Empty -->
      <Transition name="fade" mode="out-in">
        <div v-if="!loading && articles.length === 0" key="empty" class="empty-state">
          <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="8" y="12" width="48" height="40" rx="2" />
            <line x1="20" y1="24" x2="44" y2="24" />
            <line x1="20" y1="32" x2="36" y2="32" />
          </svg>
          <p>暂无相关文章</p>
        </div>

        <div v-else key="list" :class="{ 'list--loading': loading }">
```

改为（新增 skeleton 分支，empty 分支改成 `v-else-if`）：

```html
      <!-- Skeleton (首屏加载，尚无数据) / Empty / List -->
      <Transition name="fade" mode="out-in">
        <ArticleListSkeleton
          v-if="loading && articles.length === 0"
          key="skeleton"
          :rows="6"
        />

        <div v-else-if="articles.length === 0" key="empty" class="empty-state">
          <svg viewBox="0 0 64 64" fill="none" stroke="currentColor" stroke-width="1.5">
            <rect x="8" y="12" width="48" height="40" rx="2" />
            <line x1="20" y1="24" x2="44" y2="24" />
            <line x1="20" y1="32" x2="36" y2="32" />
          </svg>
          <p>暂无相关文章</p>
        </div>

        <div v-else key="list" :class="{ 'list--loading': loading }">
```

（`v-else` 的 list 块、分页块、`</Transition>` 全部保持不变。）

- [ ] **Step 3: 类型检查 + lint**

Run: `npm run type-check && npm run lint`
Expected: PASS

- [ ] **Step 4: 浏览器验证**

Run: `npm run dev`，DevTools Network 节流 "Slow 3G"，访问 `/articles`。
Expected: 进入页面先看到 6 行**流光骨架**（而非空白），数据返回后平滑切换为真实列表；筛选/翻页时是"旧列表变暗"而非骨架；真正无结果时显示"暂无相关文章"。

- [ ] **Step 5: 提交**

```bash
git add src/views/ArticleListView.vue
git commit -m "feat(public): 文章列表首屏渲染骨架屏替代空白"
```

---

### Task 3: 首页近期文章渲染骨架屏

**Files:**
- Modify: `src/views/HomeView.vue:2`（import 区）
- Modify: `src/views/HomeView.vue:49-67`（article-list 块）

首页 `onMounted` 期间 `articles` 为空，列表区为空白。加入骨架屏占位（取 7 条，与 `pageSize: 7` 对齐）。

- [ ] **Step 1: 引入骨架组件**

在 `src/views/HomeView.vue` 的 import 区（第 5 行 `getArticlePage` import 之后）加入：

```ts
import ArticleListSkeleton from '@/components/common/ArticleListSkeleton.vue'
```

- [ ] **Step 2: 首屏无数据时显示骨架**

把现有（`src/views/HomeView.vue` 49 行起）：

```html
      <div ref="articleListRef" class="article-list" :class="{ 'article-list--loading': loading }">
        <RouterLink
```

改为在列表外层包一个条件：当 `loading && articles.length === 0` 显示骨架，否则显示真实列表。具体把第 49 行那个 `<div ref="articleListRef" ...>` 整块替换为：

```html
      <ArticleListSkeleton v-if="loading && articles.length === 0" :rows="7" />

      <div
        v-else
        ref="articleListRef"
        class="article-list"
        :class="{ 'article-list--loading': loading }"
      >
        <RouterLink
```

（`RouterLink` 循环、`</div>`、`more-link` 均保持不变。）

> 注意：`initRowAnimation()` 在 `onMounted` 里 `await nextTick()` 后执行，此时 `loading` 已为 false、真实列表已渲染、`articleListRef` 已存在，逐行入场动画不受影响。

- [ ] **Step 3: 类型检查 + lint**

Run: `npm run type-check && npm run lint`
Expected: PASS

- [ ] **Step 4: 浏览器验证**

DevTools Network 节流 "Slow 3G"，访问 `/`。
Expected: 近期文章区先显示 7 行流光骨架，数据到达后切换为真实列表并保留逐行入场动画。

- [ ] **Step 5: 提交**

```bash
git add src/views/HomeView.vue
git commit -m "feat(public): 首页近期文章渲染骨架屏替代空白"
```

---

### Task 4: 文章详情缓存 + 在途去重 + 预取模块

**Files:**
- Create: `src/api/articleCache.ts`

提供：同步读缓存（详情页可立即出内容）、按 id 取详情并写缓存、并发去重、预取（hover 时静默拉取）。零依赖、纯内存（刷新即清空，符合"实时内容"诉求）。

- [ ] **Step 1: 创建缓存模块**

`src/api/articleCache.ts`:

```ts
import { getArticleDetail, type ArticleDetailVO } from '@/api/article'

// 纯内存缓存：页面刷新即清空，保证内容实时性；仅用于会话内秒开 + 预取。
const detailCache = new Map<number, ArticleDetailVO>()
const inflight = new Map<number, Promise<ArticleDetailVO>>()

/** 同步读取已缓存的文章详情；未命中返回 undefined。 */
export function getCachedArticleDetail(id: number): ArticleDetailVO | undefined {
  return detailCache.get(id)
}

/** 取详情：命中在途请求则复用，成功后写入缓存。 */
export function fetchArticleDetail(id: number): Promise<ArticleDetailVO> {
  const existing = inflight.get(id)
  if (existing) return existing

  const p = getArticleDetail(id)
    .then((data) => {
      detailCache.set(id, data)
      return data
    })
    .finally(() => {
      inflight.delete(id)
    })

  inflight.set(id, p)
  return p
}

/** 预取：已缓存或已在途则跳过；静默拉取，忽略错误。 */
export function prefetchArticleDetail(id: number): void {
  if (detailCache.has(id) || inflight.has(id)) return
  void fetchArticleDetail(id).catch(() => {})
}
```

- [ ] **Step 2: 类型检查**

Run: `npm run type-check`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add src/api/articleCache.ts
git commit -m "feat(public): 新增文章详情内存缓存与预取模块"
```

---

### Task 5: 列表/首页 hover 预取详情

**Files:**
- Modify: `src/views/ArticleListView.vue`（import 区 + `.article-row` 的 RouterLink）
- Modify: `src/views/HomeView.vue`（import 区 + `.article-row` 的 RouterLink）

鼠标移入或键盘聚焦文章链接时提前拉取详情，等用户真正点进去时数据多半已就绪。

- [ ] **Step 1: ArticleListView 引入并绑定预取**

在 `src/views/ArticleListView.vue` import 区加入：

```ts
import { prefetchArticleDetail } from '@/api/articleCache'
```

把列表里的 `RouterLink`（约 287–292 行）开标签：

```html
            <RouterLink
              v-for="article in articles"
              :key="article.id"
              :to="`/article/${article.id}`"
              class="article-row"
            >
```

改为：

```html
            <RouterLink
              v-for="article in articles"
              :key="article.id"
              :to="`/article/${article.id}`"
              class="article-row"
              @mouseenter="prefetchArticleDetail(article.id)"
              @focus="prefetchArticleDetail(article.id)"
            >
```

- [ ] **Step 2: HomeView 引入并绑定预取**

在 `src/views/HomeView.vue` import 区加入：

```ts
import { prefetchArticleDetail } from '@/api/articleCache'
```

把首页列表里的 `RouterLink`（约 50–55 行）开标签：

```html
        <RouterLink
          v-for="article in articles"
          :key="article.id"
          :to="`/article/${article.id}`"
          class="article-row"
        >
```

改为：

```html
        <RouterLink
          v-for="article in articles"
          :key="article.id"
          :to="`/article/${article.id}`"
          class="article-row"
          @mouseenter="prefetchArticleDetail(article.id)"
          @focus="prefetchArticleDetail(article.id)"
        >
```

- [ ] **Step 3: 类型检查 + lint**

Run: `npm run type-check && npm run lint`
Expected: PASS

- [ ] **Step 4: 浏览器验证**

`npm run dev`，DevTools Network 面板打开。在 `/articles` 把鼠标移到某文章标题上（先别点）。
Expected: Network 立即出现一条 `/v1/article/{id}` 请求；之后点进该文章，详情几乎瞬间出现（见 Task 6 后效果最明显），不再重复发请求。

- [ ] **Step 5: 提交**

```bash
git add src/views/ArticleListView.vue src/views/HomeView.vue
git commit -m "feat(public): 列表与首页 hover/聚焦预取文章详情"
```

---

### Task 6: 详情页命中缓存即时渲染

**Files:**
- Modify: `src/views/ArticleDetailView.vue:5`（import）
- Modify: `src/views/ArticleDetailView.vue:71-80`（onMounted 取数逻辑）

命中缓存（来自预取或上次访问）时立即用缓存内容渲染、跳过骨架；同时仍发一次请求刷新（stale-while-revalidate）。未命中则与现状一致（骨架 → 数据）。

- [ ] **Step 1: 替换 import**

把 `src/views/ArticleDetailView.vue` 第 5 行：

```ts
import { getArticleDetail, type ArticleDetailVO } from '@/api/article'
```

改为：

```ts
import { type ArticleDetailVO } from '@/api/article'
import { getCachedArticleDetail, fetchArticleDetail } from '@/api/articleCache'
```

- [ ] **Step 2: 改写 onMounted 取数**

把现有 `onMounted`（约 71–80 行）：

```ts
onMounted(async () => {
  const id = Number(route.params.id)
  try {
    article.value = await getArticleDetail(id)
  } catch {
    error.value = true
  }
  loading.value = false
  window.addEventListener('scroll', onScroll, { passive: true })
})
```

改为：

```ts
onMounted(async () => {
  const id = Number(route.params.id)

  // 命中缓存（预取或上次访问）→ 立即渲染，跳过骨架
  const cached = getCachedArticleDetail(id)
  if (cached) {
    article.value = cached
    loading.value = false
  }

  // 始终拉一次最新，刷新缓存内容（SWR）
  try {
    article.value = await fetchArticleDetail(id)
  } catch {
    if (!cached) error.value = true
  } finally {
    loading.value = false
  }

  window.addEventListener('scroll', onScroll, { passive: true })
})
```

- [ ] **Step 3: 类型检查 + lint**

Run: `npm run type-check && npm run lint`
Expected: PASS（确认 `getArticleDetail` 已无其它引用，移除它不报 unused/未定义）

- [ ] **Step 4: 浏览器验证**

DevTools Network 节流 "Slow 3G"。
- 场景 A（预取后进入）：在 `/articles` hover 某文章后点击 → 详情**直接出内容、不闪骨架**。
- 场景 B（直接进/刷新详情页）：骨架屏正常显示，数据到达后渲染（与改前一致）。
- 场景 C（返回列表再进同一篇）：第二次进入秒出。

- [ ] **Step 5: 提交**

```bash
git add src/views/ArticleDetailView.vue
git commit -m "feat(public): 文章详情命中缓存即时渲染（SWR）"
```

---

### Task 7: 慢加载兜底提示

**Files:**
- Modify: `src/views/ArticleDetailView.vue`（script 加 slow 状态 + onUnmounted 清理 + 模板在骨架下方加提示）
- Modify: `src/views/ArticleListView.vue`（script 加 slow 状态，在 fetchArticles 中管理 + 模板在骨架下方加提示）

后端真的慢时（超过阈值仍在 loading），显示"加载较慢，仍在努力…"，告诉用户系统在动、不是空/坏了，降低跳出。阈值取 6 秒。

- [ ] **Step 1: 详情页加 slow 状态**

在 `src/views/ArticleDetailView.vue` 的 `<script setup>` 中，`const error = ref(false)` 之后加入：

```ts
const slow = ref(false)
let slowTimer: ReturnType<typeof setTimeout> | null = null
```

在 Task 6 改好的 `onMounted` 里，紧跟 `const id = ...` 之后、缓存判断之前加入：

```ts
  slowTimer = setTimeout(() => {
    if (loading.value) slow.value = true
  }, 6000)
```

并在该 `onMounted` 的 `finally` 块中、`loading.value = false` 之后加入：

```ts
    if (slowTimer) {
      clearTimeout(slowTimer)
      slowTimer = null
    }
    slow.value = false
```

把现有 `onUnmounted`：

```ts
onUnmounted(() => window.removeEventListener('scroll', onScroll))
```

改为：

```ts
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
  if (slowTimer) clearTimeout(slowTimer)
})
```

- [ ] **Step 2: 详情页模板加提示**

把详情页的骨架块（约 91–101 行）：

```html
    <div v-if="loading" class="page-state">
      <div class="skeleton-body container">
        <div class="skeleton-line w-20" />
        <div class="skeleton-line w-70" />
        <div class="skeleton-line w-50" />
        <div class="skeleton-line w-100" />
        <div class="skeleton-line w-80" />
        <div class="skeleton-line w-100" />
        <div class="skeleton-line w-70" />
      </div>
    </div>
```

改为（骨架结构不变，末尾加一行慢提示）：

```html
    <div v-if="loading" class="page-state">
      <div class="skeleton-body container">
        <div class="skeleton-line w-20" />
        <div class="skeleton-line w-70" />
        <div class="skeleton-line w-50" />
        <div class="skeleton-line w-100" />
        <div class="skeleton-line w-80" />
        <div class="skeleton-line w-100" />
        <div class="skeleton-line w-70" />
        <Transition name="fade">
          <p v-if="slow" class="slow-hint">加载较慢，仍在努力…</p>
        </Transition>
      </div>
    </div>
```

在 `src/views/ArticleDetailView.vue` 的 `<style scoped>` 末尾追加：

```css
.slow-hint {
  margin-top: 20px;
  font-size: 13px;
  color: var(--color-text-faint);
  text-align: center;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
```

> 若该文件已存在 `.fade-*` 过渡类，则只追加 `.slow-hint`，不要重复定义 `.fade-*`（执行前用编辑器搜 `fade-enter-active` 确认）。

- [ ] **Step 3: 列表页加 slow 状态**

在 `src/views/ArticleListView.vue` 的 `const loading = ref(false)`（约 85 行）之后加入：

```ts
const slow = ref(false)
let slowTimer: ReturnType<typeof setTimeout> | null = null
```

把现有 `fetchArticles`（92–110 行）：

```ts
async function fetchArticles() {
  loading.value = true
  try {
    const res = await getArticlePage({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      categoryId: filterCategoryId.value ?? undefined,
      tagIds: filterTagIds.value.length > 0 ? filterTagIds.value : undefined,
      keyword: filterSearch.value.trim() || undefined,
    })
    articles.value = res.content
    totalElements.value = res.totalElements
    totalPages.value = res.totalPages
    hasPrevious.value = res.hasPrevious
    hasNext.value = res.hasNext
  } finally {
    loading.value = false
  }
}
```

改为：

```ts
async function fetchArticles() {
  loading.value = true
  if (slowTimer) clearTimeout(slowTimer)
  slowTimer = setTimeout(() => {
    if (loading.value) slow.value = true
  }, 6000)
  try {
    const res = await getArticlePage({
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE,
      categoryId: filterCategoryId.value ?? undefined,
      tagIds: filterTagIds.value.length > 0 ? filterTagIds.value : undefined,
      keyword: filterSearch.value.trim() || undefined,
    })
    articles.value = res.content
    totalElements.value = res.totalElements
    totalPages.value = res.totalPages
    hasPrevious.value = res.hasPrevious
    hasNext.value = res.hasNext
  } finally {
    loading.value = false
    if (slowTimer) {
      clearTimeout(slowTimer)
      slowTimer = null
    }
    slow.value = false
  }
}
```

- [ ] **Step 4: 列表页模板加提示**

在 Task 2 改好的骨架分支上加提示。把：

```html
        <ArticleListSkeleton
          v-if="loading && articles.length === 0"
          key="skeleton"
          :rows="6"
        />
```

改为（用单个 `<div>` 包裹骨架 + 提示，满足 `mode="out-in"` 的单根节点要求）：

```html
        <div v-if="loading && articles.length === 0" key="skeleton">
          <ArticleListSkeleton :rows="6" />
          <Transition name="fade">
            <p v-if="slow" class="slow-hint">加载较慢，仍在努力…</p>
          </Transition>
        </div>
```

> 外层 `<Transition mode="out-in">` 只能过渡单个根元素节点，故必须用一个 `<div key="skeleton">` 包住骨架与提示，不能用 `<template>`（它不是真实元素）。`.fade-*` 过渡类该文件已存在（见 792–800 行），无需重复定义。

在 `src/views/ArticleListView.vue` 的 `<style scoped>` 末尾追加：

```css
.slow-hint {
  margin-top: 20px;
  font-size: 13px;
  color: var(--color-text-faint);
  text-align: center;
}
```

- [ ] **Step 5: 类型检查 + lint**

Run: `npm run type-check && npm run lint`
Expected: PASS

- [ ] **Step 6: 浏览器验证**

临时在 `getArticleDetail`/`getArticlePage` 前加 `await new Promise(r => setTimeout(r, 8000))` 模拟超慢（验证后删除），或用极慢网络节流。
Expected: 骨架显示约 6 秒后，下方淡入"加载较慢，仍在努力…"；数据到达后提示与骨架一起消失。翻页/筛选若慢同样会在 6 秒后提示。

- [ ] **Step 7: 提交**

```bash
git add src/views/ArticleDetailView.vue src/views/ArticleListView.vue
git commit -m "feat(public): 慢加载超阈值显示兜底提示"
```

---

## 验收清单（全部完成后）

- [ ] `/`、`/articles` 首屏在慢网络下显示**流光骨架**而非空白；真正无结果时显示"暂无相关文章"，两者视觉可区分。
- [ ] 列表/首页 hover 文章即预取；点进详情命中缓存**秒出、不闪骨架**；未预取的直接访问仍走骨架。
- [ ] 同一篇文章二次进入秒开。
- [ ] 加载超 6 秒显示"加载较慢，仍在努力…"，数据到达后消失。
- [ ] `npm run type-check` 与 `npm run lint` 全程无新增错误。
- [ ] 切换 light/dark 主题，骨架与提示颜色均正常（均使用设计 token，无硬编码色）。

## 不在本计划范围内（如需"彻底消灭等待"另起计划）

- 把文章数据接入 vite-ssg 构建期预渲染（内容进首屏 HTML，根治"等待"，但新文章需重新构建/增量再生）。
- 后端/接口层缓存或 CDN 边缘缓存（从根上缩短响应时间）。
- 引入 TanStack Vue Query 替换手写缓存（取数场景显著增多时再考虑）。
