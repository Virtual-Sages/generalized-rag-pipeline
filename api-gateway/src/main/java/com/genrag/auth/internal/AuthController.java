package com.genrag.auth.internal;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.genrag.auth.api.AuthResponse;
import com.genrag.auth.api.AuthService;
import com.genrag.auth.api.dto.LoginRequest;
import com.genrag.auth.api.dto.RegisterRequest;
import com.genrag.auth.api.dto.TokenPair;

import io.jsonwebtoken.JwtException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE_IDENTIFIER = "refreshToken";

    private final AuthService authService;
    private final Duration refreshTtl;
    private final boolean cookieSecure;

    public AuthController(
        AuthService authService,
        @Value("${genrag.jwt.refresh-ttl}") Duration refreshTtl,
        @Value("${genrag.auth.cookie.secure}") boolean cookieSecure
    ) {
        this.authService = authService;
        this.refreshTtl = refreshTtl;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return respondWith(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return respondWith(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        @CookieValue(name = REFRESH_COOKIE_IDENTIFIER, required = false) String refreshToken
    ) {
        if (refreshToken == null) {
            throw new JwtException("No refresh token");
        }
        return respondWith(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity
            .noContent()
            .header(HttpHeaders.SET_COOKIE, expireRefreshCookie().toString())
            .build();
    }

    private ResponseEntity<AuthResponse> respondWith(TokenPair tokens) {
        return ResponseEntity
            .ok()
            .header(HttpHeaders.SET_COOKIE, refreshCookie(tokens.refreshToken()).toString())
            .body(new AuthResponse(tokens.accessToken()));
    }

    private ResponseCookie refreshCookie(String token) {
        return baseCookie(token).maxAge(refreshTtl).build();
    }

    private ResponseCookie expireRefreshCookie() {
        return baseCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie
            .from(REFRESH_COOKIE_IDENTIFIER, value)    
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Strict")
            .path("/api/auth");
    }
}
