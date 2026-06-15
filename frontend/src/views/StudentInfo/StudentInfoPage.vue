<template>
  <div class="student-info-page">
    <!-- Top action bar -->
    <div class="top-bar">
      <h2 class="page-title">学生基本信息管理</h2>
      <div class="top-actions">
        <!-- Student search for admin/teacher -->
        <div v-if="isTeacher || isAdmin" class="search-wrapper">
          <el-input
            v-model="searchKeyword"
            placeholder="请输入学生姓名或学号"
            style="width: 240px"
            clearable
            @keyup.enter="onSearchClick"
            @clear="onSearchClear"
            @focus="onSearchFocus"
          />
          <el-button
            type="primary"
            :icon="Search"
            :loading="studentsLoading"
            style="margin-left: 8px"
            @click="onSearchClick"
          >搜索</el-button>
          <el-button
            :disabled="!selectedStudentId"
            style="margin-left: 8px"
            @click="onStudentReset"
          >重置</el-button>
          <!-- Search results dropdown -->
          <div
            v-if="showSearchResults"
            class="search-results-dropdown"
            @click.stop
          >
            <div
              v-for="s in studentList"
              :key="s.id"
              class="search-result-item"
              @click="selectStudent(s)"
            >
              <span class="result-name">{{ s.name }}</span>
              <span class="result-no">{{ s.studentNo }}</span>
              <span class="result-college">{{ s.college }} {{ s.major }}</span>
            </div>
            <div v-if="studentList.length === 0" class="search-result-empty">
              未找到匹配的学生
            </div>
          </div>
        </div>
        <el-button
          v-if="canExport"
          type="success"
          :icon="Download"
          @click="handleExport"
        >导出信息</el-button>
        <el-button
          v-if="canEdit && !editing"
          type="primary"
          :icon="Edit"
          @click="startEdit"
        >编辑资料</el-button>
        <template v-if="canEdit && editing">
          <el-button type="primary" :loading="saving" :icon="Check" @click="saveProfile">保存</el-button>
          <el-button :icon="Close" @click="cancelEdit">取消</el-button>
        </template>
      </div>
    </div>

    <!-- Profile overview card -->
    <el-card class="profile-card" shadow="hover">
      <el-row :gutter="24">
        <el-col :xs="24" :sm="6" class="profile-avatar-col">
          <div class="avatar-section">
            <el-avatar :size="100" :src="profile.photoUrl" class="profile-avatar">
              <el-icon :size="48"><UserFilled /></el-icon>
            </el-avatar>
            <h3 class="profile-name">{{ profile.name || '未填写' }}</h3>
            <el-tag :type="difficultyTagType" size="small" effect="dark" round>
              {{ difficultyText }}
            </el-tag>
          </div>
        </el-col>
        <el-col :xs="24" :sm="18">
          <div class="profile-quick-info">
            <el-descriptions :column="2" border size="small" :colon="false">
              <el-descriptions-item label="学号">
                <span class="info-value">{{ profile.studentNo || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="学院">
                <span class="info-value">{{ profile.college || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="专业">
                <span class="info-value">{{ profile.major || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="年级班级">
                <span class="info-value">{{ profile.grade }}级 {{ profile.className || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="校区">
                <span class="info-value">{{ profile.campus || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="宿舍号">
                <span class="info-value">{{ profile.dormitory || '—' }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="身份证号">
                <span class="info-value mono">{{ maskedIdCard }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="手机号">
                <span class="info-value mono">{{ profile.phone || '—' }}</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <!-- Tabbed detail sections -->
    <el-card class="detail-card" shadow="hover">
      <el-tabs v-model="activeTab" type="border-card" class="info-tabs">
        <!-- Tab 1: Basic Info Form -->
        <el-tab-pane label="基本信息" name="basic">
          <template #label>
            <span class="tab-label">
              <el-icon><User /></el-icon> 基本信息
            </span>
          </template>
          <el-form :model="profile" label-width="130px" :disabled="!editing" class="info-form">
            <el-divider content-position="left">
              <el-icon><School /></el-icon> 学籍信息
              <span v-if="isStudent" style="font-size:12px;color:#909399;margin-left:8px">姓名、身份证号、宿舍号及学籍信息由学校管理，如需修改请通过"信息变更"申请</span>
              <span v-else-if="isTeacher" style="font-size:12px;color:#909399;margin-left:8px">更改此信息，请联系管理员</span>
              <span v-else style="font-size:12px;color:#67c23a;margin-left:8px">管理员可直接修改以下信息</span>
            </el-divider>
            <el-row :gutter="20">
              <el-col :xs="24" :sm="8">
                <el-form-item label="学号">
                  <el-input v-model="profile.studentNo" :disabled="!editing || !isAdmin" placeholder="请输入学号" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="姓名">
                  <el-input v-model="profile.name" :disabled="!editing || !isAdmin" placeholder="请输入姓名" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="身份证号">
                  <el-input v-model="profile.idCard" :disabled="!editing || !isAdmin" placeholder="请输入身份证号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :xs="24" :sm="8">
                <el-form-item label="学院">
                  <el-input v-model="profile.college" :disabled="!editing || !isAdmin" placeholder="请输入学院" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="专业">
                  <el-input v-model="profile.major" :disabled="!editing || !isAdmin" placeholder="请输入专业" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="年级">
                  <el-select v-model="profile.grade" :disabled="!editing || !isAdmin" placeholder="选择年级" style="width:100%">
                    <el-option v-for="g in gradeOptions" :key="g" :label="`${g}级`" :value="g" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :xs="24" :sm="8">
                <el-form-item label="班级">
                  <el-input v-model="profile.className" :disabled="!editing || !isAdmin" placeholder="请输入班级" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="校区">
                  <el-input v-model="profile.campus" :disabled="!editing || !isAdmin" placeholder="请输入校区" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="8">
                <el-form-item label="宿舍号">
                  <el-input v-model="profile.dormitory" :disabled="!editing || !isAdmin" placeholder="请输入宿舍号" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="24">
                <el-form-item label="非集中住宿地址">
                  <el-input v-model="profile.offCampusAddress" placeholder="如无集中住宿请填写外住地址" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">
              <el-icon><Star /></el-icon> 个人情况
            </el-divider>
            <el-form-item label="联系方式">
              <el-input v-model="profile.phone" placeholder="请输入手机号" />
            </el-form-item>
            <el-form-item label="病史">
              <el-input v-model="profile.medicalHistory" type="textarea" :rows="2" placeholder="请如实填写，如无则填写'无'" />
            </el-form-item>
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="爱好">
                  <el-input v-model="profile.hobbies" placeholder="如：篮球、阅读、摄影" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="性格特征">
                  <el-input v-model="profile.personality" placeholder="如：开朗、细心、善于沟通" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="职业目标">
              <el-input v-model="profile.careerGoal" type="textarea" :rows="2" placeholder="请描述你的职业规划和目标" />
            </el-form-item>

            <el-divider content-position="left">
              <el-icon><User /></el-icon> 班主任信息
              <span v-if="!isAdmin" style="font-size:12px;color:#909399;margin-left:8px">更改此信息，请联系管理员</span>
              <span v-else style="font-size:12px;color:#67c23a;margin-left:8px">管理员可直接修改</span>
            </el-divider>
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="班主任姓名">
                  <el-input v-model="profile.advisorName" :disabled="!editing || !isAdmin" placeholder="请输入班主任姓名" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="联系方式">
                  <el-input v-model="profile.advisorPhone" :disabled="!editing || !isAdmin" placeholder="请输入班主任联系方式" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-divider content-position="left">
              <el-icon><Warning /></el-icon> 困难认定
              <span v-if="!isAdmin" style="font-size:12px;color:#909399;margin-left:8px">更改此信息，请联系管理员</span>
              <span v-else style="font-size:12px;color:#67c23a;margin-left:8px">管理员可直接修改</span>
            </el-divider>
            <el-row :gutter="20">
              <el-col :xs="24" :sm="12">
                <el-form-item label="困难认定等级">
                  <el-select v-model="profile.difficultyLevel" :disabled="!editing || !isAdmin" placeholder="请选择困难等级" clearable style="width:100%">
                    <el-option label="特别困难" value="SPECIAL">
                      <el-tag type="danger" size="small">特别困难</el-tag>
                    </el-option>
                    <el-option label="困难" value="DIFFICULT">
                      <el-tag type="warning" size="small">困难</el-tag>
                    </el-option>
                    <el-option label="一般困难" value="GENERAL">
                      <el-tag type="info" size="small">一般困难</el-tag>
                    </el-option>
                    <el-option label="不困难" value="NONE">不困难</el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-tab-pane>

        <!-- Tab 2: Family Members -->
        <el-tab-pane label="家庭情况" name="family">
          <template #label>
            <span class="tab-label">
              <el-icon><Connection /></el-icon> 家庭情况
            </span>
          </template>
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="addFamilyMember" :disabled="!editing">添加成员</el-button>
            <span class="toolbar-hint" v-if="familyMembers.length === 0">请填写父母及其他家庭成员信息</span>
          </div>
          <el-table :data="familyMembers" border stripe empty-text="暂无家庭成员信息">
            <el-table-column prop="relation" label="关系" width="100" />
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="phone" label="联系电话" width="140" />
            <el-table-column prop="workUnit" label="工作单位" min-width="160" />
            <el-table-column prop="address" label="住址" min-width="160" />
            <el-table-column label="操作" width="80" fixed="right" v-if="editing">
              <template #default="{ $index }">
                <el-button type="danger" size="small" :icon="Delete" circle @click="removeFamilyMember($index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button v-if="editing" type="primary" style="margin-top: 12px" @click="saveFamily" :loading="savingFamily">保存家庭信息</el-button>
        </el-tab-pane>

        <!-- Tab 3: Emergency Contacts -->
        <el-tab-pane label="紧急联系人" name="emergency">
          <template #label>
            <span class="tab-label">
              <el-icon><Phone /></el-icon> 紧急联系人
            </span>
          </template>
          <el-alert
            v-if="emergencyContacts.length < 2"
            title="根据要求，需至少填写2位紧急联系人"
            type="warning"
            show-icon
            :closable="false"
            style="margin-bottom: 12px"
          />
          <div class="tab-toolbar">
            <el-button type="primary" size="small" :icon="Plus" @click="addEmergencyContact" :disabled="!editing">添加联系人</el-button>
          </div>
          <el-table :data="emergencyContacts" border stripe empty-text="暂无紧急联系人">
            <el-table-column prop="name" label="姓名" width="120" />
            <el-table-column prop="relation" label="关系" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ row.relation }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="联系电话" width="150" />
            <el-table-column prop="address" label="住址" min-width="200" />
            <el-table-column label="操作" width="80" fixed="right" v-if="editing">
              <template #default="{ $index }">
                <el-button type="danger" size="small" :icon="Delete" circle @click="removeEmergencyContact($index)" />
              </template>
            </el-table-column>
          </el-table>
          <el-button v-if="editing" type="primary" style="margin-top: 12px" @click="saveEmergencyContactsFn" :loading="savingEmergency">保存紧急联系人</el-button>
        </el-tab-pane>

        <!-- Tab 4: Info Change Requests -->
        <el-tab-pane label="信息变更" name="changes">
          <template #label>
            <span class="tab-label">
              <el-icon><EditPen /></el-icon> 信息变更
            </span>
          </template>
          <div class="tab-toolbar">
            <el-button v-if="isStudent" type="primary" size="small" :icon="Plus" @click="openChangeDialog">提交变更申请</el-button>
          </div>
          <el-table :data="changeRequests" border stripe empty-text="暂无变更申请">
            <el-table-column prop="fieldName" label="变更字段" width="160">
              <template #default="{ row }">{{ changeFieldText(row.fieldName) }}</template>
            </el-table-column>
            <el-table-column prop="oldValue" label="原值" min-width="140" show-overflow-tooltip />
            <el-table-column prop="newValue" label="新值" min-width="140" show-overflow-tooltip />
            <el-table-column prop="reason" label="变更原因" min-width="160" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewComment" label="审核意见" min-width="140" show-overflow-tooltip />
            <el-table-column v-if="isTeacher || isAdmin" label="审核操作" width="140" fixed="right">
              <template #default="{ row }">
                <template v-if="row.status === 'PENDING'">
                  <el-button type="success" size="small" @click="handleReview(row.id, 'APPROVED')">通过</el-button>
                  <el-button type="danger" size="small" @click="handleReview(row.id, 'REJECTED')">拒绝</el-button>
                </template>
                <span v-else style="color: #c0c4cc">—</span>
              </template>
            </el-table-column>
            <el-table-column v-if="isStudent" label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'PENDING'"
                  type="danger"
                  size="small"
                  :icon="Delete"
                  circle
                  @click="handleCancelRequest(row.id)"
                />
                <span v-else style="color: #c0c4cc">—</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- Family Member Dialog -->
    <el-dialog v-model="familyDialogVisible" title="添加家庭成员" width="520px" destroy-on-close>
      <el-form :model="familyForm" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="关系"><el-input v-model="familyForm.relation" placeholder="如：父亲、母亲" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名"><el-input v-model="familyForm.name" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="联系电话"><el-input v-model="familyForm.phone" /></el-form-item>
        <el-form-item label="工作单位"><el-input v-model="familyForm.workUnit" /></el-form-item>
        <el-form-item label="住址"><el-input v-model="familyForm.address" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="familyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddFamily">确定</el-button>
      </template>
    </el-dialog>

    <!-- Emergency Contact Dialog -->
    <el-dialog v-model="emergencyDialogVisible" title="添加紧急联系人" width="520px" destroy-on-close>
      <el-form :model="emergencyForm" label-width="80px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="姓名"><el-input v-model="emergencyForm.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="关系"><el-input v-model="emergencyForm.relation" placeholder="如：父亲、朋友" /></el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="联系电话"><el-input v-model="emergencyForm.phone" /></el-form-item>
        <el-form-item label="住址"><el-input v-model="emergencyForm.address" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="emergencyDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmAddEmergency">确定</el-button>
      </template>
    </el-dialog>

    <!-- Change Request Dialog -->
    <el-dialog v-model="changeDialogVisible" title="信息变更申请" width="520px" destroy-on-close>
      <el-form :model="changeForm" label-width="90px">
        <el-form-item label="变更字段">
          <el-select v-model="changeForm.fieldName" placeholder="请选择要变更的字段" style="width:100%" @change="onChangeFieldSelect">
            <el-option label="学号" value="studentNo" />
            <el-option label="姓名" value="name" />
            <el-option label="身份证号" value="idCard" />
            <el-option label="学院" value="college" />
            <el-option label="专业" value="major" />
            <el-option label="年级" value="grade" />
            <el-option label="班级" value="className" />
            <el-option label="校区" value="campus" />
            <el-option label="宿舍号" value="dormitory" />
            <el-option label="班主任姓名" value="advisorName" />
            <el-option label="班主任联系方式" value="advisorPhone" />
          </el-select>
        </el-form-item>
        <el-form-item label="原值"><el-input v-model="changeForm.oldValue" disabled /></el-form-item>
        <el-form-item label="新值"><el-input v-model="changeForm.newValue" placeholder="请填写变更后的值" /></el-form-item>
        <el-form-item label="变更原因">
          <el-input v-model="changeForm.reason" type="textarea" :rows="2" placeholder="请说明变更原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="changeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmSubmitChange">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import {
  User, Star, School, Warning, Connection, Phone,
  EditPen, Plus, Delete, Download, Edit, Check, Close, UserFilled, Search,
} from '@element-plus/icons-vue'
import {
  getMyProfile, getProfileById, searchStudents, updateProfile,
  getFamilyMembers, saveFamilyMembers as saveFamilyApi,
  getEmergencyContacts, saveEmergencyContacts as saveContactsApi,
  getChangeRequests, submitChangeRequest, reviewChangeRequest, cancelChangeRequest, exportStudentInfo,
} from '@/api/student'
import { useUserStore } from '@/store/user'
import type { StudentProfile, FamilyMember, EmergencyContact, InfoChangeRequest } from '@/types'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const activeTab = ref('basic')
const editing = ref(false)
const saving = ref(false)
const savingFamily = ref(false)
const savingEmergency = ref(false)

// Student selector state (admin/teacher)
const studentList = ref<StudentProfile[]>([])
const selectedStudentId = ref<number | null>(null)
const studentsLoading = ref(false)

// Effective student ID for data loading
const viewingStudentId = computed(() => {
  if (isStudent.value) return profile.id ?? null
  return selectedStudentId.value ?? profile.id ?? null
})

const isStudent = computed(() => userInfo.value?.role === 'STUDENT')
const isTeacher = computed(() => userInfo.value?.role === 'TEACHER')
const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
const canEdit = computed(() => {
  if (isStudent.value || isAdmin.value) return true
  if (isTeacher.value) return !selectedStudentId.value // only own profile
  return false
})
const canExport = computed(() => isAdmin.value || isTeacher.value || isStudent.value)

const gradeOptions = ['2022', '2023', '2024', '2025', '2026']

const maskedIdCard = computed(() => {
  const id = profile.idCard
  if (!id || id.length < 8) return '—'
  return id.substring(0, 4) + '****' + id.substring(id.length - 4)
})

const difficultyText = computed(() => {
  const map: Record<string, string> = { SPECIAL: '特别困难', DIFFICULT: '困难', GENERAL: '一般困难', NONE: '不困难' }
  return map[profile.difficultyLevel] || '未认定'
})

const difficultyTagType = computed(() => {
  const map: Record<string, string> = { SPECIAL: 'danger', DIFFICULT: 'warning', GENERAL: 'info', NONE: '' }
  return map[profile.difficultyLevel] || 'info'
})

// Basic profile
const profile = reactive<StudentProfile>({
  studentNo: '', name: '', photoUrl: '', phone: '', idCard: '',
  college: '', major: '', grade: '', className: '', campus: '',
  dormitory: '', offCampusAddress: '', medicalHistory: '',
  hobbies: '', personality: '', careerGoal: '',
  advisorName: '', advisorPhone: '', difficultyLevel: '',
})

const backupProfile = reactive<StudentProfile>({ ...profile })

function startEdit() {
  Object.assign(backupProfile, { ...profile })
  editing.value = true
}

function cancelEdit() {
  Object.assign(profile, { ...backupProfile })
  editing.value = false
}

// Family
const familyMembers = ref<FamilyMember[]>([])
const familyDialogVisible = ref(false)
const familyForm = reactive<FamilyMember>({ relation: '', name: '', phone: '', workUnit: '', address: '' })

// Emergency contacts
const emergencyContacts = ref<EmergencyContact[]>([])
const emergencyDialogVisible = ref(false)
const emergencyForm = reactive<EmergencyContact>({ name: '', relation: '', phone: '', address: '' })

// Change requests
const changeRequests = ref<InfoChangeRequest[]>([])
const changeDialogVisible = ref(false)
const changeForm = reactive<InfoChangeRequest>({ fieldName: '', oldValue: '', newValue: '', reason: '' })

// Helper functions
function statusTagType(status?: string) {
  return { PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger', CANCELLED: 'info' }[status || ''] || 'info'
}
function statusText(status?: string) {
  return { PENDING: '审核中', APPROVED: '已通过', REJECTED: '已拒绝', CANCELLED: '已取消' }[status || ''] || status || '—'
}
function changeFieldText(field: string) {
  const map: Record<string, string> = {
    name: '姓名', studentNo: '学号', idCard: '身份证号', phone: '手机号', dormitory: '宿舍号',
    college: '学院', major: '专业', grade: '年级', className: '班级', campus: '校区',
    offCampusAddress: '非集中住宿地址', advisorName: '班主任', advisorPhone: '班主任联系方式',
    medicalHistory: '病史', hobbies: '爱好', personality: '性格特征', careerGoal: '职业目标',
    difficultyLevel: '困难认定等级',
  }
  return map[field] || field
}
// Load profile on mount (student list is loaded on-demand via remote search)
async function loadAll() {
  await loadMyProfile()
}

async function loadMyProfile() {
  try {
    const profileRes = await getMyProfile()
    if (profileRes.data.data) Object.assign(profile, profileRes.data.data)
    if (profile.id) {
      await loadStudentSubData()
    }
  } catch { /* handled */ }
}

const searchKeyword = ref('')
const showSearchResults = ref(false)

function onSearchClick() {
  const kw = searchKeyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  studentsLoading.value = true
  searchStudents(kw).then(res => {
    studentList.value = res.data.data || []
    showSearchResults.value = true
  }).catch(() => {
    studentList.value = []
    showSearchResults.value = true
  }).finally(() => {
    studentsLoading.value = false
  })
}

function onSearchClear() {
  searchKeyword.value = ''
  studentList.value = []
  showSearchResults.value = false
}

function onSearchFocus() {
  if (studentList.value.length > 0) {
    showSearchResults.value = true
  }
}

function selectStudent(s: StudentProfile) {
  selectedStudentId.value = s.id!
  searchKeyword.value = `${s.name} (${s.studentNo})`
  showSearchResults.value = false
  onStudentChange()
}

function closeSearchResults(e: MouseEvent) {
  const target = e.target as HTMLElement
  if (!target.closest('.search-wrapper')) {
    showSearchResults.value = false
  }
}

async function loadStudentSubData() {
  const sid = viewingStudentId.value
  if (!sid) return
  try {
    const [familyRes, contactsRes, changesRes] = await Promise.all([
      getFamilyMembers(sid),
      getEmergencyContacts(sid),
      getChangeRequests(sid),
    ])
    familyMembers.value = familyRes.data.data || []
    emergencyContacts.value = contactsRes.data.data || []
    changeRequests.value = changesRes.data.data || []
  } catch { /* handled */ }
}

function onStudentReset() {
  selectedStudentId.value = null
  onStudentChange()
}

async function onStudentChange() {
  editing.value = false
  if (!selectedStudentId.value) {
    // Clear existing data before reloading own profile,
    // so stale student data is wiped even if user has no profile
    const empty: StudentProfile = { studentNo: '', name: '', photoUrl: '', phone: '', idCard: '', college: '', major: '', grade: '', className: '', campus: '', dormitory: '', offCampusAddress: '', medicalHistory: '', hobbies: '', personality: '', careerGoal: '', advisorName: '', advisorPhone: '', difficultyLevel: '' }
    Object.assign(profile, empty)
    familyMembers.value = []
    emergencyContacts.value = []
    changeRequests.value = []
    await loadMyProfile()
    return
  }
  try {
    const profileRes = await getProfileById(selectedStudentId.value)
    if (profileRes.data.data) Object.assign(profile, profileRes.data.data)
    await loadStudentSubData()
  } catch { ElMessage.error('加载学生信息失败') }
}

// Save profile
async function saveProfile() {
  // Validate required fields
  const required: { key: string; label: string }[] = [
    { key: 'phone', label: '手机号' },
  ]
  // Admins can edit name and idCard directly, so require them
  if (isAdmin.value) {
    required.push({ key: 'name', label: '姓名' }, { key: 'idCard', label: '身份证号' })
  }
  for (const field of required) {
    if (!(profile as any)[field.key]) {
      ElMessage.warning(`请填写${field.label}`)
      return
    }
  }
  saving.value = true
  try {
    await updateProfile(profile)
    ElMessage.success('个人信息保存成功')
    editing.value = false
  } catch { /* handled */ } finally { saving.value = false }
}

// Export
async function handleExport() {
  const sid = viewingStudentId.value
  if (!sid) {
    ElMessage.warning('暂无档案数据可导出')
    return
  }
  try {
    const res = await exportStudentInfo(sid)
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `学生信息_${profile.name || profile.studentNo || sid}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

// Family operations
function addFamilyMember() { familyDialogVisible.value = true; Object.assign(familyForm, { relation: '', name: '', phone: '', workUnit: '', address: '' }) }
function confirmAddFamily() { familyMembers.value.push({ ...familyForm }); familyDialogVisible.value = false }
function removeFamilyMember(index: number) { familyMembers.value.splice(index, 1) }
async function saveFamily() {
  savingFamily.value = true
  try { await saveFamilyApi(familyMembers.value, selectedStudentId.value ?? undefined); ElMessage.success('家庭信息保存成功') } catch { /* */ } finally { savingFamily.value = false }
}

// Emergency contacts operations
function addEmergencyContact() { emergencyDialogVisible.value = true; Object.assign(emergencyForm, { name: '', relation: '', phone: '', address: '' }) }
function confirmAddEmergency() { emergencyContacts.value.push({ ...emergencyForm }); emergencyDialogVisible.value = false }
function removeEmergencyContact(index: number) { emergencyContacts.value.splice(index, 1) }
async function saveEmergencyContactsFn() {
  savingEmergency.value = true
  try { await saveContactsApi(emergencyContacts.value, selectedStudentId.value ?? undefined); ElMessage.success('紧急联系人保存成功') } catch { /* */ } finally { savingEmergency.value = false }
}

// Change request operations
function openChangeDialog() {
  Object.assign(changeForm, { fieldName: '', oldValue: '', newValue: '', reason: '' })
  changeDialogVisible.value = true
}

function onChangeFieldSelect(fieldName: string) {
  changeForm.oldValue = (profile as any)[fieldName] || ''
}

async function confirmSubmitChange() {
  if (!changeForm.fieldName || !changeForm.oldValue || !changeForm.newValue || !changeForm.reason) {
    ElMessage.warning('请填写完整信息'); return
  }
  try { await submitChangeRequest(changeForm); ElMessage.success('变更申请已提交'); changeDialogVisible.value = false; loadStudentSubData() } catch { /* */ }
}

async function handleCancelRequest(requestId: number) {
  try {
    await cancelChangeRequest(requestId)
    ElMessage.success('申请已取消')
    await loadStudentSubData()
  } catch { ElMessage.error('取消失败') }
}

async function handleReview(requestId: number, status: string) {
  try {
    await reviewChangeRequest(requestId, status, status === 'APPROVED' ? '审核通过' : '审核拒绝')
    ElMessage.success(status === 'APPROVED' ? '已通过' : '已拒绝')
    // Refresh change requests and profile (for teacher/admin, loadAll only refreshes student list)
    await loadStudentSubData()
    if (viewingStudentId.value) {
      const profileRes = await getProfileById(viewingStudentId.value)
      if (profileRes.data.data) Object.assign(profile, profileRes.data.data)
    }
  } catch { ElMessage.error('审核失败') }
}

onMounted(() => { loadAll(); document.addEventListener('click', closeSearchResults) })
onBeforeUnmount(() => { document.removeEventListener('click', closeSearchResults) })
</script>

<style scoped>
.student-info-page {
  max-width: 1400px;
}

/* Top bar */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  flex-wrap: wrap;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}

.top-actions {
  display: flex;
  gap: 8px;
}

/* Profile card */
.profile-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
}

.profile-avatar-col {
  display: flex;
  justify-content: center;
}

.avatar-section {
  text-align: center;
  padding: 16px 0;
}

.profile-avatar {
  border: 3px solid #f0f2f5;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.avatar-upload-btn {
  margin-top: 8px;
}

.profile-name {
  margin: 12px 0 8px;
  font-size: 18px;
  color: #1d2129;
}

.profile-quick-info {
  padding: 4px 0;
}

.info-value {
  font-weight: 500;
  color: #303133;
}

.info-value.mono {
  font-family: 'Consolas', 'Courier New', monospace;
  letter-spacing: 0.5px;
}

/* Detail card */
.detail-card {
  border-radius: 12px;
  border: none;
}

.info-tabs {
  box-shadow: none;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 4px;
}

.info-form {
  max-width: 1000px;
}

.tab-toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.toolbar-hint {
  color: #909399;
  font-size: 13px;
}

/* Search */
.search-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.search-results-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  max-width: 360px;
  margin-top: 4px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  z-index: 1000;
  max-height: 320px;
  overflow-y: auto;
}

.search-result-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.search-result-item:hover {
  background: #f0f5ff;
}

.search-result-item + .search-result-item {
  border-top: 1px solid #f2f3f5;
}

.result-name {
  font-weight: 600;
  color: #1d2129;
  min-width: 60px;
}

.result-no {
  color: #606266;
  font-size: 13px;
  min-width: 100px;
}

.result-college {
  color: #909399;
  font-size: 12px;
  margin-left: auto;
  text-align: right;
  white-space: nowrap;
}

.search-result-empty {
  padding: 24px 14px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
