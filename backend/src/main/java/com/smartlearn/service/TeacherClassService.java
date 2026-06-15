package com.smartlearn.service;

import java.util.List;

public interface TeacherClassService {

    /** 获取教师管理的班级名称列表 */
    List<String> getManagedClassNames(Long teacherId);

    /** 获取教师管理的学生档案ID列表 */
    List<Long> getManagedStudentProfileIds(Long teacherId);

    /** 获取教师管理的学生用户ID列表 */
    List<Long> getManagedStudentUserIds(Long teacherId);

    /** 判断学生（档案ID）是否在教师管理的班级中 */
    boolean isStudentManagedByTeacher(Long studentProfileId, Long teacherId);

    /** 判断学生（用户ID）是否在教师管理的班级中 */
    boolean isStudentUserManagedByTeacher(Long studentUserId, Long teacherId);
}
