package com.smartlearn.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartlearn.common.Result;
import com.smartlearn.entity.*;
import com.smartlearn.mapper.*;
import com.smartlearn.security.RequireAdmin;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@RequireAdmin
public class TeacherClassController {

    private final TeacherClassMapper teacherClassMapper;
    private final SysUserMapper sysUserMapper;

    @GetMapping("/teachers/{teacherId}/classes")
    public Result<List<TeacherClass>> getTeacherClasses(@PathVariable Long teacherId) {
        List<TeacherClass> list = teacherClassMapper.selectList(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId)
        );
        return Result.success(list);
    }

    @PostMapping("/teachers/{teacherId}/classes")
    public Result<TeacherClass> addTeacherClass(@PathVariable Long teacherId, @RequestBody TeacherClass tc) {
        SysUser teacher = sysUserMapper.selectById(teacherId);
        if (teacher == null || !"TEACHER".equals(teacher.getRole())) {
            return Result.error("用户不存在或不是教师");
        }
        Long count = teacherClassMapper.selectCount(
                new LambdaQueryWrapper<TeacherClass>()
                        .eq(TeacherClass::getTeacherId, teacherId)
                        .eq(TeacherClass::getClassName, tc.getClassName())
        );
        if (count > 0) {
            return Result.error("该班级已关联");
        }
        tc.setId(null);
        tc.setTeacherId(teacherId);
        teacherClassMapper.insert(tc);
        return Result.success(tc);
    }

    @DeleteMapping("/teachers/classes/{id}")
    public Result<Void> removeTeacherClass(@PathVariable Long id) {
        teacherClassMapper.deleteById(id);
        return Result.success();
    }

    @GetMapping("/teachers")
    public Result<List<SysUser>> listTeachers() {
        List<SysUser> teachers = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRole, "TEACHER")
        );
        return Result.success(teachers);
    }
}
