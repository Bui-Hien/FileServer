package com.buihien.fileserver.audit;

import com.buihien.fileserver.audit.dto.AuditLogResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * API lấy toàn bộ nhật ký hệ thống.
     */
    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAllLogs() {
        List<AuditLogResponse> response = auditLogService.getAllLogs();
        return ResponseEntity.ok(response);
    }

    /**
     * API lấy nhật ký hoạt động của riêng một người dùng.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByUser(@PathVariable Long userId) {
        List<AuditLogResponse> response = auditLogService.getLogsByUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * API lấy nhật ký hoạt động liên quan tới một tài nguyên cụ thể (tệp tin hoặc thư mục).
     */
    @GetMapping("/resource")
    public ResponseEntity<List<AuditLogResponse>> getLogsByResource(
            @RequestParam("resourceType") String resourceType,
            @RequestParam("resourceId") Long resourceId) {
        if (resourceType == null || resourceId == null) {
            return ResponseEntity.badRequest().build();
        }
        List<AuditLogResponse> response = auditLogService.getLogsByResource(resourceType, resourceId);
        return ResponseEntity.ok(response);
    }
}
