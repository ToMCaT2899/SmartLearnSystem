import request from '@/utils/request'
import { downloadBlob } from '@/utils/download'
import type { ApiResult, PageResult, PrivateMessage, ClassNotice, Feedback, ActivityPost } from '@/types'

// Messages
export function sendMessage(data: PrivateMessage) {
  return request.post<ApiResult>('/communication/messages', data)
}
export function getConversation(userId: number) {
  return request.get<ApiResult<PrivateMessage[]>>(`/communication/messages/conversation/${userId}`)
}
export function getUnreadMessages() {
  return request.get<ApiResult<PrivateMessage[]>>('/communication/messages/unread')
}
export function getAllMessages() {
  return request.get<ApiResult<PrivateMessage[]>>('/communication/messages')
}
export function markAsRead(id: number) {
  return request.put<ApiResult>(`/communication/messages/${id}/read`)
}
export function markConversationAsRead(contactId: number) {
  return request.put<ApiResult>(`/communication/messages/conversation/${contactId}/read`)
}
export function deleteMessage(id: number) {
  return request.delete<ApiResult>(`/communication/messages/${id}`)
}
export function clearConversation(contactId: number) {
  return request.delete<ApiResult>(`/communication/messages/conversation/${contactId}`)
}
export function getContacts() {
  return request.get<ApiResult<{ id: number; name: string; role: string; studentNo?: string }[]>>('/communication/messages/contacts')
}
export function findUserByStudentNo(studentNo: string) {
  return request.get<ApiResult<{ id: number; name: string; role: string; studentNo: string }>>(`/communication/messages/find-by-student-no/${encodeURIComponent(studentNo)}`)
}

// Notices
export function getClassNotices(classIdentifier?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<ClassNotice>>>('/communication/notices', { params: { classIdentifier, page, size } })
}
export function createClassNotice(data: ClassNotice) {
  return request.post<ApiResult>('/communication/notices', data)
}
export function deleteClassNotice(id: number) {
  return request.delete<ApiResult>(`/communication/notices/${id}`)
}

// Feedback
export function submitFeedback(data: Feedback) {
  return request.post<ApiResult>('/communication/feedback', data)
}
export function getMyFeedback(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<Feedback>>>('/communication/feedback/my', { params: { page, size } })
}
export function getPendingFeedback(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<Feedback>>>('/communication/feedback/pending', { params: { page, size } })
}
export function getAllFeedback(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<Feedback>>>('/communication/feedback', { params: { page, size } })
}
export function replyFeedback(id: number, reply: string) {
  return request.put<ApiResult>(`/communication/feedback/${id}/reply`, { reply })
}
export function deleteFeedback(id: number) {
  return request.delete<ApiResult>(`/communication/feedback/${id}`)
}

// Activities
export function getActivities(category?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<ActivityPost>>>('/communication/activities', { params: { category, page, size } })
}
export function createActivity(data: ActivityPost) {
  return request.post<ApiResult>('/communication/activities', data)
}
export function deleteActivity(id: number) {
  return request.delete<ApiResult>(`/communication/activities/${id}`)
}
export function registerForActivity(id: number) {
  return request.post<ApiResult>(`/communication/activities/${id}/register`)
}
export function cancelRegistration(id: number) {
  return request.delete<ApiResult>(`/communication/activities/${id}/register`)
}
export function getMyRegistrations(page = 1, size = 10) {
  return request.get<ApiResult<PageResult<any[]>>>('/communication/activities/my-registrations', { params: { page, size } })
}

export function exportActivitiesExcel() {
  return downloadBlob('/communication/export/activities', '活动报名情况.xlsx')
}
