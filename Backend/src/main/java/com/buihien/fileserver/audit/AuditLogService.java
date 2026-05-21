package com.buihien.fileserver.audit;

import com.buihien.fileserver.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Lấy toàn bộ nhật ký hệ thống.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getAllLogs() {
        return auditLogRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy nhật ký thao tác của một người dùng cụ thể.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByUser(Long userId) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy nhật ký lịch sử của một tài nguyên (File hoặc Folder) cụ thể.
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getLogsByResource(String resourceType, Long resourceId) {
        return auditLogRepository.findByResourceTypeAndResourceIdOrderByCreatedAtDesc(resourceType, resourceId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Ánh xạ thực thể AuditLog sang DTO AuditLogResponse.
     */
    public AuditLogResponse mapToResponse(AuditLog log) {
        if (log == null) return null;
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .username(log.getUser() != null ? log.getUser().getUsername() : "ANONYMOUS")
                .action(log.getAction())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .ipAddress(log.getIpAddress())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
