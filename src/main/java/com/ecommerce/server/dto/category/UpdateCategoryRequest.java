package com.ecommerce.server.dto.category;

import jakarta.validation.constraints.Size;

/**
 * Partial update request. Fields that are {@code null} are left unchanged.
 */
public record UpdateCategoryRequest(
        @Size(max = 100, message = "Category name must contain at most 100 characters")
        String name
) {}
