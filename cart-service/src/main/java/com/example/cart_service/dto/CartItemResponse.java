package com.example.cart_service.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CartItemResponse(
        UUID id,
        UUID productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal subtotal,
        Instant addedAt,
        Boolean priceChanged,
        BigDecimal currentPrice,
        Boolean available
) {
}

