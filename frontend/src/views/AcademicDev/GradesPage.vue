<template>
  <div class="grades-page">
    <h2 class="page-title">成绩管理</h2>

    <!-- Stats cards -->
    <div class="stats-row" v-if="grades.length > 0">
      <div class="stat-card">
        <div class="stat-icon avg"><el-icon :size="20"><DataAnalysis /></el-icon></div>
        <div>
          <div class="stat-value">{{ avgScore }}</div>
          <div class="stat-label">平均分</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon max"><el-icon :size="20"><TrendCharts /></el-icon></div>
        <div>
          <div class="stat-value">{{ maxScore }}</div>
          <div class="stat-label">最高分</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon min"><el-icon :size="20"><Bottom /></el-icon></div>
        <div>
          <div class="stat-value">{{ minScore }}</div>
          <div class="stat-label">最低分</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon count"><el-icon :size="20"><Document /></el-icon></div>
        <div>
          <div class="stat-value">{{ grades.length }}</div>
          <div class="stat-label">课程数</div>
        </div>
      </div>
    </div>

    <!-- Student View: own grades -->
    <template v-if="isStudent">
      <div class="toolbar">
        <el-select v-model="semesterFilter" placeholder="选择学期" clearable @change="currentPage = 1; loadGrades()" style="width: 200px">
          <el-option v-for="s in semesterList" :key="s" :label="s" :value="s" />
        </el-select>
      </div>

      <el-table :data="grades" border stripe v-loading="loading" empty-text="暂无成绩数据">
        <el-table-column label="课程信息" min-width="200">
          <template #default="{ row }">
            {{ getCourseName(row.courseId) }}
          </template>
        </el-table-column>
        <el-table-column prop="score" label="成绩" width="100" align="center" />
        <el-table-column prop="gpaPoint" label="绩点" width="100" align="center" />
        <el-table-column prop="semester" label="学期" width="140" align="center" />
        <el-table-column prop="examType" label="考试类型" width="120" align="center" />
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
    </template>

    <!-- Teacher / Admin View: tabs for query and batch import -->
    <template v-else>
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="查询" name="query">
          <div class="toolbar">
            <el-input v-model="queryStudentNo" placeholder="请输入学号" style="width: 200px" clearable />
            <el-select
              v-model="semesterFilter"
              placeholder="选择学期"
              clearable
              style="width: 200px; margin-left: 12px"
            >
              <el-option v-for="s in semesterList" :key="s" :label="s" :value="s" />
            </el-select>
            <el-button type="primary" style="margin-left: 12px" @click="currentPage = 1; loadGrades()" :disabled="!queryStudentNo">
              查询
            </el-button>
          </div>

          <el-table :data="grades" border stripe v-loading="loading" empty-text="请先查询学生成绩">
            <el-table-column label="课程信息" min-width="200">
              <template #default="{ row }">
                {{ getCourseName(row.courseId) }}
              </template>
            </el-table-column>
            <el-table-column prop="score" label="成绩" width="100" align="center" />
            <el-table-column prop="gpaPoint" label="绩点" width="100" align="center" />
            <el-table-column prop="semester" label="学期" width="140" align="center" />
            <el-table-column prop="examType" label="考试类型" width="120" align="center" />
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
        </el-tab-pane>

        <el-tab-pane label="批量导入" name="batch">
          <el-alert
            title="请粘贴包含成绩对象的 JSON 数组，每条记录需包含 studentNo（学号）, courseName（课程名称）, score, gpaPoint, semester, examType"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 12px"
          />
          <el-input
            v-model="batchJson"
            type="textarea"
            :rows="10"
            placeholder='[{"studentNo":"202313840358","courseName":"高等数学","score":85,"gpaPoint":3.5,"semester":"2025-2026-1","examType":"期末考试"}]'
          />
          <div style="margin-top: 12px">
            <el-upload
              :auto-upload="false"
              :on-change="handleFileChange"
              :limit="1"
              accept=".json"
              :show-file-list="false"
            >
              <el-button>选择 JSON 文件</el-button>
            </el-upload>
            <el-button type="primary" style="margin-left: 12px" @click="batchImport" :loading="importing">
              批量导入
            </el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { DataAnalysis, TrendCharts, Bottom, Document } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getGrades, batchImportGrades, getCourses } from '@/api/academic'
