package com.smartlearn.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.entity.StudentProfile;
import com.smartlearn.entity.TeacherClass;
import com.smartlearn.mapper.StudentProfileMapper;
import com.smartlearn.mapper.TeacherClassMapper;
import com.smartlearn.service.TeacherClassService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TeacherClassServiceImpl implements TeacherClassService {

    private final TeacherClassMapper teacherClassMapper;
    private final StudentProfileMapper studentProfileMapper;

    @Override
    public List<String> getManagedClassNames(Long teacherId) {
        List<TeacherClass> list = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId)
        );
        return list.stream()
                .map(TeacherClass::getClassName)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getManagedStudentProfileIds(Long teacherId) {
        List<String> classNames = getManagedClassNames(teacherId);
        if (classNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudentProfile> profiles = studentProfileMapper.selectList(
                new LambdaQueryWrapper<StudentProfile>()
                        .in(StudentProfile::getClassName, classNames)
        );
        return profiles.stream()
                .map(StudentProfile::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getManagedStudentUserIds(Long teacherId) {
        List<String> classNames = getManagedClassNames(teacherId);
        if (classNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<StudentProfile> profiles = studentProfileMapper.selectList(
                new LambdaQueryWrapper<StudentProfile>()
                        .in(StudentProfile::getClassName, classNames)
        );
        return profiles.stream()
                .map(StudentProfile::getUserId)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public boolean isStudentManagedByTeacher(Long studentProfileId, Long teacherId) {
        StudentProfile profile = studentProfileMapper.selectById(studentProfileId);
        if (profile == null) {
            return false;
        }
        List<String> classNames = getManagedClassNames(teacherId);
        return classNames.contains(profile.getClassName());
    }

    @Override
    public boolean isStudentUserManagedByTeacher(Long studentUserId, Long teacherId) {
        StudentProfile profile = studentProfileMapper.selectOne(
                new LambdaQueryWrapper<StudentProfile>()
                        .eq(StudentProfile::getUserId, studentUserId)
        );
        if (profile == null) {
            return false;
        }
        List<String> classNames = getManagedClassNames(teacherId);
        return classNames.contains(profile.getClassName());
    }
}
