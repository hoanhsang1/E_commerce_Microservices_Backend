package com.vti.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import com.vti.entity.RefreshToken;
import com.vti.entity.User;

/**
 * Repository thao tác với bảng refresh_tokens trong CSDL.
 * Kế thừa JpaRepository để có sẵn các hàm CRUD cơ bản (save, findById, delete...).
 */
@Repository
public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Tìm Refresh Token theo chuỗi token gửi lên từ client.
     * Trả về Optional để xử lý an toàn tránh NullPointerException nếu không tìm thấy.
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Thu hồi/xóa tất cả refresh token thuộc về một User (hữu ích khi người dùng logout
     * hoặc muốn đăng xuất khỏi tất cả các thiết bị).
     */
    @Modifying
    int deleteByUser(User user);
}
