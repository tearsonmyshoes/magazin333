package com.ecommerce.server.dto.auth;

public record AuthResponse(
   String token,
   String tokenType,
   Long userId,
   String email,
   String role
) {}
