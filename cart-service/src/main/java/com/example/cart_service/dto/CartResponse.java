package com.example.cart_service.dto;

import com.example.cart_service.entity.CartStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CartResponse(
        UUID id,
        UUID userId,
        CartStatus status,
        List<CartItemResponse> items,
        BigDecimal total,
        Instant createdAt,
        Instant updatedAt
) {
}

