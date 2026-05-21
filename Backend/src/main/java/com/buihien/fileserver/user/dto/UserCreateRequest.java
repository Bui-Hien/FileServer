package com.buihien.fileserver.user.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String status;
    private Long maxStorage; // optional
    private Set<String> roleCodes; // optional initial roles
}
