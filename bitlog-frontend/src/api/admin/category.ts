import request from '@/utils/request'

export interface Category {
  id: number
  name: string
  articleCount: number
  createTime: string
}

export function getCategories(name?: string): Promise<Category[]> {
  return request.get<never, Category[]>('/v1/category', { params: name ? { name } : undefined })
}

export function createCategory(name: string): Promise<number> {
  return request.post('/v1/category', { name })
}

export function updateCategory(id: number, name: string): Promise<void> {
  return request.put(`/v1/category/${id}`, { name })
}

export function deleteCategory(id: number): Promise<void> {
  return request.delete(`/v1/category/${id}`)
}

export function getOrCreateCategory(name: string): Promise<number> {
  return request.post<never, number>('/v1/category/get-or-create', null, { params: { name } })
}
