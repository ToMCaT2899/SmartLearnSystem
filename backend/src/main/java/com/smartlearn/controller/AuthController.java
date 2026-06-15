package com.smartlearn.controller;

import com.smartlearn.common.Result;
import com.smartlearn.dto.ChangePasswordRequest;
import com.smartlearn.dto.LoginRequest;
import com.smartlearn.dto.RegisterRequest;
import com.smartlearn.security.UserPrincipal;
import com.smartlearn.service.AuthService;
import com.smartlearn.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功，请登录", null);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                       @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUserId(), request);
        return Result.success("密码修改成功", null);
    }

    @GetMapping("/me")
    public Result<LoginVO> me(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.success(authService.getCurrentUser(principal.getUserId()));
    }
}
