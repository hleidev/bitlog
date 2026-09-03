import request from '@/utils/request'

export interface CategoryVO {
  id: number
  name: string
  articleCount: number
  createTime: string
}

export function getCategories(): Promise<CategoryVO[]> {
  return request.get('/v1/category')
}
