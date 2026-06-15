import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

/**
 * Check whether a stored JWT token is still valid (not expired).
 * Returns false if the token is missing, malformed, or expired.
 */
function isTokenValid(): boolean {
  const token = localStorage.getItem('token')
  if (!token) return false
  try {
    // JWT: header.payload.signature
    const payload = JSON.parse(atob(token.split('.')[1]))
    const exp = payload.exp * 1000 // JWT exp is in seconds
    return Date.now() < exp
  } catch {
    // Token is malformed — treat as invalid
    return false
  }
}

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login/LoginPage.vue'),
    meta: { noAuth: true },
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Login/RegisterPage.vue'),
    meta: { noAuth: true },
  },
  {
    path: '/',
    component: () => import('@/components/Layout/AppLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard/DashboardPage.vue'),
        meta: { title: '首页' },
      },
      {
        path: 'change-password',
        name: 'ChangePassword',
        component: () => import('@/views/Profile/ChangePasswordPage.vue'),
        meta: { title: '修改密码' },
      },
      // Module 1: Student Info
      {
        path: 'student-info',
        name: 'StudentInfo',
        component: () => import('@/views/StudentInfo/StudentInfoPage.vue'),
        meta: { title: '学生信息管理' },
      },
      // Module 2: Financial Aid
      {
        path: 'financial-aid/scholarships',
        name: 'Scholarships',
        component: () => import('@/views/FinancialAid/ScholarshipPage.vue'),
        meta: { title: '奖学金管理' },
      },
      {
        path: 'financial-aid/grants',
        name: 'Grants',
        component: () => import('@/views/FinancialAid/GrantPage.vue'),
        meta: { title: '助学金管理' },
      },
      {
        path: 'financial-aid/work-study',
        name: 'WorkStudy',
        component: () => import('@/views/FinancialAid/WorkStudyPage.vue'),
        meta: { title: '勤工助学' },
      },
      {
        path: 'financial-aid/admitted-list',
        name: 'AdmittedList',
        component: () => import('@/views/FinancialAid/AdmittedListPage.vue'),
        meta: { title: '录取名单' },
      },
      {
        path: 'financial-aid/my-applications',
        name: 'MyApplications',
        component: () => import('@/views/FinancialAid/MyApplicationsPage.vue'),
        meta: { title: '我的申请' },
      },
      // Module 3: Academic
      {
        path: 'academic/grades',
        name: 'Grades',
        component: () => import('@/views/AcademicDev/GradesPage.vue'),
        meta: { title: '成绩查询' },
      },
      {
        path: 'academic/awards',
        name: 'Awards',
        component: () => import('@/views/AcademicDev/AwardsPage.vue'),
        meta: { title: '评奖评优' },
      },
      {
        path: 'academic/discipline',
        name: 'Discipline',
        component: () => import('@/views/AcademicDev/DisciplinePage.vue'),
        meta: { title: '违纪记录' },
      },
      {
        path: 'academic/midterm',
        name: 'Midterm',
        component: () => import('@/views/AcademicDev/MidtermPage.vue'),
        meta: { title: '中期鉴定' },
      },
      {
        path: 'academic/second-classroom',
        name: 'SecondClassroom',
        component: () => import('@/views/AcademicDev/SecondClassroomPage.vue'),
        meta: { title: '第二课堂' },
      },
      {
        path: 'academic/competitions',
        name: 'Competitions',
        component: () => import('@/views/AcademicDev/CompetitionsPage.vue'),
        meta: { title: '比赛获奖' },
      },
      // Module 4: Career
      {
        path: 'career/assessment',
        name: 'CareerAssessment',
        component: () => import('@/views/CareerPlan/AssessmentPage.vue'),
        meta: { title: '生涯测评' },
      },
      {
        path: 'career/appointments',
        name: 'CounselingAppointments',
        component: () => import('@/views/CareerPlan/AppointmentsPage.vue'),
        meta: { title: '就业指导预约' },
      },
      {
        path: 'career/jobs',
        name: 'Jobs',
        component: () => import('@/views/CareerPlan/JobsPage.vue'),
        meta: { title: '就业信息' },
      },
      // Module 5: Safety
      {
        path: 'safety/late-returns',
        name: 'LateReturns',
        component: () => import('@/views/SafetyManage/LateReturnsPage.vue'),
        meta: { title: '晚归登记' },
      },
      {
        path: 'safety/outing-reports',
        name: 'OutingReports',
        component: () => import('@/views/SafetyManage/OutingReportsPage.vue'),
        meta: { title: '外出报备' },
      },
      {
        path: 'safety/reports',
        name: 'SafetyReports',
        component: () => import('@/views/SafetyManage/SafetyReportsPage.vue'),
        meta: { title: '异常上报' },
      },
      // Module 6: Communication
      {
        path: 'communication/messages',
        name: 'Messages',
        component: () => import('@/views/Communication/MessagesPage.vue'),
        meta: { title: '私信' },
      },
      {
        path: 'communication/notices',
        name: 'Notices',
        component: () => import('@/views/Communication/NoticesPage.vue'),
        meta: { title: '班级公告' },
      },
      {
        path: 'communication/feedback',
        name: 'Feedback',
        component: () => import('@/views/Communication/FeedbackPage.vue'),
        meta: { title: '意见反馈' },
      },
      {
        path: 'communication/activities',
        name: 'Activities',
        component: () => import('@/views/Communication/ActivitiesPage.vue'),
        meta: { title: '学术交流/活动报名' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory('/smartlearn/'),
  routes,
})

router.beforeEach((to, _from, next) => {
  const authenticated = isTokenValid()
  if (to.meta.noAuth) {
    if (authenticated && (to.path === '/login' || to.path === '/register')) {
      next('/dashboard')
    } else {
      next()
    }
  } else {
    if (!authenticated) {
      // Clear any stale data before redirecting
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      next('/login')
    } else {
      next()
    }
  }
})

export default router
