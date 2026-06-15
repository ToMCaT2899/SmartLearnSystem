<template>
  <div class="activities-page">
    <h2 class="page-title">活动管理</h2>

    <!-- Filter and actions bar -->
    <div class="toolbar">
      <el-select v-model="categoryFilter" placeholder="按分类筛选" clearable style="width: 200px" @change="handleCategoryChange">
        <el-option label="学术讲座" value="ACADEMIC" />
        <el-option label="文体活动" value="SPORTS" />
        <el-option label="社会实践" value="PRACTICE" />
        <el-option label="志愿服务" value="VOLUNTEER" />
        <el-option label="竞赛" value="COMPETITION" />
        <el-option label="其他" value="OTHER" />
      </el-select>
      <el-button v-if="isTeacherOrAdmin" type="primary" @click="openCreateDialog" style="margin-left: 8px">发布活动</el-button>
      <el-button v-if="isTeacherOrAdmin" type="success" @click="handleExport" style="margin-left: 8px">导出报名情况</el-button>
      <el-button @click="showMyRegistrations = !showMyRegistrations" style="margin-left: 8px">
        {{ showMyRegistrations ? '查看全部活动' : '我的报名' }}
      </el-button>
    </div>

    <!-- Activities card grid -->
    <template v-if="!showMyRegistrations">
      <div v-loading="loading" class="card-grid">
        <el-empty v-if="activities.length === 0" description="暂无活动" />
        <el-card v-for="act in activities" :key="act.id" class="activity-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <el-tag size="small" type="primary">{{ categoryText(act.category) }}</el-tag>
              <span class="card-title">{{ act.title }}</span>
            </div>
          </template>
          <div class="card-body">
            <p><strong>发布者:</strong> {{ act.publisherName }}</p>
            <p><strong>主办方:</strong> {{ act.organizer }}</p>
            <p><strong>地点:</strong> {{ act.location }}</p>
            <p><strong>时间:</strong> {{ act.eventTime }}</p>
            <p><strong>人数:</strong> {{ act.currentParticipants || 0 }} / {{ act.maxParticipants }}
              <el-tag v-if="act.participantType" size="small" type="info" style="margin-left: 6px">
                {{ participantTypeText(act.participantType) }}
              </el-tag>
              <el-tag v-if="act.targetClass" size="small" type="warning" style="margin-left: 4px">
                {{ act.targetClass }}
              </el-tag>
            </p>
            <el-progress
              :percentage="Math.round(((act.currentParticipants || 0) / act.maxParticipants) * 100)"
              :stroke-width="8"
              :status="(act.currentParticipants || 0) >= act.maxParticipants ? 'exception' : undefined"
            />
          </div>
          <div class="card-footer">
            <template v-if="isRegistered(act.id)">
              <el-tag type="success" size="small">已报名</el-tag>
              <el-button type="warning" size="small" :loading="regLoadingId === act.id" @click="handleCancel(act)">取消报名</el-button>
            </template>
            <template v-else>
              <el-button
                type="primary"
                size="small"
                :disabled="(act.currentParticipants || 0) >= act.maxParticipants"
                :loading="regLoadingId === act.id"
                @click="handleRegister(act)"
              >
                {{ (act.currentParticipants || 0) >= act.maxParticipants ? '名额已满' : '报名' }}
              </el-button>
            </template>
            <el-button
              v-if="isTeacherOrAdmin"
              type="danger"
              size="small"
              @click="handleDelete(act)"
            >删除</el-button>
          </div>
        </el-card>
      </div>

      <el-pagination
        v-model:current-page="actCurrentPage"
        v-model:page-size="actPageSize"
        :page-sizes="[10, 20, 50]"
        :total="actTotal"
        layout="total, sizes, prev, pager, next, jumper"
        background
        style="margin-top: 16px; justify-content: flex-end"
      />
    </template>

    <!-- My registrations table -->
    <template v-else>
      <el-table :data="myRegistrations" border stripe v-loading="loadingRegs">
        <el-table-column prop="activityId" label="活动ID" width="100" />
        <el-table-column prop="title" label="活动名称" min-width="200" />
        <el-table-column prop="category" label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ categoryText(row.category) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="eventTime" label="活动时间" width="180" />
        <el-table-column prop="location" label="地点" width="150" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="warning" size="small" @click="handleRegCancel(row)">取消报名</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="regCurrentPage"
        v-model:page-size="regPageSize"
        :page-sizes="[10, 20, 50]"
        :total="regTotal"
        layout="total, sizes, prev, pager, next, jumper"
        background
        style="margin-top: 16px; justify-content: flex-end"
      />
    </template>

    <!-- Create activity dialog -->
    <el-dialog v-model="createVisible" title="发布活动" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="学术讲座" value="ACADEMIC" />
            <el-option label="文体活动" value="SPORTS" />
            <el-option label="社会实践" value="PRACTICE" />
            <el-option label="志愿服务" value="VOLUNTEER" />
            <el-option label="竞赛" value="COMPETITION" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入活动标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="3" placeholder="请输入活动描述" />
        </el-form-item>
        <el-form-item label="主办方">
          <el-input v-model="form.organizer" placeholder="请输入主办方" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" placeholder="请输入活动地点" />
        </el-form-item>
        <el-form-item label="活动时间">
          <el-date-picker
            v-model="form.eventTime"
            type="datetime"
            placeholder="请选择活动时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="最大人数">
          <el-input-number v-model="form.maxParticipants" :min="1" :max="10000" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="isAdmin" label="参与对象">
          <el-radio-group v-model="form.participantType">
            <el-radio-button label="STUDENT">仅学生</el-radio-button>
            <el-radio-button label="TEACHER">仅教师</el-radio-button>
            <el-radio-button label="BOTH">师生均可</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="isTeacher" label="目标班级">
          <el-input v-model="form.targetClass" placeholder="请输入目标班级名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import {
  getActivities, createActivity, deleteActivity,
  registerForActivity, cancelRegistration, getMyRegistrations,
  exportActivitiesExcel,
} from '@/api/communication'
import type { ActivityPost } from '@/types'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isTeacherOrAdmin = computed(() => role.value === 'TEACHER' || role.value === 'ADMIN')
const isAdmin = computed(() => role.value === 'ADMIN')
const isTeacher = computed(() => role.value === 'TEACHER')

