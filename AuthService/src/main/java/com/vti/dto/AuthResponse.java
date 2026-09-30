package com.vti.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO trả về kết quả xác thực cho Client sau khi:
 * 1. Đăng nhập thành công (/api/v1/auth/login)
 * 2. Cấp mới token thành công (/api/v1/auth/refresh)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    /** Access Token (JWT): Dùng để gửi kèm mỗi request trong Authorization Header */
    private String accessToken;

    /** Refresh Token (UUID): Dùng để đổi Access Token mới khi Access Token cũ hết hạn */
    private String refreshToken;

    /** Loại token: mặc định chuẩn RFC 6750 là "Bearer" */
    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String username;
    private String role;

    /** Constructor tương thích ngược cho code cũ chỉ truyền 1 token */
    public AuthResponse(String accessToken) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
    }

    /** Giữ method getToken() để tương thích ngược nếu có code cũ gọi */
    public String getToken() {
        return accessToken;
    }
}