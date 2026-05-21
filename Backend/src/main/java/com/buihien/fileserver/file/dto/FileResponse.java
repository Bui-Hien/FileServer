package com.buihien.fileserver.file.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileResponse {
    private Long id;
    private String fileName;
    private String extension;
    private String mimeType;
    private Long size;
    private String storagePath;
    private String checksum;
    private Integer version;
    private Long folderId;
    private Long ownerId;
    private String ownerUsername;
    private String status;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
