package com.smartlearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.common.PageResult;
import com.smartlearn.common.Result;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.MidtermAssessmentMapper;
import com.smartlearn.mapper.StudentProfileMapper;
import com.smartlearn.security.SecurityUtil;
import com.smartlearn.security.UserPrincipal;
import com.smartlearn.service.AcademicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academic")
@RequiredArgsConstructor
public class AcademicController {

    private final AcademicService academicService;
    private final SecurityUtil securityUtil;
    private final StudentProfileMapper studentProfileMapper;
    private final MidtermAssessmentMapper midtermAssessmentMapper;

    /**
     * 学号 → userId，学生未传学号时自动从当前登录用户解析。
     */
    private String resolveStudentNo(String studentNo, UserPrincipal principal) {
        if (studentNo != null && !studentNo.isBlank()) {
            if (securityUtil.isStudent()) {
                StudentProfile self = studentProfileMapper.selectOne(
                        new LambdaQueryWrapper<StudentProfile>()
                                .eq(StudentProfile::getUserId, principal.getUserId())
                );
                if (self == null || !studentNo.equals(self.getStudentNo())) {
                    throw new IllegalArgumentException("仅可查看自己的数据");
                }
            }
            return studentNo;
        }
        // 未传学号，从当前用户解析
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, principal.getUserId())
        );
        if (profile == null) {
            throw new IllegalArgumentException("当前用户未绑定学生档案");
        }
        return profile.getStudentNo();
    }

    @GetMapping("/grades")
    public Result<PageResult<Grade>> getGrades(
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String semester,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sno = resolveStudentNo(studentNo, principal);
        return Result.success(academicService.getGrades(sno, semester, page, size));
    }

    @PostMapping("/grades/batch")
    public Result<Void> batchImportGrades(@RequestBody List<Grade> grades) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可导入成绩");
        }
        academicService.batchImportGrades(grades);
        return Result.success("批量导入成功", null);
    }

    @DeleteMapping("/grades/{id}")
    public Result<Void> deleteGrade(@PathVariable Long id) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可删除成绩");
        }
        academicService.deleteGrade(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/courses")
    public Result<List<Course>> getAllCourses() {
        return Result.success(academicService.getAllCourses());
    }

    @PostMapping("/courses")
    public Result<Course> saveCourse(@Valid @RequestBody Course course) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可管理课程");
        }
        return Result.success(academicService.saveCourse(course));
    }

    @DeleteMapping("/courses/{id}")
    public Result<Void> deleteCourse(@PathVariable Long id) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可删除课程");
        }
        academicService.deleteCourse(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/awards")
    public Result<PageResult<AwardRecord>> getAwards(
            @RequestParam(required = false) String studentNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sno = resolveStudentNo(studentNo, principal);
        return Result.success(academicService.getAwards(sno, page, size));
    }

    @PostMapping("/awards")
    public Result<AwardRecord> addAward(@Valid @RequestBody AwardRecord record,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        if (record.getStudentNo() == null || record.getStudentNo().isBlank()) {
            record.setStudentNo(resolveStudentNo(null, principal));
        } else if (securityUtil.isStudent()) {
            String selfNo = resolveStudentNo(null, principal);
            if (!selfNo.equals(record.getStudentNo())) {
                return Result.forbidden("仅可添加自己的获奖记录");
            }
        }
        return Result.success(academicService.addAward(record));
    }

    @PutMapping("/awards/{id}")
    public Result<AwardRecord> updateAward(@PathVariable Long id, @Valid @RequestBody AwardRecord record) {
        record.setId(id);
        return Result.success(academicService.updateAward(record));
    }

    @DeleteMapping("/awards/{id}")
    public Result<Void> deleteAward(@PathVariable Long id) {
        academicService.deleteAward(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/discipline")
    public Result<PageResult<DisciplinaryRecord>> getDisciplinaryRecords(
            @RequestParam(required = false) String studentNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sno = resolveStudentNo(studentNo, principal);
        return Result.success(academicService.getDisciplinaryRecords(sno, page, size));
    }

    @PostMapping("/discipline")
    public Result<DisciplinaryRecord> addDisciplinaryRecord(
            @Valid @RequestBody DisciplinaryRecord record,
            @RequestParam(required = false) String studentNo) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可添加违纪记录");
        }
        if ((record.getStudentId() == null || record.getStudentId() == 0)
                && studentNo != null && !studentNo.isBlank()) {
            StudentProfile profile = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>()
                            .eq(StudentProfile::getStudentNo, studentNo)
            );
            if (profile == null) {
                return Result.error(400, "学号不存在: " + studentNo);
            }
            record.setStudentId(profile.getUserId());
        }
        return Result.success(academicService.addDisciplinaryRecord(record));
    }

    @PutMapping("/discipline/{id}")
    public Result<DisciplinaryRecord> updateDisciplinaryRecord(@PathVariable Long id,
                                                               @Valid @RequestBody DisciplinaryRecord record) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可更新违纪记录");
        }
        record.setId(id);
        return Result.success(academicService.updateDisciplinaryRecord(record));
    }

    @DeleteMapping("/discipline/{id}")
    public Result<Void> deleteDisciplinaryRecord(@PathVariable Long id) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可删除违纪记录");
        }
        academicService.deleteDisciplinaryRecord(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/midterm")
    public Result<PageResult<MidtermAssessment>> getMidtermAssessments(
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) String semester,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sno = resolveStudentNo(studentNo, principal);
        return Result.success(academicService.getMidtermAssessments(sno, semester, page, size));
    }

    @PostMapping("/midterm")
    public Result<MidtermAssessment> saveMidtermAssessment(@Valid @RequestBody MidtermAssessment assessment,
                                                           @RequestParam(required = false) String studentNo) {
        if ((assessment.getStudentId() == null || assessment.getStudentId() == 0)
                && studentNo != null && !studentNo.isBlank()) {
            StudentProfile profile = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>()
                            .eq(StudentProfile::getStudentNo, studentNo)
            );
            if (profile == null) {
                return Result.error(400, "学号不存在: " + studentNo);
            }
            assessment.setStudentId(profile.getUserId());
        }
        if (securityUtil.isStudent()) {
            // Student: only allow updating selfSummary, preserve advisor fields
            if (assessment.getId() != null) {
                MidtermAssessment existing = midtermAssessmentMapper.selectById(assessment.getId());
                if (existing == null) {
                    return Result.error(404, "评估记录不存在");
                }
                existing.setSelfSummary(assessment.getSelfSummary());
                existing.setStatus("PENDING");
                midtermAssessmentMapper.updateById(existing);
                return Result.success(existing);
            }
            assessment.setAdvisorComment(null);
            assessment.setScore(null);
            assessment.setStatus("PENDING");
        } else if (securityUtil.isTeacher() || securityUtil.isAdmin()) {
            // Teacher/Admin: update advisor comment and score
            if (assessment.getId() != null) {
                MidtermAssessment existing = midtermAssessmentMapper.selectById(assessment.getId());
                if (existing == null) {
                    return Result.error(404, "评估记录不存在");
                }
                existing.setAdvisorComment(assessment.getAdvisorComment());
                existing.setScore(assessment.getScore());
                existing.setStatus("REVIEWED");
                midtermAssessmentMapper.updateById(existing);
                return Result.success(existing);
            }
            // New record: preserve incoming status or default to DRAFT
            if (assessment.getStatus() == null || assessment.getStatus().isBlank()) {
                assessment.setStatus("DRAFT");
            }
        } else {
            return Result.forbidden("无权操作");
        }
        return Result.success(academicService.saveMidtermAssessment(assessment));
    }

    @DeleteMapping("/midterm/{id}")
    public Result<Void> deleteMidtermAssessment(@PathVariable Long id) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可删除评估");
        }
        academicService.deleteMidtermAssessment(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/second-classroom")
    public Result<PageResult<SecondClassroomScore>> getSecondClassroomScores(
            @RequestParam(required = false) String studentNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sno = resolveStudentNo(studentNo, principal);
        return Result.success(academicService.getSecondClassroomScores(sno, page, size));
    }

    @PostMapping("/second-classroom")
    public Result<SecondClassroomScore> addSecondClassroomScore(
            @Valid @RequestBody SecondClassroomScore score,
            @RequestParam(required = false) String studentNo) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可添加第二课堂成绩");
        }
        if ((score.getStudentId() == null || score.getStudentId() == 0)
                && studentNo != null && !studentNo.isBlank()) {
            StudentProfile profile = studentProfileMapper.selectOne(
                    new LambdaQueryWrapper<StudentProfile>()
                            .eq(StudentProfile::getStudentNo, studentNo)
            );
            if (profile == null) {
                return Result.error(400, "学号不存在: " + studentNo);
            }
            score.setStudentId(profile.getUserId());
        }
        return Result.success(academicService.addSecondClassroomScore(score));
    }

    @PutMapping("/second-classroom/{id}")
    public Result<SecondClassroomScore> updateSecondClassroomScore(@PathVariable Long id,
                                                                   @Valid @RequestBody SecondClassroomScore score) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可更新第二课堂成绩");
        }
        score.setId(id);
        return Result.success(academicService.updateSecondClassroomScore(score));
    }

    @DeleteMapping("/second-classroom/{id}")
    public Result<Void> deleteSecondClassroomScore(@PathVariable Long id) {
        if (!securityUtil.isTeacher() && !securityUtil.isAdmin()) {
            return Result.forbidden("仅教师和管理员可删除第二课堂成绩");
        }
        academicService.deleteSecondClassroomScore(id);
        return Result.success("删除成功", null);
    }

    @GetMapping("/competitions")
    public Result<PageResult<CompetitionRecord>> getCompetitions(
            @RequestParam(required = false) String studentNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        String sno = resolveStudentNo(studentNo, principal);
        return Result.success(academicService.getCompetitions(sno, page, size));
    }

    @PostMapping("/competitions")
    public Result<CompetitionRecord> addCompetition(@Valid @RequestBody CompetitionRecord record,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (record.getStudentNo() == null || record.getStudentNo().isBlank()) {
            record.setStudentNo(resolveStudentNo(null, principal));
        } else if (securityUtil.isStudent()) {
            String selfNo = resolveStudentNo(null, principal);
            if (!selfNo.equals(record.getStudentNo())) {
                return Result.forbidden("仅可添加自己的竞赛记录");
            }
        }
        return Result.success(academicService.addCompetition(record));
    }

    @PutMapping("/competitions/{id}")
    public Result<CompetitionRecord> updateCompetition(@PathVariable Long id,
                                                       @Valid @RequestBody CompetitionRecord record) {
        record.setId(id);
        return Result.success(academicService.updateCompetition(record));
    }

    @DeleteMapping("/competitions/{id}")
    public Result<Void> deleteCompetition(@PathVariable Long id) {
        academicService.deleteCompetition(id);
        return Result.success("删除成功", null);
    }
}
