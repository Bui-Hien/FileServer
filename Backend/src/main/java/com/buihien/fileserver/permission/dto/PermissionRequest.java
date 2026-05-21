package com.buihien.fileserver.permission.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionRequest {
    private String code;
    private String name;
}
