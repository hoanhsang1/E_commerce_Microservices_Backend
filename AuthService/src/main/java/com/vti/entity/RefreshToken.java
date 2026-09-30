package com.vti.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * RefreshToken là một bản ghi lưu trong database, gồm:
 * - token:      một chuỗi ngẫu nhiên (UUID) dùng để xin cấp lại Access Token
 * - user:       user sở hữu token này
 * - expiryDate: thời điểm token hết hạn (thường 7–30 ngày)
 * - revoked:    nếu true → token đã bị thu hồi (logout), không dùng được nữa
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Giá trị token — UUID ngẫu nhiên, lưu vào DB và gửi cho client */
    @Column(nullable = false, unique = true, length = 512)
    private String token;

    /**
     * Quan hệ nhiều-1 với User:
     * Một user có thể có nhiều refresh token (nhiều thiết bị),
     * nhưng mỗi token chỉ thuộc về 1 user.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Thời điểm token hết hạn (dùng Instant để chuẩn UTC) */
    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    /** Nếu true → token đã bị thu hồi (do logout hoặc đăng nhập thiết bị mới) */
    @Builder.Default
    @Column(nullable = false)
    private boolean revoked = false;
}
