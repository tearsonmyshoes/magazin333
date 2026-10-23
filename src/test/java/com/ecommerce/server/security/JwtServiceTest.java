package com.ecommerce.server.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {
    private static final String SECRET =
            "test-secret-key-for-jwt-service-tests-0123456789abcdef";

    @Test
    void acceptsValidTokenForMatchingEmail() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(1L, "user@example.com", "USER");

        assertTrue(jwtService.isTokenValid(token, "user@example.com"));
    }

    @Test
    void rejectsValidTokenForDifferentEmail() {
        JwtService jwtService = new JwtService(SECRET, 60_000);
        String token = jwtService.generateToken(1L, "user@example.com", "USER");

        assertFalse(jwtService.isTokenValid(token, "other@example.com"));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwtService = new JwtService(SECRET, -60_000);
        String token = jwtService.generateToken(1L, "user@example.com", "USER");

        assertFalse(jwtService.isTokenValid(token, "user@example.com"));
    }
}
