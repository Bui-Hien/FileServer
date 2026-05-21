package com.buihien.fileserver.acl.dto;

import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareRequest {
    private String resourceType; // "FILE" hoặc "FOLDER"
    private Long resourceId;
    private Set<Long> userIds; // Chia sẻ cho danh sách cá nhân cụ thể. Nếu trống và shareWithEveryone = true, chia sẻ cho mọi người.
    private Boolean shareWithEveryone; // true = chia sẻ cho tất cả mọi người (Public)
    private String permissionCode; // Mặc định "FILE_READ"
    private Boolean allow; // true = Cho phép, false = Chặn
}