const loading = ref(false)
const loadingRegs = ref(false)
const submitting = ref(false)
const regLoadingId = ref<number | null>(null)

const activities = ref<ActivityPost[]>([])
const myRegistrations = ref<any[]>([])
const showMyRegistrations = ref(false)
const categoryFilter = ref('')

const actCurrentPage = ref(1)
const actPageSize = ref(10)
const actTotal = ref(0)

const regCurrentPage = ref(1)
const regPageSize = ref(10)
const regTotal = ref(0)

const createVisible = ref(false)

const form = reactive<ActivityPost>({
  title: '',
  content: '',
  category: '',
  organizer: '',
  location: '',
  eventTime: '',
  maxParticipants: 50,
  participantType: 'STUDENT',
  targetClass: '',
})

function categoryText(cat: string) {
  const map: Record<string, string> = {
    ACADEMIC: '学术讲座',
    SPORTS: '文体活动',
    PRACTICE: '社会实践',
    VOLUNTEER: '志愿服务',
    COMPETITION: '竞赛',
    OTHER: '其他',
  }
  return map[cat] || cat
}

function participantTypeText(type: string) {
  const map: Record<string, string> = {
    STUDENT: '学生活动',
    TEACHER: '教师活动',
    BOTH: '师生均可',
  }
  return map[type] || type
}

function isRegistered(activityId?: number): boolean {
  if (!activityId) return false
  return myRegistrations.value.some(r => r.activityId === activityId)
}

async function loadActivities() {
  loading.value = true
  try {
    const res = await getActivities(categoryFilter.value || undefined, actCurrentPage.value, actPageSize.value)
    activities.value = res.data.data.records || []
    actTotal.value = res.data.data.total
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

async function loadRegistrations() {
  loadingRegs.value = true
  try {
    const res = await getMyRegistrations(regCurrentPage.value, regPageSize.value)
    myRegistrations.value = res.data.data.records || []
    regTotal.value = res.data.data.total
  } catch {
    // handled
  } finally {
    loadingRegs.value = false
  }
}

async function handleRegister(act: ActivityPost) {
  regLoadingId.value = act.id!
  try {
    await registerForActivity(act.id!)
    ElMessage.success('报名成功')
    await Promise.all([loadActivities(), loadRegistrations()])
  } catch {
    // handled
  } finally {
    regLoadingId.value = null
  }
}

async function handleCancel(act: ActivityPost) {
  regLoadingId.value = act.id!
  try {
    await cancelRegistration(act.id!)
    ElMessage.success('已取消报名')
    await Promise.all([loadActivities(), loadRegistrations()])
  } catch {
    // handled
  } finally {
    regLoadingId.value = null
  }
}

async function handleRegCancel(row: any) {
  try {
    await cancelRegistration(row.activityId!)
    ElMessage.success('已取消报名')
    await Promise.all([loadActivities(), loadRegistrations()])
  } catch {
    // handled
  }
}

async function handleCreate() {
  if (!form.category || !form.title || !form.organizer || !form.location || !form.eventTime) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (isTeacher.value && !form.targetClass?.trim()) {
    ElMessage.warning('请输入目标班级')
    return
  }
  submitting.value = true
  try {
    await createActivity({ ...form })
    ElMessage.success('活动发布成功')
    createVisible.value = false
    resetCreateForm()
    await loadActivities()
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

function resetCreateForm() {
  Object.assign(form, {
    title: '', content: '', category: '', organizer: '',
    location: '', eventTime: '', maxParticipants: 50,
    participantType: 'STUDENT', targetClass: '',
  })
}

async function handleDelete(act: ActivityPost) {
  try {
    await ElMessageBox.confirm('确定删除该活动吗？', '确认删除', { type: 'warning' })
    await deleteActivity(act.id!)
    ElMessage.success('删除成功')
    await loadActivities()
  } catch {
    // user cancelled or error
  }
}

async function handleExport() {
  try {
    await exportActivitiesExcel()
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

function openCreateDialog() {
  resetCreateForm()
  createVisible.value = true
}

function handleCategoryChange() {
  actCurrentPage.value = 1
  loadActivities()
}

watch([actCurrentPage, actPageSize], () => {
  loadActivities()
})

watch([regCurrentPage, regPageSize], () => {
  loadRegistrations()
})

onMounted(() => {
  loadActivities()
  loadRegistrations()
})
</script>

<style scoped>
.activities-page {
  max-width: 1400px;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}

.toolbar {
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 16px;
}

.activity-card {
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-title {
  font-weight: 600;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-body {
  flex: 1;
}

.card-body p {
  margin: 6px 0;
  font-size: 14px;
  color: #606266;
}

.card-footer {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: flex-end;
}
</style>
