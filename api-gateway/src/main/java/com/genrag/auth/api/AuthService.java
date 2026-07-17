package com.genrag.auth.api;

import com.genrag.auth.api.dto.LoginRequest;
import com.genrag.auth.api.dto.RegisterRequest;
import com.genrag.auth.api.dto.TokenPair;

public interface AuthService {
    TokenPair register(RegisterRequest request);
    TokenPair login(LoginRequest request);
    TokenPair refresh(String refreshToken);
}
