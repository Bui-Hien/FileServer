package com.buihien.fileserver.folder;

import com.buihien.fileserver.folder.dto.FolderCreateRequest;
import com.buihien.fileserver.folder.dto.FolderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/folders")
@RequiredArgsConstructor
public class FolderController {

    private final FolderService folderService;

    /**
     * API tạo mới thư mục.
     */
    @PostMapping
    public ResponseEntity<FolderResponse> createFolder(@RequestBody FolderCreateRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        FolderResponse response = folderService.createFolder(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API lấy danh sách các thư mục hoạt động của người dùng hiện hành.
     */
    @GetMapping
    public ResponseEntity<List<FolderResponse>> getFolders() {
        List<FolderResponse> response = folderService.getFolders();
        return ResponseEntity.ok(response);
    }

    /**
     * API xóa mềm thư mục và tất cả thư mục con cháu bên trong nó.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
        folderService.deleteFolder(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * API đổi tên thư mục.
     */
    @PutMapping("/{id}/rename")
    public ResponseEntity<FolderResponse> renameFolder(
            @PathVariable Long id,
            @RequestParam("name") String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        FolderResponse response = folderService.renameFolder(id, name.trim());
        return ResponseEntity.ok(response);
    }
}
