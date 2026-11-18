package com.example.cart_service.dto;

import java.util.List;
import java.util.UUID;

public record BulkProductRequest(
        List<UUID> ids
) {
}

