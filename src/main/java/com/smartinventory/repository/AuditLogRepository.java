package com.smartinventory.repository;

import com.smartinventory.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUsername(String username);
    List<AuditLog> findByActionType(String actionType);
    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
