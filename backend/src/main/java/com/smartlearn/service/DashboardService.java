package com.smartlearn.service;

import com.smartlearn.vo.DashboardVO;

public interface DashboardService {
    DashboardVO getDashboard(Long userId, String role, String period);
}
