package com.example.productservice.dto;

import java.util.UUID;
import java.util.List;

public record BulkProductResponse(List<ProductResponse> items, List<UUID> missingIds) {
}
