package com.mindos.backend.service;

import com.mindos.backend.dto.AuthRequest;
import com.mindos.backend.dto.AuthResponse;
import com.mindos.backend.dto.RegisterRequest;
import com.mindos.backend.entity.User;
import com.mindos.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;



    private String normalizeMobile(String mobile) {
        if (mobile == null) return "";
        return mobile.replaceAll("\\D", "");
    }

    private String formatEmail(String mobile) {
        String nm = normalizeMobile(mobile);
        if (nm.contains("@")) return nm;
        return nm + "@mindos.com";
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String mobile = normalizeMobile(request.getMobile());
        String email = formatEmail(mobile);

        if (userRepository.findByEmail(email).isPresent()) {
            return new AuthResponse(false, "Mobile number is already registered. Please log in.", null, null, null, null);
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(email)
                .passwordHash(request.getPassword())
                .build();

        User savedUser = userRepository.save(user);

        return new AuthResponse(true, "Account registered successfully!", savedUser.getId(), savedUser.getName(), mobile, String.valueOf(savedUser.getId()));
    }

    @Transactional
    public AuthResponse login(AuthRequest request) {
        String mobile = normalizeMobile(request.getMobile());
        String email = formatEmail(mobile);

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Auto-register mock or fallback user for effortless dev testing if first login
            User user = User.builder()
                    .name("User (" + mobile + ")")
                    .email(email)
                    .passwordHash(request.getPassword())
                    .build();
            userOpt = Optional.of(userRepository.save(user));
        }

        User user = userOpt.get();
        if (!user.getPasswordHash().equals(request.getPassword())) {
            return new AuthResponse(false, "Invalid password.", null, null, null, null);
        }

        return new AuthResponse(true, "Login successful.", user.getId(), user.getName(), mobile, String.valueOf(user.getId()));
    }

    public User getOrCreateUserByMobileOrId(String userIdentifier, String userName) {
        if (userIdentifier == null || userIdentifier.isBlank()) {
            userIdentifier = "default_user";
        }
        
        // Try parsing ID if numeric
        try {
            Long userId = Long.parseLong(userIdentifier);
            Optional<User> uOpt = userRepository.findById(userId);
            if (uOpt.isPresent()) return uOpt.get();
        } catch (NumberFormatException ignored) {}

        String email = formatEmail(userIdentifier);
        Optional<User> uOpt = userRepository.findByEmail(email);
        if (uOpt.isPresent()) return uOpt.get();

        // Create new user in PostgreSQL
        String name = (userName != null && !userName.isBlank()) ? userName : "User (" + userIdentifier + ")";
        User newUser = User.builder()
                .name(name)
                .email(email)
                .passwordHash("password123")
                .build();
        return userRepository.save(newUser);
    }
}
