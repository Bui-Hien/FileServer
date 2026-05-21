package com.buihien.fileserver.audit.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String resourceType;
    private Long resourceId;
    private String ipAddress;
    private LocalDateTime createdAt;
}
