package com.buihien.fileserver.permission;

import com.buihien.fileserver.permission.dto.PermissionRequest;
import com.buihien.fileserver.permission.dto.PermissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemPermissionService {

    private final PermissionRepository permissionRepository;

    /**
     * Tạo mới quyền hạn chi tiết trong hệ thống.
     */
    @Transactional
    public PermissionResponse createPermission(PermissionRequest request) {
        if (permissionRepository.findByCode(request.getCode().toUpperCase()).isPresent()) {
            throw new RuntimeException("Mã quyền hạn đã tồn tại: " + request.getCode());
        }

        Permission permission = Permission.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .build();

        Permission savedPermission = permissionRepository.save(permission);
        return mapToResponse(savedPermission);
    }

    /**
     * Lấy toàn bộ danh sách các quyền hạn có trong hệ thống.
     */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Ánh xạ thực thể Permission sang DTO PermissionResponse.
     */
    public PermissionResponse mapToResponse(Permission permission) {
        if (permission == null) return null;
        return PermissionResponse.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .build();
    }
}
