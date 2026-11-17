package com.example.productservice.security;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

	@Value("${FRONTEND_ORIGIN:}")
	private String frontendOrigin;

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
				.cors(cors -> cors.configurationSource(corsConfigurationSource()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.GET, "/api/v1/products", "/api/v1/products/*",
								"/api/v1/categories").permitAll()
						.requestMatchers(HttpMethod.POST, "/api/v1/products/bulk").permitAll()
						.requestMatchers("/api/v1/products", "/api/v1/products/*", "/api/v1/categories",
								"/api/v1/categories/*")
						.hasRole("ADMIN")
						.requestMatchers("/actuator/health", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
						.anyRequest().authenticated())
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint(jsonError(HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED"))
						.accessDeniedHandler(jsonDenied()))
				.oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
		return http.build();
	}

	@Bean
	CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		if (!frontendOrigin.isBlank()) {
			configuration.setAllowedOrigins(List.of(frontendOrigin));
			configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
			configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		}
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	private AuthenticationEntryPoint jsonError(int status, String code) {
		return (request, response, exception) -> writeError(response, status, code, "Authentication is required", request.getRequestURI());
	}

	private AccessDeniedHandler jsonDenied() {
		return (request, response, exception) -> writeError(response, HttpServletResponse.SC_FORBIDDEN,
				"FORBIDDEN", "You do not have permission to access this resource", request.getRequestURI());
	}

	private void writeError(HttpServletResponse response, int status, String code, String message, String path)
			throws java.io.IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		new ObjectMapper().writeValue(response.getOutputStream(),
				java.util.Map.of("timestamp", Instant.now().toString(), "status", status, "code", code,
						"message", message, "path", path));
	}

	@Bean
	JwtDecoder jwtDecoder(@Value("${JWT_SECRET:}") String secret) {
		if (secret.isBlank()) {
			throw new IllegalStateException("JWT_SECRET must be configured");
		}
		SecretKey key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		return NimbusJwtDecoder.withSecretKey(key).build();
	}

	private Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(new RoleClaimConverter());
		return converter;
	}

	private static final class RoleClaimConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
		@Override
		public Collection<GrantedAuthority> convert(Jwt jwt) {
			String role = jwt.getClaimAsString("role");
			return role == null || role.isBlank()
					? List.of()
					: List.of(new SimpleGrantedAuthority("ROLE_" + role));
		}
	}
}
