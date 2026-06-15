package com.smartlearn.service;

import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;
import java.util.List;
import java.util.Map;

public interface CommunicationService {

    // Private messages
    PrivateMessage sendMessage(PrivateMessage message);

    List<PrivateMessage> getConversation(Long userId1, Long userId2);

    List<PrivateMessage> getUnreadMessages(Long receiverId);

    List<PrivateMessage> getAllMessages(Long userId);

    void markAsRead(Long messageId);

    void markConversationAsRead(Long userId, Long contactId);

    void deleteMessage(Long id);

    void clearConversation(Long userId1, Long userId2);

    List<Map<String, Object>> getContacts(Long userId);

    Map<String, Object> findUserByStudentNo(String studentNo);

    // Class notices
    ClassNotice createClassNotice(ClassNotice notice);

    PageResult<ClassNotice> getClassNotices(String classIdentifier, int page, int size);

    void updateClassNotice(ClassNotice notice);

    void deleteClassNotice(Long id, Long userId);

    // Feedback
    Feedback submitFeedback(Feedback feedback);

    PageResult<Feedback> getMyFeedback(Long userId, int page, int size);

    PageResult<Feedback> getPendingFeedback(int page, int size);

    PageResult<Feedback> getAllFeedback(int page, int size);

    void deleteFeedback(Long id, Long userId);

    void replyFeedback(Long id, String reply);

    // Activities
    ActivityPost createActivity(ActivityPost activity);

    PageResult<ActivityPost> getActivities(Long userId, String category, int page, int size);

    ActivityPost getActivityById(Long id);

    void updateActivity(ActivityPost activity);

    void registerForActivity(Long activityId, Long userId);

    void cancelRegistration(Long activityId, Long userId);

    PageResult<Map<String, Object>> getMyRegistrations(Long userId, int page, int size);

    void deleteActivity(Long id, Long userId);

    byte[] exportActivityRegistrations();
}
