package com.vti.service;

import java.util.Optional;

import com.vti.dto.AuthResponse;
import com.vti.entity.RefreshToken;
import com.vti.form.RefreshTokenRequest;

/**
 * Interface định nghĩa các nghiệp vụ liên quan đến Refresh Token:
 * 1. Tạo mới Refresh Token khi đăng nhập
 * 2. Cấp Access Token mới khi Access Token cũ hết hạn
 * 3. Kiểm tra hạn dùng của Refresh Token
 * 4. Thu hồi Refresh Token khi đăng xuất (Logout)
 */
public interface IRefreshTokenService {

    /**
     * Tạo Refresh Token mới gắn liền với userId và lưu vào Database.
     * @param userId ID của người dùng vừa đăng nhập thành công
     * @return Đối tượng RefreshToken đã lưu
     */
    RefreshToken createRefreshToken(Long userId);

    /**
     * Kiểm tra xem Refresh Token có bị thu hồi (revoked) hoặc quá hạn (expired) hay không.
     * @param token Đối tượng RefreshToken cần kiểm tra
     * @return Token hợp lệ
     * @throws org.springframework.web.server.ResponseStatusException nếu token hết hạn hoặc không hợp lệ
     */
    RefreshToken verifyExpiration(RefreshToken token);

    /**
     * Tìm Refresh Token theo chuỗi ký tự token.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Xử lý yêu cầu cấp Access Token mới từ Client dựa trên Refresh Token.
     * @param request Form chứa chuỗi refreshToken
     * @return AuthResponse chứa Access Token mới và Refresh Token
     */
    AuthResponse refreshAccessToken(RefreshTokenRequest request);

    /**
     * Thu hồi (revoke) một Refresh Token cụ thể khi người dùng đăng xuất.
     * @param token Chuỗi token cần thu hồi
     */
    void revokeToken(String token);

    /**
     * Xóa tất cả Refresh Token của một user (đăng xuất khỏi mọi thiết bị).
     * @param userId ID của người dùng
     */
    void deleteByUserId(Long userId);
}
