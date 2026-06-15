import request from '@/utils/request'
import type { ApiResult, PageResult, Scholarship, GrantInfo, WorkStudyJob } from '@/types'
import { downloadBlob } from '@/utils/download'

// Scholarships
export const scholarshipApi = {
  list: (page = 1, size = 10) => request.get<ApiResult<PageResult<Scholarship>>>('/financial-aid/scholarships', { params: { page, size } }),
  get: (id: number) => request.get<ApiResult<Scholarship>>(`/financial-aid/scholarships/${id}`),
  create: (data: Scholarship) => request.post<ApiResult>('/financial-aid/scholarships', data),
  update: (data: Scholarship) => request.put<ApiResult>(`/financial-aid/scholarships/${data.id}`, data),
  delete: (id: number) => request.delete<ApiResult>(`/financial-aid/scholarships/${id}`),
  apply: (id: number, reason: string, fileUrls: string[]) =>
    request.post<ApiResult>(`/financial-aid/scholarships/${id}/apply`, { reason, fileUrls }),
  cancel: (id: number) =>
    request.put<ApiResult>(`/financial-aid/scholarships/applications/${id}/cancel`),
  edit: (id: number, reason: string, fileUrls: string[]) =>
    request.put<ApiResult>(`/financial-aid/scholarships/applications/${id}`, { reason, fileUrls }),
  review: (id: number, status: string, comment: string) =>
    request.put<ApiResult>(`/financial-aid/scholarships/applications/${id}/review`, { status, comment }),
}

// Grants
export const grantApi = {
  list: (page = 1, size = 10) => request.get<ApiResult<PageResult<GrantInfo>>>('/financial-aid/grants', { params: { page, size } }),
  get: (id: number) => request.get<ApiResult<GrantInfo>>(`/financial-aid/grants/${id}`),
  create: (data: GrantInfo) => request.post<ApiResult>('/financial-aid/grants', data),
  update: (data: GrantInfo) => request.put<ApiResult>(`/financial-aid/grants/${data.id}`, data),
  delete: (id: number) => request.delete<ApiResult>(`/financial-aid/grants/${id}`),
  apply: (id: number, reason: string, fileUrls: string[]) =>
    request.post<ApiResult>(`/financial-aid/grants/${id}/apply`, { reason, fileUrls }),
  cancel: (id: number) =>
    request.put<ApiResult>(`/financial-aid/grants/applications/${id}/cancel`),
  edit: (id: number, reason: string, fileUrls: string[]) =>
    request.put<ApiResult>(`/financial-aid/grants/applications/${id}`, { reason, fileUrls }),
  review: (id: number, status: string, comment: string) =>
    request.put<ApiResult>(`/financial-aid/grants/applications/${id}/review`, { status, comment }),
}

// Work Study
export const workStudyApi = {
  list: (page = 1, size = 10) => request.get<ApiResult<PageResult<WorkStudyJob>>>('/financial-aid/work-study', { params: { page, size } }),
  get: (id: number) => request.get<ApiResult<WorkStudyJob>>(`/financial-aid/work-study/${id}`),
  create: (data: WorkStudyJob) => request.post<ApiResult>('/financial-aid/work-study', data),
  update: (data: WorkStudyJob) => request.put<ApiResult>(`/financial-aid/work-study/${data.id}`, data),
  delete: (id: number) => request.delete<ApiResult>(`/financial-aid/work-study/${id}`),
  apply: (id: number, resume: string, fileUrls: string[]) =>
    request.post<ApiResult>(`/financial-aid/work-study/${id}/apply`, { resume, fileUrls }),
  cancel: (id: number) =>
    request.put<ApiResult>(`/financial-aid/work-study/applications/${id}/cancel`),
  edit: (id: number, resume: string, fileUrls: string[]) =>
    request.put<ApiResult>(`/financial-aid/work-study/applications/${id}`, { resume, fileUrls }),
  review: (id: number, status: string, comment: string) =>
    request.put<ApiResult>(`/financial-aid/work-study/applications/${id}/review`, { status, comment }),
}

export function getMyApplications(page = 1, size = 10) {
  return request.get<ApiResult<Record<string, PageResult<any>>>>('/financial-aid/my-applications', { params: { page, size } })
}

export function getReviewQueue(page = 1, size = 10) {
  return request.get<ApiResult<Record<string, PageResult<any>>>>('/financial-aid/review-queue', { params: { page, size } })
}

// Per-item application overview (for teacher/admin)
export function getScholarshipApplications(id: number, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<any>>>(`/financial-aid/scholarships/${id}/applications`, { params: { page, size } })
}

export function getGrantApplications(id: number, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<any>>>(`/financial-aid/grants/${id}/applications`, { params: { page, size } })
}

export function getWorkStudyApplications(id: number, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<any>>>(`/financial-aid/work-study/${id}/applications`, { params: { page, size } })
}

export function getAdmittedList(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<any>>>('/financial-aid/admitted-list', { params: { page, size } })
}

// Export
export function exportScholarshipApplications(id: number) {
  return downloadBlob(`/financial-aid/scholarships/${id}/applications/export`, '奖学金申请名单.xlsx')
}

export function exportGrantApplications(id: number) {
  return downloadBlob(`/financial-aid/grants/${id}/applications/export`, '助学金申请名单.xlsx')
}

export function exportWorkStudyApplications(id: number) {
  return downloadBlob(`/financial-aid/work-study/${id}/applications/export`, '勤工助学申请名单.xlsx')
}
