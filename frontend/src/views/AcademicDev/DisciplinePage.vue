<template>
  <div class="discipline-page">
    <h2 class="page-title">违纪记录管理</h2>

    <!-- Query bar for TEACHER / ADMIN -->
    <div class="toolbar" v-if="!isStudent">
      <el-input v-model="queryStudentNo" placeholder="请输入学号" style="width: 200px" clearable />
      <el-button type="primary" @click="currentPage = 1; loadRecords()" :disabled="!queryStudentNo" style="margin-left: 12px">
        查询
      </el-button>
      <el-button type="warning" @click="openAddDialog" :disabled="!queryStudentNo" style="margin-left: 12px">
        添加违纪记录
      </el-button>
    </div>

    <el-table :data="records" border stripe v-loading="loading" empty-text="暂无违纪记录">
      <el-table-column prop="violationType" label="违纪类型" width="140" align="center" />
      <el-table-column prop="description" label="违纪描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="punishment" label="处分" width="140" align="center">
        <template #default="{ row }">
          <el-tag :type="punishmentTagType(row.punishment)">{{ row.punishment }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="recordDate" label="记录日期" width="130" align="center" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="row.status === 'ACTIVE' ? 'danger' : 'info'" size="small">
            {{ row.status === 'ACTIVE' ? '生效中' : (row.status === 'RESCINDED' ? '已撤销' : row.status || '--') }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="!isStudent" label="操作" width="160" align="center" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'ACTIVE'"
            type="warning" size="small"
            @click="handleToggleStatus(row)"
          >撤销</el-button>
          <el-button
            v-else-if="row.status === 'RESCINDED'"
            type="success" size="small"
            @click="handleToggleStatus(row)"
          >恢复</el-button>
          <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- Add Discipline Record Dialog -->
    <el-dialog v-model="dialogVisible" title="添加违纪记录" width="540px" @closed="resetForm">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item v-if="!isStudent" label="学号">
          <el-input v-model="form.studentNo" disabled />
        </el-form-item>
        <el-form-item label="违纪类型" prop="violationType">
          <el-select v-model="form.violationType" placeholder="请选择违纪类型" style="width: 100%">
            <el-option label="迟到早退" value="迟到早退" />
            <el-option label="旷课" value="旷课" />
            <el-option label="考试作弊" value="考试作弊" />
            <el-option label="宿舍违纪" value="宿舍违纪" />
            <el-option label="打架斗殴" value="打架斗殴" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="违纪描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入违纪详细描述" />
        </el-form-item>
        <el-form-item label="处分" prop="punishment">
          <el-select v-model="form.punishment" placeholder="请选择处分" style="width: 100%">
            <el-option label="通报批评" value="通报批评" />
            <el-option label="警告" value="警告" />
            <el-option label="严重警告" value="严重警告" />
            <el-option label="记过" value="记过" />
            <el-option label="留校察看" value="留校察看" />
            <el-option label="开除学籍" value="开除学籍" />
          </el-select>
        </el-form-item>
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker v-model="form.recordDate" type="date" placeholder="请选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
            <el-option label="生效中" value="ACTIVE" />
            <el-option label="已撤销" value="RESCINDED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getDisciplineRecords, addDisciplineRecord, updateDisciplineRecord, deleteDisciplineRecord } from '@/api/academic'

interface DisciplineRecord {
  id?: number
  studentId: number
  violationType: string
  description: string
  punishment: string
  recordDate: string
  status?: string
}

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isStudent = computed(() => userInfo.value?.role === 'STUDENT')

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const queryStudentNo = ref('')
const records = ref<DisciplineRecord[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const resolvedStudentId = ref(0)
const formRef = ref<FormInstance>()

const form = reactive<DisciplineRecord & { studentNo: string }>({
  studentId: 0,
  studentNo: '',
  violationType: '',
  description: '',
  punishment: '',
  recordDate: '',
  status: 'ACTIVE',
})

const formRules: FormRules = {
  violationType: [{ required: true, message: '请选择违纪类型', trigger: 'change' }],
  description: [{ required: true, message: '请输入违纪描述', trigger: 'blur' }],
  punishment: [{ required: true, message: '请选择处分', trigger: 'change' }],
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
}

function punishmentTagType(punishment: string): string {
  const map: Record<string, string> = {
    '通报批评': 'info',
    '警告': 'warning',
    '严重警告': 'warning',
    '记过': 'danger',
    '留校察看': 'danger',
    '开除学籍': 'danger',
  }
  return map[punishment] || 'info'
}

async function loadRecords() {
  const sno = isStudent.value ? undefined : (queryStudentNo.value || undefined)
  if (!isStudent.value && !sno) {
    ElMessage.warning('请输入学号')
    return
  }
  loading.value = true
  try {
    const res = await getDisciplineRecords(sno, currentPage.value, pageSize.value)
    records.value = res.data.data.records || []
    total.value = res.data.data.total || 0
    if (!isStudent.value && records.value.length > 0) {
      resolvedStudentId.value = records.value[0].studentId
    }
  } catch {
    // interceptor handles
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  if (!isStudent.value && !queryStudentNo.value) {
    ElMessage.warning('请先输入学号')
    return
  }
  resetForm()
  form.studentId = isStudent.value ? (userInfo.value?.userId ?? 0) : resolvedStudentId.value
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  form.studentId = isStudent.value ? (userInfo.value?.userId ?? 0) : resolvedStudentId.value
  form.studentNo = isStudent.value ? '' : queryStudentNo.value
  form.violationType = ''
  form.description = ''
  form.punishment = ''
  form.recordDate = ''
  form.status = 'ACTIVE'
}

async function handleAdd() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const { studentNo, ...payload } = form
    await addDisciplineRecord(payload, isStudent.value ? undefined : queryStudentNo.value)
    ElMessage.success('添加违纪记录成功')
    dialogVisible.value = false
    await loadRecords()
  } catch {
    // interceptor handles
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: DisciplineRecord) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.violationType}」违纪记录吗？`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteDisciplineRecord(row.id!)
    ElMessage.success('删除成功')
    await loadRecords()
  } catch {
    // user cancelled or interceptor handles
  }
}

async function handleToggleStatus(row: DisciplineRecord) {
  const newStatus = row.status === 'ACTIVE' ? 'RESCINDED' : 'ACTIVE'
  const actionText = newStatus === 'RESCINDED' ? '撤销' : '恢复'
  try {
    await ElMessageBox.confirm(
      `确定要${actionText}该违纪记录吗？`,
      `确认${actionText}`,
      { type: 'warning', confirmButtonText: actionText, cancelButtonText: '取消' },
    )
    await updateDisciplineRecord(row.id!, { ...row, status: newStatus })
    ElMessage.success(`${actionText}成功`)
    await loadRecords()
  } catch {
    // user cancelled or interceptor handles
  }
}

watch([currentPage, pageSize], () => { loadRecords() })

onMounted(() => {
  if (isStudent.value) {
    loadRecords()
  }
})
</script>

<style scoped>
.discipline-page {
  max-width: 1400px;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}
</style>
