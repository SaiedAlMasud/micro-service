package com.example.user_service.controller;

import com.example.user_service.dto.request.UpdateProfileRequest;
import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final AuthService authService;

    public UserController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * GET /api/v1/users/me
     * Requires JWT – returns the authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(authService.getProfile(userId));
    }

    /**
     * PUT /api/v1/users/me
     * Requires JWT – update fullName and/or phone.
     */
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UpdateProfileRequest req) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(authService.updateProfile(userId, req));
    }

    /**
     * GET /api/v1/users/{id}
     * Requires ADMIN role – look up any user by ID.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> getUserById(@PathVariable UUID id) {
        return ResponseEntity.ok(authService.getUserById(id));
    }
}

