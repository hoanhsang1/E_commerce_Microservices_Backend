package com.vti.aspect;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Aspect
@Component
public class InternalApiAspect {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Before("@annotation(com.vti.annotation.InternalApi) || @within(com.vti.annotation.InternalApi)")
    public void verifyInternalApiKey() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Endpoint nội bộ, không có ngữ cảnh HTTP Request");
        }
        HttpServletRequest request = attributes.getRequest();
        String apiKey = request.getHeader("X-Internal-Api-Key");

        if (internalApiKey == null || !internalApiKey.equals(apiKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Endpoint nội bộ, không cho phép gọi trực tiếp");
        }
    }
}
