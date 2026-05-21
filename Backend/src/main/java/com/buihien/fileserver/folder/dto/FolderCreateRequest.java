package com.buihien.fileserver.folder.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderCreateRequest {
    private String name;
    private Long parentId;
}
