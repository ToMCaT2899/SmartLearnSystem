import request from '@/utils/request'
import type { ApiResult, PageResult, Grade, Course, AwardRecord, CompetitionRecord, DisciplineRecord, MidtermAssessment, SecondClassroomScore } from '@/types'

export function getGrades(studentNo?: string, semester?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<Grade>>>('/academic/grades', { params: { studentNo, semester, page, size } })
}

export function batchImportGrades(data: Grade[]) {
  return request.post<ApiResult>('/academic/grades/batch', data)
}

export function getCourses() {
  return request.get<ApiResult<Course[]>>('/academic/courses')
}

export function createCourse(data: Course) {
  return request.post<ApiResult>('/academic/courses', data)
}

// Awards
export function getAwards(studentNo?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<AwardRecord>>>('/academic/awards', { params: { studentNo, page, size } })
}
export function addAward(data: AwardRecord) {
  return request.post<ApiResult>('/academic/awards', data)
}
export function deleteAward(id: number) {
  return request.delete<ApiResult>(`/academic/awards/${id}`)
}

// Discipline
export function getDisciplineRecords(studentNo?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<DisciplineRecord>>>('/academic/discipline', { params: { studentNo, page, size } })
}
export function addDisciplineRecord(data: any, studentNo?: string) {
  return request.post<ApiResult>('/academic/discipline', data, { params: { studentNo } })
}
export function updateDisciplineRecord(id: number, data: any) {
  return request.put<ApiResult>(`/academic/discipline/${id}`, data)
}
export function deleteDisciplineRecord(id: number) {
  return request.delete<ApiResult>(`/academic/discipline/${id}`)
}

// Midterm
export function getMidtermAssessments(studentNo?: string, semester?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<MidtermAssessment>>>('/academic/midterm', { params: { studentNo, semester, page, size } })
}
export function saveMidtermAssessment(data: any, studentNo?: string) {
  return request.post<ApiResult>('/academic/midterm', data, { params: { studentNo } })
}
export function deleteMidtermAssessment(id: number) {
  return request.delete<ApiResult>(`/academic/midterm/${id}`)
}

// Second Classroom
export function getSecondClassroomScores(studentNo?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<SecondClassroomScore>>>('/academic/second-classroom', { params: { studentNo, page, size } })
}
export function addSecondClassroomScore(data: any, studentNo?: string) {
  return request.post<ApiResult>('/academic/second-classroom', data, { params: { studentNo } })
}
export function updateSecondClassroomScore(id: number, data: any) {
  return request.put<ApiResult>(`/academic/second-classroom/${id}`, data)
}
export function deleteSecondClassroomScore(id: number) {
  return request.delete<ApiResult>(`/academic/second-classroom/${id}`)
}

// Competitions
export function getCompetitions(studentNo?: string, page = 1, size = 10) {
  return request.get<ApiResult<PageResult<CompetitionRecord>>>('/academic/competitions', { params: { studentNo, page, size } })
}
export function addCompetition(data: CompetitionRecord) {
  return request.post<ApiResult>('/academic/competitions', data)
}
export function deleteCompetition(id: number) {
  return request.delete<ApiResult>(`/academic/competitions/${id}`)
}
