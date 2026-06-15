import request from '@/utils/request'
import type {
  ApiResult, StudentProfile, FamilyMember, EmergencyContact,
  SupportingDocument, InfoChangeRequest
} from '@/types'

// Profile
export function searchStudents(keyword: string) {
  return request.get<ApiResult<StudentProfile[]>>('/student/profiles', { params: { keyword } })
}

export function getMyProfile() {
  return request.get<ApiResult<StudentProfile>>('/student/profile')
}

export function getProfileById(studentId: number) {
  return request.get<ApiResult<StudentProfile>>(`/student/profile/${studentId}`)
}

export function updateProfile(data: StudentProfile) {
  return request.put<ApiResult>('/student/profile', data)
}

// Family
export function getFamilyMembers(studentId?: number) {
  const params = studentId ? { studentId } : {}
  return request.get<ApiResult<FamilyMember[]>>('/student/family', { params })
}

export function saveFamilyMembers(data: FamilyMember[], studentId?: number) {
  const params = studentId ? { studentId } : {}
  return request.put<ApiResult>('/student/family', data, { params })
}

// Emergency Contacts
export function getEmergencyContacts(studentId?: number) {
  const params = studentId ? { studentId } : {}
  return request.get<ApiResult<EmergencyContact[]>>('/student/emergency-contacts', { params })
}

export function saveEmergencyContacts(data: EmergencyContact[], studentId?: number) {
  const params = studentId ? { studentId } : {}
  return request.put<ApiResult>('/student/emergency-contacts', data, { params })
}

// Documents
export function getDocuments(studentId?: number) {
  const params = studentId ? { studentId } : {}
  return request.get<ApiResult<SupportingDocument[]>>('/student/documents', { params })
}

export function uploadDocument(file: File, docType: string, studentId?: number) {
  const formData = new FormData()
  formData.append('file', file)
  if (studentId) formData.append('studentId', String(studentId))
  formData.append('docType', docType)
  return request.post<ApiResult<string>>('/student/documents', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// Change Requests
export function getChangeRequests(studentId?: number) {
  const params = studentId ? { studentId } : {}
  return request.get<ApiResult<InfoChangeRequest[]>>('/student/change-requests', { params })
}

export function submitChangeRequest(data: InfoChangeRequest) {
  return request.post<ApiResult>('/student/change-requests', data)
}

export function reviewChangeRequest(id: number, status: string, comment: string) {
  return request.put<ApiResult>(`/student/change-requests/${id}/review`, { status, comment })
}

export function cancelChangeRequest(id: number) {
  return request.put<ApiResult>(`/student/change-requests/${id}/cancel`)
}

export function exportStudentInfo(studentId: number) {
  return request.get(`/student/export/${studentId}`, { responseType: 'blob' })
}
