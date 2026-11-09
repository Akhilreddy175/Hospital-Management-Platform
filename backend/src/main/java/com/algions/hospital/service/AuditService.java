package com.algions.hospital.service;

import com.algions.hospital.entity.AuditLog;
import com.algions.hospital.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordEvent(String action, String entityType, Long entityId, String userEmail, String details) {
        AuditLog auditLog = new AuditLog(action, entityType, entityId, userEmail, details);
        auditLogRepository.save(auditLog);
        log.info("AUDIT_EVENT: action={}, entityType={}, entityId={}, userEmail={}, details={}",
            action, entityType, entityId, userEmail, details);
    }
}
