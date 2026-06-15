<template>
  <div class="my-apps-page">
    <h2 class="page-title">{{ isTeacherOrAdmin ? '导出申请名单' : '我的申请' }}</h2>

    <!-- Teacher/Admin: Export -->
    <template v-if="isTeacherOrAdmin">
      <el-card class="export-card">
        <div class="export-row">
          <el-select v-model="exportType" placeholder="选择项目类型" style="width: 180px" @change="onExportTypeChange">
            <el-option label="奖学金" value="scholarship" />
            <el-option label="助学金" value="grant" />
            <el-option label="勤工助学" value="workStudy" />
          </el-select>
          <el-select
            v-model="exportItemId"
            placeholder="请选择具体项目"
            style="width: 280px; margin-left: 12px"
            :disabled="!exportType"
            v-loading="itemsLoading"
          >
            <el-option
              v-for="item in exportItems"
              :key="item.id"
              :label="item.label"
              :value="item.id"
            />
          </el-select>
          <el-button
            type="primary"
            :disabled="!exportItemId"
            :loading="exporting"
            style="margin-left: 12px"
            @click="handleExport"
          >
            <el-icon style="margin-right: 4px"><Download /></el-icon>导出 Excel
          </el-button>
        </div>
      </el-card>
    </template>

    <!-- Student: Stats -->
    <template v-if="isStudent">
      <div class="stats-row" v-if="allApps.length > 0">
        <div class="stat-card">
          <div class="stat-icon pending"><el-icon :size="22"><Clock /></el-icon></div>
          <div>
            <div class="stat-value">{{ pendingCount }}</div>
            <div class="stat-label">待审核</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon approved"><el-icon :size="22"><CircleCheck /></el-icon></div>
          <div>
            <div class="stat-value">{{ approvedCount }}</div>
            <div class="stat-label">已通过</div>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon rejected"><el-icon :size="22"><CircleClose /></el-icon></div>
          <div>
            <div class="stat-value">{{ rejectedCount }}</div>
            <div class="stat-label">已拒绝</div>
          </div>
        </div>
      </div>

      <!-- Filter tabs -->
      <el-tabs v-model="activeFilter" @tab-change="handleFilterChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane label="奖学金" name="scholarship" />
        <el-tab-pane label="助学金" name="grant" />
        <el-tab-pane label="勤工助学" name="workStudy" />
      </el-tabs>

      <!-- Table -->
      <el-table :data="filteredList" v-loading="loading" border stripe empty-text="暂无申请记录">
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="projectName" label="项目名称" min-width="180" />
        <el-table-column prop="applyReason" label="申请理由" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reviewComment" label="审核意见" min-width="150">
          <template #default="{ row }">
            <span v-if="row.reviewComment">{{ row.reviewComment }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="170">
          <template #default="{ row }">
            {{ row.applyTime ? formatTime(row.applyTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="审核时间" width="170">
          <template #default="{ row }">
            {{ row.reviewTime ? formatTime(row.reviewTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="danger" size="small" link @click="cancelApplication(row)">取消申请</el-button>
            </template>
            <span v-else style="color:#c0c4cc">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
        style="margin-top: 16px; justify-content: flex-end"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getMyApplications,
  scholarshipApi,
  grantApi,
  workStudyApi,
  exportScholarshipApplications,
  exportGrantApplications,
  exportWorkStudyApplications,
} from '@/api/financial-aid'
import { Clock, CircleCheck, CircleClose, Download } from '@element-plus/icons-vue'

interface AppItem {
  id?: number
  type: string
  projectId?: number
  projectName: string
  applyReason: string
  status: string
  reviewComment?: string
  applyTime?: string
  reviewTime?: string
}

interface ExportItem {
  id: number
  label: string
}

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isStudent = computed(() => role.value === 'STUDENT')
const isTeacherOrAdmin = computed(() => role.value === 'TEACHER' || role.value === 'ADMIN')

// Student state
const loading = ref(false)
const allApps = ref<AppItem[]>([])
const activeFilter = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// Export state
const exportType = ref('')
const exportItemId = ref<number | null>(null)
const exportItems = ref<ExportItem[]>([])
const itemsLoading = ref(false)
const exporting = ref(false)

const pendingCount = computed(() => allApps.value.filter(a => a.status === 'PENDING').length)
const approvedCount = computed(() => allApps.value.filter(a => a.status === 'APPROVED').length)
const rejectedCount = computed(() => allApps.value.filter(a => a.status === 'REJECTED').length)

const filteredList = computed(() => {
  if (activeFilter.value === 'all') return allApps.value
  return allApps.value.filter(a => a.type === activeFilter.value)
})

function formatTime(time: string) {
  const d = new Date(time)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function typeLabel(type: string) {
  const map: Record<string, string> = { scholarship: '奖学金', grant: '助学金', workStudy: '勤工助学' }
  return map[type] || type
}

function typeTag(type: string) {
  const map: Record<string, string> = { scholarship: 'warning', grant: 'success', workStudy: '' }
  return map[type] || 'info'
}

function statusTagType(status: string) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[status] || 'info'
}

function statusText(status: string) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }[status] || status
}

function handleFilterChange() {
  currentPage.value = 1
}

async function cancelApplication(row: AppItem) {
  try {
    await ElMessageBox.confirm('确定要取消该申请吗？取消后可以重新申请。', '确认取消', { type: 'warning' })
    if (row.type === 'scholarship') {
      await scholarshipApi.cancel(row.id!)
    } else if (row.type === 'grant') {
      await grantApi.cancel(row.id!)
    } else if (row.type === 'workStudy') {
      await workStudyApi.cancel(row.id!)
    }
    ElMessage.success('申请已取消')
    await loadData()
  } catch { /* cancelled or error */ }
}

// Export logic
async function onExportTypeChange() {
  exportItemId.value = null
  exportItems.value = []
  if (!exportType.value) return
  itemsLoading.value = true
  try {
    let list: any[] = []
    if (exportType.value === 'scholarship') {
      const res = await scholarshipApi.list(1, 1000)
      list = res.data.data?.records || []
      exportItems.value = list.map((s: any) => ({ id: s.id!, label: s.name }))
    } else if (exportType.value === 'grant') {
      const res = await grantApi.list(1, 1000)
      list = res.data.data?.records || []
      exportItems.value = list.map((g: any) => ({ id: g.id!, label: g.name }))
    } else if (exportType.value === 'workStudy') {
      const res = await workStudyApi.list(1, 1000)
      list = res.data.data?.records || []
      exportItems.value = list.map((w: any) => ({ id: w.id!, label: w.title }))
    }
  } catch {
    ElMessage.error('加载项目列表失败')
  } finally {
    itemsLoading.value = false
  }
}

async function handleExport() {
  if (!exportType.value || !exportItemId.value) return
  exporting.value = true
  try {
    if (exportType.value === 'scholarship') {
      await exportScholarshipApplications(exportItemId.value)
    } else if (exportType.value === 'grant') {
      await exportGrantApplications(exportItemId.value)
    } else if (exportType.value === 'workStudy') {
      await exportWorkStudyApplications(exportItemId.value)
    }
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

async function loadData() {
  loading.value = true
  try {
    const res = await getMyApplications(currentPage.value, pageSize.value)
    const data = (res.data as any)?.data
    const list: AppItem[] = []

    const scholarshipPR = data?.scholarshipApplications
    if (scholarshipPR?.records) {
      scholarshipPR.records.forEach((a: any) => {
        list.push({
          id: a.id,
          type: 'scholarship',
          projectId: a.scholarshipId,
          projectName: a.scholarshipName || '奖学金 #' + a.scholarshipId,
          applyReason: a.applyReason || '',
          status: a.status,
          reviewComment: a.reviewComment,
          applyTime: a.applyTime,
          reviewTime: a.reviewTime,
        })
      })
    }

    const grantPR = data?.grantApplications
    if (grantPR?.records) {
      grantPR.records.forEach((a: any) => {
        list.push({
          id: a.id,
          type: 'grant',
          projectId: a.grantId,
          projectName: a.grantName || '助学金 #' + a.grantId,
          applyReason: a.applyReason || '',
          status: a.status,
          reviewComment: a.reviewComment,
          applyTime: a.applyTime,
          reviewTime: a.reviewTime,
        })
      })
    }

    const workStudyPR = data?.workStudyApplications
    if (workStudyPR?.records) {
      workStudyPR.records.forEach((a: any) => {
        list.push({
          id: a.id,
          type: 'workStudy',
          projectId: a.jobId,
          projectName: a.jobTitle || '勤工助学 #' + a.jobId,
          applyReason: typeof a.resume === 'string' ? a.resume.replace(/附件:[\s\S]*$/, '').trim() : '',
          status: a.status,
          reviewComment: a.reviewComment,
          applyTime: a.applyTime,
          reviewTime: a.reviewTime,
        })
      })
    }

    // Sort by applyTime desc
    list.sort((a, b) => new Date(b.applyTime || 0).getTime() - new Date(a.applyTime || 0).getTime())
    allApps.value = list
    total.value = (scholarshipPR?.total || 0) + (grantPR?.total || 0) + (workStudyPR?.total || 0)
  } catch {
    ElMessage.error('加载申请记录失败')
  } finally {
    loading.value = false
  }
}

watch([currentPage, pageSize], () => { loadData() })

onMounted(() => {
  if (isStudent.value) loadData()
})
</script>

<style scoped>
.my-apps-page {
  padding: 0 4px;
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 2px solid #f0f2f5;
}

.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.stat-icon.pending { background: linear-gradient(135deg, #e6a23c, #f0ad4e); }
.stat-icon.approved { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-icon.rejected { background: linear-gradient(135deg, #f56c6c, #f89898); }

.stat-value {
  font-size: 20px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.export-card {
  margin-bottom: 20px;
}

.export-row {
  display: flex;
  align-items: center;
}
</style>
