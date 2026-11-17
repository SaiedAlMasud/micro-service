package com.example.productservice.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record BulkProductRequest(@NotEmpty @Size(max = 50) List<UUID> ids) {
}
