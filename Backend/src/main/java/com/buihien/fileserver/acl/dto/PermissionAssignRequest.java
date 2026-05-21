package com.buihien.fileserver.acl.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionAssignRequest {
    private String resourceType; // e.g., FILE or FOLDER
    private Long resourceId;
    private Long userId;
    private String permissionCode; // e.g., FILE_READ, FILE_WRITE, FILE_DELETE, FOLDER_DELETE
    private Boolean allow;
}
