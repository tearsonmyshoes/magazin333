package com.ecommerce.server.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
        @NotBlank
        @Size(max = 150)
        String name,

        String description,

        @NotNull
        @DecimalMin(value = "0.00")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @PositiveOrZero
        Integer stock,

        Long categoryId
) {}
