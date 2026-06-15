<template>
  <div class="competitions-page">
    <h2 class="page-title">竞赛成果管理</h2>

    <!-- Query bar for TEACHER / ADMIN -->
    <div class="toolbar" v-if="!isStudent">
      <el-input v-model="queryStudentNo" placeholder="请输入学号" style="width: 200px" clearable />
      <el-button type="primary" @click="currentPage = 1; loadCompetitions()" :disabled="!queryStudentNo" style="margin-left: 12px">
        查询
      </el-button>
      <el-button type="success" @click="openAddDialog" :disabled="!queryStudentNo" style="margin-left: 12px">
        添加竞赛
      </el-button>
    </div>

    <div class="toolbar" v-else>
      <el-button type="success" @click="openAddDialog">添加竞赛</el-button>
    </div>

    <el-table :data="competitions" border stripe v-loading="loading" empty-text="暂无竞赛记录">
      <el-table-column prop="competitionName" label="竞赛名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="level" label="级别" width="120" align="center">
        <template #default="{ row }">
          <el-tag :type="levelTagType(row.level)">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="award" label="获奖情况" width="140" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.award" :type="awardTagType(row.award)" size="small">{{ row.award }}</el-tag>
          <span v-else style="color: #c0c4cc">--</span>
        </template>
      </el-table-column>
      <el-table-column prop="date" label="日期" width="130" align="center" />
      <el-table-column label="证书文件" width="100" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.certificateFile" type="success" size="small">已上传</el-tag>
          <span v-else style="color: #c0c4cc">--</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.certificateFile" type="primary" link size="small" @click="openDetail(row)">详情</el-button>
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

    <!-- Add Competition Dialog -->
    <el-dialog v-model="dialogVisible" title="添加竞赛记录" width="520px" @closed="resetForm">
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item v-if="!isStudent" label="学号" prop="studentNo">
          <el-input v-model="form.studentNo" placeholder="请输入学号" disabled />
        </el-form-item>
        <el-form-item label="竞赛名称" prop="competitionName">
          <el-input v-model="form.competitionName" placeholder="请输入竞赛名称" />
        </el-form-item>
        <el-form-item label="级别" prop="level">
          <el-select v-model="form.level" placeholder="请选择竞赛级别" style="width: 100%">
            <el-option label="国际级" value="国际级" />
            <el-option label="国家级" value="国家级" />
            <el-option label="省部级" value="省部级" />
            <el-option label="市厅级" value="市厅级" />
            <el-option label="校级" value="校级" />
            <el-option label="院级" value="院级" />
          </el-select>
        </el-form-item>
        <el-form-item label="获奖情况" prop="award">
          <el-select v-model="form.award" placeholder="请选择获奖情况" style="width: 100%">
            <el-option label="特等奖" value="特等奖" />
            <el-option label="一等奖" value="一等奖" />
            <el-option label="二等奖" value="二等奖" />
            <el-option label="三等奖" value="三等奖" />
            <el-option label="优秀奖" value="优秀奖" />
            <el-option label="参与奖" value="参与奖" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker v-model="form.date" type="date" placeholder="请选择日期" style="width: 100%" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="证书文件">
          <el-upload
            v-model:file-list="certFileList"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.jpg,.jpeg,.png"
            list-type="text"
          >
            <el-button type="primary" plain size="small">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 PDF、JPG、PNG 格式，不超过 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>

    <!-- Certificate Detail Dialog -->
    <el-dialog v-model="detailVisible" title="证书详情" width="80%" top="5vh" @closed="detailUrl = ''">
      <div v-if="detailLoading" style="text-align:center;padding:40px">
        <el-icon :size="32" class="is-loading"><Loading /></el-icon>
        <p style="margin-top:12px;color:#909399">加载证书文件中...</p>
      </div>
      <div v-else-if="detailUrl" style="text-align:center">
        <iframe v-if="isPdf" :src="detailUrl" style="width:100%;height:70vh;border:none" />
        <img v-else :src="detailUrl" style="max-width:100%;max-height:70vh" alt="证书文件" />
      </div>
      <div v-else style="text-align:center;padding:40px;color:#909399">
        无法加载证书文件
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadUserFile } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getCompetitions, addCompetition, deleteCompetition } from '@/api/academic'
import { uploadFile, getDownloadUrl } from '@/api/file'
import type { CompetitionRecord } from '@/types'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isStudent = computed(() => userInfo.value?.role === 'STUDENT')

