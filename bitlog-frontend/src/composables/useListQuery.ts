import { computed, nextTick, onScopeDispose, reactive, ref, watch, type Ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { stripEmpty, type PageResult, type SortOrder } from '@/api/types'
import { DEFAULT_PAGE_SIZE, PAGE_SIZE_OPTIONS, PAGE_WINDOW } from '@/constants/pagination'

type Filters = Record<string, unknown>

export interface UseListQueryOptions<F extends Filters, T> {
  /** 过滤条件初值，键名即 URL 参数名 */
  filters: F
  /** 拉取列表，入参已剥空 */
  fetch: (params: Record<string, unknown>) => Promise<PageResult<T>>
  /** 过滤条件到接口参数的映射，默认原样透传 */
  toParams?: (filters: F) => Record<string, unknown>
  pageSize?: number
  sortField?: string
  sortOrder?: SortOrder
  /** 需要防抖的过滤键，通常是文本输入框 */
  debounce?: (keyof F)[]
  debounceMs?: number
  /** 双向同步到 URL，仅管理端使用；公开页有入站链接契约，自行处理 */
  syncUrl?: boolean
  /** URL 恢复之后、首次拉取之前调用，用于把非法的 URL 值回落到默认值 */
  sanitize?: (filters: F) => void
  /** 首次是否自动拉取；SSG 预渲染场景传 false，并在挂载后调用 start() 再开始监听过滤变更 */
  immediate?: boolean
  onError?: (err: unknown) => void
}

export interface UseListQuery<F extends Filters, T> {
  filters: F
  items: Ref<T[]>
  loading: Ref<boolean>
  pageNum: Ref<number>
  pageSize: Ref<number>
  total: Ref<number>
  totalPages: Ref<number>
  hasPrevious: Ref<boolean>
  hasNext: Ref<boolean>
  pageNumbers: Ref<(number | '…')[]>
  sortField: Ref<string | undefined>
  sortOrder: Ref<SortOrder | undefined>
  pageSizeOptions: number[]
  load: () => Promise<void>
  /** immediate: false 时手动开启过滤监听，避免初始化赋值触发请求 */
  start: () => void
  goPage: (n: number) => void
  setPageSize: (n: number) => void
  setSort: (field: string | undefined, order?: SortOrder) => void
  /** 改任意过滤后调用：回第 1 页并重拉 */
  applyFilters: () => void
  /** 清空全部过滤并回第 1 页 */
  reset: () => void
  /** SSG 预渲染结果注入，不触发请求 */
  applyPrerendered: (result: PageResult<T>) => void
}

/**
 * 列表查询状态机：分页 + 排序 + 过滤
 *
 * 内置四条统一行为：过滤变更自动回第 1 页、空值统一剥除、URL 同步、当前页删空自动回退
 */
export function useListQuery<F extends Filters, T>(
  options: UseListQueryOptions<F, T>,
): UseListQuery<F, T> {
  const {
    fetch,
    toParams = (f: F) => ({ ...f }),
    debounce = [],
    debounceMs = 300,
    syncUrl = false,
    immediate = true,
    sanitize,
    onError,
  } = options

  const route = syncUrl ? useRoute() : null
  const router = syncUrl ? useRouter() : null

  const defaults = { ...options.filters }
  const defaultPageSize = options.pageSize ?? DEFAULT_PAGE_SIZE

  const filters = reactive({ ...options.filters }) as F
  const items = ref([]) as Ref<T[]>
  const loading = ref(false)
  const pageNum = ref(1)
  const pageSize = ref(defaultPageSize)
  const total = ref(0)
  const totalPages = ref(0)
  const hasPrevious = ref(false)
  const hasNext = ref(false)
  const sortField = ref(options.sortField)
  const sortOrder = ref(options.sortOrder)

  // ── 从 URL 恢复 ─────────────────────────────────────────────────────────────
  if (route) {
    const q = route.query
    const page = Number(q.page)
    if (Number.isFinite(page) && page >= 1) pageNum.value = page
    const size = Number(q.size)
    if (PAGE_SIZE_OPTIONS.includes(size)) pageSize.value = size
    if (typeof q.sort === 'string') sortField.value = q.sort
    if (q.order === 'ASC' || q.order === 'DESC') sortOrder.value = q.order
    for (const key of Object.keys(defaults)) {
      const raw = q[key]
      if (typeof raw !== 'string') continue
      const fallback = defaults[key]
      ;(filters as Filters)[key] = typeof fallback === 'number' ? Number(raw) : raw
    }
  }
  if (sanitize) sanitize(filters)

  function writeUrl() {
    if (!router) return
    const query: Record<string, string> = {}
    for (const [key, value] of Object.entries(stripEmpty(filters as Filters))) {
      if (value === defaults[key]) continue
      query[key] = String(value)
    }
    if (pageNum.value !== 1) query.page = String(pageNum.value)
    if (pageSize.value !== defaultPageSize) query.size = String(pageSize.value)
    if (sortField.value) query.sort = sortField.value
    if (sortOrder.value) query.order = sortOrder.value
    router.replace({ query })
  }

  // ── 拉取 ────────────────────────────────────────────────────────────────────
  let loadSeq = 0
  async function load(): Promise<void> {
    const seq = ++loadSeq
    loading.value = true
    try {
      const params = stripEmpty({
        ...toParams(filters),
        pageNum: pageNum.value,
        pageSize: pageSize.value,
        sortField: sortField.value,
        sortOrder: sortOrder.value,
      })
      const res = await fetch(params)
      // 已有更新的请求发出，丢弃本次结果，避免旧响应后到覆盖新内容
      if (seq !== loadSeq) return
      // 空页直接跳到最后一页；不能逐页递减，URL 里的 page 可以是任意大的数
      const lastPage = Math.max(1, res.totalPages)
      if (res.content.length === 0 && pageNum.value > lastPage) {
        pageNum.value = lastPage
        writeUrl()
        return await load()
      }
      applyPrerendered(res)
    } catch (err) {
      if (onError) onError(err)
      else throw err
    } finally {
      // 过期请求不关 loading，后发的那次还在跑
      if (seq === loadSeq) loading.value = false
    }
  }

  function applyPrerendered(res: PageResult<T>) {
    items.value = res.content
    total.value = res.totalElements
    totalPages.value = res.totalPages
    hasPrevious.value = res.hasPrevious
    hasNext.value = res.hasNext
  }

  // ── 变更入口，一律回第 1 页 ──────────────────────────────────────────────────
  function applyFilters() {
    // 回车提交时防抖计时器可能还挂着，不取消会紧接着再发一次
    if (timer) {
      clearTimeout(timer)
      timer = null
    }
    pageNum.value = 1
    writeUrl()
    void load()
  }

  function goPage(n: number) {
    if (n < 1 || n === pageNum.value) return
    if (totalPages.value > 0 && n > totalPages.value) return
    pageNum.value = n
    writeUrl()
    void load()
  }

  function setPageSize(n: number) {
    if (n === pageSize.value) return
    pageSize.value = n
    applyFilters()
  }

  function setSort(field: string | undefined, order?: SortOrder) {
    sortField.value = field
    sortOrder.value = order
    applyFilters()
  }

  function reset() {
    // 这里的赋值会在微任务里触发过滤监听，先压住，否则连发两次请求
    suppressed = true
    for (const [key, value] of Object.entries(defaults)) {
      ;(filters as Filters)[key] = Array.isArray(value) ? [...value] : value
    }
    void nextTick(() => {
      suppressed = false
    })
    applyFilters()
  }

  // ── 过滤变更监听：文本输入防抖，其余立即生效 ─────────────────────────────────
  const debounceKeys = debounce as string[]
  const immediateKeys = Object.keys(defaults).filter((k) => !debounceKeys.includes(k))
  let timer: ReturnType<typeof setTimeout> | null = null
  let started = false
  let suppressed = false

  if (debounceKeys.length > 0) {
    watch(
      () => debounceKeys.map((k) => (filters as Filters)[k]),
      () => {
        if (!started || suppressed) return
        if (timer) clearTimeout(timer)
        timer = setTimeout(applyFilters, debounceMs)
      },
    )
  }
  if (immediateKeys.length > 0) {
    watch(
      () => immediateKeys.map((k) => (filters as Filters)[k]),
      () => {
        if (started && !suppressed) applyFilters()
      },
    )
  }

  // 卸载后防抖回调仍会跑，syncUrl 页面会把 query 写到已经切走的新路由上
  onScopeDispose(() => {
    if (timer) clearTimeout(timer)
  })

  const totalPagesSafe = computed(() => totalPages.value || 1)
  const pageNumbers = computed<(number | '…')[]>(() => {
    const cur = pageNum.value
    const last = totalPagesSafe.value
    if (last <= PAGE_WINDOW) return Array.from({ length: last }, (_, i) => i + 1)
    const pages: (number | '…')[] = [1]
    if (cur > 3) pages.push('…')
    for (let i = Math.max(2, cur - 1); i <= Math.min(last - 1, cur + 1); i++) pages.push(i)
    if (cur < last - 2) pages.push('…')
    pages.push(last)
    return pages
  })

  function start() {
    started = true
  }

  if (immediate) {
    start()
    void load()
  }

  return {
    filters,
    items,
    loading,
    pageNum,
    pageSize,
    total,
    totalPages: totalPagesSafe,
    hasPrevious,
    hasNext,
    pageNumbers,
    sortField,
    sortOrder,
    pageSizeOptions: PAGE_SIZE_OPTIONS,
    load,
    start,
    goPage,
    setPageSize,
    setSort,
    applyFilters,
    reset,
    applyPrerendered,
  }
}
