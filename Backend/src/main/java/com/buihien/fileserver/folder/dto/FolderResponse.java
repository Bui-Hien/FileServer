package com.buihien.fileserver.folder.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderResponse {
    private Long id;
    private String name;
    private Long parentId;
    private String path;
    private Long ownerId;
    private String ownerUsername;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
