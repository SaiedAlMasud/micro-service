package com.example.user_service.service;

import com.example.user_service.dto.request.LoginRequest;
import com.example.user_service.dto.request.RefreshTokenRequest;
import com.example.user_service.dto.request.RegisterRequest;
import com.example.user_service.dto.request.UpdateProfileRequest;
import com.example.user_service.dto.response.AuthResponse;
import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.dto.response.UserSummary;
import com.example.user_service.entity.RefreshToken;
import com.example.user_service.entity.User;
import com.example.user_service.enums.UserStatus;
import com.example.user_service.exception.*;
import com.example.user_service.repository.RefreshTokenRepository;
import com.example.user_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    @Value("${jwt.access-token-expiry-seconds:3600}")
    private long accessTokenExpirySeconds;

    @Value("${jwt.refresh-token-expiry-seconds:604800}")
    private long refreshTokenExpirySeconds;

    @Value("${jwt.issuer:user-service}")
    private String jwtIssuer;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    // ── Register ─────────────────────────────────────────────────────────────

    public UserResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new EmailAlreadyExistsException(req.getEmail());
        }
        User user = new User();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setPhone(req.getPhone());

        user = userRepository.save(user);
        return toResponse(user);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    public AuthResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException();
        }

        return issueTokenPair(user);
    }

    // ── Refresh token ─────────────────────────────────────────────────────────

    public AuthResponse refresh(RefreshTokenRequest req) {
        RefreshToken rt = refreshTokenRepository.findByToken(req.getRefreshToken())
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        if (rt.isRevoked()) {
            throw new InvalidRefreshTokenException("Refresh token has been revoked");
        }
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token has expired");
        }

        User user = userRepository.findById(rt.getUserId())
                .orElseThrow(() -> new UserNotFoundException(rt.getUserId().toString()));

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new AccountBlockedException();
        }

        // Rotate: revoke old, issue new pair
        rt.setRevoked(true);
        refreshTokenRepository.save(rt);

        return issueTokenPair(user);
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    public void logout(UUID userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ── Get profile ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));
        return toResponse(user);
    }

    // ── Update profile ────────────────────────────────────────────────────────

    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId.toString()));

        if (req.getFullName() != null && !req.getFullName().isBlank()) {
            user.setFullName(req.getFullName());
        }
        if (req.getPhone() != null) {
            user.setPhone(req.getPhone());
        }
        return toResponse(userRepository.save(user));
    }

    // ── Get user by ID (ADMIN) ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        return toResponse(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.toString())));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AuthResponse issueTokenPair(User user) {
        String accessToken = buildAccessToken(user);
        String rawRefreshToken = buildAndSaveRefreshToken(user);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                accessTokenExpirySeconds,
                refreshTokenExpirySeconds,
                new UserSummary(user.getId(), user.getFullName(), user.getRole())
        );
    }

    private String buildAccessToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiresAt(now.plus(accessTokenExpirySeconds, ChronoUnit.SECONDS))
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("email", user.getEmail())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private String buildAndSaveRefreshToken(User user) {
        String raw = UUID.randomUUID().toString() + "-" + UUID.randomUUID();

        RefreshToken rt = new RefreshToken();
        rt.setUserId(user.getId());
        rt.setToken(raw);
        rt.setExpiresAt(Instant.now().plus(refreshTokenExpirySeconds, ChronoUnit.SECONDS));
        refreshTokenRepository.save(rt);

        return raw;
    }

    private UserResponse toResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setFullName(user.getFullName());
        r.setEmail(user.getEmail());
        r.setPhone(user.getPhone());
        r.setRole(user.getRole());
        r.setStatus(user.getStatus());
        r.setCreatedAt(user.getCreatedAt());
        r.setUpdatedAt(user.getUpdatedAt());
        return r;
    }
}
