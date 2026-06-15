package com.smartlearn.controller;

import com.smartlearn.common.Result;
import com.smartlearn.entity.*;
import com.smartlearn.security.SecurityUtil;
import com.smartlearn.security.UserPrincipal;
import com.smartlearn.service.OssService;
import com.smartlearn.service.StudentInfoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/student")
@RequiredArgsConstructor
public class StudentInfoController {

    private final StudentInfoService studentInfoService;
    private final SecurityUtil securityUtil;
    private final OssService ossService;

    /**
     * Resolve the current student's profile ID from the authenticated user.
     */
    private Long getCurrentStudentId() {
        Long userId = securityUtil.getCurrentUserId();
        StudentProfile profile = studentInfoService.getProfile(userId);
        if (profile == null) {
            throw new IllegalArgumentException("当前用户未绑定学生档案");
        }
        return profile.getId();
    }

    /**
     * Resolve student profile ID, returning null for non-student users.
     */
    private Long getCurrentStudentIdOrNull() {
        Long userId = securityUtil.getCurrentUserId();
        StudentProfile profile = studentInfoService.getProfile(userId);
        return profile != null ? profile.getId() : null;
    }

    @GetMapping("/profiles")
    public Result<List<StudentProfile>> listStudentProfiles(@RequestParam(required = false) String keyword) {
        if (keyword != null && !keyword.isBlank()) {
            return Result.success(studentInfoService.searchStudentProfiles(keyword.trim()));
        }
        return Result.success(studentInfoService.listStudentProfiles());
    }

    @GetMapping("/profile")
    public Result<StudentProfile> getMyProfile() {
        StudentProfile profile = studentInfoService.getProfile(securityUtil.getCurrentUserId());
        return Result.success(profile);
    }

    @PutMapping("/profile")
    public Result<Void> updateMyProfile(@Valid @RequestBody StudentProfile profile,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        if (securityUtil.isAdmin()) {
            if (profile.getId() == null) {
                throw new IllegalArgumentException("缺少学生档案ID");
            }
            StudentProfile existing = studentInfoService.getProfileById(profile.getId());
            if (existing == null) {
                throw new IllegalArgumentException("学生档案不存在");
            }
            profile.setUserId(existing.getUserId());
        } else {
            // Student or teacher updating own profile
            StudentProfile existing = studentInfoService.getProfile(principal.getUserId());
            if (existing == null) {
                throw new IllegalArgumentException("当前用户未绑定学生档案");
            }
            // Non-admin users can only update their own profile
            if (profile.getId() != null && !profile.getId().equals(existing.getId())) {
                return Result.forbidden("仅可修改自己的档案信息");
            }
            profile.setId(existing.getId());
            profile.setUserId(principal.getUserId());
            // Non-admin users cannot directly modify these fields — must use change requests
            profile.setName(null);
            profile.setIdCard(null);
            profile.setDormitory(null);
            profile.setStudentNo(null);
            profile.setCollege(null);
            profile.setMajor(null);
            profile.setGrade(null);
            profile.setClassName(null);
            profile.setCampus(null);
            profile.setAdvisorName(null);
            profile.setAdvisorPhone(null);
            profile.setDifficultyLevel(null);
        }
        studentInfoService.updateProfile(profile);
        return Result.success("个人信息更新成功", null);
    }

    @GetMapping("/profile/{studentId}")
    public Result<StudentProfile> getProfileById(@PathVariable Long studentId) {
        return Result.success(studentInfoService.getProfileById(studentId));
    }

    @GetMapping("/family")
    public Result<List<FamilyMember>> getFamilyMembers(@RequestParam(required = false) Long studentId) {
        Long sid = resolveStudentId(studentId);
        if (sid == null) return Result.success(Collections.emptyList());
        return Result.success(studentInfoService.getFamilyMembers(sid));
    }

