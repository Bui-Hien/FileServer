package com.buihien.fileserver.user.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String status;
    private Long usedStorage;
    private Long maxStorage;
    private String formattedUsedStorage;
    private String formattedMaxStorage;
    private String roleNames;
    private Set<String> roles;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
