<template>
  <el-container class="app-container">
    <!-- Sidebar -->
    <el-aside :width="isCollapse ? '64px' : '220px'" class="app-aside">
      <div class="logo">
        <div class="logo-icon">
          <el-icon :size="28" color="#fff"><School /></el-icon>
        </div>
        <transition name="fade">
          <div v-if="!isCollapse" class="logo-text">
            <span class="logo-title">智慧学工</span>
            <span class="logo-subtitle">SmartLearn</span>
          </div>
        </transition>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        background-color="#001529"
        text-color="#ffffffb3"
        active-text-color="#fff"
        router
        class="sidebar-menu"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <template #title>首页概览</template>
        </el-menu-item>

        <el-sub-menu index="student">
          <template #title>
            <el-icon><User /></el-icon>
            <span>学生信息</span>
          </template>
          <el-menu-item index="/student-info">
            <el-icon><UserFilled /></el-icon>
            <span>基本信息管理</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="aid">
          <template #title>
            <el-icon><Money /></el-icon>
            <span>奖助服务</span>
          </template>
          <el-menu-item index="/financial-aid/scholarships">
            <el-icon><Trophy /></el-icon>
            <span>奖学金</span>
          </el-menu-item>
          <el-menu-item index="/financial-aid/grants">
            <el-icon><Wallet /></el-icon>
            <span>助学金</span>
          </el-menu-item>
          <el-menu-item index="/financial-aid/work-study">
            <el-icon><Briefcase /></el-icon>
            <span>勤工助学</span>
          </el-menu-item>
          <el-menu-item v-if="userInfo?.role !== 'STUDENT'" index="/financial-aid/admitted-list">
            <el-icon><List /></el-icon>
            <span>录取名单</span>
          </el-menu-item>
          <el-menu-item v-if="userInfo?.role === 'STUDENT'" index="/financial-aid/my-applications">
            <el-icon><List /></el-icon>
            <span>我的申请</span>
          </el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="academic">
          <template #title>
            <el-icon><Document /></el-icon>
            <span>学业发展</span>
          </template>
          <el-menu-item index="/academic/grades">成绩查询</el-menu-item>
          <el-menu-item index="/academic/awards">评奖评优</el-menu-item>
          <el-menu-item index="/academic/discipline">违纪记录</el-menu-item>
          <el-menu-item index="/academic/midterm">中期鉴定</el-menu-item>
          <el-menu-item index="/academic/second-classroom">第二课堂</el-menu-item>
          <el-menu-item index="/academic/competitions">比赛获奖</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="career">
          <template #title>
            <el-icon><Guide /></el-icon>
            <span>职业规划</span>
          </template>
          <el-menu-item index="/career/assessment">生涯测评</el-menu-item>
          <el-menu-item index="/career/appointments">就业指导预约</el-menu-item>
          <el-menu-item index="/career/jobs">就业信息</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="safety">
          <template #title>
            <el-icon><Warning /></el-icon>
            <span>安全管理</span>
          </template>
          <el-menu-item index="/safety/late-returns">晚归登记</el-menu-item>
          <el-menu-item index="/safety/outing-reports">外出报备</el-menu-item>
          <el-menu-item index="/safety/reports">异常上报</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="communication">
          <template #title>
            <el-icon><ChatDotRound /></el-icon>
            <span>沟通互动</span>
          </template>
          <el-menu-item index="/communication/messages">私信</el-menu-item>
          <el-menu-item index="/communication/notices">班级公告</el-menu-item>
          <el-menu-item index="/communication/feedback">意见反馈</el-menu-item>
          <el-menu-item index="/communication/activities">活动报名</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- Main area -->
    <el-container class="main-container">
      <!-- Header -->
      <el-header class="app-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse" :size="20">
            <Fold v-if="!isCollapse" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">
              <el-icon style="vertical-align: -2px"><HomeFilled /></el-icon>
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-tooltip content="全屏" placement="bottom">
            <el-icon :size="20" class="header-icon" @click="toggleFullscreen"><FullScreen /></el-icon>
          </el-tooltip>

          <el-dropdown @command="handleCommand" trigger="click" class="user-dropdown">
            <span class="user-info">
              <el-avatar :size="34" :src="userInfo?.avatar" />
              <span class="user-name">{{ userInfo?.realName || '用户' }}</span>
              <el-tag :type="roleTagType" size="small" effect="dark" round>{{ roleShortText }}</el-tag>
              <el-icon class="arrow-icon"><CaretBottom /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <div class="dropdown-user-info">
                  <el-avatar :size="48" :src="userInfo?.avatar" />
                  <div class="dropdown-user-detail">
                    <strong>{{ userInfo?.realName || '用户' }}</strong>
                    <p>{{ roleText }} · {{ userInfo?.username }}</p>
                  </div>
                </div>
                <el-dropdown-item command="profile" divided>
                  <el-icon><User /></el-icon> 个人中心
                </el-dropdown-item>
                <el-dropdown-item command="password">
                  <el-icon><Lock /></el-icon> 修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Page tabs -->
      <div class="tabs-bar">
        <div class="tabs-list">
          <div
            v-for="tab in visitedTabs"
            :key="tab.path"
            :class="['tab-item', { active: tab.path === activeMenu }]"
            @click="router.push(tab.path)"
          >
            <span>{{ tab.title }}</span>
            <el-icon
              v-if="tab.path !== '/dashboard'"
              class="tab-close"
              @click.stop="closeTab(tab)"
            ><Close /></el-icon>
          </div>
        </div>
      </div>

      <!-- Main content -->
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade-slide" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>

      <!-- Footer -->
      <el-footer class="app-footer" height="36px">
        <span>SmartLearn 智慧学工系统 &copy; 2026</span>
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import {
  HomeFilled, Fold, Expand, FullScreen, CaretBottom,
  User, Lock, SwitchButton, Close, School, DataAnalysis,
  UserFilled, Money, Trophy, Wallet, Briefcase, Document,
  Guide, Warning, ChatDotRound, List,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const userInfo = computed(() => userStore.userInfo)
const activeMenu = computed(() => route.path)

const currentTitle = computed(() => {
  const meta = route.meta as { title?: string }
  return meta?.title || ''
})

const roleText = computed(() => {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[userInfo.value?.role || ''] || ''
})

const roleShortText = computed(() => {
  const map: Record<string, string> = { STUDENT: '学', TEACHER: '师', ADMIN: '管' }
  return map[userInfo.value?.role || ''] || ''
})

const roleTagType = computed(() => {
  const map: Record<string, string> = { STUDENT: 'success', TEACHER: 'warning', ADMIN: 'danger' }
  return map[userInfo.value?.role || ''] || 'info'
})

// Tabs management
interface TabItem { path: string; title: string }
const visitedTabs = ref<TabItem[]>([
  { path: '/dashboard', title: '首页概览' },
])

watch(() => route.path, (newPath) => {
  if (!visitedTabs.value.find(t => t.path === newPath)) {
    const meta = route.meta as { title?: string }
    visitedTabs.value.push({
      path: newPath,
      title: meta?.title || newPath,
    })
  }
})

function closeTab(tab: TabItem) {
  const idx = visitedTabs.value.findIndex(t => t.path === tab.path)
  if (idx > -1) {
    visitedTabs.value.splice(idx, 1)
    if (tab.path === activeMenu.value) {
      const next = visitedTabs.value[Math.min(idx, visitedTabs.value.length - 1)]
      if (next) router.push(next.path)
    }
  }
}

function handleCommand(command: string) {
  switch (command) {
    case 'profile':
      router.push('/student-info')
      break
    case 'password':
      router.push('/change-password')
      break
    case 'logout':
      userStore.logout()
      router.push('/login')
      break
  }
}

function toggleFullscreen() {
  if (document.fullscreenElement) {
    document.exitFullscreen()
  } else {
    document.documentElement.requestFullscreen()
  }
}

onMounted(async () => {
  if (!userStore.isLoggedIn()) return

  // Verify the stored token is not expired before making API calls
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      if (Date.now() >= payload.exp * 1000) {
        // Token already expired — don't bother fetching, just redirect
        userStore.logout()
        router.push('/login')
        return
      }
    }
  } catch {
    userStore.logout()
    router.push('/login')
    return
  }

  if (!userStore.userInfo) {
    userStore.loadUserFromStorage()
    try {
      await userStore.fetchUserInfo()
    } catch { /* 401 handled by interceptor; will redirect to /login */ }
  }
})
</script>

