package com.buihien.fileserver.file.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersionResponse {
    private Long id;
    private Long fileId;
    private Integer version;
    private String storagePath;
    private String checksum;
    private Long size;
    private Long createdBy; // stored as Long id
    private LocalDateTime createdAt;
}
