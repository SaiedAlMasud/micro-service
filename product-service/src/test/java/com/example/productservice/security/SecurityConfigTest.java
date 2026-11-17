package com.example.productservice.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class SecurityConfigTest {

	private static final String SECRET = "a-secure-shared-secret-that-is-at-least-32-bytes";
	private static final String ISSUER = "user-service";
	private SecurityConfig securityConfig;
	private JwtDecoder decoder;

	@BeforeEach
	void setUp() {
		securityConfig = new SecurityConfig();
		decoder = securityConfig.jwtDecoder(SECRET, ISSUER);
	}

	@Test
	void acceptsUserServiceAdminTokenAndMapsAdminAuthority() {
		UUID userId = UUID.randomUUID();
		Jwt jwt = decoder.decode(createToken(SECRET, ISSUER, userId, "ADMIN",
				Instant.now().plusSeconds(300)));

		var authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);

		assertEquals(userId.toString(), jwt.getSubject());
		assertEquals("ADMIN", jwt.getClaimAsString("role"));
		assertTrue(jwt.getIssuedAt() != null);
		assertTrue(jwt.getExpiresAt() != null);
		assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
	}

	@Test
	void mapsUserRoleToRoleUserAuthority() {
		Jwt jwt = decoder.decode(createToken(SECRET, ISSUER, UUID.randomUUID(), "USER",
				Instant.now().plusSeconds(300)));

		var authentication = securityConfig.jwtAuthenticationConverter().convert(jwt);

		assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
	}

	@Test
	void rejectsTokenSignedWithDifferentSecret() {
		String token = createToken("wrong-secret-that-is-also-long-enough-123", ISSUER,
				UUID.randomUUID(), "ADMIN", Instant.now().plusSeconds(300));

		assertThrows(RuntimeException.class, () -> decoder.decode(token));
	}

	@Test
	void rejectsExpiredToken() {
		String token = createToken(SECRET, ISSUER, UUID.randomUUID(), "ADMIN",
				Instant.now().minusSeconds(120));

		assertThrows(RuntimeException.class, () -> decoder.decode(token));
	}

	@Test
	void rejectsTokenWithWrongIssuer() {
		String token = createToken(SECRET, "another-service", UUID.randomUUID(), "ADMIN",
				Instant.now().plusSeconds(300));

		assertThrows(RuntimeException.class, () -> decoder.decode(token));
	}

	private String createToken(String secret, String issuer, UUID userId, String role,
			Instant expiresAt) {
		var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		var claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject(userId.toString())
				.claim("role", role)
				.claim("email", "test@example.com")
				.issuedAt(expiresAt.isBefore(Instant.now())
						? expiresAt.minusSeconds(1)
						: Instant.now().minusSeconds(1))
				.expiresAt(expiresAt)
				.build();
		return encoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
}
