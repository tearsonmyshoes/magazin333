package com.ecommerce.server.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Partial update request. Fields that are {@code null} are left unchanged.
 */
public record UpdateProductRequest(
        @Size(max = 150)
        String name,

        @Size(max = 5000)
        String description,

        @DecimalMin(value = "0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @PositiveOrZero
        Integer stock,

        Long categoryId
) {}
