package com.smartlearn.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.entity.StudentProfile;
import com.smartlearn.entity.TeacherClass;
import com.smartlearn.mapper.StudentProfileMapper;
import com.smartlearn.mapper.TeacherClassMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AccessChecker {

    private final SecurityUtil securityUtil;
    private final TeacherClassMapper teacherClassMapper;
    private final StudentProfileMapper studentProfileMapper;

    /**
     * Returns user IDs of students in the current teacher's managed classes.
     * Returns null for admin (meaning "all students").
     * Returns empty list for teacher with no class mappings.
     */
    public List<Long> getManagedStudentIds() {
        if (securityUtil.isAdmin()) {
            return null;
        }
        Long teacherId = securityUtil.getCurrentUserId();
        List<TeacherClass> teacherClasses = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId)
        );
        if (teacherClasses.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> classList = teacherClasses.stream()
                .map(TeacherClass::getClassName)
                .distinct()
                .collect(Collectors.toList());
        List<StudentProfile> profiles = studentProfileMapper.selectList(
                new LambdaQueryWrapper<StudentProfile>()
                        .in(StudentProfile::getClassName, classList)
        );
        return profiles.stream()
                .map(StudentProfile::getUserId)
                .collect(Collectors.toList());
    }

    /**
     * Checks whether the current user can access the given student's data.
     * Admins can access all. Anyone can access themselves.
     * Teachers can access students in their managed classes.
     */
    public void checkStudentAccess(Long studentId) {
        if (securityUtil.isAdmin()) return;
        if (securityUtil.getCurrentUserId().equals(studentId)) return;
        List<Long> managed = getManagedStudentIds();
        if (managed != null && !managed.contains(studentId)) {
            throw new IllegalArgumentException("无权查看该学生的数据");
        }
    }

    /** @throws AccessDeniedException if the current user has none of the specified roles. */
    public void requireAnyRole(String... roles) {
        String currentRole = securityUtil.getCurrentUserRole();
        for (String role : roles) {
            if (role.equals(currentRole)) return;
        }
        throw new AccessDeniedException("权限不足，需要角色: " + String.join(", ", roles));
    }

    /** @throws AccessDeniedException if the current user is not a teacher or admin. */
    public void requireTeacherOrAdmin() {
        requireAnyRole("TEACHER", "ADMIN");
    }

    /** @throws AccessDeniedException if the current user is not an admin. */
    public void requireAdmin() {
        requireAnyRole("ADMIN");
    }

    /** @throws AccessDeniedException if the current user is not a student. */
    public void requireStudent() {
        requireAnyRole("STUDENT");
    }
}
