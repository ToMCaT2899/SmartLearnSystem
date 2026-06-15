package com.smartlearn.service;

import com.smartlearn.dto.ChangePasswordRequest;
import com.smartlearn.dto.LoginRequest;
import com.smartlearn.dto.RegisterRequest;
import com.smartlearn.vo.LoginVO;

public interface AuthService {
    LoginVO login(LoginRequest request);
    void register(RegisterRequest request);
    LoginVO getCurrentUser(Long userId);
    void changePassword(Long userId, ChangePasswordRequest request);
}