import type { Grade, Course } from '@/types'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)
const isStudent = computed(() => userInfo.value?.role === 'STUDENT')

const avgScore = computed(() => {
  if (grades.value.length === 0) return '--'
  return (grades.value.reduce((s, g) => s + (g.score || 0), 0) / grades.value.length).toFixed(1)
})
const maxScore = computed(() => {
  if (grades.value.length === 0) return '--'
  return Math.max(...grades.value.map(g => g.score || 0))
})
const minScore = computed(() => {
  if (grades.value.length === 0) return '--'
  return Math.min(...grades.value.map(g => g.score || 0))
})

const loading = ref(false)
const importing = ref(false)
const activeTab = ref('query')
const semesterFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const queryStudentNo = ref('')
const batchJson = ref('')
const grades = ref<Grade[]>([])
const courseMap = ref<Record<number, string>>({})

const semesterList = [
  '2023-2024-1', '2023-2024-2',
  '2024-2025-1', '2024-2025-2',
  '2025-2026-1', '2025-2026-2',
]

function getCourseName(courseId: number): string {
  return courseMap.value[courseId] || `课程#${courseId}`
}

async function loadCourses() {
  try {
    const res = await getCourses()
    const courses: Course[] = res.data.data || []
    const map: Record<number, string> = {}
    courses.forEach((c) => {
      if (c.id != null) {
        map[c.id] = `${c.courseName} (${c.courseCode ?? ''})`
      }
    })
    courseMap.value = map
  } catch {
    // interceptor handles error
  }
}

async function loadGrades() {
  const sno = isStudent.value ? undefined : (queryStudentNo.value || undefined)
  if (!isStudent.value && !sno) {
    ElMessage.warning('请输入学号')
    return
  }
  loading.value = true
  try {
    const res = await getGrades(sno, semesterFilter.value || undefined, currentPage.value, pageSize.value)
    grades.value = res.data.data.records || []
    total.value = res.data.data.total || 0
    if (grades.value.length === 0) {
      ElMessage.info('未查询到成绩数据')
    }
  } catch {
    // interceptor handles error
  } finally {
    loading.value = false
  }
}

async function initData() {
  await loadCourses()
  if (isStudent.value) {
    await loadGrades()
  }
}

async function batchImport() {
  if (!batchJson.value.trim()) {
    ElMessage.warning('请输入 JSON 数据')
    return
  }
  let data: Grade[]
  try {
    data = JSON.parse(batchJson.value)
    if (!Array.isArray(data)) {
      throw new Error('JSON 必须是数组格式')
    }
    if (data.length === 0) {
      ElMessage.warning('导入数据不能为空')
      return
    }
  } catch {
    ElMessage.error('JSON 格式不正确，请检查后重试')
    return
  }
  importing.value = true
  try {
    await batchImportGrades(data)
    ElMessage.success(`成功导入 ${data.length} 条成绩记录`)
    batchJson.value = ''
  } catch {
    // interceptor handles error
  } finally {
    importing.value = false
  }
}

function handleFileChange(file: any) {
  const reader = new FileReader()
  reader.onload = (e) => {
    const text = e.target?.result as string
    batchJson.value = text
    ElMessage.success('文件已读取，点击"批量导入"上传')
  }
  reader.onerror = () => {
    ElMessage.error('文件读取失败，请重试')
  }
  reader.readAsText(file.raw)
}

watch([currentPage, pageSize], () => { loadGrades() })

onMounted(() => {
  initData()
})
</script>

<style scoped>
.grades-page {
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

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: box-shadow 0.3s, transform 0.2s;
  border: 1px solid #f0f0f0;
}

.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  color: #fff;
}

.stat-icon.avg { background: linear-gradient(135deg, #667eea, #764ba2); }
.stat-icon.max { background: linear-gradient(135deg, #43e97b, #38f9d7); }
.stat-icon.min { background: linear-gradient(135deg, #f093fb, #f5576c); }
.stat-icon.count { background: linear-gradient(135deg, #4facfe, #00f2fe); }

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
</style>
