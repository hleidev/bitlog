import request from '@/utils/request'

export interface TagVO {
  id: number
  name: string
  articleCount: number
  createTime: string
  updateTime: string
}

export function getTags(): Promise<TagVO[]> {
  return request.get('/v1/tag')
}
