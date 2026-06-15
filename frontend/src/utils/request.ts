import axios from 'axios'
import type { AxiosInstance, AxiosResponse } from 'axios'
import type { ApiResult } from '@/types'
import { ElMessage } from 'element-plus'

const request: AxiosInstance = axios.create({
  baseURL: '/api/v1',
  timeout: 15000,
})

// Prevent multiple 401 error popups and redirects firing simultaneously
let isHandling401 = false

function handle401(message?: string) {
  localStorage.removeItem('token')
  localStorage.removeItem('user')

  if (isHandling401) return
  isHandling401 = true

  ElMessage.error(message || '登录已过期，请重新登录')

  // Use vue-router base path: /smartlearn/login
  const currentPath = window.location.pathname
  const loginPath = '/smartlearn/login'
  if (currentPath !== loginPath && currentPath !== '/smartlearn/register') {
    window.location.href = loginPath
  }
}

request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

request.interceptors.response.use(
  (response: AxiosResponse<ApiResult>) => {
    // Blob responses (e.g., file exports) — pass through
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response
    }
    const res = response.data
    if (res.code === 401) {
      handle401(res.message || '登录已过期')
      return Promise.reject(new Error(res.message || '登录已过期'))
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return response
  },
  (error) => {
    // Use backend error message when available
    const backendMsg = error.response?.data?.message
    if (error.response?.status === 401) {
      handle401(backendMsg || '登录已过期，请重新登录')
      return Promise.reject(error)
    }
    const msg = backendMsg || error.message || '网络错误，请确认后端服务已启动'
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default request
