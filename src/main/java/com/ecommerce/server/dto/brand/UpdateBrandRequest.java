package com.ecommerce.server.dto.brand;

import jakarta.validation.constraints.Size;

/**
 * Partial update request. Fields that are {@code null} are left unchanged.
 */
public record UpdateBrandRequest(
        @Size(max = 100, message = "Brand name must contain at most 100 characters")
        String name
) {}
