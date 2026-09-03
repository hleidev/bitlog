import request from '@/utils/request'

export interface UploadRes {
  fileKey: string
  fileUrl: string
}

export type UploadScene = 'avatar' | 'article'

const ALLOWED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

const SIZE_LIMITS: Record<UploadScene, number> = {
  avatar: 1 * 1024 * 1024,
  article: 5 * 1024 * 1024,
}

export function validateUploadFile(file: File, scene: UploadScene): string | null {
  if (!ALLOWED_TYPES.includes(file.type)) {
    return file.type.includes('heic') || file.name.toLowerCase().endsWith('.heic')
      ? '不支持 HEIC 格式，请转换为 JPEG、PNG 或 WebP 后重试'
      : '不支持的文件类型，仅支持 JPEG、PNG、WebP'
  }
  const limit = SIZE_LIMITS[scene]
  if (file.size > limit) {
    const mb = limit / (1024 * 1024)
    return `文件大小超出限制：最大允许 ${mb}MB`
  }
  return null
}

export function uploadFile(file: File, scene: UploadScene): Promise<UploadRes> {
  const err = validateUploadFile(file, scene)
  if (err) return Promise.reject(new Error(err))

  const formData = new FormData()
  formData.append('scene', scene)
  formData.append('file', file)
  return request.post('/v1/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
