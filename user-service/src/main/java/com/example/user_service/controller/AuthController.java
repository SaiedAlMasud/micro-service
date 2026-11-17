package com.example.user_service.controller;

import com.example.user_service.dto.request.LoginRequest;
import com.example.user_service.dto.request.RefreshTokenRequest;
import com.example.user_service.dto.request.RegisterRequest;
import com.example.user_service.dto.response.AuthResponse;
import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * POST /api/v1/auth/register
     * Public – create a new user account.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    /**
     * POST /api/v1/auth/login
     * Public – returns access token + refresh token.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /**
     * POST /api/v1/auth/refresh
     * Public – exchange a valid refresh token for a new token pair.
     */
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(authService.refresh(req));
    }

    /**
     * POST /api/v1/auth/logout
     * Requires JWT – revokes all refresh tokens for the authenticated user.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        authService.logout(userId);
        return ResponseEntity.noContent().build();
    }
}

