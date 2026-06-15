package com.smartlearn.controller;

import com.smartlearn.common.PageResult;
import com.smartlearn.common.Result;
import com.smartlearn.entity.*;
import com.smartlearn.security.SecurityUtil;
import com.smartlearn.security.UserPrincipal;
import com.smartlearn.service.CareerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/career")
@RequiredArgsConstructor
public class CareerController {

    private final CareerService careerService;
    private final SecurityUtil securityUtil;

    @GetMapping("/jobs")
    public Result<PageResult<JobInfo>> getJobs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(careerService.getJobs(keyword, location, page, size));
    }

    @GetMapping("/jobs/{id}")
    public Result<JobInfo> getJobById(@PathVariable Long id) {
        return Result.success(careerService.getJobById(id));
    }

    @PostMapping("/jobs")
    public Result<JobInfo> createJob(
            @Valid @RequestBody JobInfo job,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可发布岗位");
        }
        job.setPublisherId(principal.getUserId());
        return Result.success(careerService.createJob(job));
    }

    @PutMapping("/jobs/{id}")
    public Result<JobInfo> updateJob(@PathVariable Long id,
                                      @Valid @RequestBody JobInfo job,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可修改岗位");
        }
        job.setId(id);
        return Result.success(careerService.updateJob(job, principal.getUserId()));
    }

    @DeleteMapping("/jobs/{id}")
    public Result<Void> deleteJob(@PathVariable Long id,
                                   @AuthenticationPrincipal UserPrincipal principal) {
        careerService.deleteJob(id, principal.getUserId());
        return Result.success("删除成功", null);
    }

    @PutMapping("/subscription")
    public Result<JobSubscription> saveSubscription(
            @Valid @RequestBody JobSubscription subscription,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可设置订阅");
        }
        return Result.success(careerService.saveSubscription(principal.getUserId(), subscription));
    }

    @GetMapping("/subscription")
    public Result<JobSubscription> getSubscription(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(careerService.getSubscription(principal.getUserId()));
    }

    @DeleteMapping("/subscription")
    public Result<Void> deleteSubscription(@AuthenticationPrincipal UserPrincipal principal) {
        careerService.deleteSubscription(principal.getUserId());
        return Result.success("删除成功", null);
    }
}
