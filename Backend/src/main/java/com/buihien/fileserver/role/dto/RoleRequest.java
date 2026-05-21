package com.buihien.fileserver.role.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleRequest {
    private String code;
    private String name;
}
