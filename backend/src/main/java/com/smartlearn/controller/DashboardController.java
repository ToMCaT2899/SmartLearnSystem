package com.smartlearn.controller;

import com.smartlearn.common.Result;
import com.smartlearn.security.SecurityUtil;
import com.smartlearn.service.DashboardService;
import com.smartlearn.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final SecurityUtil securityUtil;

    @GetMapping
    public Result<DashboardVO> getDashboard(@RequestParam(defaultValue = "week") String period) {
        Long userId = securityUtil.getCurrentUserId();
        String role = securityUtil.getCurrentUserRole();
        return Result.success(dashboardService.getDashboard(userId, role, period));
    }
}
