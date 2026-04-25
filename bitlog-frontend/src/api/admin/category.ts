import request from '@/utils/request'

// 后端返回的平铺结构（parentId=0 表示顶级）
interface CategoryRaw {
  id: number
  parentId: number
  name: string
  articleCount: number
  createTime: string
}

// 前端使用的树形结构（parentId=0 转为 null）
export interface Category {
  id: number
  parentId: number | null
  name: string
  articleCount: number
  createTime: string
  children?: Category[]
}

// 平铺列表组装为两级树
function buildTree(list: CategoryRaw[]): Category[] {
  const parents: Category[] = []
  const childMap = new Map<number, Category[]>()

  for (const item of list) {
    const node: Category = { ...item, parentId: item.parentId || null, children: [] }
    if (!item.parentId) {
      parents.push(node)
    } else {
      if (!childMap.has(item.parentId)) childMap.set(item.parentId, [])
      childMap.get(item.parentId)!.push(node)
    }
  }

  for (const parent of parents) {
    parent.children = childMap.get(parent.id) ?? []
    childMap.delete(parent.id)
  }

  if (import.meta.env.DEV && childMap.size > 0) {
    console.warn('[buildTree] 存在找不到父分类的子分类，已忽略:', [...childMap.values()].flat())
  }

  return parents
}

export function getCategories(name?: string): Promise<Category[]> {
  return request
    .get<CategoryRaw[]>('/v1/category', { params: name ? { name } : undefined })
    .then(buildTree)
}

export function createCategory(name: string, parentId?: number): Promise<number> {
  return request.post('/v1/category', {
    name,
    ...(parentId !== undefined ? { parentId } : {}),
  })
}

export function updateCategory(id: number, name: string, parentId?: number): Promise<void> {
  return request.put(`/v1/category/${id}`, {
    name,
    ...(parentId !== undefined ? { parentId } : {}),
  })
}

export function deleteCategory(id: number): Promise<void> {
  return request.delete(`/v1/category/${id}`)
}
