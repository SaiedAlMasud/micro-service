package com.example.user_service.dto.response;

import com.example.user_service.enums.Role;

import java.util.UUID;

public class UserSummary {

    private UUID id;
    private String fullName;
    private Role role;

    public UserSummary() {}

    public UserSummary(UUID id, String fullName, Role role) {
        this.id = id;
        this.fullName = fullName;
        this.role = role;
    }

    public UUID getId() { return id; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
}

