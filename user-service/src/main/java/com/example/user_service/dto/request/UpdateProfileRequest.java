package com.example.user_service.dto.request;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 120)
    private String fullName;

    @Size(max = 30)
    private String phone;

    // ── Getters & Setters ────────────────────────────────────────────────────

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}

