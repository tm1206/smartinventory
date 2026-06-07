package com.smartinventory.service;

import com.smartinventory.model.AuditLog;
import com.smartinventory.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Async
    public void log(String username, String actionType, String entityType,
                    String entityId, String details, String ipAddress) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .actionType(actionType)
                    .entityType(entityType)
                    .entityId(entityId)
                    .details(details)
                    .ipAddress(ipAddress)
                    .timestamp(LocalDateTime.now())
                    .build();
            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }

    public Page<AuditLog> getAuditLogsByDateRange(LocalDateTime start, LocalDateTime end, Pageable pageable) {
        return auditLogRepository.findByTimestampBetween(start, end, pageable);
    }
}
