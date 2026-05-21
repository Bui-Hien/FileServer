package com.buihien.fileserver.acl.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourcePermissionResponse {
    private Long id;
    private String resourceType;
    private Long resourceId;
    private Long userId;
    private String username;
    private String permissionCode;
    private Boolean allow;
    private Long createdBy;
    private LocalDateTime createdAt;
}
