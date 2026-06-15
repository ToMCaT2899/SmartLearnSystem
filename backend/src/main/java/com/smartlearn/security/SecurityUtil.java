package com.smartlearn.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtil {

    public UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) auth.getPrincipal();
        }
        throw new RuntimeException("用户未登录");
    }

    public Long getCurrentUserId() {
        return getCurrentUser().getUserId();
    }

    public String getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    public boolean isStudent() {
        return "STUDENT".equals(getCurrentUserRole());
    }

    public boolean isTeacher() {
        return "TEACHER".equals(getCurrentUserRole());
    }

    public boolean isAdmin() {
        return "ADMIN".equals(getCurrentUserRole());
    }
}
