import request from '@/utils/request'
import type { ApiResult, JobInfo, PageResult } from '@/types'

export function getJobs(keyword?: string, location?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<JobInfo>>>('/career/jobs', { params: { keyword, location, page, size } })
}
export function getJobById(id: number) {
  return request.get<ApiResult<JobInfo>>(`/career/jobs/${id}`)
}
export function createJob(data: JobInfo) {
  return request.post<ApiResult>('/career/jobs', data)
}
export function updateJob(id: number, data: JobInfo) {
  return request.put<ApiResult>(`/career/jobs/${id}`, data)
}
export function deleteJob(id: number) {
  return request.delete<ApiResult>(`/career/jobs/${id}`)
}
export function saveSubscription(data: any) {
  return request.put<ApiResult>('/career/subscription', data)
}
export function getSubscription() {
  return request.get<ApiResult<any>>('/career/subscription')
}