    @PutMapping("/family")
    public Result<Void> saveFamilyMembers(@RequestBody List<FamilyMember> members,
                                          @RequestParam(required = false) Long studentId) {
        if (studentId != null && !securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可修改其他学生的信息");
        }
        Long sid = (studentId != null && securityUtil.isAdmin())
                ? studentId : getCurrentStudentId();
        studentInfoService.saveFamilyMembers(sid, members);
        return Result.success("家庭成员信息保存成功", null);
    }

    @GetMapping("/emergency-contacts")
    public Result<List<EmergencyContact>> getEmergencyContacts(@RequestParam(required = false) Long studentId) {
        Long sid = resolveStudentId(studentId);
        if (sid == null) return Result.success(Collections.emptyList());
        return Result.success(studentInfoService.getEmergencyContacts(sid));
    }

    @PutMapping("/emergency-contacts")
    public Result<Void> saveEmergencyContacts(@RequestBody List<EmergencyContact> contacts,
                                              @RequestParam(required = false) Long studentId) {
        if (studentId != null && !securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可修改其他学生的信息");
        }
        Long sid = (studentId != null && securityUtil.isAdmin())
                ? studentId : getCurrentStudentId();
        studentInfoService.saveEmergencyContacts(sid, contacts);
        return Result.success("紧急联系人保存成功", null);
    }

    @GetMapping("/documents")
    public Result<List<SupportingDocument>> getDocuments(@RequestParam(required = false) Long studentId) {
        Long sid = resolveStudentId(studentId);
        if (sid == null) return Result.success(Collections.emptyList());
        return Result.success(studentInfoService.getDocuments(sid));
    }

    @PostMapping("/documents")
    public Result<Void> uploadDocument(@RequestParam("file") MultipartFile file,
                                       @RequestParam String docType,
                                       @RequestParam(required = false) Long studentId) {
        if (studentId != null && !securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可修改其他学生的信息");
        }
        Long sid = (studentId != null && securityUtil.isAdmin())
                ? studentId : getCurrentStudentId();
        String url = ossService.uploadFile(file, "documents");
        studentInfoService.uploadDocument(sid, docType, url);
        return Result.success(url, null);
    }

    @GetMapping("/change-requests")
    public Result<List<InfoChangeRequest>> getChangeRequests(@RequestParam(required = false) Long studentId) {
        Long sid = resolveStudentId(studentId);
        if (sid == null) return Result.success(Collections.emptyList());
        return Result.success(studentInfoService.getChangeRequests(sid));
    }

    private Long resolveStudentId(Long studentId) {
        if (studentId != null && (securityUtil.isTeacher() || securityUtil.isAdmin())) {
            return studentId;
        }
        return getCurrentStudentIdOrNull();
    }

    @PostMapping("/change-requests")
    public Result<Void> submitChangeRequest(@Valid @RequestBody InfoChangeRequest request) {
        Long studentId = getCurrentStudentId();
        studentInfoService.submitChangeRequest(studentId, request);
        return Result.success("变更申请已提交", null);
    }

    @PutMapping("/change-requests/{id}/review")
    public Result<Void> reviewChangeRequest(@PathVariable Long id,
                                            @RequestBody Map<String, String> body,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可审核变更申请");
        }
        String status = body.get("status");
        String comment = body.get("comment");
        studentInfoService.reviewChangeRequest(id, status, comment, principal.getUserId());
        return Result.success("审核完成", null);
    }

    @PutMapping("/change-requests/{id}/cancel")
    public Result<Void> cancelChangeRequest(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        if (!securityUtil.isStudent()) {
            return Result.forbidden("仅学生可取消申请");
        }
        studentInfoService.cancelChangeRequest(id, principal.getUserId());
        return Result.success("申请已取消", null);
    }

    @GetMapping("/export/{studentId}")
    public ResponseEntity<byte[]> exportStudentInfo(@PathVariable Long studentId) {
        StudentProfile profile = studentInfoService.getProfileById(studentId);
        String filename = (profile != null ? profile.getName() : "student") + "_info.xlsx";
        byte[] excel = studentInfoService.exportStudentInfo(studentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excel);
    }

}
