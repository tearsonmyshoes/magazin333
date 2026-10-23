package com.ecommerce.server.dto.product;

import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductFilterRequest(
        String name,
        Long categoryId,
        @PositiveOrZero BigDecimal minPrice,
        @PositiveOrZero BigDecimal maxPrice,
        @PositiveOrZero Integer minStock,
        @PositiveOrZero Integer maxStock
) {}
