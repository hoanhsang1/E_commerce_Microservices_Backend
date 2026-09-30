package com.vti.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.vti.dto.AuthResponse;
import com.vti.form.AuthRequest;
import com.vti.form.RefreshTokenRequest;
import com.vti.form.RegisterRequest;
import com.vti.service.IAuthService;
import com.vti.service.IRefreshTokenService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    @Autowired
    private IAuthService authService;

    @Autowired
    private IRefreshTokenService refreshTokenService;

    /**
     * API Đăng nhập truyền thống:
     * Nhận username & password -> Trả về Access Token (JWT) + Refresh Token (UUID)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * API Đăng ký tài khoản mới:
     * Lưu thông tin người dùng và gửi message qua RabbitMQ cho NotifyService
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    /**
     * API Cấp lại Access Token mới bằng Refresh Token:
     * Khi Access Token (JWT) hết hạn, client gọi API này kèm refreshToken.
     * Server kiểm tra hạn trong DB và cấp Access Token mới mà không cần bắt user nhập lại password.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        AuthResponse response = refreshTokenService.refreshAccessToken(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API Đăng xuất:
     * Thu hồi Refresh Token trong DB để token này không thể dùng để cấp Access Token mới nữa.
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody @Valid RefreshTokenRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công. Refresh token đã được thu hồi."));
    }
}