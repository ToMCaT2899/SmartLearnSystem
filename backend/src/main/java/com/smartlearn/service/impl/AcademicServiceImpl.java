package com.smartlearn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartlearn.common.PageResult;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import com.smartlearn.service.AcademicService;
import com.smartlearn.service.OssService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AcademicServiceImpl implements AcademicService {

    private final GradeMapper gradeMapper;
    private final CourseMapper courseMapper;
    private final AwardRecordMapper awardRecordMapper;
    private final DisciplinaryRecordMapper disciplinaryRecordMapper;
    private final MidtermAssessmentMapper midtermAssessmentMapper;
    private final SecondClassroomScoreMapper secondClassroomScoreMapper;
    private final CompetitionRecordMapper competitionRecordMapper;
    private final StudentProfileMapper studentProfileMapper;
    private final OssService ossService;

    private Long resolveUserId(String studentNo) {
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getStudentNo, studentNo)
        );
        return profile != null ? profile.getUserId() : null;
    }

    @Override
    public PageResult<Grade> getGrades(String studentNo, String semester, int page, int size) {
        Long userId = resolveUserId(studentNo);
        if (userId == null) return new PageResult<>(List.of(), 0, page, size);
        Page<Grade> p = new Page<>(page, size);
        LambdaQueryWrapper<Grade> wrapper = new LambdaQueryWrapper<Grade>()
                .eq(Grade::getStudentId, userId);
        if (StringUtils.hasText(semester)) {
            wrapper.eq(Grade::getSemester, semester);
        }
        IPage<Grade> result = gradeMapper.selectPage(p, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    @Transactional
    public void batchImportGrades(List<Grade> grades) {
        for (Grade grade : grades) {
            // 优先使用 studentNo（学号）解析 studentId
            if (grade.getStudentNo() != null && !grade.getStudentNo().isBlank()) {
                Long resolvedId = resolveUserId(grade.getStudentNo());
                if (resolvedId == null) {
                    throw new IllegalArgumentException("学号不存在: " + grade.getStudentNo());
                }
                grade.setStudentId(resolvedId);
            }
            // 优先使用 courseName（课程名称）解析 courseId
            if (grade.getCourseName() != null && !grade.getCourseName().isBlank()) {
                Course course = courseMapper.selectOne(
                        new LambdaQueryWrapper<Course>()
                                .eq(Course::getCourseName, grade.getCourseName())
                );
                if (course == null) {
                    throw new IllegalArgumentException("课程不存在: " + grade.getCourseName());
                }
                grade.setCourseId(course.getId());
            }
            if (grade.getStudentId() == null) {
                throw new IllegalArgumentException("每条记录必须包含 studentNo（学号）或 studentId");
            }
            if (grade.getCourseId() == null) {
                throw new IllegalArgumentException("每条记录必须包含 courseName（课程名称）或 courseId");
            }
            Grade existing = gradeMapper.selectOne(
                    new LambdaQueryWrapper<Grade>()
                            .eq(Grade::getStudentId, grade.getStudentId())
                            .eq(Grade::getCourseId, grade.getCourseId())
                            .eq(Grade::getSemester, grade.getSemester())
            );
            if (existing != null) {
                grade.setId(existing.getId());
                gradeMapper.updateById(grade);
            } else {
                gradeMapper.insert(grade);
            }
        }
    }

    @Override
    public void deleteGrade(Long id) {
        gradeMapper.deleteById(id);
    }

    @Override
    public List<Course> getAllCourses() {
        return courseMapper.selectList(null);
    }

    @Override
    public Course saveCourse(Course course) {
        if (course.getId() != null) {
            courseMapper.updateById(course);
        } else {
            courseMapper.insert(course);
        }
        return course;
    }

    @Override
    public void deleteCourse(Long id) {
        courseMapper.deleteById(id);
    }

    @Override
    public PageResult<AwardRecord> getAwards(String studentNo, int page, int size) {
        Long userId = resolveUserId(studentNo);
        if (userId == null) return new PageResult<>(List.of(), 0, page, size);
        Page<AwardRecord> p = new Page<>(page, size);
        IPage<AwardRecord> result = awardRecordMapper.selectPage(p,
                new LambdaQueryWrapper<AwardRecord>()
                        .eq(AwardRecord::getStudentId, userId)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public AwardRecord addAward(AwardRecord record) {
        resolveAwardStudentNo(record);
        awardRecordMapper.insert(record);
        return record;
    }

    @Override
    public AwardRecord updateAward(AwardRecord record) {
        resolveAwardStudentNo(record);
        awardRecordMapper.updateById(record);
        return record;
    }

    private void resolveAwardStudentNo(AwardRecord record) {
        if (record.getStudentNo() != null && !record.getStudentNo().isBlank()) {
            Long resolvedId = resolveUserId(record.getStudentNo());
            if (resolvedId == null) {
                throw new IllegalArgumentException("学号不存在: " + record.getStudentNo());
            }
            record.setStudentId(resolvedId);
        }
        if (record.getStudentId() == null) {
            throw new IllegalArgumentException("必须提供 studentNo（学号）或 studentId");
        }
    }

    @Override
    public void deleteAward(Long id) {
        AwardRecord record = awardRecordMapper.selectById(id);
        if (record != null) {
            if (record.getCertificateFile() != null && !record.getCertificateFile().isBlank()) {
                ossService.deleteFile(record.getCertificateFile());
            }
            awardRecordMapper.deleteById(id);
        }
    }

    @Override
    public PageResult<DisciplinaryRecord> getDisciplinaryRecords(String studentNo, int page, int size) {
        Long userId = resolveUserId(studentNo);
        if (userId == null) return new PageResult<>(List.of(), 0, page, size);
        Page<DisciplinaryRecord> p = new Page<>(page, size);
        IPage<DisciplinaryRecord> result = disciplinaryRecordMapper.selectPage(p,
                new LambdaQueryWrapper<DisciplinaryRecord>()
                        .eq(DisciplinaryRecord::getStudentId, userId)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public DisciplinaryRecord addDisciplinaryRecord(DisciplinaryRecord record) {
        disciplinaryRecordMapper.insert(record);
        return record;
    }

    @Override
    public DisciplinaryRecord updateDisciplinaryRecord(DisciplinaryRecord record) {
        disciplinaryRecordMapper.updateById(record);
        return record;
    }

    @Override
    public void deleteDisciplinaryRecord(Long id) {
        disciplinaryRecordMapper.deleteById(id);
    }

    @Override
    public PageResult<MidtermAssessment> getMidtermAssessments(String studentNo, String semester, int page, int size) {
        Long userId = resolveUserId(studentNo);
        if (userId == null) return new PageResult<>(List.of(), 0, page, size);
        Page<MidtermAssessment> p = new Page<>(page, size);
        LambdaQueryWrapper<MidtermAssessment> wrapper = new LambdaQueryWrapper<MidtermAssessment>()
                .eq(MidtermAssessment::getStudentId, userId);
        if (StringUtils.hasText(semester)) {
            wrapper.eq(MidtermAssessment::getSemester, semester);
        }
        wrapper.orderByDesc(MidtermAssessment::getCreatedAt);
        IPage<MidtermAssessment> result = midtermAssessmentMapper.selectPage(p, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public MidtermAssessment saveMidtermAssessment(MidtermAssessment assessment) {
        if (assessment.getId() != null) {
            midtermAssessmentMapper.updateById(assessment);
        } else {
            midtermAssessmentMapper.insert(assessment);
        }
        return assessment;
    }

    @Override
    public void deleteMidtermAssessment(Long id) {
        midtermAssessmentMapper.deleteById(id);
    }

    @Override
    public PageResult<SecondClassroomScore> getSecondClassroomScores(String studentNo, int page, int size) {
        Long userId = resolveUserId(studentNo);
        if (userId == null) return new PageResult<>(List.of(), 0, page, size);
        Page<SecondClassroomScore> p = new Page<>(page, size);
        IPage<SecondClassroomScore> result = secondClassroomScoreMapper.selectPage(p,
                new LambdaQueryWrapper<SecondClassroomScore>()
                        .eq(SecondClassroomScore::getStudentId, userId)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public SecondClassroomScore addSecondClassroomScore(SecondClassroomScore score) {
        secondClassroomScoreMapper.insert(score);
        return score;
    }

    @Override
    public SecondClassroomScore updateSecondClassroomScore(SecondClassroomScore score) {
        secondClassroomScoreMapper.updateById(score);
        return score;
    }

    @Override
    public void deleteSecondClassroomScore(Long id) {
        secondClassroomScoreMapper.deleteById(id);
    }

    @Override
    public PageResult<CompetitionRecord> getCompetitions(String studentNo, int page, int size) {
        Long userId = resolveUserId(studentNo);
        if (userId == null) return new PageResult<>(List.of(), 0, page, size);
        Page<CompetitionRecord> p = new Page<>(page, size);
        IPage<CompetitionRecord> result = competitionRecordMapper.selectPage(p,
                new LambdaQueryWrapper<CompetitionRecord>()
                        .eq(CompetitionRecord::getStudentId, userId)
        );
        return new PageResult<>(result.getRecords(), result.getTotal(), page, size);
    }

    @Override
    public CompetitionRecord addCompetition(CompetitionRecord record) {
        resolveCompetitionStudentNo(record);
        competitionRecordMapper.insert(record);
        return record;
    }

    @Override
    public CompetitionRecord updateCompetition(CompetitionRecord record) {
        resolveCompetitionStudentNo(record);
        competitionRecordMapper.updateById(record);
        return record;
    }

    private void resolveCompetitionStudentNo(CompetitionRecord record) {
        if (record.getStudentNo() != null && !record.getStudentNo().isBlank()) {
            Long resolvedId = resolveUserId(record.getStudentNo());
            if (resolvedId == null) {
                throw new IllegalArgumentException("学号不存在: " + record.getStudentNo());
            }
            record.setStudentId(resolvedId);
        }
        if (record.getStudentId() == null) {
            throw new IllegalArgumentException("必须提供 studentNo（学号）或 studentId");
        }
    }

    @Override
    public void deleteCompetition(Long id) {
        CompetitionRecord record = competitionRecordMapper.selectById(id);
        if (record != null) {
            if (record.getCertificateFile() != null && !record.getCertificateFile().isBlank()) {
                ossService.deleteFile(record.getCertificateFile());
            }
            competitionRecordMapper.deleteById(id);
        }
    }
}
