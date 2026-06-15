import request from '@/utils/request'
import type { ApiResult, LateReturnRecord, OutingReport, SafetyReport, PageResult } from '@/types'

// Late returns
export function registerLateReturn(data: LateReturnRecord) {
  return request.post<ApiResult>('/safety/late-returns', data)
}
export function getLateReturns(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<LateReturnRecord>>>('/safety/late-returns', { params: { page, size } })
}
export function getPendingLateReturns(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<LateReturnRecord>>>('/safety/late-returns/pending', { params: { page, size } })
}
export function verifyLateReturn(id: number) {
  return request.put<ApiResult>(`/safety/late-returns/${id}/verify`)
}
export function updateLateReturn(id: number, data: LateReturnRecord) {
  return request.put<ApiResult>(`/safety/late-returns/${id}`, data)
}
export function cancelLateReturn(id: number) {
  return request.put<ApiResult>(`/safety/late-returns/${id}/cancel`)
}
export function deleteLateReturn(id: number) {
  return request.delete<ApiResult>(`/safety/late-returns/${id}`)
}

// Outing reports
export function submitOutingReport(data: OutingReport) {
  return request.post<ApiResult>('/safety/outing-reports', data)
}
export function getOutingReports(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<OutingReport>>>('/safety/outing-reports', { params: { page, size } })
}
export function getPendingOutingReports(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<OutingReport>>>('/safety/outing-reports/pending', { params: { page, size } })
}
export function reviewOutingReport(id: number, status: string) {
  return request.put<ApiResult>(`/safety/outing-reports/${id}/review`, { status })
}
export function updateOutingReport(id: number, data: OutingReport) {
  return request.put<ApiResult>(`/safety/outing-reports/${id}`, data)
}
export function cancelOutingReport(id: number) {
  return request.put<ApiResult>(`/safety/outing-reports/${id}/cancel`)
}
export function deleteOutingReport(id: number) {
  return request.delete<ApiResult>(`/safety/outing-reports/${id}`)
}

// Safety reports
export function submitSafetyReport(data: SafetyReport) {
  return request.post<ApiResult>('/safety/reports', data)
}
export function getSafetyReports(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<SafetyReport>>>('/safety/reports', { params: { page, size } })
}
export function getPendingSafetyReports(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<SafetyReport>>>('/safety/reports/pending', { params: { page, size } })
}
export function handleSafetyReport(id: number) {
  return request.put<ApiResult>(`/safety/reports/${id}/handle`)
}
export function updateSafetyReport(id: number, data: SafetyReport) {
  return request.put<ApiResult>(`/safety/reports/${id}`, data)
}
export function cancelSafetyReport(id: number) {
  return request.put<ApiResult>(`/safety/reports/${id}/cancel`)
}
export function deleteSafetyReport(id: number) {
  return request.delete<ApiResult>(`/safety/reports/${id}`)
}
