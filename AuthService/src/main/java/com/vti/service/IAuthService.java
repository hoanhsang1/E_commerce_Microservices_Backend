package com.vti.service;

import com.vti.dto.AuthResponse;
import com.vti.entity.User;
import com.vti.form.AuthRequest;
import com.vti.form.RegisterRequest;

public interface IAuthService {
    AuthResponse login(AuthRequest request);
    User register(RegisterRequest request);
}