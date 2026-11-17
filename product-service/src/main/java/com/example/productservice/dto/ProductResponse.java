package com.example.productservice.dto;

import com.example.productservice.entity.Product;
import com.example.productservice.entity.ProductStatus;
import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(UUID id, String name, String description, UUID categoryId, String brand,
		BigDecimal price, String currency, int stockQuantity, String imageUrl, ProductStatus status) {

	public static ProductResponse from(Product product) {
		return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
				product.getCategory().getId(), product.getBrand(), product.getPrice(), product.getCurrency(),
				product.getStockQuantity(), product.getImageUrl(), product.getStatus());
	}
}
