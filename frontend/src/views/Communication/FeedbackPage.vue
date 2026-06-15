<template>
  <div class="feedback-page">
    <h2 class="page-title">意见反馈</h2>

    <!-- Student: Submit feedback form -->
    <template v-if="isNotAdmin">
      <el-card class="form-card">
        <template #header>提交反馈</template>
        <el-form :model="form" label-width="100px" style="max-width: 600px">
          <el-form-item label="标题">
            <el-input v-model="form.title" placeholder="请输入反馈标题" />
          </el-form-item>
          <el-form-item label="内容">
            <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入反馈内容" />
          </el-form-item>
          <el-form-item label="类型">
            <el-select v-model="form.type" placeholder="请选择反馈类型" style="width: 100%">
              <el-option label="建议" value="SUGGESTION" />
              <el-option label="问题反馈" value="ISSUE" />
              <el-option label="投诉" value="COMPLAINT" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="匿名提交">
            <el-switch v-model="form.isAnonymous" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="submitting" @click="handleSubmit">提交反馈</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </template>

    <!-- Student: My feedback table -->
    <template v-if="isNotAdmin">
      <el-card class="table-card">
        <template #header>我的反馈</template>
        <el-table :data="myFeedback" border stripe v-loading="loading">
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
          <el-table-column prop="type" label="类型" width="100">
            <template #default="{ row }">
              <el-tag>{{ typeText(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reply" label="回复" min-width="200" show-overflow-tooltip>
            <template #default="{ row }">
              <span v-if="row.reply">{{ row.reply }}</span>
              <span v-else style="color: #c0c4cc">暂无回复</span>
            </template>
          </el-table-column>
          <el-table-column prop="repliedAt" label="回复时间" width="180" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button type="primary" size="small" link @click="showFeedbackDetail(row)">详情</el-button>
              <el-button
                v-if="row.status === 'PENDING'"
                type="danger"
                size="small"
                link
                @click="handleDeleteFeedback(row)"
              >删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-model:current-page="myCurrentPage"
          v-model:page-size="myPageSize"
          :page-sizes="[10, 20, 50]"
          :total="myTotal"
          layout="total, sizes, prev, pager, next, jumper"
          background
          style="margin-top: 16px; justify-content: flex-end"
        />
      </el-card>
    </template>

    <!-- Teacher/Admin: Pending feedback -->
    <template v-if="isAdmin">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="待回复" name="pending">
          <el-table :data="pendingFeedback" border stripe v-loading="loading">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column prop="title" label="标题" min-width="180" />
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag>{{ typeText(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="提交者" width="100">
              <template #default="{ row }">
                <span v-if="row.isAnonymous" style="color: #c0c4cc">匿名</span>
                <span v-else>{{ row.submitterName }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="showFeedbackDetail(row)">详情</el-button>
                <el-button type="primary" size="small" @click="openReplyDialog(row)">回复</el-button>
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
        </el-tab-pane>
        <el-tab-pane label="全部反馈" name="all">
          <el-table :data="allFeedback" border stripe v-loading="loadingAll">
            <el-table-column type="index" label="序号" width="60" />
            <el-table-column label="提交者" width="100">
              <template #default="{ row }">
                <span v-if="row.isAnonymous" style="color: #c0c4cc">匿名</span>
                <span v-else>{{ row.submitterName }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" min-width="150" />
            <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="100">
              <template #default="{ row }">
                <el-tag>{{ typeText(row.type) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reply" label="回复" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" size="small" link @click="showFeedbackDetail(row)">详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-model:current-page="allCurrentPage"
            v-model:page-size="allPageSize"
            :page-sizes="[10, 20, 50]"
            :total="allTotal"
            layout="total, sizes, prev, pager, next, jumper"
            background
            style="margin-top: 16px; justify-content: flex-end"
          />
        </el-tab-pane>
      </el-tabs>
    </template>

    <!-- Feedback detail dialog -->
    <el-dialog v-model="feedbackDetailVisible" title="反馈详情" width="600px" destroy-on-close>
      <template v-if="detailTarget">
        <div class="feedback-detail">
          <div class="feedback-detail__header">
            <el-tag :type="statusTagType(detailTarget.status || '')" size="small">{{ statusText(detailTarget.status || '') }}</el-tag>
            <el-tag type="info" size="small">{{ typeText(detailTarget.type) }}</el-tag>
            <h3 class="feedback-detail__title">{{ detailTarget.title }}</h3>
          </div>
          <el-divider />
          <div class="feedback-detail__meta">
            <div class="feedback-detail__meta-item">
              <el-icon><User /></el-icon>
              <span v-if="detailTarget.isAnonymous" style="color: #c0c4cc">匿名提交</span>
              <span v-else>{{ detailTarget.submitterName || '用户' + detailTarget.userId }}</span>
            </div>
            <div class="feedback-detail__meta-item">
              <el-icon><Clock /></el-icon>
              <span>{{ detailTarget.createdAt }}</span>
            </div>
          </div>
          <el-divider />
          <div class="feedback-detail__section">
            <h4>反馈内容</h4>
            <div class="feedback-detail__content">{{ detailTarget.content }}</div>
          </div>
          <div v-if="detailTarget.reply" class="feedback-detail__section">
            <h4>回复</h4>
            <div class="feedback-detail__reply">
              <div class="feedback-detail__reply-text">{{ detailTarget.reply }}</div>
              <div class="feedback-detail__reply-time">回复于 {{ detailTarget.repliedAt }}</div>
            </div>
          </div>
        </div>
      </template>
    </el-dialog>

    <!-- Reply dialog -->
    <el-dialog v-model="replyVisible" title="回复反馈" width="500px">
      <div style="margin-bottom: 12px; color: #606266">
        <strong>{{ replyTarget?.title }}</strong>
        <p style="margin-top: 8px">{{ replyTarget?.content }}</p>
      </div>
      <el-input
        v-model="replyContent"
        type="textarea"
        :rows="4"
        placeholder="请输入回复内容"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="handleReply">回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Clock } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { submitFeedback, getMyFeedback, getPendingFeedback, getAllFeedback, replyFeedback, deleteFeedback } from '@/api/communication'
import type { Feedback } from '@/types'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isNotAdmin = computed(() => role.value === 'STUDENT' || role.value === 'TEACHER')
const isAdmin = computed(() => role.value === 'ADMIN')

const activeTab = ref('pending')
const loading = ref(false)
const loadingAll = ref(false)
const submitting = ref(false)
const replying = ref(false)

const form = reactive<Feedback>({
  title: '',
  content: '',
  type: '',
  isAnonymous: 0,
})

const myFeedback = ref<Feedback[]>([])
const pendingFeedback = ref<Feedback[]>([])
const allFeedback = ref<Feedback[]>([])

const myCurrentPage = ref(1)
const myPageSize = ref(10)
const myTotal = ref(0)

const pendingCurrentPage = ref(1)
const pendingPageSize = ref(10)
const pendingTotal = ref(0)

const allCurrentPage = ref(1)
const allPageSize = ref(10)
const allTotal = ref(0)

const replyVisible = ref(false)
const replyTarget = ref<Feedback | null>(null)
const replyContent = ref('')

const feedbackDetailVisible = ref(false)
const detailTarget = ref<Feedback | null>(null)

function typeText(type: string) {
  const map: Record<string, string> = {
    SUGGESTION: '建议',
    ISSUE: '问题反馈',
    COMPLAINT: '投诉',
    OTHER: '其他',
  }
  return map[type] || type
}

function statusTagType(status: string) {
  const map: Record<string, string> = { PENDING: 'warning', REPLIED: 'success' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { PENDING: '待回复', REPLIED: '已回复' }
  return map[status] || status
}

async function handleSubmit() {
  if (!form.title || !form.content || !form.type) {
    ElMessage.warning('请填写完整信息')
    return
  }
  submitting.value = true
  try {
    await submitFeedback({ ...form })
    ElMessage.success('反馈提交成功')
    Object.assign(form, { title: '', content: '', type: '', isAnonymous: 0 })
    await loadMyFeedback()
  } catch {
    // handled
  } finally {
    submitting.value = false
  }
}

function showFeedbackDetail(row: Feedback) {
  detailTarget.value = row
  feedbackDetailVisible.value = true
}

async function handleDeleteFeedback(row: Feedback) {
  try {
    await ElMessageBox.confirm('确定删除该反馈吗？删除后无法恢复。', '确认删除', { type: 'warning' })
    await deleteFeedback(row.id!)
    ElMessage.success('删除成功')
    await loadMyFeedback()
  } catch {
    // user cancelled or error
  }
}

function openReplyDialog(row: Feedback) {
  replyTarget.value = row
  replyContent.value = ''
  replyVisible.value = true
}

async function handleReply() {
  if (!replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyFeedback(replyTarget.value!.id!, replyContent.value.trim())
    ElMessage.success('回复成功')
    replyVisible.value = false
    await loadPending()
  } catch {
    // handled
  } finally {
    replying.value = false
  }
}

async function loadMyFeedback() {
  loading.value = true
  try {
    const res = await getMyFeedback(myCurrentPage.value, myPageSize.value)
    myFeedback.value = res.data.data.records || []
    myTotal.value = res.data.data.total
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

async function loadPending() {
  loading.value = true
  try {
    const res = await getPendingFeedback(pendingCurrentPage.value, pendingPageSize.value)
    pendingFeedback.value = res.data.data.records || []
    pendingTotal.value = res.data.data.total
  } catch {
    // handled
  } finally {
    loading.value = false
  }
}

async function loadAllFeedback() {
  loadingAll.value = true
  try {
    const res = await getAllFeedback(allCurrentPage.value, allPageSize.value)
    allFeedback.value = res.data.data.records || []
    allTotal.value = res.data.data.total
  } catch {
    // handled
  } finally {
    loadingAll.value = false
  }
}

async function loadData() {
  if (isNotAdmin.value) {
    await loadMyFeedback()
  }
  if (isAdmin.value) {
    await loadPending()
    await loadAllFeedback()
  }
}

watch([myCurrentPage, myPageSize], () => {
  loadMyFeedback()
})

watch([pendingCurrentPage, pendingPageSize], () => {
  loadPending()
})

watch([allCurrentPage, allPageSize], () => {
  loadAllFeedback()
})

onMounted(() => { loadData() })
</script>

<style scoped>
.feedback-page {
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

.feedback-detail__header {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.feedback-detail__title {
  margin: 0;
  font-size: 18px;
  color: #1d2129;
  width: 100%;
}

.feedback-detail__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  color: #86909c;
  font-size: 14px;
}

.feedback-detail__meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.feedback-detail__section {
  margin-bottom: 16px;
}

.feedback-detail__section h4 {
  margin: 0 0 10px 0;
  font-size: 15px;
  color: #1d2129;
}

.feedback-detail__content {
  background: #f7f8fa;
  border-radius: 6px;
  padding: 16px 20px;
  line-height: 1.8;
  font-size: 15px;
  color: #1d2129;
  white-space: pre-wrap;
  min-height: 80px;
}

.feedback-detail__reply {
  background: #ecfdf5;
  border-radius: 6px;
  padding: 16px 20px;
}

.feedback-detail__reply-text {
  line-height: 1.8;
  font-size: 15px;
  color: #1d2129;
  white-space: pre-wrap;
}

.feedback-detail__reply-time {
  margin-top: 10px;
  font-size: 13px;
  color: #86909c;
}
</style>
