package com.smartlearn.service;

import com.smartlearn.entity.*;

import java.util.List;

public interface StudentInfoService {

    List<StudentProfile> listStudentProfiles();

    List<StudentProfile> searchStudentProfiles(String keyword);

    StudentProfile getProfile(Long userId);

    StudentProfile getProfileById(Long studentId);

    void updateProfile(StudentProfile profile);

    List<FamilyMember> getFamilyMembers(Long studentId);

    void saveFamilyMembers(Long studentId, List<FamilyMember> members);

    List<EmergencyContact> getEmergencyContacts(Long studentId);

    void saveEmergencyContacts(Long studentId, List<EmergencyContact> contacts);

    List<SupportingDocument> getDocuments(Long studentId);

    void uploadDocument(Long studentId, String docType, String filePath);

    List<InfoChangeRequest> getChangeRequests(Long studentId);

    void submitChangeRequest(Long studentId, InfoChangeRequest request);

    void reviewChangeRequest(Long requestId, String status, String comment, Long reviewerId);

    void cancelChangeRequest(Long requestId, Long userId);

    byte[] exportStudentInfo(Long studentId);
}
