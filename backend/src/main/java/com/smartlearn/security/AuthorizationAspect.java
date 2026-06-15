package com.smartlearn.security;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuthorizationAspect {

    private final AccessChecker accessChecker;

    @Before("@annotation(com.smartlearn.security.RequireAdmin) || @within(com.smartlearn.security.RequireAdmin)")
    public void checkAdmin() {
        accessChecker.requireAdmin();
    }

    @Before("@annotation(com.smartlearn.security.RequireTeacher) || @within(com.smartlearn.security.RequireTeacher)")
    public void checkTeacher() {
        accessChecker.requireAnyRole("TEACHER");
    }

    @Before("@annotation(com.smartlearn.security.RequireTeacherOrAdmin) || @within(com.smartlearn.security.RequireTeacherOrAdmin)")
    public void checkTeacherOrAdmin() {
        accessChecker.requireTeacherOrAdmin();
    }

    @Before("@annotation(com.smartlearn.security.RequireStudent) || @within(com.smartlearn.security.RequireStudent)")
    public void checkStudent() {
        accessChecker.requireStudent();
    }
}
