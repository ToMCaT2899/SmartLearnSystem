package com.smartlearn.controller;

import com.smartlearn.common.PageResult;
import com.smartlearn.common.Result;
import com.smartlearn.entity.*;
import com.smartlearn.security.SecurityUtil;
import com.smartlearn.security.UserPrincipal;
import com.smartlearn.service.SafetyService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/safety")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;
    private final SecurityUtil securityUtil;

    @PostMapping("/late-returns")
    public Result<LateReturnRecord> registerLateReturn(@RequestBody LateReturnRecord record) {
        if (!securityUtil.isStudent() && !securityUtil.isTeacher()) {
            return Result.forbidden("仅学生和教师可登记晚归");
        }
        record.setStudentId(securityUtil.getCurrentUserId());
        return Result.success(safetyService.registerLateReturn(record));
    }

    @GetMapping("/late-returns")
    public Result<PageResult<LateReturnRecord>> getLateReturns(
            @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long queryId = resolveQueryId(studentId, principal);
        return Result.success(safetyService.getLateReturns(queryId, page, size));
    }

    @GetMapping("/late-returns/pending")
    public Result<PageResult<LateReturnRecord>> getPendingLateReturns(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可查看待审核记录");
        }
        return Result.success(safetyService.getPendingLateReturns(page, size));
    }

    @PutMapping("/late-returns/{id}/verify")
    public Result<Void> verifyLateReturn(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可审核");
        }
        String status = body != null ? (String) body.getOrDefault("status", "VERIFIED") : "VERIFIED";
        if (!"VERIFIED".equals(status) && !"REJECTED".equals(status)) {
            return Result.error("审核状态只能为 VERIFIED 或 REJECTED");
        }
        safetyService.verifyLateReturn(id, securityUtil.getCurrentUserId(), status);
        return Result.success();
    }

    @DeleteMapping("/late-returns/{id}")
    public Result<Void> deleteLateReturn(@PathVariable Long id) {
        safetyService.deleteLateReturn(id, securityUtil.getCurrentUserId());
        return Result.success("删除成功", null);
    }

    @PutMapping("/late-returns/{id}")
    public Result<Void> updateLateReturn(@PathVariable Long id, @RequestBody LateReturnRecord record) {
        record.setId(id);
        safetyService.updateLateReturn(record, securityUtil.getCurrentUserId());
        return Result.success("修改成功", null);
    }

    @PutMapping("/late-returns/{id}/cancel")
    public Result<Void> cancelLateReturn(@PathVariable Long id) {
        safetyService.cancelLateReturn(id, securityUtil.getCurrentUserId());
        return Result.success("已取消", null);
    }

    @PostMapping("/outing-reports")
    public Result<OutingReport> submitOutingReport(@RequestBody OutingReport report) {
        if (!securityUtil.isStudent() && !securityUtil.isTeacher()) {
            return Result.forbidden("仅学生和教师可提交外出报告");
        }
        report.setStudentId(securityUtil.getCurrentUserId());
        return Result.success(safetyService.submitOutingReport(report));
    }

    @GetMapping("/outing-reports")
    public Result<PageResult<OutingReport>> getOutingReports(
            @RequestParam(required = false) Long studentId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long queryId = resolveQueryId(studentId, principal);
        return Result.success(safetyService.getOutingReports(queryId, page, size));
    }

    @GetMapping("/outing-reports/pending")
    public Result<PageResult<OutingReport>> getPendingOutingReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可查看待审核报告");
        }
        return Result.success(safetyService.getPendingOutingReports(page, size));
    }

    @PutMapping("/outing-reports/{id}/review")
    public Result<Void> reviewOutingReport(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可审核");
        }
        String status = (String) body.get("status");
        if (status == null || (!"APPROVED".equals(status) && !"REJECTED".equals(status))) {
            return Result.error("审核状态只能为 APPROVED 或 REJECTED");
        }
        Long reviewerId = body.get("reviewerId") != null
                ? ((Number) body.get("reviewerId")).longValue()
                : securityUtil.getCurrentUserId();
        safetyService.reviewOutingReport(id, status, reviewerId);
        return Result.success();
    }

    @DeleteMapping("/outing-reports/{id}")
    public Result<Void> deleteOutingReport(@PathVariable Long id) {
        safetyService.deleteOutingReport(id, securityUtil.getCurrentUserId());
        return Result.success("删除成功", null);
    }

    @PutMapping("/outing-reports/{id}")
    public Result<Void> updateOutingReport(@PathVariable Long id, @RequestBody OutingReport report) {
        report.setId(id);
        safetyService.updateOutingReport(report, securityUtil.getCurrentUserId());
        return Result.success("修改成功", null);
    }

    @PutMapping("/outing-reports/{id}/cancel")
    public Result<Void> cancelOutingReport(@PathVariable Long id) {
        safetyService.cancelOutingReport(id, securityUtil.getCurrentUserId());
        return Result.success("已取消", null);
    }

    @PostMapping("/reports")
    public Result<SafetyReport> submitSafetyReport(@RequestBody SafetyReport report) {
        report.setReporterId(securityUtil.getCurrentUserId());
        return Result.success(safetyService.submitSafetyReport(report));
    }

    @GetMapping("/reports")
    public Result<PageResult<SafetyReport>> getSafetyReports(
            @RequestParam(required = false) Long reporterId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long queryId = resolveQueryId(reporterId, principal);
        return Result.success(safetyService.getSafetyReports(queryId, page, size));
    }

    @GetMapping("/reports/pending")
    public Result<PageResult<SafetyReport>> getPendingSafetyReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可查看待处理报告");
        }
        return Result.success(safetyService.getPendingSafetyReports(page, size));
    }

    @PutMapping("/reports/{id}/handle")
    public Result<Void> handleSafetyReport(@PathVariable Long id, @RequestBody(required = false) Map<String, Object> body) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可处理报告");
        }
        String status = body != null ? (String) body.getOrDefault("status", "RESOLVED") : "RESOLVED";
        if (!"RESOLVED".equals(status) && !"DISMISSED".equals(status)) {
            return Result.error("处理状态只能为 RESOLVED 或 DISMISSED");
        }
        safetyService.handleSafetyReport(id, securityUtil.getCurrentUserId(), status);
        return Result.success();
    }

    @DeleteMapping("/reports/{id}")
    public Result<Void> deleteSafetyReport(@PathVariable Long id) {
        safetyService.deleteSafetyReport(id, securityUtil.getCurrentUserId());
        return Result.success("删除成功", null);
    }

    @PutMapping("/reports/{id}")
    public Result<Void> updateSafetyReport(@PathVariable Long id, @RequestBody SafetyReport report) {
        report.setId(id);
        safetyService.updateSafetyReport(report, securityUtil.getCurrentUserId());
        return Result.success("修改成功", null);
    }

    @PutMapping("/reports/{id}/cancel")
    public Result<Void> cancelSafetyReport(@PathVariable Long id) {
        safetyService.cancelSafetyReport(id, securityUtil.getCurrentUserId());
        return Result.success("已取消", null);
    }

    private Long resolveQueryId(Long requestedId, UserPrincipal principal) {
        if (!securityUtil.isAdmin()) {
            return principal.getUserId();
        }
        return requestedId != null ? requestedId : principal.getUserId();
    }
}
