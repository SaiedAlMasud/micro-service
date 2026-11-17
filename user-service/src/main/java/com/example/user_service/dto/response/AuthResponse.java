package com.example.user_service.dto.response;

public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private long accessTokenExpiresIn;   // seconds
    private long refreshTokenExpiresIn;  // seconds
    private UserSummary user;

    public AuthResponse() {}

    public AuthResponse(String accessToken, String refreshToken,
                        long accessTokenExpiresIn, long refreshTokenExpiresIn,
                        UserSummary user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.accessTokenExpiresIn = accessTokenExpiresIn;
        this.refreshTokenExpiresIn = refreshTokenExpiresIn;
        this.user = user;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getAccessToken() { return accessToken; }
    public String getRefreshToken() { return refreshToken; }
    public String getTokenType() { return tokenType; }
    public long getAccessTokenExpiresIn() { return accessTokenExpiresIn; }
    public long getRefreshTokenExpiresIn() { return refreshTokenExpiresIn; }
    public UserSummary getUser() { return user; }
}

