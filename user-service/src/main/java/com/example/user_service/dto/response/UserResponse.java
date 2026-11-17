package com.example.user_service.dto.response;

import com.example.user_service.enums.Role;
import com.example.user_service.enums.UserStatus;

import java.time.Instant;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phone;
    private Role role;
    private UserStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public UserResponse() {}

    // ── Getters & Setters ────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus status) { this.status = status; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}

