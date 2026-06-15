package com.smartlearn.service;

import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;

import java.util.Map;

public interface FinancialAidService {

    // ==================== Scholarships ====================

    PageResult<Scholarship> listScholarships(int page, int size);

    Scholarship getScholarshipById(Long id);

    Scholarship createScholarship(Scholarship scholarship);

    Scholarship updateScholarship(Scholarship scholarship, Long userId);

    void deleteScholarship(Long id, Long userId);

    void applyScholarship(Long scholarshipId, Long studentId, String reason, String files);

    void reviewScholarshipApplication(Long applicationId, String status, String comment, Long reviewerId);

    void cancelScholarshipApplication(Long applicationId, Long studentId);

    void editScholarshipApplication(Long applicationId, Long studentId, String reason, String files);

    PageResult<Map<String, Object>> getScholarshipApplications(Long scholarshipId, int page, int size);

    // ==================== Grants ====================

    PageResult<GrantInfo> listGrants(int page, int size);

    GrantInfo getGrantById(Long id);

    GrantInfo createGrant(GrantInfo grant);

    GrantInfo updateGrant(GrantInfo grant, Long userId);

    void deleteGrant(Long id, Long userId);

    void applyGrant(Long grantId, Long studentId, String reason, String files);

    void reviewGrantApplication(Long applicationId, String status, String comment, Long reviewerId);

    void cancelGrantApplication(Long applicationId, Long studentId);

    void editGrantApplication(Long applicationId, Long studentId, String reason, String files);

    PageResult<Map<String, Object>> getGrantApplications(Long grantId, int page, int size);

    // ==================== Work-Study Jobs ====================

    PageResult<WorkStudyJob> listWorkStudyJobs(int page, int size);

    WorkStudyJob getWorkStudyJobById(Long id);

    WorkStudyJob createWorkStudyJob(WorkStudyJob job);

    WorkStudyJob updateWorkStudyJob(WorkStudyJob job, Long userId);

    void deleteWorkStudyJob(Long id, Long userId);

    void applyWorkStudyJob(Long jobId, Long studentId, String resume);

    void reviewWorkStudyApplication(Long applicationId, String status, String comment, Long reviewerId);

    void cancelWorkStudyApplication(Long applicationId, Long studentId);

    void editWorkStudyApplication(Long applicationId, Long studentId, String resume);

    PageResult<Map<String, Object>> getWorkStudyApplications(Long jobId, int page, int size);

    // ==================== Cross-cutting ====================

    Map<String, PageResult<?>> getMyApplications(Long studentId, int page, int size);

    Map<String, PageResult<?>> getReviewQueue(Long reviewerId, int page, int size);

    PageResult<Map<String, Object>> getAdmittedList(int page, int size);

    // ==================== Export ====================

    byte[] exportScholarshipApplications(Long scholarshipId);

    byte[] exportGrantApplications(Long grantId);

    byte[] exportWorkStudyApplications(Long jobId);

}
