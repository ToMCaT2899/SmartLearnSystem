package com.smartlearn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import java.util.ArrayList;
import com.smartlearn.service.FinancialAidService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinancialAidServiceImpl implements FinancialAidService {

    private final ScholarshipMapper scholarshipMapper;
    private final ScholarshipApplicationMapper scholarshipApplicationMapper;
    private final GrantInfoMapper grantInfoMapper;
    private final GrantApplicationMapper grantApplicationMapper;
    private final WorkStudyJobMapper workStudyJobMapper;
    private final WorkStudyApplicationMapper workStudyApplicationMapper;
    private final SysUserMapper sysUserMapper;

    // ==================== Scholarships ====================

    @Override
    public PageResult<Scholarship> listScholarships(int page, int size) {
        Page<Scholarship> p = new Page<>(page, size);
        IPage<Scholarship> result = scholarshipMapper.selectPage(p,
                new LambdaQueryWrapper<Scholarship>()
                        .orderByDesc(Scholarship::getCreatedAt));
        for (Scholarship s : result.getRecords()) {
            if (s.getPublisherId() != null) {
                SysUser publisher = sysUserMapper.selectById(s.getPublisherId());
                if (publisher != null) {
                    s.setPublisherName(publisher.getRealName());
                    s.setPublisherRole(publisher.getRole());
                }
            }
            Long count = scholarshipApplicationMapper.selectCount(
                    new LambdaQueryWrapper<ScholarshipApplication>()
                            .eq(ScholarshipApplication::getScholarshipId, s.getId()));
            s.setApplicationCount(count.intValue());
            Long admitted = scholarshipApplicationMapper.selectCount(
                    new LambdaQueryWrapper<ScholarshipApplication>()
                            .eq(ScholarshipApplication::getScholarshipId, s.getId())
                            .eq(ScholarshipApplication::getStatus, "APPROVED"));
            s.setAdmittedCount(admitted.intValue());
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public Scholarship getScholarshipById(Long id) {
        return scholarshipMapper.selectById(id);
    }

    @Override
    @Transactional
    public Scholarship createScholarship(Scholarship scholarship) {
        scholarship.setId(null);
        scholarship.setStatus("OPEN");
        scholarship.setCreatedAt(LocalDateTime.now());
        scholarshipMapper.insert(scholarship);

        return scholarship;
    }

    @Override
    public Scholarship updateScholarship(Scholarship scholarship, Long userId) {
        Scholarship existing = scholarshipMapper.selectById(scholarship.getId());
        if (existing == null) {
            throw new IllegalArgumentException("奖学金不存在");
        }
        checkModifyPermission(existing.getPublisherId(), userId);
        scholarshipMapper.updateById(scholarship);
        return scholarship;
    }

    @Override
    @Transactional
    public void deleteScholarship(Long id, Long userId) {
        Scholarship item = scholarshipMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("奖学金不存在");
        }
        checkModifyPermission(item.getPublisherId(), userId);
        scholarshipApplicationMapper.delete(
                new LambdaQueryWrapper<ScholarshipApplication>()
                        .eq(ScholarshipApplication::getScholarshipId, id));
        scholarshipMapper.deleteById(id);
    }

    @Override
    public void applyScholarship(Long scholarshipId, Long studentId, String reason, String files) {
        checkIsStudent(studentId);
        Scholarship scholarship = scholarshipMapper.selectById(scholarshipId);
        if (scholarship == null) {
            throw new IllegalArgumentException("奖学金不存在");
        }
        if (!"OPEN".equals(scholarship.getStatus())) {
            throw new IllegalArgumentException("该奖学金已关闭申请");
        }
        if (scholarship.getDeadline() != null && scholarship.getDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("该奖学金截止日期已过（" + scholarship.getDeadline() + "），无法申请");
        }
        // 检查是否已有申请（待审核或已通过）
        Long count = scholarshipApplicationMapper.selectCount(
                new LambdaQueryWrapper<ScholarshipApplication>()
                        .eq(ScholarshipApplication::getScholarshipId, scholarshipId)
                        .eq(ScholarshipApplication::getStudentId, studentId)
                        .in(ScholarshipApplication::getStatus, "PENDING", "APPROVED")
        );
        if (count > 0) {
            throw new IllegalArgumentException("您已申请过该奖学金，请勿重复申请");
        }

        ScholarshipApplication application = new ScholarshipApplication();
        application.setScholarshipId(scholarshipId);
        application.setStudentId(studentId);
        application.setApplyReason(reason);
        application.setSupportingFiles(files);
        application.setStatus("PENDING");
        application.setApplyTime(LocalDateTime.now());
        scholarshipApplicationMapper.insert(application);
    }

    @Override
    @Transactional
    public void reviewScholarshipApplication(Long applicationId, String status, String comment, Long reviewerId) {
        ScholarshipApplication application = scholarshipApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!"PENDING".equals(application.getStatus())) {
            throw new IllegalArgumentException("该申请已被审核，无法重复审核");
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("审核状态只能为 APPROVED 或 REJECTED");
        }
        Scholarship scholarship = scholarshipMapper.selectById(application.getScholarshipId());
        if (scholarship == null) {
            throw new IllegalArgumentException("奖学金不存在");
        }
        if (!scholarship.getPublisherId().equals(reviewerId) && !isAdmin(reviewerId)) {
            throw new IllegalArgumentException("只能审核自己发布的奖学金申请");
        }
        if (scholarship.getDeadline() != null && scholarship.getDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("该奖学金已过截止日期（" + scholarship.getDeadline() + "），无法审核");
        }
        if ("APPROVED".equals(status)) {
            Long approvedCount = scholarshipApplicationMapper.selectCount(
                    new LambdaQueryWrapper<ScholarshipApplication>()
                            .eq(ScholarshipApplication::getScholarshipId, application.getScholarshipId())
                            .eq(ScholarshipApplication::getStatus, "APPROVED"));
            if (scholarship.getQuota() != null && approvedCount >= scholarship.getQuota()) {
                throw new IllegalArgumentException("该奖学金名额已满（" + scholarship.getQuota() + "人），无法继续通过");
            }
        }
        application.setStatus(status);
        application.setReviewComment(comment);
        application.setReviewerId(reviewerId);
        application.setReviewTime(LocalDateTime.now());
        scholarshipApplicationMapper.updateById(application);
    }

    @Override
    public void cancelScholarshipApplication(Long applicationId, Long studentId) {
        ScholarshipApplication app = scholarshipApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!app.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("只能取消自己的申请");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalArgumentException("只能取消待审核的申请");
        }
        app.setStatus("CANCELLED");
        app.setReviewComment("学生自行取消");
        app.setReviewTime(LocalDateTime.now());
        scholarshipApplicationMapper.updateById(app);
    }

    @Override
    public void editScholarshipApplication(Long applicationId, Long studentId, String reason, String files) {
        ScholarshipApplication app = scholarshipApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!app.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("只能编辑自己的申请");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalArgumentException("只能编辑待审核的申请");
        }
        app.setApplyReason(reason);
        if (files != null && !files.isEmpty()) {
            String existing = app.getSupportingFiles();
            app.setSupportingFiles((existing != null && !existing.isEmpty()) ? existing + "," + files : files);
        }
        scholarshipApplicationMapper.updateById(app);
    }

    @Override
    public PageResult<Map<String, Object>> getScholarshipApplications(Long scholarshipId, int page, int size) {
        Page<ScholarshipApplication> p = new Page<>(page, size);
        IPage<ScholarshipApplication> pageResult = scholarshipApplicationMapper.selectPage(p,
                new LambdaQueryWrapper<ScholarshipApplication>()
                        .eq(ScholarshipApplication::getScholarshipId, scholarshipId)
                        .orderByDesc(ScholarshipApplication::getApplyTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (ScholarshipApplication app : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("scholarshipId", app.getScholarshipId());
            item.put("studentId", app.getStudentId());
            item.put("applyReason", app.getApplyReason());
            item.put("supportingFiles", app.getSupportingFiles());
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewComment", app.getReviewComment());
            item.put("reviewTime", app.getReviewTime());
            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            result.add(item);
        }
        return new PageResult<>(result, pageResult.getTotal(), page, size);
    }

    // ==================== Grants ====================

    @Override
    public PageResult<GrantInfo> listGrants(int page, int size) {
        Page<GrantInfo> p = new Page<>(page, size);
        IPage<GrantInfo> result = grantInfoMapper.selectPage(p,
                new LambdaQueryWrapper<GrantInfo>()
                        .orderByDesc(GrantInfo::getCreatedAt));
        for (GrantInfo g : result.getRecords()) {
            if (g.getPublisherId() != null) {
                SysUser publisher = sysUserMapper.selectById(g.getPublisherId());
                if (publisher != null) {
                    g.setPublisherName(publisher.getRealName());
                    g.setPublisherRole(publisher.getRole());
                }
            }
            Long count = grantApplicationMapper.selectCount(
                    new LambdaQueryWrapper<GrantApplication>()
                            .eq(GrantApplication::getGrantId, g.getId()));
            g.setApplicationCount(count.intValue());
            Long admitted = grantApplicationMapper.selectCount(
                    new LambdaQueryWrapper<GrantApplication>()
                            .eq(GrantApplication::getGrantId, g.getId())
                            .eq(GrantApplication::getStatus, "APPROVED"));
            g.setAdmittedCount(admitted.intValue());
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public GrantInfo getGrantById(Long id) {
        return grantInfoMapper.selectById(id);
    }

    @Override
    @Transactional
    public GrantInfo createGrant(GrantInfo grant) {
        grant.setId(null);
        grant.setStatus("OPEN");
        grant.setCreatedAt(LocalDateTime.now());
        grantInfoMapper.insert(grant);

        return grant;
    }

    @Override
    public GrantInfo updateGrant(GrantInfo grant, Long userId) {
        GrantInfo existing = grantInfoMapper.selectById(grant.getId());
        if (existing == null) {
            throw new IllegalArgumentException("助学金不存在");
        }
        checkModifyPermission(existing.getPublisherId(), userId);
        grantInfoMapper.updateById(grant);
        return grant;
    }

    @Override
    @Transactional
    public void deleteGrant(Long id, Long userId) {
        GrantInfo item = grantInfoMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("助学金不存在");
        }
        checkModifyPermission(item.getPublisherId(), userId);
        grantApplicationMapper.delete(
                new LambdaQueryWrapper<GrantApplication>()
                        .eq(GrantApplication::getGrantId, id));
        grantInfoMapper.deleteById(id);
    }

    @Override
    public void applyGrant(Long grantId, Long studentId, String reason, String files) {
        checkIsStudent(studentId);
        GrantInfo grant = grantInfoMapper.selectById(grantId);
        if (grant == null) {
            throw new IllegalArgumentException("助学金不存在");
        }
        if (!"OPEN".equals(grant.getStatus())) {
            throw new IllegalArgumentException("该助学金已关闭申请");
        }
        if (grant.getDeadline() != null && grant.getDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("该助学金截止日期已过（" + grant.getDeadline() + "），无法申请");
        }
        // 检查是否已有申请（待审核或已通过）
        Long count = grantApplicationMapper.selectCount(
                new LambdaQueryWrapper<GrantApplication>()
                        .eq(GrantApplication::getGrantId, grantId)
                        .eq(GrantApplication::getStudentId, studentId)
                        .in(GrantApplication::getStatus, "PENDING", "APPROVED")
        );
        if (count > 0) {
            throw new IllegalArgumentException("您已申请过该助学金，请勿重复申请");
        }

        GrantApplication application = new GrantApplication();
        application.setGrantId(grantId);
        application.setStudentId(studentId);
        application.setApplyReason(reason);
        application.setSupportingFiles(files);
        application.setStatus("PENDING");
        application.setApplyTime(LocalDateTime.now());
        grantApplicationMapper.insert(application);
    }

    @Override
    @Transactional
    public void reviewGrantApplication(Long applicationId, String status, String comment, Long reviewerId) {
        GrantApplication application = grantApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!"PENDING".equals(application.getStatus())) {
            throw new IllegalArgumentException("该申请已被审核，无法重复审核");
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("审核状态只能为 APPROVED 或 REJECTED");
        }
        GrantInfo grant = grantInfoMapper.selectById(application.getGrantId());
        if (grant == null) {
            throw new IllegalArgumentException("助学金不存在");
        }
        if (!grant.getPublisherId().equals(reviewerId) && !isAdmin(reviewerId)) {
            throw new IllegalArgumentException("只能审核自己发布的助学金申请");
        }
        if (grant.getDeadline() != null && grant.getDeadline().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("该助学金已过截止日期（" + grant.getDeadline() + "），无法审核");
        }
        if ("APPROVED".equals(status)) {
            Long approvedCount = grantApplicationMapper.selectCount(
                    new LambdaQueryWrapper<GrantApplication>()
                            .eq(GrantApplication::getGrantId, application.getGrantId())
                            .eq(GrantApplication::getStatus, "APPROVED"));
            if (grant.getQuota() != null && approvedCount >= grant.getQuota()) {
                throw new IllegalArgumentException("该助学金名额已满（" + grant.getQuota() + "人），无法继续通过");
            }
        }
        application.setStatus(status);
        application.setReviewComment(comment);
        application.setReviewerId(reviewerId);
        application.setReviewTime(LocalDateTime.now());
        grantApplicationMapper.updateById(application);
    }

    @Override
    public void cancelGrantApplication(Long applicationId, Long studentId) {
        GrantApplication app = grantApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!app.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("只能取消自己的申请");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalArgumentException("只能取消待审核的申请");
        }
        app.setStatus("CANCELLED");
        app.setReviewComment("学生自行取消");
        app.setReviewTime(LocalDateTime.now());
        grantApplicationMapper.updateById(app);
    }

    @Override
    public void editGrantApplication(Long applicationId, Long studentId, String reason, String files) {
        GrantApplication app = grantApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!app.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("只能编辑自己的申请");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalArgumentException("只能编辑待审核的申请");
        }
        app.setApplyReason(reason);
        if (files != null && !files.isEmpty()) {
            String existing = app.getSupportingFiles();
            app.setSupportingFiles((existing != null && !existing.isEmpty()) ? existing + "," + files : files);
        }
        grantApplicationMapper.updateById(app);
    }

    @Override
    public PageResult<Map<String, Object>> getGrantApplications(Long grantId, int page, int size) {
        Page<GrantApplication> p = new Page<>(page, size);
        IPage<GrantApplication> pageResult = grantApplicationMapper.selectPage(p,
                new LambdaQueryWrapper<GrantApplication>()
                        .eq(GrantApplication::getGrantId, grantId)
                        .orderByDesc(GrantApplication::getApplyTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (GrantApplication app : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("grantId", app.getGrantId());
            item.put("studentId", app.getStudentId());
            item.put("applyReason", app.getApplyReason());
            item.put("supportingFiles", app.getSupportingFiles());
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewComment", app.getReviewComment());
            item.put("reviewTime", app.getReviewTime());
            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            result.add(item);
        }
        return new PageResult<>(result, pageResult.getTotal(), page, size);
    }

    // ==================== Work-Study Jobs ====================

    @Override
    public PageResult<WorkStudyJob> listWorkStudyJobs(int page, int size) {
        Page<WorkStudyJob> p = new Page<>(page, size);
        IPage<WorkStudyJob> result = workStudyJobMapper.selectPage(p,
                new LambdaQueryWrapper<WorkStudyJob>()
                        .orderByDesc(WorkStudyJob::getCreatedAt));
        for (WorkStudyJob j : result.getRecords()) {
            if (j.getPublisherId() != null) {
                SysUser publisher = sysUserMapper.selectById(j.getPublisherId());
                if (publisher != null) {
                    j.setPublisherName(publisher.getRealName());
                    j.setPublisherRole(publisher.getRole());
                }
            }
            Long count = workStudyApplicationMapper.selectCount(
                    new LambdaQueryWrapper<WorkStudyApplication>()
                            .eq(WorkStudyApplication::getJobId, j.getId()));
            j.setApplicationCount(count.intValue());
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public WorkStudyJob getWorkStudyJobById(Long id) {
        return workStudyJobMapper.selectById(id);
    }

    @Override
    @Transactional
    public WorkStudyJob createWorkStudyJob(WorkStudyJob job) {
        job.setId(null);
        job.setEnrolledCount(0);
        job.setStatus("OPEN");
        job.setCreatedAt(LocalDateTime.now());
        workStudyJobMapper.insert(job);

        return job;
    }

    @Override
    public WorkStudyJob updateWorkStudyJob(WorkStudyJob job, Long userId) {
        WorkStudyJob existing = workStudyJobMapper.selectById(job.getId());
        if (existing == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        checkModifyPermission(existing.getPublisherId(), userId);
        workStudyJobMapper.updateById(job);
        return job;
    }

    @Override
    @Transactional
    public void deleteWorkStudyJob(Long id, Long userId) {
        WorkStudyJob item = workStudyJobMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        checkModifyPermission(item.getPublisherId(), userId);
        workStudyApplicationMapper.delete(
                new LambdaQueryWrapper<WorkStudyApplication>()
                        .eq(WorkStudyApplication::getJobId, id));
        workStudyJobMapper.deleteById(id);
    }

    @Override
    public void applyWorkStudyJob(Long jobId, Long studentId, String resume) {
        checkIsStudent(studentId);
        WorkStudyJob job = workStudyJobMapper.selectById(jobId);
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        if (!"OPEN".equals(job.getStatus())) {
            throw new IllegalArgumentException("该岗位已关闭申请");
        }
        // 检查是否已有申请（待审核或已通过）
        Long count = workStudyApplicationMapper.selectCount(
                new LambdaQueryWrapper<WorkStudyApplication>()
                        .eq(WorkStudyApplication::getJobId, jobId)
                        .eq(WorkStudyApplication::getStudentId, studentId)
                        .in(WorkStudyApplication::getStatus, "PENDING", "APPROVED")
        );
        if (count > 0) {
            throw new IllegalArgumentException("您已申请过该岗位，请勿重复申请");
        }

        WorkStudyApplication application = new WorkStudyApplication();
        application.setJobId(jobId);
        application.setStudentId(studentId);
        application.setResume(resume);
        application.setStatus("PENDING");
        application.setApplyTime(LocalDateTime.now());
        workStudyApplicationMapper.insert(application);
    }

    @Override
    @Transactional
    public void reviewWorkStudyApplication(Long applicationId, String status, String comment, Long reviewerId) {
        WorkStudyApplication application = workStudyApplicationMapper.selectById(applicationId);
        if (application == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!"PENDING".equals(application.getStatus())) {
            throw new IllegalArgumentException("该申请已被审核，无法重复审核");
        }
        if (!"APPROVED".equals(status) && !"REJECTED".equals(status)) {
            throw new IllegalArgumentException("审核状态只能为 APPROVED 或 REJECTED");
        }
        WorkStudyJob job = workStudyJobMapper.selectById(application.getJobId());
        if (job == null) {
            throw new IllegalArgumentException("岗位不存在");
        }
        if (!job.getPublisherId().equals(reviewerId) && !isAdmin(reviewerId)) {
            throw new IllegalArgumentException("只能审核自己发布的岗位申请");
        }
        if ("APPROVED".equals(status)) {
            int enrolled = job.getEnrolledCount() != null ? job.getEnrolledCount() : 0;
            if (job.getQuota() != null && enrolled >= job.getQuota()) {
                throw new IllegalArgumentException("该岗位名额已满（" + job.getQuota() + "人），无法继续通过");
            }
            job.setEnrolledCount(enrolled + 1);
            workStudyJobMapper.updateById(job);
        }

        application.setStatus(status);
        application.setReviewComment(comment);
        application.setReviewerId(reviewerId);
        application.setReviewTime(LocalDateTime.now());
        workStudyApplicationMapper.updateById(application);
    }

    @Override
    public void cancelWorkStudyApplication(Long applicationId, Long studentId) {
        WorkStudyApplication app = workStudyApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!app.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("只能取消自己的申请");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalArgumentException("只能取消待审核的申请");
        }
        app.setStatus("CANCELLED");
        app.setReviewComment("学生自行取消");
        app.setReviewTime(LocalDateTime.now());
        workStudyApplicationMapper.updateById(app);
    }

    @Override
    public void editWorkStudyApplication(Long applicationId, Long studentId, String resume) {
        WorkStudyApplication app = workStudyApplicationMapper.selectById(applicationId);
        if (app == null) {
            throw new IllegalArgumentException("申请不存在");
        }
        if (!app.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("只能编辑自己的申请");
        }
        if (!"PENDING".equals(app.getStatus())) {
            throw new IllegalArgumentException("只能编辑待审核的申请");
        }
        // Merge: keep existing attachments + new attachments, update the text part
        String existing = app.getResume();

        // Extract existing attachment URLs
        String existingAttachments = "";
        if (existing != null) {
            int idx = existing.indexOf("附件:");
            if (idx > 0) {
                existingAttachments = existing.substring(idx + 3).trim().replaceAll("^[\\s,]+", "");
            }
        }

        // Extract new text and new attachments from input
        String newText = resume;
        String newAttachments = "";
        if (resume != null && !resume.isEmpty()) {
            int idx = resume.indexOf("附件:");
            if (idx > 0) {
                newText = resume.substring(0, idx).trim();
                newAttachments = resume.substring(idx + 3).trim().replaceAll("^[\\s,]+", "");
            }
        }

        // Merge all attachment URLs
        java.util.List<String> allUrls = new java.util.ArrayList<>();
        if (!existingAttachments.isEmpty()) {
            for (String url : existingAttachments.split(",")) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) allUrls.add(trimmed);
            }
        }
        if (!newAttachments.isEmpty()) {
            for (String url : newAttachments.split(",")) {
                String trimmed = url.trim();
                if (!trimmed.isEmpty()) allUrls.add(trimmed);
            }
        }

        StringBuilder result = new StringBuilder(newText != null ? newText : "");
        if (!allUrls.isEmpty()) {
            result.append("\n附件: ").append(String.join(",", allUrls));
        }
        app.setResume(result.toString());
        workStudyApplicationMapper.updateById(app);
    }

    @Override
    public PageResult<Map<String, Object>> getWorkStudyApplications(Long jobId, int page, int size) {
        Page<WorkStudyApplication> p = new Page<>(page, size);
        IPage<WorkStudyApplication> pageResult = workStudyApplicationMapper.selectPage(p,
                new LambdaQueryWrapper<WorkStudyApplication>()
                        .eq(WorkStudyApplication::getJobId, jobId)
                        .orderByDesc(WorkStudyApplication::getApplyTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (WorkStudyApplication app : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("jobId", app.getJobId());
            item.put("studentId", app.getStudentId());
            item.put("resume", app.getResume());
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewComment", app.getReviewComment());
            item.put("reviewTime", app.getReviewTime());
            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            result.add(item);
        }
        return new PageResult<>(result, pageResult.getTotal(), page, size);
    }

    // ==================== Cross-cutting ====================

    @Override
    public Map<String, PageResult<?>> getMyApplications(Long studentId, int page, int size) {
        Page<ScholarshipApplication> sp = new Page<>(page, size);
        IPage<ScholarshipApplication> scholarshipResult = scholarshipApplicationMapper.selectPage(sp,
                new LambdaQueryWrapper<ScholarshipApplication>()
                        .eq(ScholarshipApplication::getStudentId, studentId)
                        .orderByDesc(ScholarshipApplication::getApplyTime));

        Page<GrantApplication> gp = new Page<>(page, size);
        IPage<GrantApplication> grantResult = grantApplicationMapper.selectPage(gp,
                new LambdaQueryWrapper<GrantApplication>()
                        .eq(GrantApplication::getStudentId, studentId)
                        .orderByDesc(GrantApplication::getApplyTime));

        Page<WorkStudyApplication> wp = new Page<>(page, size);
        IPage<WorkStudyApplication> workStudyResult = workStudyApplicationMapper.selectPage(wp,
                new LambdaQueryWrapper<WorkStudyApplication>()
                        .eq(WorkStudyApplication::getStudentId, studentId)
                        .orderByDesc(WorkStudyApplication::getApplyTime));

        Map<String, PageResult<?>> result = new HashMap<>();
        result.put("scholarshipApplications", new PageResult<>(scholarshipResult.getRecords(), scholarshipResult.getTotal(), page, size));
        result.put("grantApplications", new PageResult<>(grantResult.getRecords(), grantResult.getTotal(), page, size));
        result.put("workStudyApplications", new PageResult<>(workStudyResult.getRecords(), workStudyResult.getTotal(), page, size));
        return result;
    }

    @Override
    public Map<String, PageResult<?>> getReviewQueue(Long reviewerId, int page, int size) {
        boolean admin = isAdmin(reviewerId);

        // Admins see all pending applications; others see only their own published items
        IPage<ScholarshipApplication> scholarshipPage;
        IPage<GrantApplication> grantPage;
        IPage<WorkStudyApplication> workStudyPage;

        if (admin) {
            Page<ScholarshipApplication> sp = new Page<>(page, size);
            scholarshipPage = scholarshipApplicationMapper.selectPage(sp,
                    new LambdaQueryWrapper<ScholarshipApplication>()
                            .eq(ScholarshipApplication::getStatus, "PENDING")
                            .orderByAsc(ScholarshipApplication::getApplyTime));
            Page<GrantApplication> gp = new Page<>(page, size);
            grantPage = grantApplicationMapper.selectPage(gp,
                    new LambdaQueryWrapper<GrantApplication>()
                            .eq(GrantApplication::getStatus, "PENDING")
                            .orderByAsc(GrantApplication::getApplyTime));
            Page<WorkStudyApplication> wp = new Page<>(page, size);
            workStudyPage = workStudyApplicationMapper.selectPage(wp,
                    new LambdaQueryWrapper<WorkStudyApplication>()
                            .eq(WorkStudyApplication::getStatus, "PENDING")
                            .orderByAsc(WorkStudyApplication::getApplyTime));
        } else {
            List<Long> myScholarshipIds = scholarshipMapper.selectList(
                    new LambdaQueryWrapper<Scholarship>().eq(Scholarship::getPublisherId, reviewerId)
            ).stream().map(Scholarship::getId).collect(java.util.stream.Collectors.toList());
            List<Long> myGrantIds = grantInfoMapper.selectList(
                    new LambdaQueryWrapper<GrantInfo>().eq(GrantInfo::getPublisherId, reviewerId)
            ).stream().map(GrantInfo::getId).collect(java.util.stream.Collectors.toList());
            List<Long> myJobIds = workStudyJobMapper.selectList(
                    new LambdaQueryWrapper<WorkStudyJob>().eq(WorkStudyJob::getPublisherId, reviewerId)
            ).stream().map(WorkStudyJob::getId).collect(java.util.stream.Collectors.toList());

            Page<ScholarshipApplication> sp = new Page<>(page, size);
            scholarshipPage = myScholarshipIds.isEmpty() ? new Page<>(page, 0)
                    : scholarshipApplicationMapper.selectPage(sp,
                    new LambdaQueryWrapper<ScholarshipApplication>()
                            .eq(ScholarshipApplication::getStatus, "PENDING")
                            .in(ScholarshipApplication::getScholarshipId, myScholarshipIds)
                            .orderByAsc(ScholarshipApplication::getApplyTime));
            Page<GrantApplication> gp = new Page<>(page, size);
            grantPage = myGrantIds.isEmpty() ? new Page<>(page, 0)
                    : grantApplicationMapper.selectPage(gp,
                    new LambdaQueryWrapper<GrantApplication>()
                            .eq(GrantApplication::getStatus, "PENDING")
                            .in(GrantApplication::getGrantId, myGrantIds)
                            .orderByAsc(GrantApplication::getApplyTime));
            Page<WorkStudyApplication> wp = new Page<>(page, size);
            workStudyPage = myJobIds.isEmpty() ? new Page<>(page, 0)
                    : workStudyApplicationMapper.selectPage(wp,
                    new LambdaQueryWrapper<WorkStudyApplication>()
                            .eq(WorkStudyApplication::getStatus, "PENDING")
                            .in(WorkStudyApplication::getJobId, myJobIds)
                            .orderByAsc(WorkStudyApplication::getApplyTime));
        }

        // Enrich applications with names
        List<Map<String, Object>> enrichedScholarship = new ArrayList<>();
        for (ScholarshipApplication app : scholarshipPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("scholarshipId", app.getScholarshipId());
            item.put("studentId", app.getStudentId());
            item.put("applyReason", app.getApplyReason());
            item.put("supportingFiles", app.getSupportingFiles());
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewComment", app.getReviewComment());
            item.put("reviewerId", app.getReviewerId());
            item.put("reviewTime", app.getReviewTime());

            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            Scholarship scholarship = scholarshipMapper.selectById(app.getScholarshipId());
            item.put("scholarshipName", scholarship != null ? scholarship.getName() : null);
            enrichedScholarship.add(item);
        }

        List<Map<String, Object>> enrichedGrant = new ArrayList<>();
        for (GrantApplication app : grantPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("grantId", app.getGrantId());
            item.put("studentId", app.getStudentId());
            item.put("applyReason", app.getApplyReason());
            item.put("supportingFiles", app.getSupportingFiles());
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewComment", app.getReviewComment());
            item.put("reviewerId", app.getReviewerId());
            item.put("reviewTime", app.getReviewTime());

            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            GrantInfo grant = grantInfoMapper.selectById(app.getGrantId());
            item.put("grantName", grant != null ? grant.getName() : null);
            enrichedGrant.add(item);
        }

        List<Map<String, Object>> enrichedWorkStudy = new ArrayList<>();
        for (WorkStudyApplication app : workStudyPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("jobId", app.getJobId());
            item.put("studentId", app.getStudentId());
            item.put("resume", app.getResume());
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewComment", app.getReviewComment());

            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            WorkStudyJob job = workStudyJobMapper.selectById(app.getJobId());
            item.put("jobTitle", job != null ? job.getTitle() : null);
            enrichedWorkStudy.add(item);
        }

        Map<String, PageResult<?>> result = new HashMap<>();
        result.put("scholarshipApplications", new PageResult<>(enrichedScholarship, scholarshipPage.getTotal(), page, size));
        result.put("grantApplications", new PageResult<>(enrichedGrant, grantPage.getTotal(), page, size));
        result.put("workStudyApplications", new PageResult<>(enrichedWorkStudy, workStudyPage.getTotal(), page, size));
        return result;
    }

    // ==================== Internal helpers ====================

    @Override
    public PageResult<Map<String, Object>> getAdmittedList(int page, int size) {
        List<Map<String, Object>> result = new ArrayList<>();
        long total = 0;

        // Approved scholarship applications
        Page<ScholarshipApplication> sp = new Page<>(page, size);
        IPage<ScholarshipApplication> scholarshipPage = scholarshipApplicationMapper.selectPage(sp,
                new LambdaQueryWrapper<ScholarshipApplication>()
                        .eq(ScholarshipApplication::getStatus, "APPROVED")
                        .orderByDesc(ScholarshipApplication::getReviewTime));
        for (ScholarshipApplication app : scholarshipPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("type", "scholarship");
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewTime", app.getReviewTime());
            item.put("reviewComment", app.getReviewComment());
            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            item.put("studentPhone", student != null ? student.getPhone() : null);
            Scholarship scholarship = scholarshipMapper.selectById(app.getScholarshipId());
            item.put("projectId", app.getScholarshipId());
            item.put("projectName", scholarship != null ? scholarship.getName() : null);
            item.put("projectAmount", scholarship != null ? scholarship.getAmount() : null);
            item.put("projectStatus", scholarship != null ? scholarship.getStatus() : null);
            item.put("applyReason", app.getApplyReason());
            item.put("supportingFiles", app.getSupportingFiles());
            result.add(item);
        }
        total += scholarshipPage.getTotal();

        // Approved grant applications
        Page<GrantApplication> gp = new Page<>(page, size);
        IPage<GrantApplication> grantPage = grantApplicationMapper.selectPage(gp,
                new LambdaQueryWrapper<GrantApplication>()
                        .eq(GrantApplication::getStatus, "APPROVED")
                        .orderByDesc(GrantApplication::getReviewTime));
        for (GrantApplication app : grantPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("type", "grant");
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewTime", app.getReviewTime());
            item.put("reviewComment", app.getReviewComment());
            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            item.put("studentPhone", student != null ? student.getPhone() : null);
            GrantInfo grant = grantInfoMapper.selectById(app.getGrantId());
            item.put("projectId", app.getGrantId());
            item.put("projectName", grant != null ? grant.getName() : null);
            item.put("projectAmount", grant != null ? grant.getAmount() : null);
            item.put("projectStatus", grant != null ? grant.getStatus() : null);
            item.put("applyReason", app.getApplyReason());
            item.put("supportingFiles", app.getSupportingFiles());
            result.add(item);
        }
        total += grantPage.getTotal();

        // Approved work-study applications
        Page<WorkStudyApplication> wp = new Page<>(page, size);
        IPage<WorkStudyApplication> workStudyPage = workStudyApplicationMapper.selectPage(wp,
                new LambdaQueryWrapper<WorkStudyApplication>()
                        .eq(WorkStudyApplication::getStatus, "APPROVED")
                        .orderByDesc(WorkStudyApplication::getReviewTime));
        for (WorkStudyApplication app : workStudyPage.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", app.getId());
            item.put("type", "workStudy");
            item.put("status", app.getStatus());
            item.put("applyTime", app.getApplyTime());
            item.put("reviewTime", app.getReviewTime());
            item.put("reviewComment", app.getReviewComment());
            SysUser student = sysUserMapper.selectById(app.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : null);
            item.put("studentPhone", student != null ? student.getPhone() : null);
            WorkStudyJob job = workStudyJobMapper.selectById(app.getJobId());
            item.put("projectId", app.getJobId());
            item.put("projectName", job != null ? job.getTitle() : null);
            item.put("projectAmount", job != null ? job.getSalary() : null);
            item.put("projectStatus", job != null ? job.getStatus() : null);
            item.put("resume", app.getResume());
            result.add(item);
        }
        total += workStudyPage.getTotal();

        return new PageResult<>(result, total, page, size);
    }

    // ==================== Export ====================

    @Override
    public byte[] exportScholarshipApplications(Long scholarshipId) {
        List<Map<String, Object>> apps = getScholarshipApplications(scholarshipId, 1, Integer.MAX_VALUE).getRecords();
        Scholarship scholarship = scholarshipMapper.selectById(scholarshipId);
        String sheetName = scholarship != null ? scholarship.getName() : "奖学金申请名单";
        return buildExcel(sheetName, new String[]{"序号", "学生姓名", "申请理由", "状态", "申请时间", "审核意见", "审核时间"}, apps,
                new String[]{"index", "studentName", "applyReason", "status", "applyTime", "reviewComment", "reviewTime"});
    }

    @Override
    public byte[] exportGrantApplications(Long grantId) {
        List<Map<String, Object>> apps = getGrantApplications(grantId, 1, Integer.MAX_VALUE).getRecords();
        GrantInfo grant = grantInfoMapper.selectById(grantId);
        String sheetName = grant != null ? grant.getName() : "助学金申请名单";
        return buildExcel(sheetName, new String[]{"序号", "学生姓名", "申请理由", "状态", "申请时间", "审核意见", "审核时间"}, apps,
                new String[]{"index", "studentName", "applyReason", "status", "applyTime", "reviewComment", "reviewTime"});
    }

    @Override
    public byte[] exportWorkStudyApplications(Long jobId) {
        List<Map<String, Object>> apps = getWorkStudyApplications(jobId, 1, Integer.MAX_VALUE).getRecords();
        for (Map<String, Object> app : apps) {
            String resume = (String) app.get("resume");
            if (resume != null) {
                int idx = resume.indexOf("附件:");
                if (idx > 0) {
                    app.put("resume", resume.substring(0, idx).trim());
                }
            }
        }
        WorkStudyJob job = workStudyJobMapper.selectById(jobId);
        String sheetName = job != null ? job.getTitle() : "勤工助学申请名单";
        return buildExcel(sheetName, new String[]{"序号", "学生姓名", "申请理由", "状态", "申请时间", "审核意见", "审核时间"}, apps,
                new String[]{"index", "studentName", "resume", "status", "applyTime", "reviewComment", "reviewTime"});
    }

    private byte[] buildExcel(String sheetName, String[] headers, List<Map<String, Object>> data, String[] fields) {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            var sheet = wb.createSheet(sheetName);
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            for (Map<String, Object> item : data) {
                var row = sheet.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);
                for (int f = 1; f < fields.length; f++) {
                    Object val = item.get(fields[f]);
                    String text = val != null ? val.toString() : "";
                    if ("status".equals(fields[f])) {
                        text = switch (text) {
                            case "PENDING" -> "待审核";
                            case "APPROVED" -> "已通过";
                            case "REJECTED" -> "已拒绝";
                            case "CANCELLED" -> "已取消";
                            default -> text;
                        };
                    }
                    row.createCell(f).setCellValue(text);
                }
                rowIdx++;
            }
            var baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    private void checkIsStudent(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || !"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException("仅学生可以申请");
        }
    }

    private boolean isAdmin(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null && "ADMIN".equals(user.getRole());
    }

    private void checkModifyPermission(Long publisherId, Long userId) {
        if (publisherId.equals(userId)) return;
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser != null && "ADMIN".equals(currentUser.getRole())) return;
        SysUser publisher = sysUserMapper.selectById(publisherId);
        if (publisher != null && "ADMIN".equals(publisher.getRole())) {
            throw new IllegalArgumentException("不能删除管理员发布的内容");
        }
        throw new IllegalArgumentException("只能删除自己发布的内容");
    }

}
