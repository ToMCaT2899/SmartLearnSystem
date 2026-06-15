import request from '@/utils/request'
import type { ApiResult } from '@/types'

export function uploadFile(file: File, category = 'general') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('category', category)
  return request.post<ApiResult<string>>('/files/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function getDownloadUrl(url: string) {
  return request.get<ApiResult<string>>('/files/download', { params: { url } })
}
