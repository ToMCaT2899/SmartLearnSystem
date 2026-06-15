import request from '@/utils/request'
import type { ApiResult } from '@/types'

export interface StatCard {
  title: string
  value: string
  icon: string
  gradient: string
  trend?: number
}

export interface ChartData {
  days: string[]
  vals: number[]
}

export interface PieItem {
  value: number
  name: string
}

export interface DashboardData {
  stats: StatCard[]
  barChart: ChartData
  pieChart: PieItem[]
}

export function getDashboard(period: string = 'week') {
  return request.get<ApiResult<DashboardData>>('/dashboard', { params: { period } })
}
