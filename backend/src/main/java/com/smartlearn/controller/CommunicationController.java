package com.smartlearn.controller;

import com.smartlearn.common.PageResult;
import com.smartlearn.common.Result;
import com.smartlearn.entity.*;
import com.smartlearn.security.SecurityUtil;
import com.smartlearn.security.UserPrincipal;
import com.smartlearn.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/communication")
@RequiredArgsConstructor
public class CommunicationController {

    private final CommunicationService communicationService;
    private final SecurityUtil securityUtil;

    // ==================== Private Messages ====================

    @PostMapping("/messages")
    public Result<PrivateMessage> sendMessage(@RequestBody PrivateMessage message) {
        message.setSenderId(securityUtil.getCurrentUserId());
        return Result.success(communicationService.sendMessage(message));
    }

    @GetMapping("/messages/conversation/{userId}")
    public Result<List<PrivateMessage>> getConversation(@PathVariable Long userId) {
        return Result.success(communicationService.getConversation(securityUtil.getCurrentUserId(), userId));
    }

    @GetMapping("/messages/unread")
    public Result<List<PrivateMessage>> getUnreadMessages() {
        return Result.success(communicationService.getUnreadMessages(securityUtil.getCurrentUserId()));
    }

    @PutMapping("/messages/{id}/read")
    public Result<Void> markAsRead(@PathVariable Long id) {
        communicationService.markAsRead(id);
        return Result.success();
    }

    @PutMapping("/messages/conversation/{contactId}/read")
    public Result<Void> markConversationAsRead(@PathVariable Long contactId) {
        communicationService.markConversationAsRead(securityUtil.getCurrentUserId(), contactId);
        return Result.success();
    }

    @GetMapping("/messages")
    public Result<List<PrivateMessage>> getAllMessages() {
        return Result.success(communicationService.getAllMessages(securityUtil.getCurrentUserId()));
    }

    @DeleteMapping("/messages/{id}")
    public Result<Void> deleteMessage(@PathVariable Long id) {
        communicationService.deleteMessage(id);
        return Result.success();
    }

    @DeleteMapping("/messages/conversation/{contactId}")
    public Result<Void> clearConversation(@PathVariable Long contactId) {
        communicationService.clearConversation(securityUtil.getCurrentUserId(), contactId);
        return Result.success();
    }

    @GetMapping("/messages/contacts")
    public Result<List<Map<String, Object>>> getContacts() {
        return Result.success(communicationService.getContacts(securityUtil.getCurrentUserId()));
    }

    @GetMapping("/messages/find-by-student-no/{studentNo}")
    public Result<Map<String, Object>> findUserByStudentNo(@PathVariable String studentNo) {
        return Result.success(communicationService.findUserByStudentNo(studentNo));
    }

    // ==================== Class Notices ====================

    @PostMapping("/notices")
    public Result<ClassNotice> createClassNotice(@RequestBody ClassNotice notice) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可发布班级通知");
        }
        notice.setPublisherId(securityUtil.getCurrentUserId());
        return Result.success(communicationService.createClassNotice(notice));
    }

    @GetMapping("/notices")
    public Result<PageResult<ClassNotice>> getClassNotices(
            @RequestParam(required = false) String classIdentifier,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(communicationService.getClassNotices(classIdentifier, page, size));
    }

    @PutMapping("/notices/{id}")
    public Result<Void> updateClassNotice(@PathVariable Long id, @RequestBody ClassNotice notice) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可修改班级通知");
        }
        notice.setId(id);
        communicationService.updateClassNotice(notice);
        return Result.success();
    }

    @DeleteMapping("/notices/{id}")
    public Result<Void> deleteClassNotice(@PathVariable Long id,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        communicationService.deleteClassNotice(id, principal.getUserId());
        return Result.success();
    }

    // ==================== Feedback ====================

    @PostMapping("/feedback")
    public Result<Feedback> submitFeedback(@RequestBody Feedback feedback) {
        feedback.setUserId(securityUtil.getCurrentUserId());
        return Result.success(communicationService.submitFeedback(feedback));
    }

    @GetMapping("/feedback/my")
    public Result<PageResult<Feedback>> getMyFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(communicationService.getMyFeedback(securityUtil.getCurrentUserId(), page, size));
    }

    @GetMapping("/feedback/pending")
    public Result<PageResult<Feedback>> getPendingFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可查看待回复反馈");
        }
        return Result.success(communicationService.getPendingFeedback(page, size));
    }

    @GetMapping("/feedback")
    public Result<PageResult<Feedback>> getAllFeedback(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可查看全部反馈");
        }
        return Result.success(communicationService.getAllFeedback(page, size));
    }

    @DeleteMapping("/feedback/{id}")
    public Result<Void> deleteFeedback(@PathVariable Long id) {
        communicationService.deleteFeedback(id, securityUtil.getCurrentUserId());
        return Result.success();
    }

    @PutMapping("/feedback/{id}/reply")
    public Result<Void> replyFeedback(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        if (!securityUtil.isAdmin()) {
            return Result.forbidden("仅管理员可回复反馈");
        }
        String reply = (String) body.get("reply");
        communicationService.replyFeedback(id, reply);
        return Result.success();
    }

    // ==================== Activities ====================

    @PostMapping("/activities")
    public Result<ActivityPost> createActivity(@RequestBody ActivityPost activity) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可发布活动");
        }
        activity.setPublisherId(securityUtil.getCurrentUserId());
        return Result.success(communicationService.createActivity(activity));
    }

    @GetMapping("/activities")
    public Result<PageResult<ActivityPost>> getActivities(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(communicationService.getActivities(securityUtil.getCurrentUserId(), category, page, size));
    }

    @GetMapping("/activities/{id}")
    public Result<ActivityPost> getActivityById(@PathVariable Long id) {
        return Result.success(communicationService.getActivityById(id));
    }

    @PutMapping("/activities/{id}")
    public Result<Void> updateActivity(@PathVariable Long id, @RequestBody ActivityPost activity) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可修改活动");
        }
        activity.setId(id);
        communicationService.updateActivity(activity);
        return Result.success();
    }

    @DeleteMapping("/activities/{id}")
    public Result<Void> deleteActivity(@PathVariable Long id,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        communicationService.deleteActivity(id, principal.getUserId());
        return Result.success();
    }

    @PostMapping("/activities/{id}/register")
    public Result<Void> registerForActivity(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        communicationService.registerForActivity(id, principal.getUserId());
        return Result.success();
    }

    @DeleteMapping("/activities/{id}/register")
    public Result<Void> cancelRegistration(@PathVariable Long id,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        communicationService.cancelRegistration(id, principal.getUserId());
        return Result.success();
    }

    @GetMapping("/activities/my-registrations")
    public Result<PageResult<java.util.Map<String, Object>>> getMyRegistrations(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(communicationService.getMyRegistrations(principal.getUserId(), page, size));
    }

    @GetMapping("/export/activities")
    public ResponseEntity<byte[]> exportActivities() {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            throw new org.springframework.security.access.AccessDeniedException("仅教师和管理员可导出");
        }
        byte[] data = communicationService.exportActivityRegistrations();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + URLEncoder.encode("活动报名情况.xlsx", StandardCharsets.UTF_8))
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }
}
