package com.smartinventory.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username")
    private String username;

    @Column(name = "action_type", nullable = false)
    private String actionType;

    @Column(name = "entity_type")
    private String entityType;

    @Column(name = "entity_id")
    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
}
