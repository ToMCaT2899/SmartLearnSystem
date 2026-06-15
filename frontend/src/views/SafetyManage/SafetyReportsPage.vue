<template>
  <div class="safety-reports-page">
    <h2 class="page-title">安全报告</h2>

    <!-- All users: Submit / Edit report form -->
    <el-card class="form-card">
      <template #header>{{ editingId ? '编辑安全报告' : '提交安全报告' }}</template>
      <el-form :model="form" label-width="100px" style="max-width: 650px">
        <el-form-item label="报告类型">
          <el-select v-model="form.reportType" placeholder="请选择报告类型" style="width: 100%">
            <el-option label="校园隐患" value="CAMPUS_HAZARD" />
            <el-option label="个人紧急情况" value="PERSONAL_EMERGENCY" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="请输入标题" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请描述具体情况" />
        </el-form-item>
        <el-form-item label="位置">
          <el-input v-model="form.location" placeholder="请输入位置信息" />
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            ref="uploadRef"
            v-model:file-list="fileList"
            :auto-upload="false"
            :limit="5"
            accept=".jpg,.jpeg,.png"
            list-type="picture-card"
          >
            <el-icon><Plus /></el-icon>
            <template #tip>
              <div class="el-upload__tip">支持 JPG、PNG 格式，单文件不超过 10MB，最多 5 张</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ editingId ? '保存修改' : '提交报告' }}
          </el-button>
          <el-button v-if="editingId" @click="cancelEdit">取消编辑</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- My reports table -->
    <el-card v-if="!isAdmin" class="table-card">
      <template #header>我的报告</template>
      <el-table :data="myReports" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="80" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="reportType" label="类型" width="140">
          <template #default="{ row }">
            <el-tag>{{ reportTypeText(row.reportType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="location" label="位置" width="150" />
        <el-table-column label="图片" width="120">
          <template #default="{ row }">
            <span v-if="row.images">{{ getImageCount(row.images) }} 张</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
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

    <!-- Admin: Pending reports -->
    <template v-if="isAdmin">
      <el-card class="table-card">
        <template #header>待处理报告</template>
        <el-table :data="pendingReports" border stripe v-loading="loadingPending">
          <el-table-column type="index" label="序号" width="80" />
          <el-table-column prop="reporterName" label="报告人" width="100" />
          <el-table-column prop="reporterNo" label="学号/工号" width="120" />
          <el-table-column prop="title" label="标题" min-width="180" />
          <el-table-column prop="reportType" label="类型" width="140">
            <template #default="{ row }">
              <el-tag>{{ reportTypeText(row.reportType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
          <el-table-column prop="location" label="位置" width="150" />
          <el-table-column label="图片" width="120">
            <template #default="{ row }">
              <template v-if="row.images">
                <el-image
                  v-for="(url, i) in getImageUrls(row.images)"
                  :key="i"
                  :src="url"
                  :preview-src-list="getImageUrls(row.images)"
                  :initial-index="i"
                  preview-teleported
                  style="width: 36px; height: 36px; margin-right: 4px; border-radius: 4px"
                  fit="cover"
                />
              </template>
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                type="primary"
                size="small"
                :loading="handlingId === row.id"
                @click="handleProcess(row)"
              >处理</el-button>
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
      </el-card>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import {
  submitSafetyReport, getSafetyReports, getPendingSafetyReports,
  handleSafetyReport, deleteSafetyReport, updateSafetyReport, cancelSafetyReport,
} from '@/api/safety'
import { uploadFile } from '@/api/file'
import type { SafetyReport } from '@/types'

const userStore = useUserStore()
const role = computed(() => userStore.userInfo?.role || '')
const isAdmin = computed(() => role.value === 'ADMIN')
const isManager = computed(() => role.value === 'ADMIN' || role.value === 'TEACHER')

const loading = ref(false)
const loadingPending = ref(false)
const submitting = ref(false)
const handlingId = ref<number | null>(null)
const editingId = ref<number | undefined>(undefined)

const fileList = ref<any[]>([])
const uploadRef = ref()

const form = reactive<SafetyReport>({
  reportType: '',
  title: '',
  description: '',
  location: '',
  images: '',
})

const myReports = ref<SafetyReport[]>([])
const pendingReports = ref<SafetyReport[]>([])

// Pagination state for "my reports" table
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// Pagination state for "pending reports" table (admin)
const pendingCurrentPage = ref(1)
const pendingPageSize = ref(10)
const pendingTotal = ref(0)

function reportTypeText(type: string) {
  const map: Record<string, string> = {
    CAMPUS_HAZARD: '校园隐患',
    PERSONAL_EMERGENCY: '个人紧急情况',
    OTHER: '其他',
  }
  return map[type] || type
}

function statusTagType(status: string) {
  const map: Record<string, string> = { PENDING: 'warning', RESOLVED: 'success', DISMISSED: 'info', CANCELLED: 'info' }
  return map[status] || 'info'
}

function statusText(status: string) {
  const map: Record<string, string> = { PENDING: '待处理', RESOLVED: '已处理', DISMISSED: '已驳回', CANCELLED: '已取消' }
  return map[status] || status
}

function getImageCount(images: string) {
  return images.split(',').filter(u => u.trim()).length
}

function getImageUrls(images: string): string[] {
  return images.split(',').filter(u => u.trim())
}

function resetForm() {
  editingId.value = undefined
  fileList.value = []
  Object.assign(form, {
    reportType: '', title: '', description: '', location: '', images: '',
  })
}

function cancelEdit() {
  resetForm()
}

function openEditDialog(row: SafetyReport) {
  editingId.value = row.id
  Object.assign(form, {
    reportType: row.reportType,
    title: row.title,
    description: row.description,
    location: row.location,
    images: row.images || '',
  })
  // Populate fileList with existing images for display
  fileList.value = getImageUrls(row.images || '').map((url, i) => ({
    name: `image-${i + 1}`,
    url,
    status: 'success',
  }))
}

async function handleSubmit() {
  if (!form.reportType || !form.title || !form.description) {
    ElMessage.warning('请填写必要信息')
    return
  }
  submitting.value = true
  try {
    // Upload new files first
    const uploadedUrls: string[] = []
    for (const f of fileList.value) {
      if (f.raw) {
        const res = await uploadFile(f.raw, 'safety')
        uploadedUrls.push(res.data.data)
      } else if (f.url) {
        // Keep existing image URL
        uploadedUrls.push(f.url)
      }
    }
    const imagesStr = uploadedUrls.join(',')

    if (editingId.value) {
      await updateSafetyReport(editingId.value, { ...form, images: imagesStr })
      ElMessage.success('修改成功')
    } else {
      await submitSafetyReport({ ...form, images: imagesStr })
      ElMessage.success('安全报告提交成功')
    }
    resetForm()
    if (isAdmin.value) {
      pendingCurrentPage.value = 1
      await loadPending()
    } else {
      currentPage.value = 1
      await loadMyReports()
    }
  } catch (e) {
    console.error(e)
  } finally {
    submitting.value = false
  }
}

async function handleCancel(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要取消该报告吗？取消后管理员处不再显示。', '确认', { type: 'warning' })
    await cancelSafetyReport(id)
    ElMessage.success('已取消')
    if (isAdmin.value) {
      pendingCurrentPage.value = 1
      await loadPending()
    } else {
      currentPage.value = 1
      await loadMyReports()
    }
  } catch { /* cancelled or error */ }
}

async function handleDelete(id?: number) {
  if (!id) return
  try {
    await ElMessageBox.confirm('确定要删除该报告吗？', '确认', { type: 'warning' })
    await deleteSafetyReport(id)
    ElMessage.success('删除成功')
    if (isAdmin.value) {
      pendingCurrentPage.value = 1
      await loadPending()
    } else {
      currentPage.value = 1
      await loadMyReports()
    }
  } catch { /* cancelled or error */ }
}

async function handleProcess(row: SafetyReport) {
  handlingId.value = row.id!
  try {
    await handleSafetyReport(row.id!)
    ElMessage.success('已处理')
    pendingCurrentPage.value = 1
    await loadPending()
  } catch (e) {
    console.error(e)
  } finally {
    handlingId.value = null
  }
}

async function loadMyReports() {
  loading.value = true
  try {
    const res = await getSafetyReports(currentPage.value, pageSize.value)
    myReports.value = res.data.data.records || []
    total.value = res.data.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

async function loadPending() {
  loadingPending.value = true
  try {
    const res = await getPendingSafetyReports(pendingCurrentPage.value, pendingPageSize.value)
    pendingReports.value = res.data.data.records || []
    pendingTotal.value = res.data.data.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    loadingPending.value = false
  }
}

async function loadData() {
  if (!isAdmin.value) {
    await loadMyReports()
  }
  if (isAdmin.value) {
    await loadPending()
  }
}

watch([currentPage, pageSize], () => { loadMyReports() })
watch([pendingCurrentPage, pendingPageSize], () => { loadPending() })

onMounted(() => { loadData() })
</script>

<style scoped>
.safety-reports-page {
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
