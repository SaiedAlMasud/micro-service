package com.example.productservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductRequest(
		@NotBlank @Size(max = 200) String name,
		String description,
		@NotNull UUID categoryId,
		@Size(max = 100) String brand,
		@NotNull @DecimalMin(value = "0.01") BigDecimal price,
		@Size(min = 3, max = 3) String currency,
		@Min(0) int stockQuantity,
		@Size(max = 500) String imageUrl) {
}
