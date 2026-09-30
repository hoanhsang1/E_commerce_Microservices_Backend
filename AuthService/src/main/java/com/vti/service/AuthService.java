package com.vti.service;

import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.vti.authen.JwtUtil;
import com.vti.dto.AuthResponse;
import com.vti.entity.RefreshToken;
import com.vti.entity.User;
import com.vti.form.AuthRequest;
import com.vti.form.RegisterRequest;
import com.vti.rabbitmqClient.service.RabbitMQSender;
import com.vti.repository.IUserRepository;
import com.vti.entity.enums.UserRole;

@Service
public class AuthService implements IAuthService {
    @Autowired private IUserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private RabbitMQSender rabbitMQSender;
    @Autowired private IRefreshTokenService refreshTokenService;

    @Override
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Account not found"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        // 1. Tạo Access Token (JWT) ngắn hạn
        String accessToken = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), user.getId());

        // 2. Tạo Refresh Token dài hạn lưu vào DB
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        // 3. Trả về cả 2 token cho Client
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .userId(user.getId())
                .username(user.getUsername())
                .role(user.getRole().name())
                .build();
    }

    @Override
    public User register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone()); // Set user ID from request if provided
        user.setRole(
                        request.getRole() != null
                            ? UserRole.valueOf(request.getRole())
                            : UserRole.USER
                    );
        User savedUser = userRepository.save(user);

        rabbitMQSender.sendUserCreatedEvent(savedUser); 

        return savedUser;
    }
}