<style scoped>
.app-container {
  height: 100vh;
  background: #f0f2f5;
}

/* Sidebar */
.app-aside {
  background-color: #001529;
  overflow: hidden;
  transition: width 0.3s;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
}

.logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.logo-icon {
  flex-shrink: 0;
}

.logo-text {
  overflow: hidden;
  white-space: nowrap;
}

.logo-title {
  display: block;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.3;
}

.logo-subtitle {
  display: block;
  color: rgba(255, 255, 255, 0.45);
  font-size: 11px;
  letter-spacing: 0.5px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  overflow-y: auto;
}

.sidebar-menu::-webkit-scrollbar {
  width: 4px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 2px;
}

.sidebar-menu :deep(.el-sub-menu__title) {
  color: #ffffffb3 !important;
}

.sidebar-menu :deep(.el-sub-menu__title):hover {
  color: #fff !important;
  background-color: rgba(255, 255, 255, 0.06) !important;
}

.sidebar-menu :deep(.el-menu-item) {
  color: #ffffffb3 !important;
}

.sidebar-menu :deep(.el-menu-item):hover {
  color: #fff !important;
  background-color: rgba(255, 255, 255, 0.06) !important;
}

.sidebar-menu :deep(.el-menu-item.is-active) {
  background: linear-gradient(90deg, #667eea, #764ba2) !important;
  color: #fff !important;
  border-radius: 0 20px 20px 0;
  margin: 4px 8px 4px 0;
}

/* Main container */
.main-container {
  flex-direction: column;
  min-width: 0;
}

/* Header */
.app-header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 56px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  z-index: 10;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
  transition: color 0.3s;
}

