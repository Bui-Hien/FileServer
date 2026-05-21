package com.buihien.fileserver.user.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserUpdateRequest {
    private String email;
    private String password; // optional new password
    private String fullName;
    private String status;
    private Long maxStorage;
    private Set<String> roleCodes;
}
