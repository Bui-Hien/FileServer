package com.buihien.fileserver.role;

import com.buihien.fileserver.role.dto.RoleRequest;
import com.buihien.fileserver.role.dto.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * API tạo mới vai trò trong hệ thống.
     */
    @PostMapping
    public ResponseEntity<RoleResponse> createRole(@RequestBody RoleRequest request) {
        if (request.getCode() == null || request.getName() == null) {
            return ResponseEntity.badRequest().build();
        }
        RoleResponse response = roleService.createRole(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API lấy danh sách toàn bộ các vai trò có trong hệ thống.
     */
    @GetMapping
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        List<RoleResponse> response = roleService.getAllRoles();
        return ResponseEntity.ok(response);
    }

    /**
     * API gán các quyền hạn chi tiết trực tiếp cho vai trò chỉ định.
     */
    @PostMapping("/{id}/permissions")
    public ResponseEntity<RoleResponse> assignPermissions(
            @PathVariable Long id,
            @RequestBody Set<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        RoleResponse response = roleService.assignPermissions(id, permissionCodes);
        return ResponseEntity.ok(response);
    }
}
