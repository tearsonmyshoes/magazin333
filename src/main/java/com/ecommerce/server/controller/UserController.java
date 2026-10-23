package com.ecommerce.server.controller;

import com.ecommerce.server.entity.User;
import com.ecommerce.server.repository.UserRepository;

import org.springframework.http.HttpStatus;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        User user = userRepository
                .findByEmail(
                        authentication.getName()
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "User not found"
                        ));

        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    public record UserResponse(
            Long id,
            String email,
            String role
    ) {}
}
