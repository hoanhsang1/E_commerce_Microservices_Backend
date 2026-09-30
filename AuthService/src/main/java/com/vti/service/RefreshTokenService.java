package com.vti.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.vti.authen.JwtUtil;
import com.vti.dto.AuthResponse;
import com.vti.entity.RefreshToken;
import com.vti.entity.User;
import com.vti.form.RefreshTokenRequest;
import com.vti.repository.IRefreshTokenRepository;
import com.vti.repository.IUserRepository;

/**
 * Service xử lý toàn bộ logic liên quan đến Refresh Token:
 * 
 * NGUYÊN LÝ HOẠT ĐỘNG:
 * 1. Khi Client đăng nhập thành công, Server tạo 2 token:
 *    - Access Token (JWT): Hạn ngắn (ví dụ 1 ngày hoặc 15-30 phút), không lưu DB, dùng để gọi API.
 *    - Refresh Token: Chuỗi ngẫu nhiên (UUID), hạn dài (7 ngày), lưu vào bảng `refresh_tokens` trong CSDL.
 * 
 * 2. Khi Access Token hết hạn, API Gateway sẽ từ chối request (401 Unauthorized).
 *    Client không bắt người dùng nhập lại mật khẩu, mà âm thầm gửi Refresh Token lên endpoint:
 *    POST /api/v1/auth/refresh
 * 
 * 3. AuthService kiểm tra Refresh Token trong DB:
 *    - Nếu hợp lệ và chưa hết hạn: Cấp một Access Token mới cho Client.
 *    - Nếu đã hết hạn hoặc bị thu hồi: Yêu cầu người dùng đăng nhập lại từ đầu.
 */
@Service
public class RefreshTokenService implements IRefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);

    /** Thời gian sống của Refresh Token: mặc định 7 ngày = 7 * 24 * 60 * 60 * 1000 = 604,800,000 ms */
    @Value("${jwt.refresh.expiration:604800000}")
    private Long refreshTokenDurationMs;

    @Autowired
    private IRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Tạo và lưu một Refresh Token mới vào Database cho user có ID tương ứng.
     */
    @Override
    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy user với id: " + userId));

        // Tạo refresh token mới dạng UUID (ví dụ: "d3b07384-d113-4f44-8d4e-128a1c97a8f1")
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .revoked(false)
                .build();

        log.info("Tạo Refresh Token mới cho userId={}, hết hạn vào: {}", userId, refreshToken.getExpiryDate());
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Kiểm tra tính hợp lệ về thời gian và trạng thái thu hồi của Refresh Token.
     */
    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {
        // Kiểm tra xem token có bị Admin/User thu hồi trước đó chưa
        if (token.isRevoked()) {
            log.warn("Refresh Token đã bị thu hồi (revoked): {}", token.getToken());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh Token đã bị thu hồi. Vui lòng đăng nhập lại.");
        }

        // Kiểm tra xem token đã quá ngày hết hạn chưa
        if (token.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Refresh Token đã hết hạn: {}, expiryDate: {}", token.getToken(), token.getExpiryDate());
            refreshTokenRepository.delete(token);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh Token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        return token;
    }

    /**
     * Tìm Refresh Token trong DB theo chuỗi token.
     */
    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    /**
     * Cấp Access Token mới khi nhận được Refresh Token hợp lệ từ Client.
     */
    @Override
    @Transactional
    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return findByToken(requestRefreshToken)
                .map(this::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Tạo Access Token mới dựa trên thông tin User
                    String newAccessToken = jwtUtil.generateToken(
                            user.getUsername(),
                            user.getRole().name(),
                            user.getId()
                    );

                    log.info("Cấp lại Access Token mới thành công cho username: {}", user.getUsername());

                    return AuthResponse.builder()
                            .accessToken(newAccessToken)
                            .refreshToken(requestRefreshToken)
                            .tokenType("Bearer")
                            .userId(user.getId())
                            .username(user.getUsername())
                            .role(user.getRole().name())
                            .build();
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh Token không tồn tại trong hệ thống. Vui lòng đăng nhập lại."
                ));
    }

    /**
     * Thu hồi (Revoke) Refresh Token khi người dùng logout một phiên làm việc.
     */
    @Override
    @Transactional
    public void revokeToken(String token) {
        findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
            log.info("Đã thu hồi Refresh Token: {}", token);
        });
    }

    /**
     * Xóa toàn bộ Refresh Token của user khi logout toàn bộ thiết bị.
     */
    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            int deletedCount = refreshTokenRepository.deleteByUser(user);
            log.info("Đã xóa {} refresh tokens của userId: {}", deletedCount, userId);
        });
    }
}
