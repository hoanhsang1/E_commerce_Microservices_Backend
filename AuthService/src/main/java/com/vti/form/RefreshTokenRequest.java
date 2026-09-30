package com.vti.form;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * Form dữ liệu gửi từ Client lên Server khi Access Token hết hạn,
 * yêu cầu cấp lại Access Token mới bằng Refresh Token.
 */
@Getter
@Setter
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}
