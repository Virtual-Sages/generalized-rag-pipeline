package com.genrag.auth.internal;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.genrag.auth.api.AuthService;
import com.genrag.auth.api.dto.LoginRequest;
import com.genrag.auth.api.dto.RegisterRequest;
import com.genrag.auth.api.dto.TokenPair;
import com.genrag.user.api.UserDto;
import com.genrag.user.api.UserService;

import io.jsonwebtoken.Claims;

@Service
public class AuthServiceImpl implements AuthService {
    private final UserService userService;
    private final JwtService jwtService;

    public AuthServiceImpl(UserService userService, JwtService jwtService) {
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @Override
    public TokenPair register(RegisterRequest request) {
        UserDto user = userService.register(request.username(), request.email(), request.password());
        
        return issueTokensFor(user);
    }

    @Override
    public TokenPair login(LoginRequest request) {
        UserDto user = userService.authenticate(request.username(), request.password());

        return issueTokensFor(user);
    }

    @Override
    public TokenPair refresh(String refreshToken) {
        Claims claims = jwtService.parseRefreshToken(refreshToken);
        UserDto user = userService.findById(UUID.fromString(claims.getSubject()));

        return issueTokensFor(user);
    }

    private TokenPair issueTokensFor(UserDto user) {
        UUID userId = user.id();

        return new TokenPair(
            jwtService.generateAccessToken(userId),
            jwtService.generateRefreshToken(userId)
        );
    }
}
