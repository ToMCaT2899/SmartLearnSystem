<template>
  <div class="late-returns-page">
    <h2 class="page-title">晚归登记管理</h2>

    <!-- Student view -->
    <template v-if="isStudent || isTeacher">
      <el-card class="form-card">
        <template #header>{{ editingId ? '编辑晚归记录' : '登记晚归' }}</template>
        <el-form :model="form" label-width="100px" style="max-width: 500px">
          <el-form-item label="日期">
            <el-date-picker v-model="form.date" type="date" placeholder="请选择日期" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
          <el-form-item label="返回时间">
            <el-time-picker v-model="form.returnTime" placeholder="请选择返回时间" value-format="HH:mm:ss" style="width: 100%" />
          </el-form-item>
          <el-form-item label="原因">
            <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请输入晚归原因" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">
              {{ editingId ? '保存修改' : '提交登记' }}
            </el-button>
            <el-button v-if="editingId" @click="cancelEdit">取消编辑</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <el-card class="table-card">
        <template #header>我的晚归记录</template>
        <el-table :data="myRecords" border stripe v-loading="loading">
          <el-table-column prop="date" label="日期" width="140" />
          <el-table-column prop="returnTime" label="返回时间" width="120" />
          <el-table-column prop="reason" label="原因" min-width="200" />
          <el-table-column prop="status" label="状态" width="120">
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
        <el-table-column prop="date" label="日期" width="140" />
        <el-table-column prop="returnTime" label="返回时间" width="120" />
        <el-table-column prop="reason" label="原因" min-width="200" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'PENDING'"
              type="primary"
              size="small"
              @click="handleVerify(row)"
            >核实</el-button>
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
import { registerLateReturn, getLateReturns, getPendingLateReturns, verifyLateReturn, deleteLateReturn, updateLateReturn, cancelLateReturn } from '@/api/safety'
import type { LateReturnRecord } from '@/types'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isStudent = computed(() => role.value === 'STUDENT')
const isTeacher = computed(() => role.value === 'TEACHER')
const isAdmin = computed(() => role.value === 'ADMIN')
const isManager = computed(() => role.value === 'ADMIN' || role.value === 'TEACHER')

const loading = ref(false)
const submitting = ref(false)
const editingId = ref<number | undefined>(undefined)

const form = reactive<LateReturnRecord>({
  date: '',
  returnTime: '',
  reason: '',
})

const myRecords = ref<LateReturnRecord[]>([])
const pendingRecords = ref<LateReturnRecord[]>([])

// Pagination state for "my records" table
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// Pagination state for "pending records" table (admin)
const pendingCurrentPage = ref(1)
const pendingPageSize = ref(10)
const pendingTotal = ref(0)

function statusTagType(status: string) {
  const map: Record<string, string> = { PENDING: 'warning', VERIFIED: 'success', REJECTED: 'danger', CANCELLED: 'info' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { PENDING: '待核实', VERIFIED: '已核实', REJECTED: '已驳回', CANCELLED: '已取消' }
  return map[status] || status
}

function resetForm() {
  editingId.value = undefined
  form.date = ''
  form.returnTime = ''
  form.reason = ''
}

function cancelEdit() {
  resetForm()
}

function openEditDialog(row: LateReturnRecord) {
  editingId.value = row.id
  form.date = row.date
  form.returnTime = row.returnTime
  form.reason = row.reason
}

async function handleSubmit() {
  if (!form.date || !form.returnTime || !form.reason) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    if (editingId.value) {
      await updateLateReturn(editingId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await registerLateReturn({ ...form })
      ElMessage.success('晚归登记提交成功')
    }
    resetForm()
    currentPage.value = 1
    await loadData()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleCancel(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要取消该记录吗？取消后可在管理员处不再显示。', '确认', { type: 'warning' })
    await cancelLateReturn(id)
    ElMessage.success('已取消')
    currentPage.value = 1
    await loadData()
  } catch { /* cancelled or error */ }
}

async function handleDelete(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要删除该记录吗？', '确认', { type: 'warning' })
    await deleteLateReturn(id)
    ElMessage.success('删除成功')
    currentPage.value = 1
    await loadData()
  } catch { /* cancelled or error */ }
}

async function handleVerify(row: LateReturnRecord) {
  try {
    await verifyLateReturn(row.id!)
    ElMessage.success('已核实')
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
      const res = await getLateReturns(currentPage.value, pageSize.value)
      myRecords.value = res.data.data.records || []
      total.value = res.data.data.total || 0
    }
    if (isAdmin.value) {
      const res = await getPendingLateReturns(pendingCurrentPage.value, pendingPageSize.value)
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
.late-returns-page {
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
