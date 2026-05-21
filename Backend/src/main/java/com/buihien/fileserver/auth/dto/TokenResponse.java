package com.buihien.fileserver.auth.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenResponse {
    private String accessToken;
    private String refreshToken;
    private String username;
    private Set<String> roles;
    private Set<String> permissions;
}
