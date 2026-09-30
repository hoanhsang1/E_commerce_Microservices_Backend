package com.vti.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * SECURITY FILTER - BẢO VỆ SERVICE KHỎI BỊ GỌI THẲNG (BYPASS API GATEWAY)
 *
 * Mọi request hợp lệ vào service này PHẢI mang header "X-Internal-Api-Key" đúng.
 * Header này được gắn tự động bởi:
 *   1. API Gateway (JwtAuthenticationFilter) - khi client bên ngoài gọi qua Gateway.
 *   2. Feign Client (FeignInternalAuthConfig) - khi service khác gọi nội bộ.
 *
 * Kẻ tấn công gọi thẳng vào port nội bộ (localhost:8081) mà không có key này
 * sẽ bị từ chối với HTTP 403 Forbidden.
 */
@Component
public class GatewayUserHeaderFilter extends OncePerRequestFilter {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Kiểm tra mã khóa nội bộ
        String receivedKey = request.getHeader("X-Internal-Api-Key");
        if (receivedKey == null || !receivedKey.equals(internalApiKey)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"error\": \"Forbidden\", \"message\": \"Direct access to internal service is not allowed. All requests must go through the API Gateway.\"}"
            );
            return;
        }

        // 2. Trích xuất thông tin người dùng từ header do Gateway gắn
        String username = request.getHeader("X-User-Name");
        String role = request.getHeader("X-User-Role");
        String userIdHeader = request.getHeader("X-User-Id");

        if (username != null && !username.isBlank()) {
            String authority = role != null && !role.isBlank() ? "ROLE_" + role.toUpperCase() : "ROLE_USER";
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(authority))
            );
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                authentication.setDetails(userIdHeader);
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
