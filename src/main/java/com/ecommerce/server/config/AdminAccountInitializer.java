package com.ecommerce.server.config;

import com.ecommerce.server.entity.User;
import com.ecommerce.server.repository.UserRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Makes sure the built-in administrator account exists with the credentials
 * configured in application.properties.
 */
@Component
@Order(1)
public class AdminAccountInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminAccountInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;

    public AdminAccountInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:lagcompensation@admin.test}") String adminEmail,
            @Value("${app.admin.password:123456789ww}") String adminPassword
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = adminEmail.trim().toLowerCase();
        Optional<User> existing = userRepository.findByEmail(email);

        User admin;
        if (existing.isEmpty()) {
            admin = new User(email, passwordEncoder.encode(adminPassword), User.Role.ADMIN);
        } else {
            admin = existing.get();
            admin.setRole(User.Role.ADMIN);

            if (!passwordEncoder.matches(adminPassword, admin.getPasswordHash())) {
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            }
        }

        userRepository.save(admin);
        log.info("Administrator account is ready: {}", email);
    }
}
