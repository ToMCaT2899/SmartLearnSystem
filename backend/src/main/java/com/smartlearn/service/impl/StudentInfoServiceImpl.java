package com.smartlearn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import com.smartlearn.security.SecurityUtil;
import com.smartlearn.service.StudentInfoService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentInfoServiceImpl implements StudentInfoService {

    private final StudentProfileMapper studentProfileMapper;
    private final FamilyMemberMapper familyMemberMapper;
    private final EmergencyContactMapper emergencyContactMapper;
    private final SupportingDocumentMapper supportingDocumentMapper;
    private final InfoChangeRequestMapper infoChangeRequestMapper;
    private final SecurityUtil securityUtil;

    @Override
    public List<StudentProfile> listStudentProfiles() {
        return studentProfileMapper.selectList(
                new LambdaQueryWrapper<StudentProfile>()
                        .orderByAsc(StudentProfile::getStudentNo)
        );
    }

    @Override
    public List<StudentProfile> searchStudentProfiles(String keyword) {
        return studentProfileMapper.selectList(
                new LambdaQueryWrapper<StudentProfile>()
                        .and(w -> w
                                .like(StudentProfile::getName, keyword)
                                .or()
                                .like(StudentProfile::getStudentNo, keyword)
                                .or()
                                .like(StudentProfile::getPhone, keyword)
                                .or()
                                .like(StudentProfile::getCollege, keyword)
                                .or()
                                .like(StudentProfile::getMajor, keyword)
                        )
                        .orderByAsc(StudentProfile::getStudentNo)
        );
    }

    @Override
    public StudentProfile getProfile(Long userId) {
        return studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, userId)
        );
    }

    @Override
    public StudentProfile getProfileById(Long studentId) {
        return studentProfileMapper.selectById(studentId);
    }

    @Override
    @Transactional
    public void updateProfile(StudentProfile profile) {
        StudentProfile existing = studentProfileMapper.selectById(profile.getId());
        if (existing == null) {
            throw new IllegalArgumentException("学生档案不存在");
        }
        BeanUtil.copyProperties(profile, existing, CopyOptions.create().setIgnoreNullValue(true));
        existing.setUpdatedAt(LocalDateTime.now());
        studentProfileMapper.updateById(existing);
    }

    @Override
    public List<FamilyMember> getFamilyMembers(Long studentId) {
        return familyMemberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getStudentId, studentId)
        );
    }

    @Override
    @Transactional
    public void saveFamilyMembers(Long studentId, List<FamilyMember> members) {
        familyMemberMapper.delete(
                new LambdaQueryWrapper<FamilyMember>()
                        .eq(FamilyMember::getStudentId, studentId)
        );
        if (members != null && !members.isEmpty()) {
            for (FamilyMember member : members) {
                member.setId(null);
                member.setStudentId(studentId);
                familyMemberMapper.insert(member);
            }
        }
    }

    @Override
    public List<EmergencyContact> getEmergencyContacts(Long studentId) {
        return emergencyContactMapper.selectList(
                new LambdaQueryWrapper<EmergencyContact>()
                        .eq(EmergencyContact::getStudentId, studentId)
        );
    }

    @Override
    @Transactional
    public void saveEmergencyContacts(Long studentId, List<EmergencyContact> contacts) {
        emergencyContactMapper.delete(
                new LambdaQueryWrapper<EmergencyContact>()
                        .eq(EmergencyContact::getStudentId, studentId)
        );
        if (contacts != null && !contacts.isEmpty()) {
            for (EmergencyContact contact : contacts) {
                contact.setId(null);
                contact.setStudentId(studentId);
                emergencyContactMapper.insert(contact);
            }
        }
    }

    @Override
    public List<SupportingDocument> getDocuments(Long studentId) {
        return supportingDocumentMapper.selectList(
                new LambdaQueryWrapper<SupportingDocument>()
                        .eq(SupportingDocument::getStudentId, studentId)
        );
    }

    @Override
    public void uploadDocument(Long studentId, String docType, String filePath) {
        SupportingDocument doc = new SupportingDocument();
        doc.setStudentId(studentId);
        doc.setDocType(docType);
        doc.setFilePath(filePath);
        doc.setUploadTime(LocalDateTime.now());
        supportingDocumentMapper.insert(doc);
    }

    @Override
    public List<InfoChangeRequest> getChangeRequests(Long studentId) {
        return infoChangeRequestMapper.selectList(
                new LambdaQueryWrapper<InfoChangeRequest>()
                        .eq(InfoChangeRequest::getStudentId, studentId)
                        .orderByDesc(InfoChangeRequest::getCreatedAt)
        );
    }

    @Override
    public void submitChangeRequest(Long studentId, InfoChangeRequest request) {
        request.setId(null);
        request.setStudentId(studentId);
        request.setStatus("PENDING");
        request.setCreatedAt(LocalDateTime.now());
        infoChangeRequestMapper.insert(request);
    }

    @Override
    @Transactional
    public void reviewChangeRequest(Long requestId, String status, String comment, Long reviewerId) {
        InfoChangeRequest request = infoChangeRequestMapper.selectById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("变更申请不存在");
        }
        request.setStatus(status);
        request.setReviewComment(comment);
        request.setReviewerId(reviewerId);
        request.setReviewedAt(LocalDateTime.now());

        // If approved, update the corresponding field in the student profile
        if ("APPROVED".equals(status)) {
            StudentProfile profile = studentProfileMapper.selectById(request.getStudentId());
            if (profile != null) {
                switch (request.getFieldName()) {
                    case "name":
                        profile.setName(request.getNewValue());
                        break;
                    case "phone":
                        profile.setPhone(request.getNewValue());
                        break;
                    case "college":
                        profile.setCollege(request.getNewValue());
                        break;
                    case "major":
                        profile.setMajor(request.getNewValue());
                        break;
                    case "grade":
                        profile.setGrade(request.getNewValue());
                        break;
                    case "className":
                        profile.setClassName(request.getNewValue());
                        break;
                    case "campus":
                        profile.setCampus(request.getNewValue());
                        break;
                    case "dormitory":
                        profile.setDormitory(request.getNewValue());
                        break;
                    case "offCampusAddress":
                        profile.setOffCampusAddress(request.getNewValue());
                        break;
                    case "idCard":
                        profile.setIdCard(request.getNewValue());
                        break;
                    case "medicalHistory":
                        profile.setMedicalHistory(request.getNewValue());
                        break;
                    case "hobbies":
                        profile.setHobbies(request.getNewValue());
                        break;
                    case "personality":
                        profile.setPersonality(request.getNewValue());
                        break;
                    case "careerGoal":
                        profile.setCareerGoal(request.getNewValue());
                        break;
                    case "advisorName":
                        profile.setAdvisorName(request.getNewValue());
                        break;
                    case "advisorPhone":
                        profile.setAdvisorPhone(request.getNewValue());
                        break;
                    case "difficultyLevel":
                        profile.setDifficultyLevel(request.getNewValue());
                        break;
                    default:
                        break;
                }
                profile.setUpdatedAt(LocalDateTime.now());
                studentProfileMapper.updateById(profile);
            }
        }

        infoChangeRequestMapper.updateById(request);
    }

    @Override
    public void cancelChangeRequest(Long requestId, Long userId) {
        InfoChangeRequest request = infoChangeRequestMapper.selectById(requestId);
        if (request == null) {
            throw new IllegalArgumentException("变更申请不存在");
        }
        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("仅可取消审核中的申请");
        }
        // Resolve the student profile to verify ownership
        StudentProfile profile = studentProfileMapper.selectById(request.getStudentId());
        if (profile == null || !profile.getUserId().equals(userId)) {
            throw new AccessDeniedException("仅可取消自己的申请");
        }
        request.setStatus("CANCELLED");
        request.setReviewedAt(LocalDateTime.now());
        infoChangeRequestMapper.updateById(request);
    }

    @Override
    public byte[] exportStudentInfo(Long studentId) {
        // Admin, teacher, or the student themselves can export
        if (!securityUtil.isAdmin() && !securityUtil.isTeacher()) {
            Long currentUserId = securityUtil.getCurrentUserId();
            StudentProfile profile = studentProfileMapper.selectById(studentId);
            if (profile == null || !profile.getUserId().equals(currentUserId)) {
                throw new AccessDeniedException("无权导出该学生信息");
            }
        }

        StudentProfile profile = studentProfileMapper.selectById(studentId);
        if (profile == null) {
            throw new IllegalArgumentException("学生档案不存在");
        }
        List<FamilyMember> familyList = familyMemberMapper.selectList(
                new LambdaQueryWrapper<FamilyMember>().eq(FamilyMember::getStudentId, studentId));
        List<EmergencyContact> contactList = emergencyContactMapper.selectList(
                new LambdaQueryWrapper<EmergencyContact>().eq(EmergencyContact::getStudentId, studentId));
        List<InfoChangeRequest> changeList = infoChangeRequestMapper.selectList(
                new LambdaQueryWrapper<InfoChangeRequest>()
                        .eq(InfoChangeRequest::getStudentId, studentId)
                        .orderByDesc(InfoChangeRequest::getCreatedAt));

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            // ---- Sheet 1: 基本信息 (横向) ----
            var sheet1 = wb.createSheet("基本信息");
            String[] basicFields = {
                    "学号", "姓名", "身份证号", "手机号", "学院", "专业", "年级", "班级",
                    "校区", "宿舍号", "非集中住宿地址", "病史", "爱好", "性格特征",
                    "职业目标", "班主任姓名", "班主任联系方式", "困难认定等级",
            };
            String[] basicValues = {
                    profile.getStudentNo(), profile.getName(), profile.getIdCard(),
                    profile.getPhone(), profile.getCollege(), profile.getMajor(),
                    profile.getGrade(), profile.getClassName(), profile.getCampus(),
                    profile.getDormitory(), profile.getOffCampusAddress(),
                    profile.getMedicalHistory(), profile.getHobbies(),
                    profile.getPersonality(), profile.getCareerGoal(),
                    profile.getAdvisorName(), profile.getAdvisorPhone(),
                    difficultyLabel(profile.getDifficultyLevel()),
            };
            Row header1 = sheet1.createRow(0);
            Row value1 = sheet1.createRow(1);
            for (int i = 0; i < basicFields.length; i++) {
                header1.createCell(i).setCellValue(basicFields[i]);
                value1.createCell(i).setCellValue(nullToEmpty(basicValues[i]));
                sheet1.setColumnWidth(i, 16 * 256);
            }

            // ---- Sheet 2: 家庭成员 ----
            var sheet2 = wb.createSheet("家庭成员");
            Row header2 = sheet2.createRow(0);
            String[] famHeaders = {"序号", "关系", "姓名", "联系电话", "工作单位", "住址"};
            for (int i = 0; i < famHeaders.length; i++) {
                header2.createCell(i).setCellValue(famHeaders[i]);
            }
            int rowIdx = 1;
            for (FamilyMember fm : familyList) {
                Row row = sheet2.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(nullToEmpty(fm.getRelation()));
                row.createCell(2).setCellValue(nullToEmpty(fm.getName()));
                row.createCell(3).setCellValue(nullToEmpty(fm.getPhone()));
                row.createCell(4).setCellValue(nullToEmpty(fm.getWorkUnit()));
                row.createCell(5).setCellValue(nullToEmpty(fm.getAddress()));
                rowIdx++;
            }
            for (int i = 0; i < famHeaders.length; i++) {
                sheet2.setColumnWidth(i, 22 * 256);
            }

            // ---- Sheet 3: 紧急联系人 ----
            var sheet3 = wb.createSheet("紧急联系人");
            Row header3 = sheet3.createRow(0);
            String[] conHeaders = {"序号", "姓名", "关系", "联系电话", "住址"};
            for (int i = 0; i < conHeaders.length; i++) {
                header3.createCell(i).setCellValue(conHeaders[i]);
            }
            rowIdx = 1;
            for (EmergencyContact ec : contactList) {
                Row row = sheet3.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(nullToEmpty(ec.getName()));
                row.createCell(2).setCellValue(nullToEmpty(ec.getRelation()));
                row.createCell(3).setCellValue(nullToEmpty(ec.getPhone()));
                row.createCell(4).setCellValue(nullToEmpty(ec.getAddress()));
                rowIdx++;
            }
            for (int i = 0; i < conHeaders.length; i++) {
                sheet3.setColumnWidth(i, 22 * 256);
            }

            // ---- Sheet 4: 信息变更记录 ----
            var sheet4 = wb.createSheet("信息变更记录");
            Row header4 = sheet4.createRow(0);
            String[] chgHeaders = {"序号", "变更字段", "原值", "新值", "变更原因", "状态", "审核意见", "申请时间", "审核时间"};
            for (int i = 0; i < chgHeaders.length; i++) {
                header4.createCell(i).setCellValue(chgHeaders[i]);
            }
            rowIdx = 1;
            for (InfoChangeRequest cr : changeList) {
                Row row = sheet4.createRow(rowIdx);
                row.createCell(0).setCellValue(rowIdx);
                row.createCell(1).setCellValue(nullToEmpty(cr.getFieldName()));
                row.createCell(2).setCellValue(nullToEmpty(cr.getOldValue()));
                row.createCell(3).setCellValue(nullToEmpty(cr.getNewValue()));
                row.createCell(4).setCellValue(nullToEmpty(cr.getReason()));
                row.createCell(5).setCellValue(statusLabel(cr.getStatus()));
                row.createCell(6).setCellValue(nullToEmpty(cr.getReviewComment()));
                row.createCell(7).setCellValue(cr.getCreatedAt() != null ? cr.getCreatedAt().toString() : "");
                row.createCell(8).setCellValue(cr.getReviewedAt() != null ? cr.getReviewedAt().toString() : "");
                rowIdx++;
            }
            for (int i = 0; i < chgHeaders.length; i++) {
                sheet4.setColumnWidth(i, i >= 7 ? 22 * 256 : 18 * 256);
            }

            var baos = new ByteArrayOutputStream();
            wb.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private String difficultyLabel(String level) {
        if (level == null) return "";
        return switch (level) {
            case "SPECIAL" -> "特别困难";
            case "DIFFICULT" -> "困难";
            case "GENERAL" -> "一般困难";
            case "NONE" -> "不困难";
            default -> level;
        };
    }

    private String statusLabel(String s) {
        if (s == null) return "";
        return switch (s) {
            case "PENDING" -> "审核中";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已拒绝";
            default -> s;
        };
    }
}
