package com.example.cart_service.dto;

import java.util.List;
import java.util.UUID;

public record BulkProductResponse(
        List<ProductDto> items,
        List<UUID> missingIds
) {
}

