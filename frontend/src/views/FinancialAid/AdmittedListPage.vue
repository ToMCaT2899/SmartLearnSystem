<template>
  <div class="admitted-page">
    <h2 class="page-title">录取名单</h2>

    <!-- Project selector & export -->
    <el-card class="filter-card">
      <div class="filter-row">
        <el-select v-model="filterType" placeholder="选择项目类型" style="width: 180px" @change="onFilterTypeChange">
          <el-option label="奖学金" value="scholarship" />
          <el-option label="助学金" value="grant" />
          <el-option label="勤工助学" value="workStudy" />
        </el-select>
        <el-select
          v-model="filterItemId"
          placeholder="请选择具体项目"
          style="width: 280px; margin-left: 12px"
          :disabled="!filterType"
          v-loading="itemsLoading"
        >
          <el-option
            v-for="item in filterItems"
            :key="item.id"
            :label="item.label"
            :value="item.id"
          />
        </el-select>
        <el-button
          :disabled="!filterType && !filterItemId"
          style="margin-left: 12px"
          @click="handleResetFilter"
        >
          重置
        </el-button>
        <el-button
          type="primary"
          :disabled="!filterItemId"
          :loading="exporting"
          style="margin-left: 12px"
          @click="handleExport"
        >
          <el-icon style="margin-right: 4px"><Download /></el-icon>导出 Excel
        </el-button>
      </div>
    </el-card>

    <!-- Stats -->
    <div class="stats-row" v-if="list.length > 0">
      <div class="stat-card">
        <div class="stat-icon scholarship"><el-icon :size="22"><Trophy /></el-icon></div>
        <div>
          <div class="stat-value">{{ scholarshipCount }}</div>
          <div class="stat-label">奖学金已录取</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon grant"><el-icon :size="22"><Wallet /></el-icon></div>
        <div>
          <div class="stat-value">{{ grantCount }}</div>
          <div class="stat-label">助学金已录取</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon workstudy"><el-icon :size="22"><Briefcase /></el-icon></div>
        <div>
          <div class="stat-value">{{ workStudyCount }}</div>
          <div class="stat-label">勤工助学已录取</div>
        </div>
      </div>
    </div>

    <!-- Filter tabs -->
    <el-tabs v-model="activeType">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane label="奖学金" name="scholarship" />
      <el-tab-pane label="助学金" name="grant" />
      <el-tab-pane label="勤工助学" name="workStudy" />
    </el-tabs>

    <!-- Table -->
    <el-table :data="filteredList" v-loading="loading" border stripe>
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="projectName" label="项目名称" min-width="160" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="typeTag(row.type)" size="small">{{ typeLabel(row.type) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.projectStatus === 'OPEN' ? 'success' : 'info'" size="small">{{ row.projectStatus === 'OPEN' ? '进行中' : '已关闭' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="studentName" label="学生姓名" width="120" />
      <el-table-column prop="studentPhone" label="联系电话" width="140" />
      <el-table-column label="金额" width="130">
        <template #default="{ row }">
          <span v-if="row.type === 'workStudy'">{{ row.projectAmount }}</span>
          <span v-else>¥{{ row.projectAmount?.toLocaleString() }}</span>
        </template>
      </el-table-column>
      <el-table-column label="附件" min-width="150">
        <template #default="{ row }">
          <template v-if="row.type === 'workStudy'">
            <template v-if="splitResumeAndFiles(row.resume).files.length">
              <div v-for="f in splitResumeAndFiles(row.resume).files" :key="f.url">
                <el-link type="primary" @click.prevent="openFile(f.url)" :underline="false" size="small">{{ f.name }}</el-link>
              </div>
            </template>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
          <template v-else>
            <template v-if="parseFileUrls(row.supportingFiles).length">
              <div v-for="f in parseFileUrls(row.supportingFiles)" :key="f.url">
                <el-link type="primary" @click.prevent="openFile(f.url)" :underline="false" size="small">{{ f.name }}</el-link>
              </div>
            </template>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </template>
      </el-table-column>
      <el-table-column prop="reviewComment" label="审核意见" min-width="150">
        <template #default="{ row }">
          <span v-if="row.reviewComment">{{ row.reviewComment }}</span>
          <span v-else style="color: #c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="审核时间" width="170">
        <template #default="{ row }">
          {{ row.reviewTime ? formatTime(row.reviewTime) : '-' }}
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

    <el-empty v-if="!loading && filteredList.length === 0" description="暂无录取记录" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import {
  getAdmittedList,
  scholarshipApi,
  grantApi,
  workStudyApi,
  exportScholarshipApplications,
  exportGrantApplications,
  exportWorkStudyApplications,
} from '@/api/financial-aid'
import { ElMessage } from 'element-plus'
import { Trophy, Wallet, Briefcase, Download } from '@element-plus/icons-vue'
import { getDownloadUrl } from '@/api/file'

interface FilterItem {
  id: number
  label: string
}

function formatTime(time: string) {
  const d = new Date(time)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

async function openFile(url: string) {
  try {
    const res = await getDownloadUrl(url)
    window.open(res.data.data, '_blank')
  } catch { /* handled */ }
}

function extractFileName(url: string): string {
  const name = url.substring(url.lastIndexOf('/') + 1)
  const qi = name.indexOf('?')
  return qi > 0 ? name.substring(0, qi) : name
}

function parseFileUrls(urlsStr?: string): { name: string; url: string }[] {
  if (!urlsStr) return []
  return urlsStr.split(',').map(u => {
    const trimmed = u.trim()
    return trimmed ? { name: extractFileName(trimmed), url: trimmed } : null
  }).filter(Boolean) as { name: string; url: string }[]
}

function splitResumeAndFiles(resume?: string): { text: string; files: { name: string; url: string }[] } {
  if (!resume) return { text: '', files: [] }
  const attachIdx = resume.indexOf('附件:')
  if (attachIdx < 0) return { text: resume, files: [] }
  const fileUrlPart = resume.substring(attachIdx + 3).trim().replace(/^[\s,]+/, '')
  const files = fileUrlPart.split(',').map(u => {
    const trimmed = u.trim()
    return trimmed ? { name: extractFileName(trimmed), url: trimmed } : null
  }).filter(Boolean) as { name: string; url: string }[]
  return { text: '', files }
}

const list = ref<any[]>([])
const loading = ref(false)
const activeType = ref('all')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// Filter/export state
const filterType = ref('')
const filterItemId = ref<number | null>(null)
const filterItems = ref<FilterItem[]>([])
const itemsLoading = ref(false)
const exporting = ref(false)

const scholarshipCount = computed(() => list.value.filter(i => i.type === 'scholarship').length)
const grantCount = computed(() => list.value.filter(i => i.type === 'grant').length)
const workStudyCount = computed(() => list.value.filter(i => i.type === 'workStudy').length)

// Apply type tab filter first, then project filter if selected
const filteredList = computed(() => {
  let result = list.value
  if (activeType.value !== 'all') {
    result = result.filter(i => i.type === activeType.value)
  }
  if (filterType.value && filterItemId.value) {
    result = result.filter(i => i.type === filterType.value && i.projectId === filterItemId.value)
  }
  return result
})

function typeLabel(type: string) {
  const map: Record<string, string> = { scholarship: '奖学金', grant: '助学金', workStudy: '勤工助学' }
  return map[type] || type
}

function typeTag(type: string) {
  const map: Record<string, string> = { scholarship: 'warning', grant: 'success', workStudy: '' }
  return map[type] || 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getAdmittedList(currentPage.value, pageSize.value)
    list.value = (res.data as any)?.data?.records ?? []
    total.value = (res.data as any)?.data?.total ?? 0
  } catch {
    ElMessage.error('加载录取名单失败')
  } finally {
    loading.value = false
  }
}

async function onFilterTypeChange() {
  filterItemId.value = null
  filterItems.value = []
  if (!filterType.value) return
  itemsLoading.value = true
  try {
    let raw: any[] = []
    if (filterType.value === 'scholarship') {
      const res = await scholarshipApi.list(1, 1000)
      raw = res.data.data?.records || []
      filterItems.value = raw.map((s: any) => ({ id: s.id!, label: s.name }))
    } else if (filterType.value === 'grant') {
      const res = await grantApi.list(1, 1000)
      raw = res.data.data?.records || []
      filterItems.value = raw.map((g: any) => ({ id: g.id!, label: g.name }))
    } else if (filterType.value === 'workStudy') {
      const res = await workStudyApi.list(1, 1000)
      raw = res.data.data?.records || []
      filterItems.value = raw.map((w: any) => ({ id: w.id!, label: w.title }))
    }
  } catch {
    ElMessage.error('加载项目列表失败')
  } finally {
    itemsLoading.value = false
  }
}

function handleResetFilter() {
  filterType.value = ''
  filterItemId.value = null
  filterItems.value = []
}

async function handleExport() {
  if (!filterType.value || !filterItemId.value) return
  exporting.value = true
  try {
    if (filterType.value === 'scholarship') {
      await exportScholarshipApplications(filterItemId.value)
    } else if (filterType.value === 'grant') {
      await exportGrantApplications(filterItemId.value)
    } else if (filterType.value === 'workStudy') {
      await exportWorkStudyApplications(filterItemId.value)
    }
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

watch([currentPage, pageSize], () => { loadData() })

// Reset page on tab change
watch(activeType, () => { currentPage.value = 1 })

onMounted(() => loadData())
</script>

<style scoped>
.admitted-page {
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

.stat-icon.scholarship { background: linear-gradient(135deg, #f0ad4e, #ec971f); }
.stat-icon.grant { background: linear-gradient(135deg, #5cb85c, #449d44); }
.stat-icon.workstudy { background: linear-gradient(135deg, #5bc0de, #31b0d5); }

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

.filter-card {
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: center;
}
</style>
