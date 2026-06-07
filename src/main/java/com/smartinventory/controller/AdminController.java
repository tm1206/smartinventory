package com.smartinventory.controller;

import com.smartinventory.dto.ApiResponse;
import com.smartinventory.dto.UserResponse;
import com.smartinventory.model.AuditLog;
import com.smartinventory.model.User;
import com.smartinventory.repository.UserRepository;
import com.smartinventory.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin-only endpoints")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/users")
    @Operation(summary = "Get all users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{id}")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.smartinventory.exception.ResourceNotFoundException("User", "id", id));
        return ResponseEntity.ok(ApiResponse.success(toUserResponse(user)));
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable Long id,
            @RequestParam User.Role role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.smartinventory.exception.ResourceNotFoundException("User", "id", id));
        user.setRole(role);
        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Role updated successfully", toUserResponse(user)));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "Activate or deactivate a user")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.smartinventory.exception.ResourceNotFoundException("User", "id", id));
        user.setActive(active);
        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("User status updated", toUserResponse(user)));
    }

    @PutMapping("/users/{id}/password")
    @Operation(summary = "Reset user password")
    public ResponseEntity<ApiResponse<UserResponse>> resetUserPassword(
            @PathVariable Long id,
            @RequestParam String password) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new com.smartinventory.exception.ResourceNotFoundException("User", "id", id));
        user.setPassword(passwordEncoder.encode(password));
        user = userRepository.save(user);
        auditLogService.log("admin", "PASSWORD_RESET", "User", id.toString(), "Password reset by admin", null);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", toUserResponse(user)));
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get audit logs")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(auditLogService.getAuditLogs(pageable)));
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
