<template>
  <div class="notices-page">
    <h2 class="page-title">班级公告</h2>

    <!-- Toolbar -->
    <div class="toolbar">
      <template v-if="isTeacherOrAdmin">
        <el-button type="primary" @click="openCreateDialog">发布公告</el-button>
      </template>
      <el-input
        v-model="classFilter"
        placeholder="输入班级标识筛选"
        clearable
        style="width: 200px; margin-left: 12px"
        @change="handleFilterChange"
      />
    </div>

    <!-- Notices table -->
    <el-table :data="notices" border stripe v-loading="loading">
      <el-table-column type="index" label="序号" width="60" />
      <el-table-column prop="title" label="标题" min-width="200">
        <template #default="{ row }">
          <el-tag v-if="row.isPinned" type="warning" size="small" style="margin-right: 6px">置顶</el-tag>
          {{ row.title }}
        </template>
      </el-table-column>
      <el-table-column prop="content" label="内容预览" min-width="300" show-overflow-tooltip />
      <el-table-column prop="classIdentifier" label="班级" width="120" />
      <el-table-column prop="publisherName" label="发布者" width="100" />
      <el-table-column prop="publishedAt" label="发布时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" link @click="showDetail(row)">详情</el-button>
          <el-button
            v-if="isTeacherOrAdmin"
            type="danger"
            size="small"
            link
            @click="handleDelete(row)"
          >删除</el-button>
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

    <!-- Detail dialog -->
    <el-dialog v-model="detailVisible" title="公告详情" width="640px" destroy-on-close>
      <template v-if="detailRow">
        <div class="notice-detail">
          <div class="notice-detail__header">
            <el-tag v-if="detailRow.isPinned" type="warning" size="small" class="notice-detail__pin">置顶</el-tag>
            <h2 class="notice-detail__title">{{ detailRow.title }}</h2>
          </div>
          <el-divider />
          <div class="notice-detail__meta">
            <div class="notice-detail__meta-item">
              <el-icon><User /></el-icon>
              <span>{{ detailRow.publisherName }}</span>
            </div>
            <div class="notice-detail__meta-item">
              <el-icon><School /></el-icon>
              <span>{{ detailRow.classIdentifier }}</span>
            </div>
            <div class="notice-detail__meta-item">
              <el-icon><Clock /></el-icon>
              <span>{{ detailRow.publishedAt }}</span>
            </div>
          </div>
          <el-divider />
          <div class="notice-detail__content">{{ detailRow.content }}</div>
        </div>
      </template>
    </el-dialog>

    <!-- Create dialog -->
    <el-dialog v-model="createVisible" title="发布公告" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="班级">
          <el-input v-model="form.classIdentifier" placeholder="请输入班级标识" />
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="请输入公告内容" />
        </el-form-item>
        <el-form-item label="置顶">
          <el-switch v-model="form.isPinned" :active-value="1" :inactive-value="0" />
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
import { User, School, Clock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { getClassNotices, createClassNotice, deleteClassNotice } from '@/api/communication'
import type { ClassNotice } from '@/types'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isTeacherOrAdmin = computed(() => role.value === 'TEACHER' || role.value === 'ADMIN')

const loading = ref(false)
const submitting = ref(false)
const notices = ref<ClassNotice[]>([])
const classFilter = ref('')

const createVisible = ref(false)
const detailVisible = ref(false)
const detailRow = ref<ClassNotice | null>(null)

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const form = reactive<ClassNotice>({
  classIdentifier: '',
  title: '',
  content: '',
  isPinned: 0,
})

async function loadNotices() {
  loading.value = true
  try {
    const res = await getClassNotices(classFilter.value || undefined, currentPage.value, pageSize.value)
    notices.value = (res.data.data.records || []).sort((a, b) => {
      if (a.isPinned && !b.isPinned) return -1
      if (!a.isPinned && b.isPinned) return 1
      return 0
    })
    total.value = res.data.data.total
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

function showDetail(row: ClassNotice) {
  detailRow.value = row
  detailVisible.value = true
}

function openCreateDialog() {
  Object.assign(form, { classIdentifier: '', title: '', content: '', isPinned: 0 })
  createVisible.value = true
}

async function handleCreate() {
  if (!form.classIdentifier || !form.title || !form.content) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    await createClassNotice({ ...form })
    ElMessage.success('公告发布成功')
    createVisible.value = false
    await loadNotices()
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: ClassNotice) {
  try {
    await ElMessageBox.confirm('确定删除该公告吗？', '确认删除', { type: 'warning' })
    await deleteClassNotice(row.id!)
    ElMessage.success('删除成功')
    await loadNotices()
  } catch {
    // user cancelled or error
  }
}

function handleFilterChange() {
  currentPage.value = 1
  loadNotices()
}

watch([currentPage, pageSize], () => {
  loadNotices()
})

onMounted(() => { loadNotices() })
</script>

<style scoped>
.notices-page {
  max-width: 1400px;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.page-title {
  margin: 0 0 20px 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
}

.notice-detail__header {
  display: flex;
  align-items: center;
  gap: 10px;
}

.notice-detail__pin {
  flex-shrink: 0;
}

.notice-detail__title {
  margin: 0;
  font-size: 20px;
  color: #1d2129;
}

.notice-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  color: #86909c;
  font-size: 14px;
}

.notice-detail__meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.notice-detail__content {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 20px;
  line-height: 1.8;
  font-size: 15px;
  color: #1d2129;
  white-space: pre-wrap;
  min-height: 120px;
}
</style>
