package com.smartlearn.service;

import com.smartlearn.entity.*;

import com.smartlearn.common.PageResult;

import java.util.List;

public interface AcademicService {

    PageResult<Grade> getGrades(String studentNo, String semester, int page, int size);

    void batchImportGrades(List<Grade> grades);

    void deleteGrade(Long id);

    List<Course> getAllCourses();

    Course saveCourse(Course course);

    void deleteCourse(Long id);

    PageResult<AwardRecord> getAwards(String studentNo, int page, int size);

    AwardRecord addAward(AwardRecord record);

    AwardRecord updateAward(AwardRecord record);

    void deleteAward(Long id);

    PageResult<DisciplinaryRecord> getDisciplinaryRecords(String studentNo, int page, int size);

    DisciplinaryRecord addDisciplinaryRecord(DisciplinaryRecord record);

    DisciplinaryRecord updateDisciplinaryRecord(DisciplinaryRecord record);

    void deleteDisciplinaryRecord(Long id);

    PageResult<MidtermAssessment> getMidtermAssessments(String studentNo, String semester, int page, int size);

    MidtermAssessment saveMidtermAssessment(MidtermAssessment assessment);

    void deleteMidtermAssessment(Long id);

    PageResult<SecondClassroomScore> getSecondClassroomScores(String studentNo, int page, int size);

    SecondClassroomScore addSecondClassroomScore(SecondClassroomScore score);

    SecondClassroomScore updateSecondClassroomScore(SecondClassroomScore score);

    void deleteSecondClassroomScore(Long id);

    PageResult<CompetitionRecord> getCompetitions(String studentNo, int page, int size);

    CompetitionRecord addCompetition(CompetitionRecord record);

    CompetitionRecord updateCompetition(CompetitionRecord record);

    void deleteCompetition(Long id);
}
