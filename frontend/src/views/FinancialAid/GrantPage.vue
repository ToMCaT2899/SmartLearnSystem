<template>
  <div class="grant-page">
    <h2 class="page-title">助学金管理</h2>

    <!-- Stats cards -->
    <div class="stats-row" v-if="grants.length > 0">
      <div class="stat-card">
        <div class="stat-icon total"><el-icon :size="22"><Collection /></el-icon></div>
        <div>
          <div class="stat-value">{{ grants.length }}</div>
          <div class="stat-label">助学金总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon amount"><el-icon :size="22"><Money /></el-icon></div>
        <div>
          <div class="stat-value">¥{{ totalAmount.toLocaleString() }}</div>
          <div class="stat-label">平均金额</div>
        </div>
      </div>
    </div>

    <!-- STUDENT view -->
    <template v-if="userStore.userInfo?.role === 'STUDENT'">
      <el-table :data="activeGrants" v-loading="loading" border stripe>
        <el-table-column prop="name" label="助学金名称" min-width="180" />
        <el-table-column prop="amount" label="金额(元)" width="130">
          <template #default="{ row }">¥{{ row.amount?.toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="criteria" label="申请条件" min-width="220" show-overflow-tooltip />
        <el-table-column prop="deadline" label="截止日期" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'" size="small">{{ row.status === 'OPEN' ? '进行中' : '已关闭' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <template v-if="getAppStatus(row.id!) === 'PENDING'">
              <div style="display:flex;align-items:center;gap:4px">
                <el-tag type="warning" size="small">待审核</el-tag>
                <el-button type="primary" size="small" link @click="openAppEditDialog(row)">编辑</el-button>
                <el-button type="danger" size="small" link @click="cancelApplication(row)">取消</el-button>
              </div>
            </template>
            <template v-else-if="getAppStatus(row.id!) === 'APPROVED'">
              <el-tag type="success" size="small">已通过</el-tag>
            </template>
            <template v-else-if="getAppStatus(row.id!) === 'REJECTED'">
              <el-tag type="danger" size="small">已拒绝</el-tag>
            </template>
            <template v-else-if="getAppStatus(row.id!) === 'CANCELLED'">
              <el-tag type="info" size="small">已取消</el-tag>
            </template>
            <template v-else>
              <template v-if="isDeadlinePassed(row.deadline)">
                <el-tag type="info" size="small">已截止</el-tag>
              </template>
              <template v-else>
                <el-button type="primary" size="small" @click="openApplyDialog(row)">申请</el-button>
              </template>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <!-- Apply Dialog -->
      <el-dialog v-model="applyDialogVisible" :title="editingAppId ? '编辑申请' : '申请助学金'" width="550px">
        <el-form :model="applyForm" label-width="100px">
          <el-form-item label="助学金">
            <el-input :model-value="currentGrant?.name" disabled />
          </el-form-item>
          <el-form-item label="申请理由">
            <el-input v-model="applyForm.applyReason" type="textarea" :rows="4" placeholder="请填写申请理由" />
          </el-form-item>
          <el-form-item label="支撑材料">
            <div v-if="existingFiles.length > 0" style="margin-bottom:8px">
              <div style="font-size:12px;color:#909399;margin-bottom:4px">已有附件：</div>
              <div v-for="f in existingFiles" :key="f.url">
                <el-link type="primary" @click.prevent="openFile(f.url)" :underline="false" size="small">{{ f.name }}</el-link>
              </div>
            </div>
            <el-upload
              v-model:file-list="fileList"
              :auto-upload="false"
              :limit="5"
              accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
              list-type="text"
            >
              <el-button type="primary" plain size="small">{{ editingAppId ? '追加文件' : '选择文件' }}</el-button>
              <template #tip>
                <div class="el-upload__tip">支持 PDF、JPG、PNG、DOC 格式，单文件不超过 10MB，最多 5 个</div>
              </template>
            </el-upload>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="applyDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="submitting" @click="submitApply">{{ editingAppId ? '保存修改' : '提交申请' }}</el-button>
        </template>
      </el-dialog>
    </template>

    <!-- TEACHER / ADMIN view -->
    <template v-else>
      <el-tabs v-model="activeTab" type="border-card">
        <!-- Tab: 管理 -->
        <el-tab-pane label="管理" name="manage">
          <div style="margin-bottom: 12px; display: flex; gap: 8px; align-items: center;">
            <el-button type="primary" @click="openCreateDialog">新增助学金</el-button>
            <el-input v-model="searchKeyword" placeholder="搜索名称..." clearable style="width: 220px" />
            <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 140px">
              <el-option label="全部" value="" />
              <el-option label="进行中" value="OPEN" />
              <el-option label="已关闭" value="CLOSED" />
            </el-select>
          </div>
          <el-table :data="filteredGrants" v-loading="loading" border stripe>
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="name" label="名称" min-width="160" />
            <el-table-column prop="amount" label="金额" width="130">
              <template #default="{ row }">¥{{ row.amount?.toLocaleString() }}</template>
            </el-table-column>
            <el-table-column prop="quota" label="名额" width="80">
              <template #default="{ row }">{{ row.quota ?? '-' }}</template>
            </el-table-column>
            <el-table-column prop="criteria" label="申请条件" min-width="200" show-overflow-tooltip />
            <el-table-column prop="deadline" label="截止日期" width="130" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'OPEN' ? 'success' : 'info'" size="small">{{ row.status === 'OPEN' ? '进行中' : '已关闭' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="申请人数" width="100">
              <template #default="{ row }">
                <el-link type="primary" @click="openOverview(row)" :underline="false">
                  {{ row.applicationCount || 0 }} 人
                </el-link>
              </template>
            </el-table-column>
            <el-table-column label="已录取" width="80">
              <template #default="{ row }">
                <span style="color:#67c23a;font-weight:600">{{ row.admittedCount ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="发布者" width="140">
              <template #default="{ row }">
                <span>{{ row.publisherName || '-' }}</span>
                <el-tag size="small" :type="row.publisherRole === 'ADMIN' ? 'danger' : 'warning'" style="margin-left: 6px">
                  {{ row.publisherRole === 'ADMIN' ? '管理员' : row.publisherRole === 'TEACHER' ? '教师' : '-' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <template v-if="canModify(row)">
                  <el-button type="primary" size="small" @click="openEditDialog(row)">编辑</el-button>
                  <el-button type="danger" size="small" @click="handleDelete(row.id!)">删除</el-button>
                </template>
                <span v-else style="color:#999;font-size:12px">-</span>
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

          <!-- CRUD Dialog -->
          <el-dialog v-model="formDialogVisible" :title="isEdit ? '编辑助学金' : '新增助学金'" width="550px">
            <el-form :model="form" label-width="100px">
              <el-form-item label="名称">
                <el-input v-model="form.name" placeholder="请输入助学金名称" />
              </el-form-item>
              <el-form-item label="金额">
                <el-input-number v-model="form.amount" :min="0" :step="500" style="width:100%" />
              </el-form-item>
              <el-form-item label="名额">
                <el-input-number v-model="form.quota" :min="0" style="width:100%" placeholder="留空则不限制" />
              </el-form-item>
              <el-form-item label="申请条件">
                <el-input v-model="form.criteria" type="textarea" :rows="3" placeholder="请输入申请条件" />
              </el-form-item>
              <el-form-item label="截止日期">
                <el-date-picker v-model="form.deadline" type="date" placeholder="请选择截止日期" value-format="YYYY-MM-DD" style="width:100%" />
              </el-form-item>
              <el-form-item label="状态">
                <el-select v-model="form.status" placeholder="请选择状态" style="width:100%">
                  <el-option label="进行中" value="OPEN" />
                  <el-option label="已关闭" value="CLOSED" />
                </el-select>
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="formDialogVisible = false">取消</el-button>
              <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
            </template>
          </el-dialog>
        </el-tab-pane>

        <!-- Tab: 审核 -->
        <el-tab-pane label="审核" name="review">
          <el-table :data="reviewList" v-loading="reviewLoading" border stripe>
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column label="助学金" min-width="140">
              <template #default="{ row }">{{ row.grantName || row.grantId }}</template>
            </el-table-column>
            <el-table-column label="申请人" width="120">
              <template #default="{ row }">{{ row.studentName || row.studentId }}</template>
            </el-table-column>
            <el-table-column prop="applyReason" label="申请理由" min-width="180" show-overflow-tooltip />
            <el-table-column label="支撑材料" min-width="160">
              <template #default="{ row }">
                <template v-if="parseFileUrls(row.supportingFiles).length">
                  <div v-for="f in parseFileUrls(row.supportingFiles)" :key="f.url">
                    <el-link type="primary" @click.prevent="openFile(f.url)" :underline="false" size="small">{{ f.name }}</el-link>
                  </div>
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="reviewStatusTagType(row.status)">{{ reviewStatusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button type="success" size="small" @click="openReviewDialog(row, 'APPROVED')">通过</el-button>
                  <el-button type="danger" size="small" @click="openReviewDialog(row, 'REJECTED')">拒绝</el-button>
                </template>
                <span v-else>-</span>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="reviewCurrentPage"
            v-model:page-size="reviewPageSize"
            :page-sizes="[10, 20, 50]"
            :total="reviewTotal"
            layout="total, sizes, prev, pager, next, jumper"
            background
            style="margin-top: 16px; justify-content: flex-end"
          />

          <!-- Overview Dialog -->
          <el-dialog v-model="overviewVisible" :title="'申请概览 - ' + overviewItem?.name" width="900px">
            <div style="display:flex; gap:24px; margin-bottom:12px; font-size:14px;">
              <span>总申请：<b>{{ overviewList.length }}</b></span>
              <span style="color:#e6a23c">待审核：<b>{{ overviewList.filter(o => o.status === 'PENDING').length }}</b></span>
              <span style="color:#67c23a">已录取：<b>{{ overviewList.filter(o => o.status === 'APPROVED').length }}</b></span>
              <span style="color:#f56c6c">已拒绝：<b>{{ overviewList.filter(o => o.status === 'REJECTED').length }}</b></span>
            </div>
            <el-tabs v-model="overviewFilter">
              <el-tab-pane label="全部" name="ALL" />
              <el-tab-pane label="待审核" name="PENDING" />
              <el-tab-pane label="已录取" name="APPROVED" />
              <el-tab-pane label="已拒绝" name="REJECTED" />
            </el-tabs>
            <el-table :data="filteredOverviewList" v-loading="overviewLoading" border stripe max-height="400"
              :row-class-name="overviewRowClass">
              <el-table-column prop="studentName" label="申请人" width="100" />
              <el-table-column prop="applyReason" label="申请理由" min-width="180" show-overflow-tooltip />
              <el-table-column label="支撑材料" min-width="160">
                <template #default="{ row }">
                  <template v-if="parseFileUrls(row.supportingFiles).length">
                    <div v-for="f in parseFileUrls(row.supportingFiles)" :key="f.url">
                      <el-link type="primary" @click.prevent="openFile(f.url)" :underline="false" size="small">{{ f.name }}</el-link>
                    </div>
                  </template>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="90">
                <template #default="{ row }">
                  <el-tag :type="reviewStatusTagType(row.status)" size="small">{{ reviewStatusText(row.status) }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="applyTime" label="申请时间" width="160" />
              <el-table-column prop="reviewComment" label="审核意见" min-width="120" show-overflow-tooltip />
            </el-table>
            <el-pagination
              v-model:current-page="appCurrentPage"
              v-model:page-size="appPageSize"
              :page-sizes="[10, 20, 50]"
              :total="appTotal"
              layout="total, sizes, prev, pager, next, jumper"
              background
              style="margin-top: 16px; justify-content: flex-end"
            />
            <template #footer>
              <el-button @click="overviewVisible = false">关闭</el-button>
            </template>
          </el-dialog>

          <!-- Review Dialog -->
          <el-dialog v-model="reviewDialogVisible" :title="reviewAction === 'APPROVED' ? '审核通过' : '审核拒绝'" width="450px">
            <el-form label-width="80px">
              <el-form-item label="审核意见">
                <el-input v-model="reviewComment" type="textarea" :rows="3" placeholder="请输入审核意见" />
              </el-form-item>
            </el-form>
            <template #footer>
              <el-button @click="reviewDialogVisible = false">取消</el-button>
              <el-button :type="reviewAction === 'APPROVED' ? 'success' : 'danger'" :loading="submitting" @click="confirmReview">
                {{ reviewAction === 'APPROVED' ? '确认通过' : '确认拒绝' }}
              </el-button>
            </template>
          </el-dialog>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { UploadUserFile } from 'element-plus'
import { Collection, Money } from '@element-plus/icons-vue'
import { grantApi, getMyApplications, getReviewQueue, getGrantApplications } from '@/api/financial-aid'
import type { GrantApplication } from '@/types'
import { uploadFile, getDownloadUrl } from '@/api/file'
import { useUserStore } from '@/store/user'
import type { GrantInfo } from '@/types'

const userStore = useUserStore()

const totalAmount = computed(() => {
  if (grants.value.length === 0) return 0
  const sum = grants.value.reduce((s, g) => s + (g.amount || 0), 0)
  return Math.round(sum / grants.value.length)
})

// Common state
const loading = ref(false)
const submitting = ref(false)
const grants = ref<GrantInfo[]>([])
const activeGrants = computed(() => grants.value.filter(g => g.status === "OPEN"))
const activeTab = ref('manage')
const searchKeyword = ref('')
const statusFilter = ref('OPEN')

const filteredGrants = computed(() => {
  let list = grants.value
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(g => g.name?.toLowerCase().includes(kw))
  }
  if (statusFilter.value) {
    list = list.filter(g => g.status === statusFilter.value)
  }
  return list
})

// Pagination - grant list
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// Pagination - applications overview
const appCurrentPage = ref(1)
const appPageSize = ref(10)
const appTotal = ref(0)

// Pagination - review queue
const reviewCurrentPage = ref(1)
const reviewPageSize = ref(10)
const reviewTotal = ref(0)

// Apply (STUDENT)
const applyDialogVisible = ref(false)
const currentGrant = ref<GrantInfo | null>(null)
const fileList = ref<UploadUserFile[]>([])
const editingAppId = ref<number | null>(null)
const existingFiles = ref<{ name: string; url: string }[]>([])
const applyForm = reactive({
  applyReason: '',
})

// CRUD (TEACHER/ADMIN)
const formDialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<GrantInfo>({
  name: '',
  amount: 0,
  quota: undefined,
  criteria: '',
  deadline: '',
    status: 'OPEN',
})

// Review
const reviewLoading = ref(false)
const reviewList = ref<any[]>([])
const reviewDialogVisible = ref(false)
const reviewAction = ref('')
const reviewComment = ref('')
const currentReviewItem = ref<any>(null)

// Overview
const overviewVisible = ref(false)
const overviewLoading = ref(false)
const overviewList = ref<any[]>([])
const overviewItem = ref<GrantInfo | null>(null)
const overviewFilter = ref('ALL')

const filteredOverviewList = computed(() => {
  if (overviewFilter.value === 'ALL') return overviewList.value
  return overviewList.value.filter(o => o.status === overviewFilter.value)
})

function overviewRowClass({ row }: { row: any }) {
  if (row.status === 'APPROVED') return 'admitted-row'
  return ''
}

async function openOverview(row: GrantInfo) {
  overviewItem.value = row
  overviewVisible.value = true
  overviewFilter.value = 'ALL'
  appCurrentPage.value = 1
  await loadOverview()
}

async function loadOverview() {
  if (!overviewItem.value?.id) return
  overviewLoading.value = true
  try {
    const res = await getGrantApplications(overviewItem.value.id!, appCurrentPage.value, appPageSize.value)
    overviewList.value = res.data.data?.records || []
    appTotal.value = res.data.data?.total || 0
  } catch { /* handled */ } finally { overviewLoading.value = false }
}

// My applications (for checking status)
const myApps = ref<GrantApplication[]>([])

function getAppStatus(grantId: number): string | null {
  const app = myApps.value.find(a => a.grantId === grantId)
  return app?.status ?? null
}

function getMyApp(grantId: number) {
  return myApps.value.find(a => a.grantId === grantId) ?? null
}

function isDeadlinePassed(deadline?: string): boolean {
  if (!deadline) return false
  return new Date(deadline) < new Date()
}

// Helpers
function reviewStatusTagType(status: string) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[status] || 'info'
}
function reviewStatusText(status: string) {
  return { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }[status] || status
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
function resetForm() {
  Object.assign(form, { name: '', amount: 0, quota: undefined, criteria: '', deadline: '', status: 'OPEN' })
  isEdit.value = false
  editingId.value = null
}

function canModify(row: GrantInfo): boolean {
  const role = userStore.userInfo?.role
  if (role === 'ADMIN') return true
  if (role === 'TEACHER') {
    if (row.publisherRole === 'ADMIN') return false
    return row.publisherId === userStore.userInfo?.userId
  }
  return false
}

// Load grants
async function loadGrants() {
  loading.value = true
  try {
    const res = await grantApi.list(currentPage.value, pageSize.value)
    grants.value = res.data.data?.records || []
    total.value = res.data.data?.total || 0
  } catch { /* handled */ } finally { loading.value = false }
}

// Load review queue
async function loadReviewQueue() {
  reviewLoading.value = true
  try {
    const res = await getReviewQueue(reviewCurrentPage.value, reviewPageSize.value)
    const pageResult = res.data.data?.grantApplications
    reviewList.value = pageResult?.records || []
    reviewTotal.value = pageResult?.total || 0
  } catch { /* handled */ } finally { reviewLoading.value = false }
}

// === STUDENT: Apply ===
function openApplyDialog(grant: GrantInfo) {
  editingAppId.value = null
  existingFiles.value = []
  currentGrant.value = grant
  applyForm.applyReason = ''
  fileList.value = []
  applyDialogVisible.value = true
}

function openAppEditDialog(grant: GrantInfo) {
  const app = getMyApp(grant.id!)
  if (!app) return
  editingAppId.value = app.id!
  currentGrant.value = grant
  applyForm.applyReason = app.applyReason || ''
  existingFiles.value = parseFileUrls(app.supportingFiles)
  fileList.value = []
  applyDialogVisible.value = true
}

async function cancelApplication(grant: GrantInfo) {
  const app = getMyApp(grant.id!)
  if (!app) return
  try {
    await ElMessageBox.confirm('确定要取消该申请吗？取消后可以重新申请。', '确认取消', { type: 'warning' })
    await grantApi.cancel(app.id!)
    ElMessage.success('申请已取消')
    await loadMyApps()
  } catch { /* cancelled or error */ }
}

async function loadMyApps() {
  try {
    const res = await getMyApplications()
    myApps.value = res.data.data?.grantApplications?.records || []
  } catch { /* handled */ }
}

async function submitApply() {
  if (!applyForm.applyReason.trim()) {
    ElMessage.warning('请填写申请理由')
    return
  }
  submitting.value = true
  try {
    const fileUrls: string[] = []
    for (const f of fileList.value) {
      if (f.raw) {
        const res = await uploadFile(f.raw, 'grant')
        fileUrls.push(res.data.data)
      }
    }
    if (editingAppId.value) {
      await grantApi.edit(editingAppId.value, applyForm.applyReason, fileUrls)
      ElMessage.success('申请已更新')
    } else {
      await grantApi.apply(currentGrant.value!.id!, applyForm.applyReason, fileUrls)
      ElMessage.success('申请已提交')
    }
    applyDialogVisible.value = false
    await loadMyApps()
  } catch { /* handled */ } finally { submitting.value = false }
}

// === TEACHER/ADMIN: CRUD ===
function openCreateDialog() {
  resetForm()
  formDialogVisible.value = true
}

function openEditDialog(row: GrantInfo) {
  isEdit.value = true
  editingId.value = row.id!
  Object.assign(form, {
    name: row.name,
    amount: row.amount,
    quota: row.quota,
    criteria: row.criteria || '',
    deadline: row.deadline,
        status: row.status || 'OPEN',
  })
  formDialogVisible.value = true
}

async function submitForm() {
  if (!form.name.trim()) {
    ElMessage.warning('请填写助学金名称')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await grantApi.update({ id: editingId.value!, ...form })
      ElMessage.success('更新成功')
    } else {
      await grantApi.create({ ...form })
      ElMessage.success('创建成功')
    }
    formDialogVisible.value = false
    await loadGrants()
  } catch { /* handled */ } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该助学金吗？', '确认', { type: 'warning' })
    await grantApi.delete(id)
    ElMessage.success('删除成功')
    await loadGrants()
  } catch { /* cancelled or error */ }
}

// === TEACHER/ADMIN: Review ===
function openReviewDialog(row: any, action: string) {
  currentReviewItem.value = row
  reviewAction.value = action
  reviewComment.value = ''
  reviewDialogVisible.value = true
}

async function confirmReview() {
  submitting.value = true
  try {
    await grantApi.review(currentReviewItem.value.id, reviewAction.value, reviewComment.value)
    ElMessage.success(reviewAction.value === 'APPROVED' ? '已通过' : '已拒绝')
    reviewDialogVisible.value = false
    await loadReviewQueue()
  } catch { /* handled */ } finally { submitting.value = false }
}

// Watch tab changes to load review data
watch(activeTab, (val) => {
  if (val === 'review' && userStore.userInfo?.role !== 'STUDENT') {
    reviewCurrentPage.value = 1
    loadReviewQueue()
  }
})

// Pagination watches
watch([currentPage, pageSize], () => { loadGrants() })
watch([appCurrentPage, appPageSize], () => { loadOverview() })
watch([reviewCurrentPage, reviewPageSize], () => { loadReviewQueue() })

// Reset page on search/filter
watch([searchKeyword, statusFilter], () => { currentPage.value = 1 })

onMounted(() => {
  loadGrants()
  loadMyApps()
})
</script>

<style scoped>
.grant-page {
  max-width: 1400px;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}

/* Stats cards */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: box-shadow 0.3s, transform 0.2s;
  border: 1px solid #f0f0f0;
}

.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #fff;
}

.stat-icon.total { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.active { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-icon.amount { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.pending { background: linear-gradient(135deg, #4facfe, #00f2fe); }

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1d2129;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #86909c;
  margin-top: 2px;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

:deep(.admitted-row) {
  background-color: #f0f9eb !important;
}
</style>
