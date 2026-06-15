<template>
  <div class="midterm-page">
    <h2 class="page-title">期中评估管理</h2>

    <!-- Toolbar: semester filter + query for TEACHER -->
    <div class="toolbar">
      <template v-if="!isStudent">
        <el-input v-model="queryStudentNo" placeholder="请输入学号" style="width: 200px" clearable />
        <el-button type="primary" @click="currentPage = 1; loadData()" :disabled="!queryStudentNo" style="margin-left: 12px">
          查询
        </el-button>
        <el-button type="success" @click="openCreateDialog" :disabled="!queryStudentNo" style="margin-left: 12px">
          添加评估
        </el-button>
      </template>
      <el-select v-model="semesterFilter" placeholder="选择学期" clearable @change="currentPage = 1; loadData()" style="width: 200px; margin-left: 12px">
        <el-option v-for="s in semesterList" :key="s" :label="s" :value="s" />
      </el-select>
    </div>

    <el-table :data="assessments" border stripe v-loading="loading" empty-text="暂无评估数据">
      <el-table-column prop="semester" label="学期" width="140" align="center" />
      <el-table-column label="自我总结" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.selfSummary || '未填写' }}
        </template>
      </el-table-column>
      <el-table-column label="班主任评语" min-width="220" show-overflow-tooltip>
        <template #default="{ row }">
          {{ row.advisorComment || '未评语' }}
        </template>
      </el-table-column>
      <el-table-column prop="score" label="评分" width="90" align="center">
        <template #default="{ row }">
          <span v-if="row.score != null">{{ row.score }}</span>
          <span v-else style="color: #c0c4cc">--</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="isStudent" type="primary" size="small" @click="openSelfSummaryDialog(row)">
            填写总结
          </el-button>
          <template v-else>
            <el-button type="primary" size="small" @click="openAdvisorDialog(row)">
              评语/评分
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
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

    <!-- Student: self-summary dialog -->
    <el-dialog v-model="selfDialogVisible" title="填写自我总结" width="520px" @closed="resetSelfForm">
      <el-form :model="selfForm" :rules="selfRules" ref="selfFormRef" label-width="100px">
        <el-form-item label="学期">
          <el-input :model-value="selfForm.semester" disabled />
        </el-form-item>
        <el-form-item label="自我总结" prop="selfSummary">
          <el-input v-model="selfForm.selfSummary" type="textarea" :rows="6" placeholder="请填写本学期的自我总结，包括学习成果、收获与不足等" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="selfDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveSelfSummary">提交</el-button>
      </template>
    </el-dialog>

    <!-- Teacher: advisor comment + score dialog -->
    <el-dialog v-model="advisorDialogVisible" title="班主任评语与评分" width="520px" @closed="resetAdvisorForm">
      <el-form :model="advisorForm" :rules="advisorRules" ref="advisorFormRef" label-width="100px">
        <el-form-item label="学期">
          <el-input :model-value="advisorForm.semester" disabled />
        </el-form-item>
        <el-form-item label="评语" prop="advisorComment">
          <el-input v-model="advisorForm.advisorComment" type="textarea" :rows="4" placeholder="请输入班主任评语" />
        </el-form-item>
        <el-form-item label="评分" prop="score">
          <el-input-number v-model="advisorForm.score" :min="0" :max="100" placeholder="0-100" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="advisorDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSaveAdvisor">提交</el-button>
      </template>
    </el-dialog>

    <!-- Teacher: create assessment dialog -->
    <el-dialog v-model="createDialogVisible" title="添加评估记录" width="480px" @closed="resetCreateForm">
      <el-form :model="createForm" :rules="createRules" ref="createFormRef" label-width="100px">
        <el-form-item label="学号">
          <el-input v-model="queryStudentNo" disabled />
        </el-form-item>
        <el-form-item label="学期" prop="semester">
          <el-select v-model="createForm.semester" placeholder="请选择学期" style="width: 100%">
            <el-option v-for="s in semesterList" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { useUserStore } from '@/store/user'
import { getMidtermAssessments, saveMidtermAssessment, deleteMidtermAssessment } from '@/api/academic'

interface MidtermAssessment {
  id?: number
  studentId: number
  semester: string
  selfSummary: string
  advisorComment: string
  score: number | null
  status?: string
}

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isStudent = computed(() => userInfo.value?.role === 'STUDENT')

