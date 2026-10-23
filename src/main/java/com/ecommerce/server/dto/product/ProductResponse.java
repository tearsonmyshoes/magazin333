package com.ecommerce.server.dto.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Integer stock,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt
) {}
