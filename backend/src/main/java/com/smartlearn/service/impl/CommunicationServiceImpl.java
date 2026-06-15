package com.smartlearn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import com.smartlearn.service.CommunicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommunicationServiceImpl implements CommunicationService {

    private final PrivateMessageMapper privateMessageMapper;
    private final ClassNoticeMapper classNoticeMapper;
    private final FeedbackMapper feedbackMapper;
    private final ActivityPostMapper activityPostMapper;
    private final ActivityRegistrationMapper activityRegistrationMapper;
    private final SysUserMapper sysUserMapper;
    private final StudentProfileMapper studentProfileMapper;

    @Override
    @Transactional
    public PrivateMessage sendMessage(PrivateMessage message) {
        message.setSentAt(LocalDateTime.now());
        message.setIsRead(0);
        privateMessageMapper.insert(message);
        return message;
    }

    @Override
    public List<PrivateMessage> getConversation(Long userId1, Long userId2) {
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.nested(w -> w
                        .eq(PrivateMessage::getSenderId, userId1)
                        .eq(PrivateMessage::getReceiverId, userId2))
                .or()
                .nested(w -> w
                        .eq(PrivateMessage::getSenderId, userId2)
                        .eq(PrivateMessage::getReceiverId, userId1))
                .orderByAsc(PrivateMessage::getSentAt);
        return privateMessageMapper.selectList(wrapper);
    }

    @Override
    public List<PrivateMessage> getUnreadMessages(Long receiverId) {
        return privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getReceiverId, receiverId)
                        .eq(PrivateMessage::getIsRead, 0)
                        .orderByDesc(PrivateMessage::getSentAt)
        );
    }

    @Override
    public List<PrivateMessage> getAllMessages(Long userId) {
        return privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .nested(w -> w.eq(PrivateMessage::getSenderId, userId)
                                .or()
                                .eq(PrivateMessage::getReceiverId, userId))
                        .orderByDesc(PrivateMessage::getSentAt)
        );
    }

    @Override
    @Transactional
    public void markAsRead(Long messageId) {
        PrivateMessage message = privateMessageMapper.selectById(messageId);
        if (message == null) {
            throw new IllegalArgumentException("消息不存在");
        }
        message.setIsRead(1);
        privateMessageMapper.updateById(message);
    }

    @Override
    @Transactional
    public void markConversationAsRead(Long userId, Long contactId) {
        List<PrivateMessage> unread = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getSenderId, contactId)
                        .eq(PrivateMessage::getReceiverId, userId)
                        .eq(PrivateMessage::getIsRead, 0)
        );
        for (PrivateMessage msg : unread) {
            msg.setIsRead(1);
            privateMessageMapper.updateById(msg);
        }
    }

    @Override
    public void deleteMessage(Long id) {
        privateMessageMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void clearConversation(Long userId1, Long userId2) {
        LambdaQueryWrapper<PrivateMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.nested(w -> w
                        .eq(PrivateMessage::getSenderId, userId1)
                        .eq(PrivateMessage::getReceiverId, userId2))
                .or()
                .nested(w -> w
                        .eq(PrivateMessage::getSenderId, userId2)
                        .eq(PrivateMessage::getReceiverId, userId1));
        privateMessageMapper.delete(wrapper);
    }

    @Override
    public List<Map<String, Object>> getContacts(Long userId) {
        Set<Long> contactIds = new HashSet<>();

        List<PrivateMessage> received = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getReceiverId, userId)
                        .select(PrivateMessage::getSenderId)
        );
        contactIds.addAll(received.stream()
                .map(PrivateMessage::getSenderId)
                .collect(Collectors.toList()));

        List<PrivateMessage> sent = privateMessageMapper.selectList(
                new LambdaQueryWrapper<PrivateMessage>()
                        .eq(PrivateMessage::getSenderId, userId)
                        .select(PrivateMessage::getReceiverId)
        );
        contactIds.addAll(sent.stream()
                .map(PrivateMessage::getReceiverId)
                .collect(Collectors.toList()));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long contactId : contactIds) {
            Map<String, Object> contact = new HashMap<>();
            contact.put("id", contactId);
            SysUser user = sysUserMapper.selectById(contactId);
            contact.put("name", user != null ? user.getRealName() : "用户" + contactId);
            contact.put("role", user != null ? user.getRole() : "");
            // Look up student number from profile
            String studentNo = "";
            if (user != null && "STUDENT".equals(user.getRole())) {
                StudentProfile profile = studentProfileMapper.selectOne(
                        new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, contactId));
                studentNo = profile != null ? profile.getStudentNo() : "";
            }
            contact.put("studentNo", studentNo);
            result.add(contact);
        }
        return result;
    }

    @Override
    public Map<String, Object> findUserByStudentNo(String studentNo) {
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getStudentNo, studentNo)
        );
        if (profile == null) {
            throw new IllegalArgumentException("未找到学号为 " + studentNo + " 的学生");
        }
        SysUser user = sysUserMapper.selectById(profile.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("name", user.getRealName());
        result.put("role", user.getRole());
        result.put("studentNo", profile.getStudentNo());
        return result;
    }

    @Override
    @Transactional
    public ClassNotice createClassNotice(ClassNotice notice) {
        notice.setPublishedAt(LocalDateTime.now());
        if (notice.getIsPinned() == null) {
            notice.setIsPinned(0);
        }
        classNoticeMapper.insert(notice);
        return notice;
    }

    @Override
    public PageResult<ClassNotice> getClassNotices(String classIdentifier, int page, int size) {
        Page<ClassNotice> p = new Page<>(page, size);
        LambdaQueryWrapper<ClassNotice> wrapper = new LambdaQueryWrapper<ClassNotice>()
                .orderByDesc(ClassNotice::getIsPinned)
                .orderByDesc(ClassNotice::getPublishedAt);
        if (classIdentifier != null && !classIdentifier.isBlank()) {
            wrapper.eq(ClassNotice::getClassIdentifier, classIdentifier);
        }
        IPage<ClassNotice> result = classNoticeMapper.selectPage(p, wrapper);
        for (ClassNotice notice : result.getRecords()) {
            SysUser publisher = sysUserMapper.selectById(notice.getPublisherId());
            if (publisher != null) {
                notice.setPublisherName(publisher.getRealName());
            }
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void updateClassNotice(ClassNotice notice) {
        ClassNotice existing = classNoticeMapper.selectById(notice.getId());
        if (existing == null) {
            throw new IllegalArgumentException("该通知不存在");
        }
        classNoticeMapper.updateById(notice);
    }

    @Override
    @Transactional
    public void deleteClassNotice(Long id, Long userId) {
        ClassNotice notice = classNoticeMapper.selectById(id);
        if (notice == null) {
            throw new IllegalArgumentException("该通知不存在");
        }
        checkDeletePermission(notice.getPublisherId(), userId);
        classNoticeMapper.deleteById(id);
    }

    @Override
    @Transactional
    public Feedback submitFeedback(Feedback feedback) {
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setStatus("PENDING");
        if (feedback.getIsAnonymous() == null) {
            feedback.setIsAnonymous(0);
        }
        feedbackMapper.insert(feedback);
        return feedback;
    }

    @Override
    public PageResult<Feedback> getMyFeedback(Long userId, int page, int size) {
        Page<Feedback> p = new Page<>(page, size);
        IPage<Feedback> result = feedbackMapper.selectPage(p,
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getUserId, userId)
                        .orderByDesc(Feedback::getCreatedAt)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PageResult<Feedback> getPendingFeedback(int page, int size) {
        Page<Feedback> p = new Page<>(page, size);
        IPage<Feedback> result = feedbackMapper.selectPage(p,
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getStatus, "PENDING")
                        .orderByDesc(Feedback::getCreatedAt)
        );
        for (Feedback fb : result.getRecords()) {
            SysUser user = sysUserMapper.selectById(fb.getUserId());
            if (user != null) {
                fb.setSubmitterName(user.getRealName());
            }
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public PageResult<Feedback> getAllFeedback(int page, int size) {
        Page<Feedback> p = new Page<>(page, size);
        IPage<Feedback> result = feedbackMapper.selectPage(p,
                new LambdaQueryWrapper<Feedback>()
                        .orderByDesc(Feedback::getCreatedAt)
        );
        for (Feedback fb : result.getRecords()) {
            SysUser user = sysUserMapper.selectById(fb.getUserId());
            if (user != null) {
                fb.setSubmitterName(user.getRealName());
            }
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public void deleteFeedback(Long id, Long userId) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new IllegalArgumentException("该反馈不存在");
        }
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (!"ADMIN".equals(currentUser != null ? currentUser.getRole() : null)
                && !feedback.getUserId().equals(userId)) {
            throw new IllegalArgumentException("只能删除自己的反馈");
        }
        feedbackMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void replyFeedback(Long id, String reply) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new IllegalArgumentException("该反馈不存在");
        }
        feedback.setReply(reply);
        feedback.setStatus("REPLIED");
        feedback.setRepliedAt(LocalDateTime.now());
        feedbackMapper.updateById(feedback);
    }

    @Override
    @Transactional
    public ActivityPost createActivity(ActivityPost activity) {
        activity.setCreatedAt(LocalDateTime.now());
        if (activity.getCurrentParticipants() == null) {
            activity.setCurrentParticipants(0);
        }
        if (activity.getParticipantType() == null || activity.getParticipantType().isBlank()) {
            activity.setParticipantType("STUDENT");
        }
        if (activity.getTargetClass() != null && activity.getTargetClass().isBlank()) {
            activity.setTargetClass(null);
        }
        activity.setStatus("OPEN");
        activityPostMapper.insert(activity);
        return activity;
    }

    @Override
    public PageResult<ActivityPost> getActivities(Long userId, String category, int page, int size) {
        Page<ActivityPost> p = new Page<>(page, size);
        SysUser user = sysUserMapper.selectById(userId);
        String role = user != null ? user.getRole() : "STUDENT";

        LambdaQueryWrapper<ActivityPost> wrapper = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            wrapper.eq(ActivityPost::getCategory, category);
        }

        if ("ADMIN".equals(role)) {
            wrapper.orderByDesc(ActivityPost::getCreatedAt);
        } else if ("TEACHER".equals(role)) {
            wrapper.and(w -> w.eq(ActivityPost::getPublisherId, userId)
                    .or(w2 -> w2.inSql(ActivityPost::getPublisherId,
                            "SELECT id FROM sys_user WHERE role = 'ADMIN'")));
            wrapper.orderByDesc(ActivityPost::getCreatedAt);
        } else {
            StudentProfile profile = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>().eq(StudentProfile::getUserId, userId));
            String className = profile != null ? profile.getClassName() : null;

            wrapper.and(w -> {
                w.apply("publisher_id IN (SELECT id FROM sys_user WHERE role = 'ADMIN')")
                        .in(ActivityPost::getParticipantType, "STUDENT", "BOTH");
                if (className != null && !className.isBlank()) {
                    w.or().apply("publisher_id IN (SELECT id FROM sys_user WHERE role = 'TEACHER')")
                            .eq(ActivityPost::getTargetClass, className);
                }
            });
            wrapper.orderByDesc(ActivityPost::getCreatedAt);
        }

        IPage<ActivityPost> result = activityPostMapper.selectPage(p, wrapper);
        for (ActivityPost act : result.getRecords()) {
            SysUser publisher = sysUserMapper.selectById(act.getPublisherId());
            if (publisher != null) {
                act.setPublisherName(publisher.getRealName());
            }
        }
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public ActivityPost getActivityById(Long id) {
        ActivityPost activity = activityPostMapper.selectById(id);
        if (activity == null) {
            throw new IllegalArgumentException("该活动不存在");
        }
        SysUser publisher = sysUserMapper.selectById(activity.getPublisherId());
        if (publisher != null) {
            activity.setPublisherName(publisher.getRealName());
        }
        return activity;
    }

    @Override
    @Transactional
    public void updateActivity(ActivityPost activity) {
        ActivityPost existing = activityPostMapper.selectById(activity.getId());
        if (existing == null) {
            throw new IllegalArgumentException("该活动不存在");
        }
        activityPostMapper.updateById(activity);
    }

    @Override
    @Transactional
    public void registerForActivity(Long activityId, Long userId) {
        ActivityPost activity = activityPostMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("该活动不存在");
        }

        SysUser user = sysUserMapper.selectById(userId);
        String role = user != null ? user.getRole() : "STUDENT";
        String participantType = activity.getParticipantType() != null ? activity.getParticipantType() : "STUDENT";

        if ("STUDENT".equals(participantType) && !"STUDENT".equals(role)) {
            throw new IllegalArgumentException("该活动仅限学生参加");
        }
        if ("TEACHER".equals(participantType) && !"TEACHER".equals(role)) {
            throw new IllegalArgumentException("该活动仅限教师参加");
        }

        if ("TEACHER".equals(role)) {
            SysUser publisher = sysUserMapper.selectById(activity.getPublisherId());
            if (publisher == null || !"ADMIN".equals(publisher.getRole())) {
                throw new IllegalArgumentException("教师只能参加管理员发布的活动");
            }
        }

        if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            throw new IllegalStateException("该活动报名人数已满");
        }

        Long count = activityRegistrationMapper.selectCount(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getStudentId, userId)
        );
        if (count > 0) {
            throw new IllegalStateException("您已经报名了该活动");
        }

        activity.setCurrentParticipants(activity.getCurrentParticipants() + 1);
        activityPostMapper.updateById(activity);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(activityId);
        registration.setStudentId(userId);
        registration.setRegisteredAt(LocalDateTime.now());
        registration.setStatus("REGISTERED");
        activityRegistrationMapper.insert(registration);
    }

    @Override
    @Transactional
    public void cancelRegistration(Long activityId, Long userId) {
        ActivityPost activity = activityPostMapper.selectById(activityId);
        if (activity == null) {
            throw new IllegalArgumentException("该活动不存在");
        }

        List<ActivityRegistration> registrations = activityRegistrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, activityId)
                        .eq(ActivityRegistration::getStudentId, userId)
        );
        if (registrations.isEmpty()) {
            throw new IllegalArgumentException("您尚未报名该活动");
        }

        activityRegistrationMapper.deleteById(registrations.get(0).getId());

        int newCount = Math.max(activity.getCurrentParticipants() - 1, 0);
        activity.setCurrentParticipants(newCount);
        activityPostMapper.updateById(activity);
    }

    @Override
    public PageResult<Map<String, Object>> getMyRegistrations(Long userId, int page, int size) {
        Page<ActivityRegistration> p = new Page<>(page, size);
        IPage<ActivityRegistration> pageResult = activityRegistrationMapper.selectPage(p,
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getStudentId, userId)
                        .orderByDesc(ActivityRegistration::getRegisteredAt)
        );

        List<Map<String, Object>> records = new ArrayList<>();
        for (ActivityRegistration reg : pageResult.getRecords()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", reg.getId());
            item.put("activityId", reg.getActivityId());
            item.put("studentId", reg.getStudentId());
            item.put("registeredAt", reg.getRegisteredAt());
            item.put("status", reg.getStatus());

            ActivityPost activity = activityPostMapper.selectById(reg.getActivityId());
            if (activity != null) {
                item.put("title", activity.getTitle());
                item.put("category", activity.getCategory());
                item.put("eventTime", activity.getEventTime());
                item.put("location", activity.getLocation());
            }
            records.add(item);
        }
        return new PageResult<>(records, pageResult.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void deleteActivity(Long id, Long userId) {
        ActivityPost activity = activityPostMapper.selectById(id);
        if (activity == null) {
            throw new IllegalArgumentException("该活动不存在");
        }
        checkDeletePermission(activity.getPublisherId(), userId);
        activityRegistrationMapper.delete(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .eq(ActivityRegistration::getActivityId, id));
        activityPostMapper.deleteById(id);
    }

    private void checkDeletePermission(Long publisherId, Long userId) {
        if (publisherId.equals(userId)) return;
        SysUser currentUser = sysUserMapper.selectById(userId);
        if (currentUser != null && "ADMIN".equals(currentUser.getRole())) return;
        SysUser publisher = sysUserMapper.selectById(publisherId);
        if (publisher != null && "ADMIN".equals(publisher.getRole())) {
            throw new IllegalArgumentException("不能删除管理员发布的内容");
        }
        throw new IllegalArgumentException("只能删除自己发布的内容");
    }

    @Override
    public byte[] exportActivityRegistrations() {
        List<ActivityRegistration> registrations = activityRegistrationMapper.selectList(
                new LambdaQueryWrapper<ActivityRegistration>()
                        .in(ActivityRegistration::getStatus, "REGISTERED", "SIGNED_IN")
                        .orderByDesc(ActivityRegistration::getRegisteredAt)
        );
        try (var wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            var sheet = wb.createSheet("活动报名情况");
            var header = sheet.createRow(0);
            String[] cols = {"序号", "学生姓名", "活动名称", "类别", "组织方", "活动时间", "报名时间", "状态"};
            for (int i = 0; i < cols.length; i++) header.createCell(i).setCellValue(cols[i]);

            int rowIdx = 1;
            for (ActivityRegistration reg : registrations) {
                var row = sheet.createRow(rowIdx++);
                SysUser student = sysUserMapper.selectById(reg.getStudentId());
                ActivityPost activity = activityPostMapper.selectById(reg.getActivityId());
                row.createCell(0).setCellValue(rowIdx - 1);
                row.createCell(1).setCellValue(student != null ? student.getRealName() : "");
                row.createCell(2).setCellValue(activity != null ? activity.getTitle() : "");
                row.createCell(3).setCellValue(activity != null ? activity.getCategory() : "");
                row.createCell(4).setCellValue(activity != null ? activity.getOrganizer() : "");
                row.createCell(5).setCellValue(activity != null && activity.getEventTime() != null ? activity.getEventTime().toString() : "");
                row.createCell(6).setCellValue(reg.getRegisteredAt() != null ? reg.getRegisteredAt().toString() : "");
                row.createCell(7).setCellValue("REGISTERED".equals(reg.getStatus()) ? "已报名" : "已签到");
            }
            var baos = new java.io.ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出活动报名数据失败", e);
        }
    }
}