.collapse-btn:hover {
  color: #409eff;
}

.breadcrumb {
  font-size: 14px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.header-icon {
  color: #606266;
  cursor: pointer;
  transition: color 0.3s;
}

.header-icon:hover {
  color: #409eff;
}

.header-badge {
  cursor: pointer;
}

.user-dropdown {
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 14px;
  color: #303133;
  font-weight: 500;
}

.arrow-icon {
  color: #c0c4cc;
  font-size: 12px;
  transition: transform 0.3s;
}

.dropdown-user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px 8px;
}

.dropdown-user-detail strong {
  display: block;
  font-size: 14px;
  color: #303133;
}

.dropdown-user-detail p {
  margin: 4px 0 0;
  font-size: 12px;
  color: #909399;
}

/* Tabs bar */
.tabs-bar {
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 12px;
  overflow-x: auto;
  white-space: nowrap;
}

.tabs-list {
  display: flex;
  align-items: center;
  gap: 2px;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 14px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  border-radius: 4px 4px 0 0;
  transition: all 0.2s;
  position: relative;
}

.tab-item:hover {
  color: #409eff;
  background: #ecf5ff;
}

.tab-item.active {
  color: #409eff;
  background: #ecf5ff;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: #409eff;
  border-radius: 1px 1px 0 0;
}

.tab-close {
  font-size: 12px;
  transition: color 0.2s;
}

.tab-close:hover {
  color: #f56c6c;
}

/* Main content */
.app-main {
  background: #f0f2f5;
  padding: 20px;
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

/* Footer */
.app-footer {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-top: 1px solid #e4e7ed;
  font-size: 12px;
  color: #c0c4cc;
}

/* Transitions */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.fade-slide-enter-active,
.fade-slide-leave-active {
  transition: all 0.3s ease;
}

.fade-slide-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.fade-slide-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}
</style>
