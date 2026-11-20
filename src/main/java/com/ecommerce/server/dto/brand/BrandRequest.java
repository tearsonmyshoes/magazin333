package com.ecommerce.server.dto.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BrandRequest(
        @NotBlank(message = "Brand name is required")
        @Size(max = 100, message = "Brand name must contain at most 100 characters")
        String name
) {}
