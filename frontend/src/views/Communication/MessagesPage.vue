<template>
  <div class="messages-page">
    <h2 class="page-title">消息中心</h2>

    <el-row :gutter="16" class="chat-container">
      <!-- Left: Contacts list -->
      <el-col :span="6">
        <el-card class="contacts-card" shadow="never">
          <template #header>
            <div class="contacts-header">
              <span>联系人</span>
              <el-badge :value="unreadTotal" :hidden="unreadTotal === 0" />
            </div>
          </template>
          <div class="contacts-list">
            <div
              v-for="contact in contacts"
              :key="contact.id"
              :class="['contact-item', { active: activeContactId === contact.id }]"
              @click="selectContact(contact)"
            >
              <div class="contact-info">
                <span class="contact-name">
                  {{ contact.name || ('用户' + contact.id) }}
                  <span v-if="contact.studentNo" class="contact-student-no">{{ contact.studentNo }}</span>
                </span>
                <el-badge :value="contact.unreadCount" :hidden="!contact.unreadCount" class="unread-dot" />
              </div>
              <span class="contact-last">{{ contact.lastMessage || '' }}</span>
            </div>
            <el-empty v-if="contacts.length === 0" description="暂无联系人" />
          </div>

          <!-- Start new conversation -->
          <div class="new-conversation">
            <el-input v-model="newUserId" placeholder="输入学号开始对话" size="small">
              <template #append>
                <el-button @click="startConversation">开始对话</el-button>
              </template>
            </el-input>
          </div>
        </el-card>
      </el-col>

      <!-- Right: Conversation -->
      <el-col :span="18">
        <el-card class="conversation-card" shadow="never">
          <template #header>
            <div class="conv-header">
              <span v-if="activeContact">
                与 {{ activeContact.name || ('用户' + activeContact.id) }}<template v-if="activeContact.studentNo"> ({{ activeContact.studentNo }})</template> 的对话
              </span>
              <span v-else>请选择联系人</span>
              <div class="conv-header-actions">
                <el-button
                  v-if="activeContact && activeContact.unreadCount"
                  type="warning"
                  size="small"
                  @click="handleMarkAsRead"
                >标记已读</el-button>
                <el-button
                  v-if="activeContact && messages.length > 0"
                  type="danger"
                  size="small"
                  :loading="clearing"
                  @click="handleClearConversation"
                >清除历史</el-button>
              </div>
            </div>
          </template>

          <div class="messages-area" ref="messagesContainer">
            <div v-if="messages.length === 0" class="empty-chat">
              <el-empty description="暂无消息" />
            </div>
            <div
              v-for="msg in messages"
              :key="msg.id"
              :class="['message-item', msg.senderId === myId ? 'message-mine' : 'message-other']"
            >
              <div class="message-bubble">
                <div class="message-content">{{ msg.content }}</div>
                <div class="message-meta">
                  <span class="message-time">{{ msg.sentAt || '' }}</span>
                  <span v-if="msg.senderId === myId" class="read-status">
                    {{ msg.isRead ? '已读' : '未读' }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <div class="message-input" v-if="activeContact">
            <el-input
              v-model="newMessage"
              type="textarea"
              :rows="2"
              placeholder="输入消息..."
              @keydown.enter.exact.prevent="handleSend"
            />
            <el-button type="primary" :loading="sending" @click="handleSend" style="margin-top: 8px">发送</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { sendMessage, getConversation, getUnreadMessages, markConversationAsRead, getContacts, findUserByStudentNo, clearConversation } from '@/api/communication'
import type { PrivateMessage } from '@/types'

const userStore = useUserStore()
const myId = computed(() => userStore.userInfo?.userId || 0)

const contacts = ref<any[]>([])
const activeContactId = ref<number | null>(null)
const activeContact = computed(() => contacts.value.find(c => c.id === activeContactId.value) || null)

const messages = ref<PrivateMessage[]>([])
const newMessage = ref('')
const newUserId = ref('')
const sending = ref(false)
const clearing = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)

const unreadTotal = computed(() =>
  contacts.value.reduce((sum, c) => sum + (c.unreadCount || 0), 0)
)

async function loadContacts() {
  try {
    const res = await getContacts()
    const contactList: { id: number; name: string; role: string; studentNo?: string }[] = res.data.data || []
    // Get unread counts for each contact
    const unreadRes = await getUnreadMessages()
    const unreadMsgs: PrivateMessage[] = unreadRes.data.data || []

    const unreadMap: Record<number, number> = {}
    for (const msg of unreadMsgs) {
      const other = msg.senderId! === myId.value ? msg.receiverId : msg.senderId!
      unreadMap[other] = (unreadMap[other] || 0) + 1
    }

    contacts.value = contactList
      .filter(c => c.id !== myId.value)
      .map(contact => ({
        id: contact.id,
        name: contact.name || '',
        role: contact.role || '',
        studentNo: contact.studentNo || '',
        unreadCount: unreadMap[contact.id] || 0,
        lastMessage: '',
      }))

    // Add unique contacts from unread messages that aren't in the contacts list
    for (const msg of unreadMsgs) {
      const other = msg.senderId! === myId.value ? msg.receiverId : msg.senderId!
      if (!contacts.value.find(c => c.id === other) && other !== myId.value) {
        contacts.value.push({ id: other, name: '', role: '', studentNo: '', unreadCount: unreadMap[other] || 0, lastMessage: '' })
      }
    }
  } catch {
    // handled
  }
}

