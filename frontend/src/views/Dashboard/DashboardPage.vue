<template>
  <div class="dashboard">
    <!-- Welcome section -->
    <div class="welcome-section">
      <div class="welcome-text">
        <h2 class="page-title">欢迎回来，{{ userInfo?.realName || '用户' }}</h2>
        <p class="welcome-date">{{ currentDate }} · {{ roleText }} · 智慧学工系统</p>
      </div>
      <div class="welcome-avatar">
        <el-avatar :size="56" :src="userInfo?.avatar" />
      </div>
    </div>

    <!-- Stats cards -->
    <el-row :gutter="20" class="stats-row">
      <el-col v-for="(stat, idx) in stats" :key="stat.title" :xs="24" :sm="12" :lg="6">
        <el-card shadow="hover" class="stat-card" :style="{ animationDelay: `${idx * 0.1}s` }">
          <div class="stat-content">
            <div class="stat-icon" :style="{ background: stat.gradient }">
              <el-icon :size="26" color="#fff"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-title">{{ stat.title }}</div>
            </div>
          </div>
          <div class="stat-trend" v-if="stat.trend">
            <span :class="stat.trend > 0 ? 'trend-up' : 'trend-down'">
              <el-icon><component :is="stat.trend > 0 ? 'CaretTop' : 'CaretBottom'" /></el-icon>
              {{ Math.abs(stat.trend) }}%
            </span>
            <span class="trend-label">较上月</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Charts row -->
    <el-row :gutter="20" class="charts-row">
      <el-col :xs="24" :lg="14">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">数据概览</span>
              <el-radio-group v-model="chartPeriod" size="small">
                <el-radio-button label="week">近一周</el-radio-button>
                <el-radio-button label="month">近一月</el-radio-button>
                <el-radio-button label="semester">本学期</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="barChartRef" class="chart-container"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="10">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">分布统计</span>
            </div>
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Bottom row -->
    <el-row :gutter="20" class="bottom-row">
      <el-col :span="24">
        <el-card shadow="hover" class="quick-card">
          <template #header>
            <span class="card-title">快捷入口</span>
          </template>
          <div class="quick-actions-grid">
            <div
              v-for="action in quickActions"
              :key="action.title"
              class="action-card"
              @click="router.push(action.path)"
            >
              <div class="action-icon" :style="{ background: action.bg }">
                <el-icon :size="22" :color="action.color"><component :is="action.icon" /></el-icon>
              </div>
              <span>{{ action.title }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import * as echarts from 'echarts'
import {
  User, Money, Document, Guide, Warning, ChatDotRound,
  Bell, School, DataAnalysis, CaretTop, CaretBottom,
} from '@element-plus/icons-vue'
import { getDashboard, type DashboardData, type StatCard } from '@/api/dashboard'

const router = useRouter()
const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const barChartRef = ref<HTMLElement | null>(null)
const pieChartRef = ref<HTMLElement | null>(null)
const chartPeriod = ref('week')
const loading = ref(false)
const dashboardData = ref<DashboardData | null>(null)

let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const iconMap: Record<string, any> = {
  User, Money, Document, Guide, Warning, ChatDotRound,
  Bell, School, DataAnalysis, CaretTop, CaretBottom,
}

const currentDate = computed(() => {
  const now = new Date()
  const days = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${days[now.getDay()]}`
})

const roleText = computed(() => {
  const map: Record<string, string> = { STUDENT: '学生', TEACHER: '教师', ADMIN: '管理员' }
  return map[userInfo.value?.role || ''] || ''
})

const stats = computed<StatCard[]>(() => {
  if (!dashboardData.value?.stats) return []
  return dashboardData.value.stats.map((s: StatCard) => ({
    ...s,
    icon: iconMap[s.icon] || s.icon,
  }))
})

const quickActions = [
  { title: '学生信息', path: '/student-info', icon: User, color: '#667eea', bg: '#f0f0ff' },
  { title: '奖助服务', path: '/financial-aid/scholarships', icon: Money, color: '#f5576c', bg: '#fff0f3' },
  { title: '学业发展', path: '/academic/grades', icon: School, color: '#e6a23c', bg: '#fef9e7' },
  { title: '职业规划', path: '/career/assessment', icon: Guide, color: '#4facfe', bg: '#eef9ff' },
  { title: '安全管理', path: '/safety/late-returns', icon: Warning, color: '#f56c6c', bg: '#fef0f0' },
  { title: '沟通互动', path: '/communication/messages', icon: ChatDotRound, color: '#43e97b', bg: '#edfdf3' },
]

function getBarOption() {
  const chartData = dashboardData.value?.barChart
  const days = chartData?.days || []
  const vals = chartData?.vals || []
  return {
    tooltip: { trigger: 'axis', backgroundColor: '#fff', borderColor: '#e4e7ed', textStyle: { color: '#333' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: days, axisLine: { lineStyle: { color: '#dcdfe6' } }, axisLabel: { color: '#909399' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f2f3f5' } }, axisLabel: { color: '#909399' } },
    series: [
      {
        name: '活跃次数',
        type: 'bar',
        data: vals,
        itemStyle: {
          borderRadius: [6, 6, 0, 0],
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#667eea' },
            { offset: 1, color: '#a8b3ff' },
          ]),
        },
        barWidth: '40%',
        emphasis: { itemStyle: { color: '#667eea' } },
      },
    ],
  }
}

function getPieOption() {
  const pieData = dashboardData.value?.pieChart || []
  return {
    tooltip: { trigger: 'item', backgroundColor: '#fff', borderColor: '#e4e7ed', textStyle: { color: '#333' } },
    legend: { bottom: '0', textStyle: { color: '#909399', fontSize: 12 } },
    series: [
      {
        name: '分布',
        type: 'pie',
        radius: ['50%', '75%'],
        center: ['50%', '45%'],
        avoidLabelOverlap: false,
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { show: false },
        emphasis: { label: { show: true, fontSize: 16, fontWeight: 'bold' } },
        data: pieData,
        color: ['#667eea', '#f5576c', '#4facfe', '#43e97b', '#ffb74d'],
      },
    ],
  }
}

function initCharts() {
  if (barChartRef.value) {
    barChart = echarts.init(barChartRef.value)
    barChart.setOption(getBarOption())
  }
  if (pieChartRef.value) {
    pieChart = echarts.init(pieChartRef.value)
    pieChart.setOption(getPieOption())
  }
}

function refreshCharts() {
  barChart?.setOption(getBarOption())
  pieChart?.setOption(getPieOption())
}

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
}

async function fetchDashboard() {
  loading.value = true
  try {
    const res = await getDashboard(chartPeriod.value)
    dashboardData.value = res.data.data
    await nextTick()
    if (!barChart) {
      initCharts()
    } else {
      refreshCharts()
    }
  } catch {
    ElMessage.error('获取仪表盘数据失败')
  } finally {
    loading.value = false
  }
}

// Re-fetch when period changes
watch(chartPeriod, () => {
  fetchDashboard()
})

onMounted(() => {
  fetchDashboard()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  max-width: 1400px;
}

/* Welcome section */
.welcome-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24px 28px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  margin-bottom: 24px;
  color: #fff;
}

.welcome-text .page-title {
  margin: 0;
  font-size: 24px;
  color: #fff;
}

.welcome-date {
  margin: 6px 0 0;
  font-size: 14px;
  opacity: 0.85;
}

/* Stats cards */
.stats-row {
  margin-bottom: 8px;
}

.stat-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: none;
  transition: all 0.3s ease;
  animation: fadeInUp 0.5s ease both;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1;
}

.stat-title {
  font-size: 13px;
  color: #86909c;
  margin-top: 6px;
}

.stat-trend {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f2f3f5;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}

.trend-up {
  color: #00b42a;
  display: flex;
  align-items: center;
  gap: 2px;
}

.trend-down {
  color: #f53f3f;
  display: flex;
  align-items: center;
  gap: 2px;
}

.trend-label {
  color: #86909c;
}

/* Charts */
.charts-row {
  margin-bottom: 8px;
}

.chart-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: none;
}

.chart-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d2129;
}

.chart-container {
  height: 320px;
}

/* Quick actions */
.quick-card {
  border-radius: 12px;
  border: none;
  margin-bottom: 16px;
}

.quick-actions-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 18px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: #f7f8fa;
}

.action-card:hover {
  background: #ecf5ff;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-card span {
  font-size: 13px;
  color: #4e5969;
  font-weight: 500;
}

.bottom-row {
  margin-bottom: 16px;
}

/* Animations */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