const loading = ref(false)
const submitting = ref(false)
const dialogVisible = ref(false)
const queryStudentNo = ref('')
const competitions = ref<CompetitionRecord[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const formRef = ref<FormInstance>()

const certFileList = ref<UploadUserFile[]>([])
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailUrl = ref('')
const isPdf = ref(false)

const form = reactive({
  studentNo: '',
  competitionName: '',
  level: '',
  award: '',
  date: '',
})

const formRules: FormRules = {
  studentNo: [{ required: true, message: '请输入学号', trigger: 'blur' }],
  competitionName: [{ required: true, message: '请输入竞赛名称', trigger: 'blur' }],
  level: [{ required: true, message: '请选择竞赛级别', trigger: 'change' }],
  award: [{ required: true, message: '请选择获奖情况', trigger: 'change' }],
  date: [{ required: true, message: '请选择日期', trigger: 'change' }],
}

function levelTagType(level: string): string {
  const map: Record<string, string> = {
    '国际级': 'danger',
    '国家级': 'warning',
    '省部级': 'success',
    '市厅级': 'info',
    '校级': '',
    '院级': 'info',
  }
  return map[level] || 'info'
}

function awardTagType(award: string): string {
  const map: Record<string, string> = {
    '特等奖': 'danger',
    '一等奖': 'warning',
    '二等奖': 'success',
    '三等奖': '',
    '优秀奖': 'info',
    '参与奖': 'info',
  }
  return map[award] || 'info'
}

async function loadCompetitions() {
  const sno = isStudent.value ? undefined : (queryStudentNo.value || undefined)
  if (!isStudent.value && !sno) {
    ElMessage.warning('请输入学号')
    return
  }
  loading.value = true
  try {
    const res = await getCompetitions(sno, currentPage.value, pageSize.value)
    competitions.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch {
    // interceptor handles
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  if (!isStudent.value && !queryStudentNo.value) {
    ElMessage.warning('请先输入学号查询，再添加竞赛')
    return
  }
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  formRef.value?.resetFields()
  certFileList.value = []
  form.studentNo = isStudent.value ? '' : queryStudentNo.value
  form.competitionName = ''
  form.level = ''
  form.award = ''
  form.date = ''
}

async function handleAdd() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    let certificateFile = ''
    if (certFileList.value.length > 0) {
      const res = await uploadFile(certFileList.value[0].raw!, 'competitions')
      certificateFile = res.data.data
    }
    await addCompetition({ ...form, certificateFile })
    ElMessage.success('添加竞赛记录成功')
    dialogVisible.value = false
    await loadCompetitions()
  } catch {
    // interceptor handles
  } finally {
    submitting.value = false
  }
}

async function openDetail(row: CompetitionRecord) {
  if (!row.certificateFile) return
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getDownloadUrl(row.certificateFile)
    const url = res.data.data
    detailUrl.value = url
    isPdf.value = row.certificateFile.toLowerCase().endsWith('.pdf')
  } catch {
    detailUrl.value = ''
  } finally {
    detailLoading.value = false
  }
}

async function handleDelete(row: CompetitionRecord) {
  try {
    await ElMessageBox.confirm(`确定要删除竞赛记录「${row.competitionName}」吗？`, '确认删除', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await deleteCompetition(row.id!)
    ElMessage.success('删除成功')
    await loadCompetitions()
  } catch {
    // user cancelled or interceptor handles
  }
}

watch([currentPage, pageSize], () => { loadCompetitions() })

onMounted(() => {
  if (isStudent.value) {
    loadCompetitions()
  }
})
</script>

<style scoped>
.competitions-page {
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
