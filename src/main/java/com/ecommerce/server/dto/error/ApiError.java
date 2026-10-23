package com.ecommerce.server.dto.error;

public record ApiError(
        int status,
        String message
) {}
