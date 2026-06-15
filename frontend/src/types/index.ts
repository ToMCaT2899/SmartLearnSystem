export interface UserInfo {
  token?: string
  userId: number
  username: string
  realName: string
  role: 'STUDENT' | 'TEACHER' | 'ADMIN'
  phone: string
  email: string
  avatar: string
}

export interface LoginForm {
  username: string
  password: string
}

export interface RegisterForm {
  username: string
  password: string
  realName: string
  role: 'STUDENT' | 'TEACHER'
  phone: string
  email: string
}

export interface ChangePasswordForm {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

export interface ApiResult<T = any> {
  code: number
  message: string
  data: T
}

export interface PageResult<T = any> {
  records: T[]
  total: number
  page: number
  size: number
}

// Menu item for sidebar
export interface MenuItem {
  path: string
  title: string
  icon: string
  children?: MenuItem[]
  roles: string[]
}

// Module 1: Student Info
export interface StudentProfile {
  id?: number
  userId?: number
  studentNo: string
  name: string
  photoUrl: string
  phone: string
  idCard: string
  college: string
  major: string
  grade: string
  className: string
  campus: string
  dormitory: string
  offCampusAddress: string
  medicalHistory: string
  hobbies: string
  personality: string
  careerGoal: string
  advisorName: string
  advisorPhone: string
  difficultyLevel: string
}

export interface FamilyMember {
  id?: number
  studentId?: number
  relation: string
  name: string
  phone: string
  workUnit: string
  address: string
}

export interface EmergencyContact {
  id?: number
  studentId?: number
  name: string
  relation: string
  phone: string
  address: string
}

export interface SupportingDocument {
  id?: number
  studentId?: number
  docType: string
  filePath: string
  uploadTime?: string
}

export interface InfoChangeRequest {
  id?: number
  studentId?: number
  fieldName: string
  oldValue: string
  newValue: string
  reason: string
  status?: string
  reviewerId?: number
  reviewComment?: string
  createdAt?: string
  reviewedAt?: string
}

// Module 2: Financial Aid
export interface Scholarship {
  id?: number
  name: string
  type: string
  amount: number
  quota: number
  criteria: string
  deadline: string
  publisherId?: number
  publisherName?: string
  publisherRole?: string
  applicationCount?: number
  admittedCount?: number
  status?: string
}

export interface ScholarshipApplication {
  id?: number
  scholarshipId: number
  studentId?: number
  applyReason: string
  supportingFiles: string
  status?: string
}

export interface GrantApplication {
  id?: number
  grantId: number
  studentId?: number
  applyReason: string
  supportingFiles: string
  status?: string
}

export interface WorkStudyApplication {
  id?: number
  jobId: number
  studentId?: number
  resume: string
  status?: string
}

export interface GrantInfo {
  id?: number
  name: string
  amount: number
  quota?: number
  criteria: string
  deadline: string
  publisherId?: number
  publisherName?: string
  publisherRole?: string
  applicationCount?: number
  admittedCount?: number
  status?: string
}

export interface WorkStudyJob {
  id?: number
  title: string
  description: string
  requirements: string
  salary: string
  location: string
  quota: number
  enrolledCount?: number
  applicationCount?: number
  publisherId?: number
  publisherName?: string
  publisherRole?: string
  status?: string
}

// Module 3: Academic
export interface Grade {
  id?: number
  studentId?: number
  studentNo?: string
  courseId?: number
  courseName?: string
  score: number
  gpaPoint: number
  semester: string
  examType: string
}

export interface Course {
  id?: number
  courseCode: string
  courseName: string
  credit: number
  semester: string
  teacherName: string
}

export interface AwardRecord {
  id?: number
  studentId?: number
  studentNo?: string
  awardName: string
  awardLevel: string
  awardDate: string
  issuer: string
  certificateFile: string
}

export interface CompetitionRecord {
  id?: number
  studentId?: number
  studentNo?: string
  competitionName: string
  level: string
  award: string
  date: string
  certificateFile: string
}

// Module 4: Career
export interface JobInfo {
  id?: number
  title: string
  company: string
  description: string
  requirements: string
  salaryRange: string
  location: string
  publisherId?: number
  publisherName?: string
  status?: string
  publishTime?: string
  deadline: string
}

// Module 5: Safety
export interface LateReturnRecord {
  id?: number
  studentId?: number
  studentName?: string
  studentNo?: string
  date: string
  returnTime: string
  reason: string
  status?: string
  reviewerName?: string
}

export interface OutingReport {
  id?: number
  studentId?: number
  studentName?: string
  studentNo?: string
  destination: string
  departureDate: string
  returnDate: string
  companion: string
  reason: string
  emergencyContact: string
  status?: string
  reviewerName?: string
}

export interface SafetyReport {
  id?: number
  reporterId?: number
  reporterName?: string
  reporterNo?: string
  reportType: string
  title: string
  description: string
  location: string
  images: string
  status?: string
  handlerName?: string
}

// Academic - missing shared types
export interface DisciplineRecord {
  id?: number
  studentId: number
  violationType: string
  description: string
  punishment: string
  recordDate: string
  status?: string
}

export interface MidtermAssessment {
  id?: number
  studentId: number
  semester: string
  selfSummary: string
  advisorComment: string
  score: number
  status?: string
}

export interface SecondClassroomScore {
  id?: number
  studentId: number
  category: string
  activityName: string
  score: number
  recordDate: string
  verifierId?: number
}

// Financial Aid - cross-cutting types
export interface ReviewQueueData {
  scholarshipApplications: any[]
  grantApplications: any[]
  workStudyApplications: any[]
}

export interface MyApplicationsData {
  scholarshipApplications: any[]
  grantApplications: any[]
  workStudyApplications: any[]
}

// Module 6: Communication
export interface PrivateMessage {
  id?: number
  senderId?: number
  receiverId: number
  content: string
  isRead?: number
  sentAt?: string
}

export interface ClassNotice {
  id?: number
  classIdentifier: string
  title: string
  content: string
  publisherId?: number
  publisherName?: string
  isPinned?: number
  publishedAt?: string
}

export interface Feedback {
  id?: number
  userId?: number
  title: string
  content: string
  type: string
  isAnonymous: number
  status?: string
  reply?: string
  repliedAt?: string
  submitterName?: string
  createdAt?: string
}

export interface ActivityPost {
  id?: number
  title: string
  content: string
  category: string
  organizer: string
  location: string
  eventTime: string
  maxParticipants: number
  currentParticipants?: number
  publisherId?: number
  publisherName?: string
  participantType?: string
  targetClass?: string
  status?: string
}
