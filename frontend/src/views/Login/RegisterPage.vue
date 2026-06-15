<template>
  <div class="register-wrapper">
    <!-- Animated background -->
    <div class="register-bg">
      <div class="bg-shape bg-shape-1"></div>
      <div class="bg-shape bg-shape-2"></div>
      <div class="bg-shape bg-shape-3"></div>
    </div>

    <div class="register-card">
      <div class="card-header-section">
        <div class="logo-circle">
          <el-icon :size="36" color="#fff"><School /></el-icon>
        </div>
        <h1 class="register-title">创建账号</h1>
        <p class="register-subtitle">注册成为智慧学工系统用户</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" size="large">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item prop="username">
              <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" class="reg-input" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="realName">
              <el-input v-model="form.realName" placeholder="真实姓名" :prefix-icon="UserFilled" class="reg-input" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码（至少6位）" :prefix-icon="Lock" show-password class="reg-input" />
        </el-form-item>

        <el-form-item prop="role">
          <el-radio-group v-model="form.role" class="role-group">
            <el-radio-button label="STUDENT">
              <el-icon><User /></el-icon> 学生
            </el-radio-button>
            <el-radio-button label="TEACHER">
              <el-icon><UserFilled /></el-icon> 教师
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item prop="phone">
              <el-input v-model="form.phone" placeholder="手机号" :prefix-icon="Phone" class="reg-input" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item prop="email">
              <el-input v-model="form.email" placeholder="邮箱" :prefix-icon="Message" class="reg-input" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" :loading="loading" class="register-btn" @click="handleRegister">
            {{ loading ? '注册中...' : '注 册' }}
          </el-button>
          <el-button class="quick-fill-btn" @click="quickFill">
            <el-icon><MagicStick /></el-icon> 快速填充
          </el-button>
        </el-form-item>
      </el-form>

      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">立即登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/auth'
import type { RegisterForm } from '@/types'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Lock, Phone, Message, School, MagicStick } from '@element-plus/icons-vue'

const router = useRouter()
const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive<RegisterForm>({
  username: '',
  password: '',
  realName: '',
  role: 'STUDENT',
  phone: '',
  email: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不少于6位', trigger: 'blur' },
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  phone: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱', trigger: 'blur' }],
}

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await register(form)
    ElMessage.success('注册成功，请登录')
    router.push('/login')
  } catch {
    // error handled by interceptor
  } finally {
    loading.value = false
  }
}

function quickFill() {
  const ts = Date.now().toString().slice(-6)
  form.username = `test${ts}`
  form.password = '123456'
  form.realName = `测试用户${ts}`
  form.role = 'STUDENT'
  form.phone = `138${Math.random().toString().slice(2, 10)}`
  form.email = `test${ts}@test.com`
  ElMessage.success('已填充测试数据，点击注册即可')
}
</script>

<style scoped>
.register-wrapper {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%);
  position: relative;
  overflow: hidden;
}

/* Animated background */
.register-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-shape {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
  animation: floatBg 20s infinite ease-in-out;
}

.bg-shape-1 {
  width: 600px;
  height: 600px;
  background: linear-gradient(135deg, #43e97b, #38f9d7);
  top: -200px;
  left: -100px;
}

.bg-shape-2 {
  width: 500px;
  height: 500px;
  background: linear-gradient(135deg, #667eea, #764ba2);
  bottom: -150px;
  right: -100px;
  animation-delay: -7s;
}

.bg-shape-3 {
  width: 400px;
  height: 400px;
  background: linear-gradient(135deg, #f093fb, #f5576c);
  top: 40%;
  left: 50%;
  animation-delay: -14s;
}

@keyframes floatBg {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(-30px, -20px) scale(1.02); }
}

/* Card */
.register-card {
  width: 500px;
  padding: 40px 40px 32px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.3);
  position: relative;
  z-index: 1;
  backdrop-filter: blur(10px);
}

.card-header-section {
  text-align: center;
  margin-bottom: 28px;
}

.logo-circle {
  width: 60px;
  height: 60px;
  border-radius: 16px;
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 14px;
  box-shadow: 0 8px 24px rgba(67, 233, 123, 0.4);
}

.register-title {
  font-size: 22px;
  color: #1d2129;
  margin: 0 0 6px;
  font-weight: 700;
}

.register-subtitle {
  font-size: 13px;
  color: #86909c;
  margin: 0;
}

.reg-input :deep(.el-input__wrapper) {
  background: #f7f8fa;
  border-radius: 10px;
  box-shadow: none;
}

.reg-input :deep(.el-input__wrapper):hover {
  background: #f0f1f5;
}

.reg-input :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px #43e97b inset;
}

.role-group {
  width: 100%;
}

.role-group :deep(.el-radio-button) {
  flex: 1;
}

.role-group :deep(.el-radio-button__inner) {
  width: 100%;
  border-radius: 10px;
}

.register-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 10px;
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
  border: none;
  margin-top: 4px;
  transition: all 0.3s;
}

.register-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(67, 233, 123, 0.4);
}

.register-footer {
  text-align: center;
  font-size: 14px;
  color: #86909c;
  margin-top: 20px;
}

.register-footer a {
  color: #43e97b;
  margin-left: 4px;
  font-weight: 500;
  text-decoration: none;
}

.register-footer a:hover {
  text-decoration: underline;
}

.quick-fill-btn {
  width: 100%;
  height: 38px;
  margin-top: 8px;
  border-radius: 10px;
  border: 1px dashed #43e97b;
  color: #43e97b;
  background: transparent;
  transition: all 0.3s;
}

.quick-fill-btn:hover {
  background: rgba(67, 233, 123, 0.08);
  border-color: #38f9d7;
  color: #38f9d7;
}
</style>
