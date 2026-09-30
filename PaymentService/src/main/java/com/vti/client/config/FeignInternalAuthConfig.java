package com.vti.client.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignInternalAuthConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return template -> {
            template.header("X-Internal-Api-Key", internalApiKey);

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && !authHeader.isBlank()) {
                    template.header("Authorization", authHeader);
                }
                String userName = request.getHeader("X-User-Name");
                if (userName != null && !userName.isBlank()) {
                    template.header("X-User-Name", userName);
                }
                String userRole = request.getHeader("X-User-Role");
                if (userRole != null && !userRole.isBlank()) {
                    template.header("X-User-Role", userRole);
                }
                String userId = request.getHeader("X-User-Id");
                if (userId != null && !userId.isBlank()) {
                    template.header("X-User-Id", userId);
                }
            }
        };
    }
}