const loading = ref(false)
const submitting = ref(false)
const selfDialogVisible = ref(false)
const advisorDialogVisible = ref(false)
const createDialogVisible = ref(false)
const queryStudentNo = ref('')
const semesterFilter = ref('')
const resolvedStudentId = ref(0)
const assessments = ref<MidtermAssessment[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selfFormRef = ref<FormInstance>()
const advisorFormRef = ref<FormInstance>()
const createFormRef = ref<FormInstance>()

const selfForm = reactive<MidtermAssessment>({
  id: undefined,
  studentId: 0,
  semester: '',
  selfSummary: '',
  advisorComment: '',
  score: null,
})

const advisorForm = reactive<MidtermAssessment>({
  id: undefined,
  studentId: 0,
  semester: '',
  selfSummary: '',
  advisorComment: '',
  score: null,
})

const selfRules: FormRules = {
  selfSummary: [{ required: true, message: '请输入自我总结', trigger: 'blur' }],
}

const advisorRules: FormRules = {
  advisorComment: [{ required: true, message: '请输入评语', trigger: 'blur' }],
  score: [{ required: true, message: '请输入评分', trigger: 'blur' }],
}

const createForm = reactive({
  semester: '',
})

const createRules: FormRules = {
  semester: [{ required: true, message: '请选择学期', trigger: 'change' }],
}

const semesterList = [
  '2023-2024-1', '2023-2024-2',
  '2024-2025-1', '2024-2025-2',
  '2025-2026-1', '2025-2026-2',
]

function statusTagType(status?: string): string {
  const map: Record<string, string> = {
    DRAFT: 'info',
    PENDING: 'warning',
    REVIEWED: 'success',
    COMPLETE: 'success',
  }
  return map[status || ''] || 'info'
}

function statusText(status?: string): string {
  const map: Record<string, string> = {
    DRAFT: '待填写',
    PENDING: '待评估',
    REVIEWED: '已评语',
    COMPLETE: '已完成',
  }
  return map[status || ''] || (status || '未填写')
}

async function loadData() {
  const sno = isStudent.value ? undefined : (queryStudentNo.value || undefined)
  if (!isStudent.value && !sno) {
    ElMessage.warning('请输入学号')
    return
  }
  loading.value = true
  try {
    const res = await getMidtermAssessments(sno, semesterFilter.value || undefined, currentPage.value, pageSize.value)
    assessments.value = res.data.data.records || []
    total.value = res.data.data.total || 0
    if (!isStudent.value && assessments.value.length > 0) {
      resolvedStudentId.value = assessments.value[0].studentId
    }
    if (assessments.value.length === 0) {
      ElMessage.info('未查询到评估数据')
    }
  } catch {
    // interceptor handles
  } finally {
    loading.value = false
  }
}

// --- Student: self-summary ---
function openSelfSummaryDialog(row: MidtermAssessment) {
  resetSelfForm()
  selfForm.id = row.id
  selfForm.studentId = row.studentId
  selfForm.semester = row.semester
  selfForm.selfSummary = row.selfSummary || ''
  selfForm.advisorComment = row.advisorComment || ''
  selfForm.score = row.score
  selfDialogVisible.value = true
}

function resetSelfForm() {
  selfFormRef.value?.resetFields()
}

async function handleSaveSelfSummary() {
  const valid = await selfFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await saveMidtermAssessment({ ...selfForm })
    ElMessage.success('自我总结提交成功')
    selfDialogVisible.value = false
    await loadData()
  } catch {
    // interceptor handles
  } finally {
    submitting.value = false
  }
}

// --- Teacher: advisor comment ---
function openAdvisorDialog(row: MidtermAssessment) {
  resetAdvisorForm()
  advisorForm.id = row.id
  advisorForm.studentId = row.studentId
  advisorForm.semester = row.semester
  advisorForm.selfSummary = row.selfSummary || ''
  advisorForm.advisorComment = row.advisorComment || ''
  advisorForm.score = row.score
  advisorDialogVisible.value = true
}

function resetAdvisorForm() {
  advisorFormRef.value?.resetFields()
}

async function handleSaveAdvisor() {
  const valid = await advisorFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await saveMidtermAssessment({ ...advisorForm })
    ElMessage.success('评语与评分提交成功')
    advisorDialogVisible.value = false
    await loadData()
  } catch {
    // interceptor handles
  } finally {
    submitting.value = false
  }
}

// --- Teacher: create assessment ---
function openCreateDialog() {
  if (!queryStudentNo.value) {
    ElMessage.warning('请先输入学号')
    return
  }
  resetCreateForm()
  createDialogVisible.value = true
}

function resetCreateForm() {
  createFormRef.value?.resetFields()
  createForm.semester = ''
}

async function handleCreate() {
  const valid = await createFormRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await saveMidtermAssessment({
      studentId: resolvedStudentId.value,
      semester: createForm.semester,
      selfSummary: '',
      advisorComment: '',
      score: null,
      status: 'DRAFT',
    }, isStudent.value ? undefined : queryStudentNo.value)
    ElMessage.success('添加评估记录成功')
    createDialogVisible.value = false
    await loadData()
  } catch {
    // interceptor handles
  } finally {
    submitting.value = false
  }
}

// --- Delete ---
async function handleDelete(row: MidtermAssessment) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.semester}」的评估记录吗？`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteMidtermAssessment(row.id!)
    ElMessage.success('删除成功')
    await loadData()
  } catch {
    // user cancelled or interceptor handles
  }
}

watch([currentPage, pageSize], () => { loadData() })

onMounted(() => {
  if (isStudent.value) {
    loadData()
  }
})
</script>

<style scoped>
.midterm-page {
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
