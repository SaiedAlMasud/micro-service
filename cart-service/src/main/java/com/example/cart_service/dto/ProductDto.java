package com.example.cart_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String name,
        UUID categoryId,
        BigDecimal price,
        String currency,
        Integer stockQuantity,
        String status
) {
}

