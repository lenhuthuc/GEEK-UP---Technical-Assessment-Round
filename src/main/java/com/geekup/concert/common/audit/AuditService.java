package com.geekup.concert.common.audit;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(Long actorId, String actorRole, String action,
                    String targetType, String targetId,
                    String beforeData, String afterData, String ip) {
        AuditLog log = AuditLog.builder()
                .actorId(actorId)
                .actorRole(actorRole)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .beforeData(beforeData)
                .afterData(afterData)
                .ip(ip)
                .build();
        auditLogRepository.save(log);
    }
}
