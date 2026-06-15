<template>
  <div class="outing-reports-page">
    <h2 class="page-title">外出报备管理</h2>

    <!-- Student view -->
    <template v-if="isStudent || isTeacher">
      <el-card class="form-card">
        <template #header>{{ editingId ? '编辑外出报备' : '提交外出报备' }}</template>
        <el-form :model="form" label-width="120px" style="max-width: 600px">
          <el-form-item label="目的地">
            <el-input v-model="form.destination" placeholder="请输入目的地" />
          </el-form-item>
          <el-row>
            <el-col :span="12">
              <el-form-item label="出发日期">
                <el-date-picker v-model="form.departureDate" type="date" placeholder="请选择" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="返回日期">
                <el-date-picker v-model="form.returnDate" type="date" placeholder="请选择" value-format="YYYY-MM-DD" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="同行人">
            <el-input v-model="form.companion" placeholder="请输入同行人姓名" />
          </el-form-item>
          <el-form-item label="外出事由">
            <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入外出事由" />
          </el-form-item>
          <el-form-item label="紧急联系方式">
            <el-input v-model="form.emergencyContact" placeholder="请输入紧急联系电话" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              {{ editingId ? '保存修改' : '提交报备' }}
            </el-button>
            <el-button v-if="editingId" @click="cancelEdit">取消编辑</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="table-card">
        <template #header>我的外出报备记录</template>
        <el-table :data="myRecords" border stripe v-loading="loading">
          <el-table-column prop="destination" label="目的地" width="150" />
          <el-table-column prop="departureDate" label="出发日期" width="120" />
          <el-table-column prop="returnDate" label="返回日期" width="120" />
          <el-table-column prop="companion" label="同行人" width="120" />
          <el-table-column prop="reason" label="事由" min-width="180" />
          <el-table-column prop="emergencyContact" label="紧急联系方式" width="140" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button type="primary" size="small" link @click="openEditDialog(row)">编辑</el-button>
                <el-button type="warning" size="small" link @click="handleCancel(row.id)">取消</el-button>
              </template>
              <el-button v-else-if="row.status === 'CANCELLED'" type="danger" size="small" link @click="handleDelete(row.id)">删除</el-button>
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
      </el-card>
    </template>

    <!-- Admin view -->
    <template v-if="isAdmin">
      <el-table :data="pendingRecords" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="80" />
        <el-table-column prop="studentName" label="姓名" width="100" />
          <el-table-column prop="studentNo" label="学号" width="120" />
        <el-table-column prop="destination" label="目的地" width="150" />
        <el-table-column prop="departureDate" label="出发日期" width="120" />
        <el-table-column prop="returnDate" label="返回日期" width="120" />
        <el-table-column prop="companion" label="同行人" width="100" />
        <el-table-column prop="reason" label="事由" min-width="180" />
        <el-table-column prop="emergencyContact" label="紧急联系方式" width="140" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 'PENDING'">
              <el-button type="success" size="small" @click="handleReview(row, 'APPROVED')">通过</el-button>
              <el-button type="danger" size="small" @click="handleReview(row, 'REJECTED')">驳回</el-button>
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="pendingCurrentPage"
        v-model:page-size="pendingPageSize"
        :page-sizes="[10, 20, 50]"
        :total="pendingTotal"
        layout="total, sizes, prev, pager, next, jumper"
        background
        style="margin-top: 16px; justify-content: flex-end"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { submitOutingReport, getOutingReports, getPendingOutingReports, reviewOutingReport, deleteOutingReport, updateOutingReport, cancelOutingReport } from '@/api/safety'
import type { OutingReport } from '@/types'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isStudent = computed(() => role.value === 'STUDENT')
const isTeacher = computed(() => role.value === 'TEACHER')
const isAdmin = computed(() => role.value === 'ADMIN')
const isManager = computed(() => role.value === 'ADMIN' || role.value === 'TEACHER')

const loading = ref(false)
const submitting = ref(false)
const editingId = ref<number | undefined>(undefined)

const form = reactive<OutingReport>({
  destination: '',
  departureDate: '',
  returnDate: '',
  companion: '',
  reason: '',
  emergencyContact: '',
})

const myRecords = ref<OutingReport[]>([])
const pendingRecords = ref<OutingReport[]>([])

// Pagination state for "my records" table
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// Pagination state for "pending records" table (admin)
const pendingCurrentPage = ref(1)
const pendingPageSize = ref(10)
const pendingTotal = ref(0)

function statusTagType(status: string) {
  const map: Record<string, string> = { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', CANCELLED: '已取消' }
  return map[status] || status
}

function resetForm() {
  editingId.value = undefined
  Object.assign(form, {
    destination: '', departureDate: '', returnDate: '',
    companion: '', reason: '', emergencyContact: '',
  })
}

function cancelEdit() {
  resetForm()
}

function openEditDialog(row: OutingReport) {
  editingId.value = row.id
  Object.assign(form, {
    destination: row.destination,
    departureDate: row.departureDate,
    returnDate: row.returnDate,
    companion: row.companion,
    reason: row.reason,
    emergencyContact: row.emergencyContact,
  })
}

async function handleSubmit() {
  if (!form.destination || !form.departureDate || !form.returnDate || !form.reason || !form.emergencyContact) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateOutingReport(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await submitOutingReport({ ...form })
      ElMessage.success('外出报备提交成功')
    }
    resetForm()
    currentPage.value = 1
    await loadData()
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

async function handleCancel(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要取消该报告吗？取消后管理员处不再显示。', '确认', { type: 'warning' })
    await cancelOutingReport(id)
    ElMessage.success('已取消')
    currentPage.value = 1
    await loadData()
  } catch { /* cancelled or error */ }
}

async function handleDelete(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要删除该报告吗？', '确认', { type: 'warning' })
    await deleteOutingReport(id)
    ElMessage.success('删除成功')
    currentPage.value = 1
    await loadData()
  } catch { /* cancelled or error */ }
}

async function handleReview(row: OutingReport, status: string) {
  try {
    await reviewOutingReport(row.id!, status)
    ElMessage.success(status === 'APPROVED' ? '已通过' : '已驳回')
    pendingCurrentPage.value = 1
    await loadData()
  } catch {
    // handled
  }
}

async function loadData() {
  loading.value = true
  try {
    if (isStudent.value || isTeacher.value) {
      const res = await getOutingReports(currentPage.value, pageSize.value)
      myRecords.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
    if (isAdmin.value) {
      const res = await getPendingOutingReports(pendingCurrentPage.value, pendingPageSize.value)
      pendingRecords.value = res.data.data.records || []
      pendingTotal.value = res.data.data.total || 0
    }
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

watch([currentPage, pageSize], () => { loadData() })
watch([pendingCurrentPage, pendingPageSize], () => { loadData() })

onMounted(() => { loadData() })
</script>

<style scoped>
.outing-reports-page {
  max-width: 1400px;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}

.form-card {
  margin-bottom: 20px;
}

.table-card {
  margin-top: 20px;
}
</style>
