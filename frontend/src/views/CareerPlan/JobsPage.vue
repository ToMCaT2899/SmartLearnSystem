<template>
  <div class="jobs-page">
    <h2 class="page-title">就业信息</h2>

    <!-- Search bar -->
    <div class="search-bar">
      <el-form :inline="true" :model="filters">
        <el-form-item label="关键词">
          <el-input v-model="filters.keyword" placeholder="岗位名称/公司" clearable style="width: 220px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="filters.location" placeholder="工作地点" clearable style="width: 180px" @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon> 搜索
          </el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
      <el-button v-if="isManager" type="primary" @click="openCreateDialog" style="margin-bottom: 12px">
        <el-icon><Plus /></el-icon> 发布岗位
      </el-button>
    </div>

    <!-- Jobs table -->
    <el-table :data="jobs" v-loading="loading" border stripe @row-click="showDetail" style="cursor: pointer">
      <el-table-column prop="title" label="岗位名称" min-width="180">
        <template #default="{ row }">
          <el-link type="primary" @click.stop="showDetail(row)">{{ row.title }}</el-link>
        </template>
      </el-table-column>
      <el-table-column prop="company" label="公司" width="160" />
      <el-table-column prop="salaryRange" label="薪资范围" width="140" />
      <el-table-column prop="location" label="工作地点" width="130" />
      <el-table-column prop="publisherName" label="发布人" width="100" />
      <el-table-column prop="publishTime" label="发布时间" width="160">
        <template #default="{ row }">
          {{ formatTime(row.publishTime) }}
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'" size="small">
            {{ row.status === 'OPEN' ? '招聘中' : '已关闭' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="deadline" label="截止日期" width="130" />
      <el-table-column v-if="isManager" label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click.stop="openEditDialog(row)">编辑</el-button>
          <el-button
            size="small"
            link
            :type="row.status === 'OPEN' ? 'warning' : 'success'"
            @click.stop="toggleStatus(row)"
          >
            {{ row.status === 'OPEN' ? '关闭' : '开启' }}
          </el-button>
          <el-button type="danger" size="small" link @click.stop="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Pagination -->
    <div class="pagination-wrapper" v-if="jobs.length > 0">
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        background
      />
    </div>

    <!-- Detail Dialog -->
    <el-dialog v-model="detailVisible" title="岗位详情" width="650px">
      <template v-if="detailJob">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="岗位名称" :span="2">{{ detailJob.title }}</el-descriptions-item>
          <el-descriptions-item label="公司">{{ detailJob.company }}</el-descriptions-item>
          <el-descriptions-item label="工作地点">{{ detailJob.location }}</el-descriptions-item>
          <el-descriptions-item label="薪资范围">{{ detailJob.salaryRange }}</el-descriptions-item>
          <el-descriptions-item label="截止日期">{{ detailJob.deadline }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ detailJob.publisherName }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(detailJob.publishTime) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailJob.status === 'OPEN' ? 'success' : 'info'" size="small">
              {{ detailJob.status === 'OPEN' ? '招聘中' : '已关闭' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="岗位描述" :span="2">
            <div class="detail-text">{{ detailJob.description || '暂无描述' }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="任职要求" :span="2">
            <div class="detail-text">{{ detailJob.requirements || '暂无要求' }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- Create / Edit Job Dialog -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑岗位' : '发布岗位'" width="650px" @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="岗位名称" prop="title">
          <el-input v-model="form.title" placeholder="请输入岗位名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="公司名称" prop="company">
          <el-input v-model="form.company" placeholder="请输入公司名称" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="岗位描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="4" placeholder="请输入岗位描述" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="任职要求" prop="requirements">
          <el-input v-model="form.requirements" type="textarea" :rows="4" placeholder="请输入任职要求" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="薪资范围" prop="salaryRange">
          <el-input v-model="form.salaryRange" placeholder="如：8K-15K" maxlength="50" />
        </el-form-item>
        <el-form-item label="工作地点" prop="location">
          <el-input v-model="form.location" placeholder="请输入工作地点" maxlength="100" />
        </el-form-item>
        <el-form-item label="截止日期" prop="deadline">
          <el-date-picker v-model="form.deadline" type="date" placeholder="请选择截止日期" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          {{ editingId ? '保存修改' : '发布' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getJobs, createJob, updateJob, deleteJob } from '@/api/career'
import { useUserStore } from '@/store/user'
import type { JobInfo } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'

const userStore = useUserStore()
const isManager = computed(() => userStore.userInfo?.role === 'TEACHER' || userStore.userInfo?.role === 'ADMIN')

const loading = ref(false)
const submitting = ref(false)
const jobs = ref<JobInfo[]>([])
const dialogVisible = ref(false)
const detailVisible = ref(false)
const detailJob = ref<JobInfo | null>(null)
const editingId = ref<number | undefined>(undefined)
const formRef = ref<FormInstance>()

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = reactive({
  keyword: '',
  location: '',
})

const rules: FormRules = {
  title: [{ required: true, message: '请填写岗位名称', trigger: 'blur' }],
  company: [{ required: true, message: '请填写公司名称', trigger: 'blur' }],
  description: [{ required: true, message: '请填写岗位描述', trigger: 'blur' }],
  requirements: [{ required: true, message: '请填写任职要求', trigger: 'blur' }],
  salaryRange: [{ required: true, message: '请填写薪资范围', trigger: 'blur' }],
  location: [{ required: true, message: '请填写工作地点', trigger: 'blur' }],
  deadline: [{ required: true, message: '请选择截止日期', trigger: 'change' }],
}

const form = reactive<JobInfo>({
  title: '', company: '', description: '', requirements: '',
  salaryRange: '', location: '', deadline: '',
})

function resetForm() {
  editingId.value = undefined
  formRef.value?.resetFields()
  Object.assign(form, {
    title: '', company: '', description: '', requirements: '',
    salaryRange: '', location: '', deadline: '',
  })
}

async function loadJobs() {
  loading.value = true
  try {
    const res = await getJobs(filters.keyword || undefined, filters.location || undefined, currentPage.value, pageSize.value)
    jobs.value = res.data.data.records
    total.value = res.data.data.total
  } catch { /* handled */ } finally { loading.value = false }
}

function handleSearch() {
  currentPage.value = 1
  loadJobs()
}

function resetFilters() {
  filters.keyword = ''
  filters.location = ''
  currentPage.value = 1
  loadJobs()
}

function formatTime(time?: string) {
  if (!time) return '-'
  return time.replace('T', ' ')
}

function showDetail(row: JobInfo) {
  detailJob.value = row
  detailVisible.value = true
}

function openCreateDialog() {
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row: JobInfo) {
  editingId.value = row.id
  Object.assign(form, {
    title: row.title,
    company: row.company,
    description: row.description,
    requirements: row.requirements,
    salaryRange: row.salaryRange,
    location: row.location,
    deadline: row.deadline,
  })
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editingId.value) {
      await updateJob(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createJob({ ...form })
      ElMessage.success('发布成功')
    }
    dialogVisible.value = false
    await loadJobs()
  } catch { /* handled */ } finally { submitting.value = false }
}

async function toggleStatus(row: JobInfo) {
  const newStatus = row.status === 'OPEN' ? 'CLOSED' : 'OPEN'
  const action = newStatus === 'OPEN' ? '开启' : '关闭'
  try {
    await ElMessageBox.confirm(`确定要${action}该岗位吗？`, '确认', { type: 'warning' })
    await updateJob(row.id!, {
      title: row.title,
      company: row.company,
      description: row.description,
      requirements: row.requirements,
      salaryRange: row.salaryRange,
      location: row.location,
      deadline: row.deadline,
      status: newStatus,
    } as JobInfo)
    ElMessage.success(`${action}成功`)
    await loadJobs()
  } catch { /* cancelled or error */ }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该岗位信息吗？', '确认', { type: 'warning' })
    await deleteJob(id)
    ElMessage.success('删除成功')
    await loadJobs()
  } catch { /* cancelled or error */ }
}

watch([currentPage, pageSize], () => {
  loadJobs()
})

onMounted(() => {
  loadJobs()
})
</script>

<style scoped>
.jobs-page {
  max-width: 1400px;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.detail-text {
  white-space: pre-wrap;
  line-height: 1.6;
  min-height: 40px;
}
</style>
