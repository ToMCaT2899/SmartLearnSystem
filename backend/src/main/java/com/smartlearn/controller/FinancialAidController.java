package com.smartlearn.controller;

import com.smartlearn.common.PageResult;
import com.smartlearn.common.Result;
import com.smartlearn.entity.*;
import com.smartlearn.security.SecurityUtil;
import com.smartlearn.security.UserPrincipal;
import com.smartlearn.service.FinancialAidService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/financial-aid")
@RequiredArgsConstructor
public class FinancialAidController {

    private final FinancialAidService financialAidService;
    private final SecurityUtil securityUtil;

    // ==================== Scholarships ====================

    @GetMapping("/scholarships")
    public Result<PageResult<Scholarship>> listScholarships(@RequestParam(defaultValue = "1") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return Result.success(financialAidService.listScholarships(page, size));
    }

    @GetMapping("/scholarships/{id}")
    public Result<Scholarship> getScholarship(@PathVariable Long id) {
        return Result.success(financialAidService.getScholarshipById(id));
    }

    @PostMapping("/scholarships")
    public Result<Scholarship> createScholarship(@Valid @RequestBody Scholarship scholarship,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可发布奖学金");
        }
        scholarship.setPublisherId(principal.getUserId());
        return Result.success(financialAidService.createScholarship(scholarship));
    }

    @PutMapping("/scholarships/{id}")
    public Result<Scholarship> updateScholarship(@PathVariable Long id,
                                                  @Valid @RequestBody Scholarship scholarship,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可修改奖学金");
        }
        scholarship.setId(id);
        return Result.success(financialAidService.updateScholarship(scholarship, principal.getUserId()));
    }

    @DeleteMapping("/scholarships/{id}")
    public Result<Void> deleteScholarship(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        financialAidService.deleteScholarship(id, principal.getUserId());
        return Result.success("删除成功", null);
    }

    @PostMapping("/scholarships/{id}/apply")
    public Result<Void> applyScholarship(@PathVariable Long id,
                                          @RequestBody Map<String, Object> body) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可申请奖学金");
        }
        Long userId = securityUtil.getCurrentUserId();
        String reason = (String) body.getOrDefault("reason", "");
        List<String> fileUrlList = (List<String>) body.get("fileUrls");
        String fileUrls = fileUrlList != null ? String.join(",", fileUrlList) : "";
        financialAidService.applyScholarship(id, userId, reason, fileUrls);
        return Result.success("申请已提交", null);
    }

    @PutMapping("/scholarships/applications/{id}/review")
    public Result<Void> reviewScholarshipApplication(@PathVariable Long id,
                                                      @RequestBody Map<String, String> body,
                                                      @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可审核");
        }
        String status = body.get("status");
        if (status == null || (!"APPROVED".equals(status) && !"REJECTED".equals(status))) {
            return Result.error("审核状态只能为 APPROVED 或 REJECTED");
        }
        String comment = body.get("comment");
        financialAidService.reviewScholarshipApplication(id, status, comment, principal.getUserId());
        return Result.success("审核完成", null);
    }

    @PutMapping("/scholarships/applications/{id}/cancel")
    public Result<Void> cancelScholarshipApplication(@PathVariable Long id) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可取消申请");
        }
        Long userId = securityUtil.getCurrentUserId();
        financialAidService.cancelScholarshipApplication(id, userId);
        return Result.success("申请已取消", null);
    }

    @PutMapping("/scholarships/applications/{id}")
    public Result<Void> editScholarshipApplication(@PathVariable Long id,
                                                   @RequestBody Map<String, Object> body) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可编辑申请");
        }
        Long userId = securityUtil.getCurrentUserId();
        String reason = (String) body.getOrDefault("reason", "");
        List<String> fileUrlList = (List<String>) body.get("fileUrls");
        String fileUrls = fileUrlList != null ? String.join(",", fileUrlList) : "";
        financialAidService.editScholarshipApplication(id, userId, reason, fileUrls);
        return Result.success("申请已更新", null);
    }

    @GetMapping("/scholarships/{id}/applications")
    public Result<PageResult<Map<String, Object>>> getScholarshipApplications(@PathVariable Long id,
                                                                               @RequestParam(defaultValue = "1") int page,
                                                                               @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可查看");
        }
        return Result.success(financialAidService.getScholarshipApplications(id, page, size));
    }

    // ==================== Grants ====================

    @GetMapping("/grants")
    public Result<PageResult<GrantInfo>> listGrants(@RequestParam(defaultValue = "1") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        return Result.success(financialAidService.listGrants(page, size));
    }

    @GetMapping("/grants/{id}")
    public Result<GrantInfo> getGrant(@PathVariable Long id) {
        return Result.success(financialAidService.getGrantById(id));
    }

    @PostMapping("/grants")
    public Result<GrantInfo> createGrant(@Valid @RequestBody GrantInfo grant,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可发布助学金");
        }
        grant.setPublisherId(principal.getUserId());
        return Result.success(financialAidService.createGrant(grant));
    }

    @PutMapping("/grants/{id}")
    public Result<GrantInfo> updateGrant(@PathVariable Long id,
                                          @Valid @RequestBody GrantInfo grant,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可修改助学金");
        }
        grant.setId(id);
        return Result.success(financialAidService.updateGrant(grant, principal.getUserId()));
    }

    @DeleteMapping("/grants/{id}")
    public Result<Void> deleteGrant(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        financialAidService.deleteGrant(id, principal.getUserId());
        return Result.success("删除成功", null);
    }

    @PostMapping("/grants/{id}/apply")
    public Result<Void> applyGrant(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可申请助学金");
        }
        Long userId = securityUtil.getCurrentUserId();
        String reason = (String) body.getOrDefault("reason", "");
        List<String> fileUrlList = (List<String>) body.get("fileUrls");
        String fileUrls = fileUrlList != null ? String.join(",", fileUrlList) : "";
        financialAidService.applyGrant(id, userId, reason, fileUrls);
        return Result.success("申请已提交", null);
    }

    @PutMapping("/grants/applications/{id}/review")
    public Result<Void> reviewGrantApplication(@PathVariable Long id,
                                                @RequestBody Map<String, String> body,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可审核");
        }
        String status = body.get("status");
        if (status == null || (!"APPROVED".equals(status) && !"REJECTED".equals(status))) {
            return Result.error("审核状态只能为 APPROVED 或 REJECTED");
        }
        String comment = body.get("comment");
        financialAidService.reviewGrantApplication(id, status, comment, principal.getUserId());
        return Result.success("审核完成", null);
    }

    @PutMapping("/grants/applications/{id}/cancel")
    public Result<Void> cancelGrantApplication(@PathVariable Long id) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可取消申请");
        }
        Long userId = securityUtil.getCurrentUserId();
        financialAidService.cancelGrantApplication(id, userId);
        return Result.success("申请已取消", null);
    }

    @PutMapping("/grants/applications/{id}")
    public Result<Void> editGrantApplication(@PathVariable Long id,
                                              @RequestBody Map<String, Object> body) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可编辑申请");
        }
        Long userId = securityUtil.getCurrentUserId();
        String reason = (String) body.getOrDefault("reason", "");
        List<String> fileUrlList = (List<String>) body.get("fileUrls");
        String fileUrls = fileUrlList != null ? String.join(",", fileUrlList) : "";
        financialAidService.editGrantApplication(id, userId, reason, fileUrls);
        return Result.success("申请已更新", null);
    }

    @GetMapping("/grants/{id}/applications")
    public Result<PageResult<Map<String, Object>>> getGrantApplications(@PathVariable Long id,
                                                                         @RequestParam(defaultValue = "1") int page,
                                                                         @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可查看");
        }
        return Result.success(financialAidService.getGrantApplications(id, page, size));
    }

    // ==================== Work-Study Jobs ====================

    @GetMapping("/work-study")
    public Result<PageResult<WorkStudyJob>> listWorkStudyJobs(@RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return Result.success(financialAidService.listWorkStudyJobs(page, size));
    }

    @GetMapping("/work-study/{id}")
    public Result<WorkStudyJob> getWorkStudyJob(@PathVariable Long id) {
        return Result.success(financialAidService.getWorkStudyJobById(id));
    }

    @PostMapping("/work-study")
    public Result<WorkStudyJob> createWorkStudyJob(@Valid @RequestBody WorkStudyJob job,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可发布勤工助学岗位");
        }
        job.setPublisherId(principal.getUserId());
        return Result.success(financialAidService.createWorkStudyJob(job));
    }

    @PutMapping("/work-study/{id}")
    public Result<WorkStudyJob> updateWorkStudyJob(@PathVariable Long id,
                                                    @Valid @RequestBody WorkStudyJob job,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可修改岗位");
        }
        job.setId(id);
        return Result.success(financialAidService.updateWorkStudyJob(job, principal.getUserId()));
    }

    @DeleteMapping("/work-study/{id}")
    public Result<Void> deleteWorkStudyJob(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        financialAidService.deleteWorkStudyJob(id, principal.getUserId());
        return Result.success("删除成功", null);
    }

    @PostMapping("/work-study/{id}/apply")
    public Result<Void> applyWorkStudyJob(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可申请勤工助学岗位");
        }
        Long userId = securityUtil.getCurrentUserId();
        String resume = (String) body.getOrDefault("resume", "");
        String fileUrls = body.containsKey("fileUrls") ? String.join(",", (List<String>) body.get("fileUrls")) : "";
        String combinedResume = resume;
        if (!fileUrls.isEmpty()) {
            combinedResume = resume + (resume.isEmpty() ? "" : "\n") + "附件: " + fileUrls;
        }
        financialAidService.applyWorkStudyJob(id, userId, combinedResume);
        return Result.success("申请已提交", null);
    }

    @PutMapping("/work-study/applications/{id}/review")
    public Result<Void> reviewWorkStudyApplication(@PathVariable Long id,
                                                    @RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可审核");
        }
        String status = body.get("status");
        if (status == null || (!"APPROVED".equals(status) && !"REJECTED".equals(status))) {
            return Result.error("审核状态只能为 APPROVED 或 REJECTED");
        }
        String comment = body.get("comment");
        financialAidService.reviewWorkStudyApplication(id, status, comment, principal.getUserId());
        return Result.success("审核完成", null);
    }

    @PutMapping("/work-study/applications/{id}/cancel")
    public Result<Void> cancelWorkStudyApplication(@PathVariable Long id) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可取消申请");
        }
        Long userId = securityUtil.getCurrentUserId();
        financialAidService.cancelWorkStudyApplication(id, userId);
        return Result.success("申请已取消", null);
    }

    @PutMapping("/work-study/applications/{id}")
    public Result<Void> editWorkStudyApplication(@PathVariable Long id,
                                                  @RequestBody Map<String, Object> body) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可编辑申请");
        }
        Long userId = securityUtil.getCurrentUserId();
        String resume = (String) body.getOrDefault("resume", "");
        String fileUrls = body.containsKey("fileUrls") ? String.join(",", (List<String>) body.get("fileUrls")) : "";
        String combinedResume = resume;
        if (!fileUrls.isEmpty()) {
            combinedResume = resume + (resume.isEmpty() ? "" : "\n") + "附件: " + fileUrls;
        }
        financialAidService.editWorkStudyApplication(id, userId, combinedResume);
        return Result.success("申请已更新", null);
    }

    @GetMapping("/work-study/{id}/applications")
    public Result<PageResult<Map<String, Object>>> getWorkStudyApplications(@PathVariable Long id,
                                                                              @RequestParam(defaultValue = "1") int page,
                                                                              @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可查看");
        }
        return Result.success(financialAidService.getWorkStudyApplications(id, page, size));
    }

    // ==================== Cross-cutting ====================

    @GetMapping("/my-applications")
    public Result<Map<String, PageResult<?>>> getMyApplications(@RequestParam(defaultValue = "1") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(financialAidService.getMyApplications(userId, page, size));
    }

    @GetMapping("/review-queue")
    public Result<Map<String, PageResult<?>>> getReviewQueue(@AuthenticationPrincipal UserPrincipal principal,
                                                               @RequestParam(defaultValue = "1") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可查看审核队列");
        }
        return Result.success(financialAidService.getReviewQueue(principal.getUserId(), page, size));
    }

    @GetMapping("/admitted-list")
    public Result<PageResult<Map<String, Object>>> getAdmittedList(@RequestParam(defaultValue = "1") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可查看录取名单");
        }
        return Result.success(financialAidService.getAdmittedList(page, size));
    }

    // ==================== Export ====================

    @GetMapping("/scholarships/{id}/applications/export")
    public ResponseEntity<byte[]> exportScholarshipApplications(@PathVariable Long id) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            throw new AccessDeniedException("仅教师和管理员可导出");
        }
        byte[] data = financialAidService.exportScholarshipApplications(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + URLEncoder.encode("奖学金申请名单.xlsx", StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/grants/{id}/applications/export")
    public ResponseEntity<byte[]> exportGrantApplications(@PathVariable Long id) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            throw new AccessDeniedException("仅教师和管理员可导出");
        }
        byte[] data = financialAidService.exportGrantApplications(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + URLEncoder.encode("助学金申请名单.xlsx", StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @GetMapping("/work-study/{id}/applications/export")
    public ResponseEntity<byte[]> exportWorkStudyApplications(@PathVariable Long id) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            throw new AccessDeniedException("仅教师和管理员可导出");
        }
        byte[] data = financialAidService.exportWorkStudyApplications(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + URLEncoder.encode("勤工助学申请名单.xlsx", StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

}
