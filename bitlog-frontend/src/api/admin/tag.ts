import request from '@/utils/request'

export interface Tag {
  id: number
  name: string
  articleCount: number
  createTime: string
  updateTime: string
}

export function getTags(name?: string): Promise<Tag[]> {
  return request.get('/v1/tag', { params: name ? { name } : undefined })
}

export function createTag(name: string): Promise<number> {
  return request.post('/v1/tag', { name })
}

export function updateTag(id: number, name: string): Promise<void> {
  return request.put(`/v1/tag/${id}`, { name })
}

export function deleteTags(ids: number[]): Promise<void> {
  return request.delete('/v1/tag', { data: { ids } })
}

export function getOrCreateTag(name: string): Promise<number> {
  return request.post<never, number>('/v1/tag/get-or-create', null, { params: { name } })
}
