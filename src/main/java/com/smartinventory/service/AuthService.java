package com.smartinventory.service;

import com.smartinventory.dto.AuthResponse;
import com.smartinventory.dto.LoginRequest;
import com.smartinventory.dto.RegisterRequest;
import com.smartinventory.exception.ResourceNotFoundException;
import com.smartinventory.exception.UnauthorizedException;
import com.smartinventory.model.User;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final SESService sesService;
    private final CloudWatchService cloudWatchService;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered: " + request.getEmail());
        }

        User.Role role = User.Role.STAFF;
        if (request.getRole() != null) {
            try {
                role = User.Role.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                role = User.Role.STAFF;
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(role)
                .build();

        user = userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        try {
            sesService.sendWelcomeEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            log.warn("Failed to send welcome email: {}", e.getMessage());
        }

        auditLogService.log(user.getUsername(), "USER_REGISTERED", "User",
                user.getId().toString(), "New user registered", null);

        log.info("User registered: {}", user.getUsername());

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        boolean loginSuccess = false;
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            loginSuccess = true;
        } catch (BadCredentialsException e) {
            cloudWatchService.logLoginAttempt(request.getUsername(), false);
            throw new BadCredentialsException("Invalid username or password");
        }

        cloudWatchService.logLoginAttempt(request.getUsername(), loginSuccess);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", request.getUsername()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String accessToken = jwtUtil.generateAccessToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        user.setRefreshToken(refreshToken);
        userRepository.save(user);

        auditLogService.log(user.getUsername(), "USER_LOGIN", "User",
                user.getId().toString(), "User logged in", null);

        return buildAuthResponse(accessToken, refreshToken, user);
    }

    @Transactional
    public AuthResponse refreshToken(String refreshToken) {
        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (jwtUtil.isTokenExpired(refreshToken)) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String newAccessToken = jwtUtil.generateAccessToken(userDetails);
        String newRefreshToken = jwtUtil.generateRefreshToken(userDetails);

        user.setRefreshToken(newRefreshToken);
        userRepository.save(user);

        return buildAuthResponse(newAccessToken, newRefreshToken, user);
    }

    @Transactional
    public void logout(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        user.setRefreshToken(null);
        userRepository.save(user);
        auditLogService.log(username, "USER_LOGOUT", "User",
                user.getId().toString(), "User logged out", null);
        log.info("User logged out: {}", username);
    }

    private AuthResponse buildAuthResponse(String accessToken, String refreshToken, User user) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessTokenExpiration())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
