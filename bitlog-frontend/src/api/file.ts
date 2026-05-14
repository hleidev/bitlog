import request from '@/utils/request'

export interface UploadRes {
  fileKey: string
}

export function uploadFile(file: File, scene: string): Promise<UploadRes> {
  const formData = new FormData()
  formData.append('scene', scene)
  formData.append('file', file)
  return request.post('/v1/file/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
