package com.buihien.fileserver.acl;

import com.buihien.fileserver.acl.dto.PermissionAssignRequest;
import com.buihien.fileserver.acl.dto.ResourcePermissionResponse;
import com.buihien.fileserver.acl.dto.ShareRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    /**
     * API gán phân quyền động (ACL) cho một tài nguyên cụ thể (tệp tin hoặc thư mục).
     */
    @PostMapping("/{id}/permissions")
    public ResponseEntity<ResourcePermissionResponse> assignPermission(
            @PathVariable("id") Long resourceId,
            @RequestBody PermissionAssignRequest request) {

        // Gán ID tài nguyên lấy từ đường dẫn dẫn tới
        request.setResourceId(resourceId);

        if (request.getResourceType() == null || request.getPermissionCode() == null) {
            return ResponseEntity.badRequest().build();
        }

        ResourcePermissionResponse response = permissionService.assignPermission(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API CHIA SẺ tài nguyên (Tệp tin / Thư mục) cho danh sách cá nhân cụ thể hoặc cho TẤT CẢ MỌI NGƯỜI (Public Share).
     */
    @PostMapping("/{id}/share")
    public ResponseEntity<List<ResourcePermissionResponse>> shareResource(
            @PathVariable("id") Long resourceId,
            @RequestBody ShareRequest request) {

        request.setResourceId(resourceId);

        if (request.getResourceType() == null) {
            return ResponseEntity.badRequest().build();
        }

        List<ResourcePermissionResponse> responses = permissionService.shareResource(request);
        return ResponseEntity.ok(responses);
    }

    /**
     * API lấy danh sách phân quyền (ACL) hiện tại của tài nguyên.
     */
    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<ResourcePermissionResponse>> getPermissionsByResource(
            @PathVariable("id") Long resourceId,
            @RequestParam("resourceType") String resourceType) {
        List<ResourcePermissionResponse> permissions = permissionService.getPermissionsByResource(resourceId, resourceType);
        return ResponseEntity.ok(permissions);
    }

    /**
     * API thu hồi/xóa phân quyền cụ thể theo ID bản ghi phân quyền.
     */
    @DeleteMapping("/permissions/{permissionId}")
    public ResponseEntity<Void> deletePermission(@PathVariable("permissionId") Long permissionId) {
        permissionService.deletePermission(permissionId);
        return ResponseEntity.noContent().build();
    }

    /**
     * API kiểm tra xem người dùng có quyền thực hiện một hành động cụ thể trên tài nguyên hay không.
     */
    @GetMapping("/{id}/permissions/check")
    public ResponseEntity<Boolean> checkPermission(
            @PathVariable("id") Long resourceId,
            @RequestParam("resourceType") String resourceType,
            @RequestParam("userId") Long userId,
            @RequestParam("permissionCode") String permissionCode) {

        boolean hasAccess = permissionService.checkPermission(userId, resourceType, resourceId, permissionCode);
        return ResponseEntity.ok(hasAccess);
    }
}
