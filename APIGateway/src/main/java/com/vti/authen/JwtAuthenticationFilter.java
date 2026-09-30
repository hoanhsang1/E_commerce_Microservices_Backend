package com.vti.authen;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter implements GlobalFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @org.springframework.beans.factory.annotation.Value("${jwt.secret}")
    private String SECRET_KEY;

    /** Key bí mật dùng để nhận dạng request đến từ hệ thống nội bộ (Gateway hoặc Feign) */
    @org.springframework.beans.factory.annotation.Value("${internal.api.key}")
    private String INTERNAL_API_KEY;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();

        String path = request.getURI().getPath();

        // Không kiểm tra JWT khi login/register và OAuth2 endpoints
        if (path.startsWith("/api/v1/auth/")
                || path.startsWith("/oauth2/")
                || path.startsWith("/login/oauth2/")) {

            return chain.filter(exchange);
        }

        // Lấy Authorization Header
        String authHeader =
                request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        // Không có Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return onError(
                    exchange,
                    "Missing Authorization Header",
                    HttpStatus.UNAUTHORIZED
            );
        }

        // Lấy token sau chữ "Bearer "
        String token = authHeader.substring(7);

        try {

            // Kiểm tra JWT (dùng parserBuilder thay vì parser() deprecated)
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)))
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Lấy username
            String username = claims.getSubject();

            // Lấy role
            String role = claims.get("role", String.class);

            // Lấy userId dưới dạng Number để tránh lỗi Long/Integer
            Number userIdNumber = claims.get("userId", Number.class);

            Long userId = null;

            if (userIdNumber != null) {
                userId = userIdNumber.longValue();
            }

            // Kiểm tra subject
            if (username == null || username.isBlank()) {
                return onError(
                        exchange,
                        "JWT does not contain username",
                        HttpStatus.UNAUTHORIZED
                );
            }

            // Gắn thông tin user + "vé thông hành" nội bộ vào request
            // X-Internal-Api-Key: chứng minh request này đến từ Gateway hợp lệ,
            // giúp các service con phân biệt request từ Gateway vs kẻ tấn công gọi thẳng.
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Name", username)
                    .header("X-User-Role", role != null ? role : "")
                    .header("X-User-Id", userId != null
                            ? String.valueOf(userId)
                            : "")
                    .header("X-Internal-Api-Key", INTERNAL_API_KEY)
                    .build();

            // Tạo exchange mới
            ServerWebExchange mutatedExchange =
                    exchange.mutate()
                            .request(mutatedRequest)
                            .build();

            // Cho request đi tiếp
            return chain.filter(mutatedExchange);

        } catch (Exception e) {

            log.warn("JWT validation failed for path [{}]: {}", path, e.getMessage());

            return onError(
                    exchange,
                    "Invalid JWT token",
                    HttpStatus.UNAUTHORIZED
            );
        }
    }

    private Mono<Void> onError(
            ServerWebExchange exchange,
            String message,
            HttpStatus status) {

        ServerHttpResponse response = exchange.getResponse();

        response.setStatusCode(status);

        return response.setComplete();
    }
}