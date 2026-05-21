package com.buihien.fileserver.permission;

import com.buihien.fileserver.permission.dto.PermissionRequest;
import com.buihien.fileserver.permission.dto.PermissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class SystemPermissionController {

    private final SystemPermissionService systemPermissionService;

    /**
     * API tạo mới quyền hạn trong hệ thống.
     */
    @PostMapping
    public ResponseEntity<PermissionResponse> createPermission(@RequestBody PermissionRequest request) {
        if (request.getCode() == null || request.getName() == null) {
            return ResponseEntity.badRequest().build();
        }
        PermissionResponse response = systemPermissionService.createPermission(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API lấy toàn bộ danh sách quyền hạn.
     */
    @GetMapping
    public ResponseEntity<List<PermissionResponse>> getAllPermissions() {
        List<PermissionResponse> response = systemPermissionService.getAllPermissions();
        return ResponseEntity.ok(response);
    }
}
