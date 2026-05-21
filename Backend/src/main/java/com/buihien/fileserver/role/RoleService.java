package com.buihien.fileserver.role;

import com.buihien.fileserver.permission.Permission;
import com.buihien.fileserver.permission.PermissionRepository;
import com.buihien.fileserver.role.dto.RoleRequest;
import com.buihien.fileserver.role.dto.RoleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    /**
     * Tạo mới vai trò trong hệ thống.
     */
    @Transactional
    public RoleResponse createRole(RoleRequest request) {
        if (roleRepository.findByCode(request.getCode().toUpperCase()).isPresent()) {
            throw new RuntimeException("Mã vai trò đã tồn tại: " + request.getCode());
        }

        Role role = Role.builder()
                .code(request.getCode().toUpperCase())
                .name(request.getName())
                .build();

        Role savedRole = roleRepository.save(role);
        return mapToResponse(savedRole);
    }

    /**
     * Lấy toàn bộ danh sách vai trò.
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gán trực tiếp danh sách quyền hạn chi tiết cho vai trò.
     */
    @Transactional
    public RoleResponse assignPermissions(Long roleId, Set<String> permissionCodes) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy vai trò với ID: " + roleId));

        Set<Permission> permissions = new HashSet<>();
        for (String code : permissionCodes) {
            permissionRepository.findByCode(code).ifPresent(permissions::add);
        }
        role.setPermissions(permissions);

        Role updatedRole = roleRepository.save(role);
        return mapToResponse(updatedRole);
    }

    /**
     * Ánh xạ thực thể Role sang DTO RoleResponse.
     */
    public RoleResponse mapToResponse(Role role) {
        if (role == null) return null;
        return RoleResponse.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .permissions(role.getPermissions().stream().map(Permission::getCode).collect(Collectors.toSet()))
                .build();
    }
}
