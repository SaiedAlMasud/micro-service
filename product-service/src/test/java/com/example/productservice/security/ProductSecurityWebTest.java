package com.example.productservice.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.productservice.dto.ProductResponse;
import com.example.productservice.dto.CategoryResponse;
import com.example.productservice.entity.ProductStatus;
import com.example.productservice.service.CategoryService;
import com.example.productservice.service.ProductService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {
		com.example.productservice.controller.ProductController.class,
		com.example.productservice.controller.CategoryController.class
})
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
		"jwt.secret=a-secure-shared-secret-that-is-at-least-32-bytes",
		"jwt.issuer=user-service",
		"FRONTEND_ORIGIN="
})
class ProductSecurityWebTest {

	private static final String SECRET = "a-secure-shared-secret-that-is-at-least-32-bytes";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductService productService;

	@MockitoBean
	private CategoryService categoryService;

	@BeforeEach
	void setUp() {
		when(productService.search(any(), any(), any())).thenReturn(
				new org.springframework.data.domain.PageImpl<>(java.util.List.of()));
		when(productService.create(any())).thenReturn(new ProductResponse(
				UUID.randomUUID(), "Phone X", null, UUID.randomUUID(), "Example",
				BigDecimal.TEN, "BDT", 10, null, ProductStatus.ACTIVE));
		when(categoryService.findAll()).thenReturn(java.util.List.of(
				new CategoryResponse(UUID.randomUUID(), "Phones", "Mobile phones")));
	}

	@Test
	void publicProductListDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/products"))
				.andExpect(status().isOk());
	}

	@Test
	void publicCategoryListDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/categories"))
				.andExpect(status().isOk());
	}

	@Test
	void unauthenticatedProductWriteIsRejected() throws Exception {
		mockMvc.perform(post("/api/v1/products")
				.contentType("application/json")
				.content(validProductJson()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void userTokenCannotWriteProducts() throws Exception {
		mockMvc.perform(post("/api/v1/products")
				.header("Authorization", "Bearer " + token("USER", Instant.now().plusSeconds(300)))
				.contentType("application/json")
				.content(validProductJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void adminTokenCanWriteProducts() throws Exception {
		mockMvc.perform(post("/api/v1/products")
				.header("Authorization", "Bearer " + token("ADMIN", Instant.now().plusSeconds(300)))
				.contentType("application/json")
				.content(validProductJson()))
				.andExpect(status().isCreated());
	}

	private String validProductJson() {
		return """
				{
				  "name": "Phone X",
				  "categoryId": "00000000-0000-0000-0000-000000000001",
				  "price": 25000.00,
				  "currency": "BDT",
				  "stockQuantity": 10
				}
				""";
	}

	private String token(String role, Instant expiresAt) {
		var key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
		var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
		var claims = JwtClaimsSet.builder()
				.issuer("user-service")
				.subject(UUID.randomUUID().toString())
				.claim("role", role)
				.issuedAt(Instant.now().minusSeconds(1))
				.expiresAt(expiresAt)
				.build();
		return encoder.encode(JwtEncoderParameters.from(
				JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
	}
}
