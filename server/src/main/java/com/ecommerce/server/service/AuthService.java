package com.ecommerce.server.service;

import com.ecommerce.server.dto.auth.AuthResponse;
import com.ecommerce.server.dto.auth.LoginRequest;
import com.ecommerce.server.dto.auth.RegisterRequest;
import com.ecommerce.server.entity.User;
import com.ecommerce.server.repository.UserRepository;
import com.ecommerce.server.security.JwtService;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A user with this email already exists"
            );
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        User user = new User(email, hashedPassword, User.Role.USER);
        User savedUser = userRepository.save(user);

        return createResponse(savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid email or password"
                ));

        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash());
        if (!passwordMatches) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }

        return createResponse(user);
    }

    private AuthResponse createResponse(User user) {
        String token = jwtService.generateToken(
                user.getId(), user.getEmail(), user.getRole().name()
        );

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