async function selectContact(contact: any) {
  activeContactId.value = contact.id
  contact.unreadCount = 0
  try {
    const res = await getConversation(contact.id)
    messages.value = res.data.data || []
    await nextTick()
    scrollToBottom()
    // Auto mark messages from this contact as read
    markConversationAsRead(contact.id).catch(() => {})
  } catch {
    // handled
  }
}

async function startConversation() {
  const input = newUserId.value.trim()
  if (!input) {
    ElMessage.warning('请输入学号或用户ID')
    return
  }
  try {
    // Try to find by student number first
    const userRes = await findUserByStudentNo(input)
    const user = userRes.data.data
    if (!user || !user.id) {
      ElMessage.warning('未找到该学号对应的用户')
      return
    }
    if (user.id === myId.value) {
      ElMessage.warning('不能和自己对话')
      return
    }
    if (!contacts.value.find(c => c.id === user.id)) {
      contacts.value.unshift({ id: user.id, name: user.name || '', role: user.role || '', studentNo: user.studentNo || '', unreadCount: 0, lastMessage: '' })
    }
    activeContactId.value = user.id
    messages.value = []
    newUserId.value = ''
  } catch {
    ElMessage.warning('未找到该用户，请检查学号是否正确')
  }
}

async function handleSend() {
  if (!newMessage.value.trim() || !activeContactId.value) return
  sending.value = true
  try {
    await sendMessage({
      receiverId: activeContactId.value,
      content: newMessage.value.trim(),
    })
    // Add locally
    messages.value.push({
      senderId: myId.value,
      receiverId: activeContactId.value,
      content: newMessage.value.trim(),
      isRead: 0,
      sentAt: new Date().toLocaleString(),
    })
    newMessage.value = ''
    await nextTick()
    scrollToBottom()
  } catch {
    // handled
  } finally {
    sending.value = false
  }
}

async function handleMarkAsRead() {
  if (!activeContactId.value) return
  try {
    await markConversationAsRead(activeContactId.value)
    messages.value.forEach(m => {
      if (m.senderId !== myId.value) m.isRead = 1
    })
    ElMessage.success('已标记为已读')
    await loadContacts()
  } catch {
    // handled
  }
}

async function handleClearConversation() {
  if (!activeContactId.value) return
  clearing.value = true
  try {
    await clearConversation(activeContactId.value)
    messages.value = []
    ElMessage.success('对话历史已清除')
  } catch {
    // handled
  } finally {
    clearing.value = false
  }
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

onMounted(() => { loadContacts() })
</script>

<style scoped>
.messages-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.page-title {
  margin: 0 0 16px 0;
  font-size: 22px;
  color: #1d2129;
  font-weight: 600;
  flex-shrink: 0;
}

.chat-container {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.chat-container :deep(.el-col) {
  height: 100%;
  overflow: hidden;
}

.contacts-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.contacts-card :deep(.el-card__header) {
  flex-shrink: 0;
}

.contacts-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.contacts-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.contacts-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.contact-item {
  padding: 10px 12px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.contact-item:hover {
  background-color: #f5f7fa;
}

.contact-item.active {
  background-color: #ecf5ff;
}

.contact-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.contact-name {
  font-weight: 500;
}

.contact-student-no {
  font-size: 12px;
  color: #909399;
  margin-left: 6px;
}

.contact-last {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.new-conversation {
  padding-top: 10px;
  border-top: 1px solid #ebeef5;
}

.conversation-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.conversation-card :deep(.el-card__header) {
  flex-shrink: 0;
}

.conversation-card :deep(.el-card__body) {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.conv-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.conv-header-actions {
  display: flex;
  gap: 8px;
}

.messages-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 12px;
}

.message-item {
  margin-bottom: 16px;
  display: flex;
}

.message-mine {
  justify-content: flex-end;
}

.message-other {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 10px 14px;
  border-radius: 12px;
}

.message-mine .message-bubble {
  background-color: #409eff;
  color: #fff;
}

.message-other .message-bubble {
  background-color: #f0f2f5;
  color: #303133;
}

.message-content {
  word-break: break-word;
  overflow-wrap: anywhere;
  line-height: 1.5;
  white-space: pre-wrap;
}

.message-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 4px;
}

.message-time {
  font-size: 11px;
  opacity: 0.7;
}

.read-status {
  font-size: 11px;
  opacity: 0.8;
}

.message-mine .read-status {
  opacity: 0.9;
}

.message-input {
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.empty-chat {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
