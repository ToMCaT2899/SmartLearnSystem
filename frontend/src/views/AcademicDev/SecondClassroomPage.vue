<template>
  <div class="second-classroom-page">
    <h2 class="page-title">第二课堂成绩管理</h2>

    <!-- Query bar for TEACHER / ADMIN -->
    <div class="toolbar" v-if="!isStudent">
      <el-input v-model="queryStudentNo" placeholder="请输入学号" style="width: 200px" clearable />
      <el-button type="primary" @click="currentPage = 1; loadRecords()" :disabled="!queryStudentNo" style="margin-left: 12px">
        查询
      </el-button>
      <el-button type="success" @click="openAddDialog" :disabled="!queryStudentNo" style="margin-left: 12px">
        添加记录
      </el-button>
    </div>

    <el-table :data="records" border stripe v-loading="loading" empty-text="暂无第二课堂记录">
      <el-table-column prop="category" label="类别" width="140" align="center">
        <template #default="{ row }">
          <el-tag :type="categoryTagType(row.category)">{{ row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="activityName" label="活动名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="score" label="积分" width="100" align="center" />
      <el-table-column prop="recordDate" label="记录日期" width="130" align="center" />
      <el-table-column v-if="!isStudent" label="操作" width="80" align="center" fixed="right">
        <template #default="{ row }">
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

    <!-- Add Record Dialog -->
    <el-dialog v-model="dialogVisible" title="添加第二课堂记录" width="500px" @closed="resetForm">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item v-if="!isStudent" label="学号">
          <el-input v-model="form.studentNo" disabled />
        </el-form-item>
        <el-form-item label="类别" prop="category">
          <el-select v-model="form.category" placeholder="请选择类别" style="width: 100%">
            <el-option label="社会实践" value="社会实践" />
            <el-option label="志愿服务" value="志愿服务" />
            <el-option label="文体活动" value="文体活动" />
            <el-option label="社团活动" value="社团活动" />
            <el-option label="创新创业" value="创新创业" />
            <el-option label="技能培训" value="技能培训" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动名称" prop="activityName">
          <el-input v-model="form.activityName" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="积分" prop="score">
          <el-input-number v-model="form.score" :min="0" :max="100" :precision="1" placeholder="请输入积分数值" style="width: 100%" />
        </el-form-item>
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker v-model="form.recordDate" type="date" placeholder="请选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
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
import { getSecondClassroomScores, addSecondClassroomScore, deleteSecondClassroomScore } from '@/api/academic'

interface SecondClassroomScore {
  id?: number
  studentId: number
  category: string
  activityName: string
  score: number
  recordDate: string
}

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isStudent = computed(() => userInfo.value?.role === 'STUDENT')

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const queryStudentNo = ref('')
const records = ref<SecondClassroomScore[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const resolvedStudentId = ref(0)
const formRef = ref<FormInstance>()

const form = reactive<SecondClassroomScore & { studentNo: string }>({
  studentId: 0,
  studentNo: '',
  category: '',
  activityName: '',
  score: 0,
  recordDate: '',
})

const formRules: FormRules = {
  category: [{ required: true, message: '请选择类别', trigger: 'change' }],
  activityName: [{ required: true, message: '请输入活动名称', trigger: 'blur' }],
  score: [{ required: true, message: '请输入积分', trigger: 'blur' }],
  recordDate: [{ required: true, message: '请选择记录日期', trigger: 'change' }],
}

function categoryTagType(category: string): string {
  const map: Record<string, string> = {
    '社会实践': 'success',
    '志愿服务': 'warning',
    '文体活动': '',
    '社团活动': 'info',
    '创新创业': 'danger',
    '技能培训': '',
    '其他': 'info',
  }
  return map[category] || 'info'
}

async function loadRecords() {
  const sno = isStudent.value ? undefined : (queryStudentNo.value || undefined)
  if (!isStudent.value && !sno) {
    ElMessage.warning('请输入学号')
    return
  }
  loading.value = true
  try {
    const res = await getSecondClassroomScores(sno, currentPage.value, pageSize.value)
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
  form.category = ''
  form.activityName = ''
  form.score = 0
  form.recordDate = ''
}

async function handleAdd() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const { studentNo, ...payload } = form
    await addSecondClassroomScore(payload, isStudent.value ? undefined : queryStudentNo.value)
    ElMessage.success('添加第二课堂记录成功')
    dialogVisible.value = false
    await loadRecords()
  } catch {
    // interceptor handles
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: SecondClassroomScore) {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.activityName}」第二课堂记录吗？`,
      '确认删除',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
    await deleteSecondClassroomScore(row.id!)
    ElMessage.success('删除成功')
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
.second-classroom-page {
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
