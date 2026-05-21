package com.buihien.fileserver.acl;

import com.buihien.fileserver.acl.dto.PermissionAssignRequest;
import com.buihien.fileserver.acl.dto.ResourcePermissionResponse;
import com.buihien.fileserver.acl.dto.ShareRequest;
import com.buihien.fileserver.auth.AuthService;
import com.buihien.fileserver.user.User;
import com.buihien.fileserver.user.UserRepository;
import com.buihien.fileserver.role.Role;
import com.buihien.fileserver.file.FileEntity;
import com.buihien.fileserver.file.FileRepository;
import com.buihien.fileserver.folder.Folder;
import com.buihien.fileserver.folder.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final ResourcePermissionRepository resourcePermissionRepository;
    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final AuthService authService;

    /**
     * Gán quyền động chi tiết (ACL) cho người dùng cụ thể trên tài nguyên xác định.
     */
    @Transactional
    public ResourcePermissionResponse assignPermission(PermissionAssignRequest request) {
        User currentUser = authService.getCurrentUser();
        
        User targetUser = null;
        if (request.getUserId() != null) {
            targetUser = userRepository.findById(request.getUserId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + request.getUserId()));
        }

        // Kiểm tra quyền sở hữu hoặc ADMIN trước khi gán quyền
        validateSharePermission(currentUser.getId(), request.getResourceType(), request.getResourceId());

        Optional<ResourcePermission> existingPermOpt;
        if (targetUser != null) {
            existingPermOpt = resourcePermissionRepository
                    .findByResourceTypeAndResourceIdAndUserIdAndPermissionCode(
                            request.getResourceType(),
                            request.getResourceId(),
                            request.getUserId(),
                            request.getPermissionCode()
                    );
        } else {
            existingPermOpt = resourcePermissionRepository
                    .findByResourceTypeAndResourceIdAndUserNullAndPermissionCode(
                            request.getResourceType(),
                            request.getResourceId(),
                            request.getPermissionCode()
                    );
        }

        ResourcePermission permission;
        if (existingPermOpt.isPresent()) {
            permission = existingPermOpt.get();
            permission.setAllow(request.getAllow() != null ? request.getAllow() : true);
            permission.setCreatedBy(currentUser.getId());
        } else {
            permission = ResourcePermission.builder()
                    .resourceType(request.getResourceType())
                    .resourceId(request.getResourceId())
                    .user(targetUser)
                    .permissionCode(request.getPermissionCode())
                    .allow(request.getAllow() != null ? request.getAllow() : true)
                    .createdBy(currentUser.getId())
                    .build();
        }

        ResourcePermission savedPermission = resourcePermissionRepository.save(permission);
        return mapToResponse(savedPermission);
    }

    /**
     * Thực hiện CHIA SẺ tài nguyên cho một nhóm người dùng hoặc cho TẤT CẢ MỌI NGƯỜI (Public Share).
     */
    @Transactional
    public List<ResourcePermissionResponse> shareResource(ShareRequest request) {
        User currentUser = authService.getCurrentUser();
        
        // KIỂM TRA BẢO MẬT: Chỉ chủ sở hữu tài nguyên hoặc ADMIN mới có quyền chia sẻ tài nguyên này
        validateSharePermission(currentUser.getId(), request.getResourceType(), request.getResourceId());

        String permissionCode = request.getPermissionCode() != null ? request.getPermissionCode() : "FILE_READ";
        boolean allow = request.getAllow() != null ? request.getAllow() : true;
        List<ResourcePermissionResponse> responses = new ArrayList<>();

        // 1. Chia sẻ cho tất cả mọi người (Public / Everyone)
        if (Boolean.TRUE.equals(request.getShareWithEveryone())) {
            Optional<ResourcePermission> existingEveryoneOpt = resourcePermissionRepository
                    .findByResourceTypeAndResourceIdAndUserNullAndPermissionCode(
                            request.getResourceType(), request.getResourceId(), permissionCode);

            ResourcePermission everyonePerm;
            if (existingEveryoneOpt.isPresent()) {
                everyonePerm = existingEveryoneOpt.get();
                everyonePerm.setAllow(allow);
                everyonePerm.setCreatedBy(currentUser.getId());
            } else {
                everyonePerm = ResourcePermission.builder()
                        .resourceType(request.getResourceType())
                        .resourceId(request.getResourceId())
                        .user(null) // User = null đại diện cho MỌI NGƯỜI
                        .permissionCode(permissionCode)
                        .allow(allow)
                        .createdBy(currentUser.getId())
                        .build();
            }
            responses.add(mapToResponse(resourcePermissionRepository.save(everyonePerm)));
        }

        // 2. Chia sẻ cho danh sách cá nhân cụ thể
        if (request.getUserIds() != null && !request.getUserIds().isEmpty()) {
            for (Long targetUserId : request.getUserIds()) {
                User targetUser = userRepository.findById(targetUserId)
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng nhận chia sẻ với ID: " + targetUserId));

                Optional<ResourcePermission> existingUserOpt = resourcePermissionRepository
                        .findByResourceTypeAndResourceIdAndUserIdAndPermissionCode(
                                request.getResourceType(), request.getResourceId(), targetUserId, permissionCode);

                ResourcePermission userPerm;
                if (existingUserOpt.isPresent()) {
                    userPerm = existingUserOpt.get();
                    userPerm.setAllow(allow);
                    userPerm.setCreatedBy(currentUser.getId());
                } else {
                    userPerm = ResourcePermission.builder()
                            .resourceType(request.getResourceType())
                            .resourceId(request.getResourceId())
                            .user(targetUser)
                            .permissionCode(permissionCode)
                            .allow(allow)
                            .createdBy(currentUser.getId())
                            .build();
                }
                responses.add(mapToResponse(resourcePermissionRepository.save(userPerm)));
            }
        }

        return responses;
    }

    /**
     * Kiểm tra quyền hạn truy cập tài nguyên của người dùng theo luồng xử lý:
     * 1. Quyền tối cao ADMIN.
     * 2. Quyền Chủ sở hữu tài nguyên (Owner).
     * 3. Phân quyền trực tiếp cá nhân (ACL).
     * 3b. Phân quyền cho Mọi người (Everyone Share).
     * 4. Phân quyền vai trò (RBAC).
     * 5. Kế thừa quyền từ thư mục cha gần nhất.
     */
    @Transactional(readOnly = true)
    public boolean checkPermission(Long userId, String resourceType, Long resourceId, String permissionCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng kiểm tra với ID: " + userId));

        // 1. Kiểm tra quyền ADMIN tối cao
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getCode().equalsIgnoreCase("ADMIN"));
        if (isAdmin) {
            return true;
        }

        // 2. Kiểm tra chủ sở hữu tài nguyên (Owner)
        if ("FILE".equalsIgnoreCase(resourceType)) {
            Optional<FileEntity> fileOpt = fileRepository.findById(resourceId);
            if (fileOpt.isPresent() && fileOpt.get().getOwner().getId().equals(userId)) {
                return true;
            }
        } else if ("FOLDER".equalsIgnoreCase(resourceType)) {
            Optional<Folder> folderOpt = folderRepository.findById(resourceId);
            if (folderOpt.isPresent() && folderOpt.get().getOwner().getId().equals(userId)) {
                return true;
            }
        }

        // 3. Kiểm tra danh sách phân quyền ACL trực tiếp cho người dùng này trên tài nguyên này
        // Ghi chú: Quyền Ghi (FILE_WRITE) luôn bao gồm cả quyền Đọc (FILE_READ) và quyền tạo/xóa thư mục con (FOLDER_CREATE/DELETE)
        List<String> validCodes = new ArrayList<>();
        validCodes.add(permissionCode.toUpperCase());
        if ("FILE_READ".equalsIgnoreCase(permissionCode)) {
            validCodes.add("FILE_WRITE");
        }
        if ("FOLDER_CREATE".equalsIgnoreCase(permissionCode) || "FOLDER_DELETE".equalsIgnoreCase(permissionCode)) {
            validCodes.add("FILE_WRITE");
        }

        List<ResourcePermission> userPerms = resourcePermissionRepository.findByResourceTypeAndResourceIdAndUserId(resourceType, resourceId, userId);
        Optional<ResourcePermission> aclOpt = userPerms.stream()
                .filter(p -> validCodes.contains(p.getPermissionCode().toUpperCase()))
                .findFirst();
        if (aclOpt.isPresent()) {
            return aclOpt.get().getAllow(); // true = allow, false = deny
        }

        // 3b. Kiểm tra danh sách phân quyền ACL cho MỌI NGƯỜI (User Null) trên tài nguyên này
        List<ResourcePermission> allPerms = resourcePermissionRepository.findByResourceTypeAndResourceId(resourceType, resourceId);
        Optional<ResourcePermission> aclEveryoneOpt = allPerms.stream()
                .filter(p -> p.getUser() == null && validCodes.contains(p.getPermissionCode().toUpperCase()))
                .findFirst();
        if (aclEveryoneOpt.isPresent()) {
            return aclEveryoneOpt.get().getAllow();
        }

        // 5. Kiểm tra quyền kế thừa từ thư mục cha gần nhất (nếu tài nguyên nằm trong thư mục)
        if ("FILE".equalsIgnoreCase(resourceType)) {
            Optional<FileEntity> fileOpt = fileRepository.findById(resourceId);
            if (fileOpt.isPresent() && fileOpt.get().getFolder() != null) {
                // Kế thừa quyền từ thư mục chứa file
                return checkPermission(userId, "FOLDER", fileOpt.get().getFolder().getId(), permissionCode);
            }
        } else if ("FOLDER".equalsIgnoreCase(resourceType)) {
            Optional<Folder> folderOpt = folderRepository.findById(resourceId);
            if (folderOpt.isPresent() && folderOpt.get().getParent() != null) {
                // Kế thừa quyền từ thư mục cha
                return checkPermission(userId, "FOLDER", folderOpt.get().getParent().getId(), permissionCode);
            }
        }

        // Từ chối truy cập mặc định
        return false;
    }

    // Helper: Xác thực xem người dùng có phải ADMIN hoặc Chủ sở hữu tài nguyên để có quyền chia sẻ/cấp quyền hay không
    private void validateSharePermission(Long currentUserId, String resourceType, Long resourceId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng hiện tại!"));

        // 1. Nếu là Admin hệ thống thì có toàn quyền chia sẻ
        boolean isAdmin = user.getRoles().stream()
                .anyMatch(r -> r.getCode().equalsIgnoreCase("ADMIN"));
        if (isAdmin) {
            return;
        }

        // 2. Nếu không phải Admin, bắt buộc phải là Chủ sở hữu tài nguyên (Owner)
        if ("FILE".equalsIgnoreCase(resourceType)) {
            FileEntity file = fileRepository.findById(resourceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy tệp tin với ID: " + resourceId));
            if (!file.getOwner().getId().equals(currentUserId)) {
                throw new SecurityException("Từ chối chia sẻ! Bạn không phải là chủ sở hữu của tệp tin này.");
            }
        } else if ("FOLDER".equalsIgnoreCase(resourceType)) {
            Folder folder = folderRepository.findById(resourceId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thư mục với ID: " + resourceId));
            if (!folder.getOwner().getId().equals(currentUserId)) {
                throw new SecurityException("Từ chối chia sẻ! Bạn không phải là chủ sở hữu của thư mục này.");
            }
        }
    }

    /**
     * Lấy danh sách toàn bộ phân quyền hiện tại của một tài nguyên.
     */
    @Transactional(readOnly = true)
    public List<ResourcePermissionResponse> getPermissionsByResource(Long resourceId, String resourceType) {
        return resourcePermissionRepository.findByResourceTypeAndResourceId(resourceType, resourceId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Thu hồi/xóa một phân quyền cụ thể.
     */
    @Transactional
    public void deletePermission(Long permissionId) {
        User currentUser = authService.getCurrentUser();
        ResourcePermission perm = resourcePermissionRepository.findById(permissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phân quyền với ID: " + permissionId));
        
        validateSharePermission(currentUser.getId(), perm.getResourceType(), perm.getResourceId());
        
        resourcePermissionRepository.delete(perm);
    }

    /**
     * Lấy danh sách ID các tài nguyên được chia sẻ cho một người dùng hoặc cho mọi người.
     */
    @Transactional(readOnly = true)
    public List<Long> getSharedResourceIds(Long userId, String resourceType) {
        List<ResourcePermission> sharedPerms = resourcePermissionRepository.findByResourceTypeAndUserId(resourceType, userId);
        List<ResourcePermission> everyonePerms = resourcePermissionRepository.findByResourceTypeAndUserNull(resourceType);

        List<Long> ids = new ArrayList<>();
        sharedPerms.stream().filter(ResourcePermission::getAllow).forEach(p -> ids.add(p.getResourceId()));
        everyonePerms.stream().filter(ResourcePermission::getAllow).forEach(p -> ids.add(p.getResourceId()));

        return ids.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Lấy danh sách những người dùng (chủ sở hữu) đã chia sẻ tài nguyên cho người dùng hiện tại.
     */
    @Transactional(readOnly = true)
    public List<User> getSharedResourceOwners(Long userId) {
        List<ResourcePermission> sharedPerms = resourcePermissionRepository.findByUserId(userId);
        List<ResourcePermission> everyonePerms = resourcePermissionRepository.findByUserNull();

        List<User> owners = new ArrayList<>();
        
        for (ResourcePermission p : sharedPerms) {
            if (Boolean.TRUE.equals(p.getAllow())) {
                User owner = getResourceOwner(p.getResourceType(), p.getResourceId());
                if (owner != null && !owner.getId().equals(userId)) {
                    owners.add(owner);
                }
            }
        }

        for (ResourcePermission p : everyonePerms) {
            if (Boolean.TRUE.equals(p.getAllow())) {
                User owner = getResourceOwner(p.getResourceType(), p.getResourceId());
                if (owner != null && !owner.getId().equals(userId)) {
                    owners.add(owner);
                }
            }
        }

        // Loại bỏ trùng lặp chủ sở hữu dựa trên ID của User
        return owners.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(User::getId, u -> u, (u1, u2) -> u1))
                .values().stream()
                .collect(Collectors.toList());
    }

    private User getResourceOwner(String resourceType, Long resourceId) {
        if ("FILE".equalsIgnoreCase(resourceType)) {
            return fileRepository.findById(resourceId)
                    .map(FileEntity::getOwner)
                    .orElse(null);
        } else if ("FOLDER".equalsIgnoreCase(resourceType)) {
            return folderRepository.findById(resourceId)
                    .map(Folder::getOwner)
                    .orElse(null);
        }
        return null;
    }

    private ResourcePermissionResponse mapToResponse(ResourcePermission perm) {
        if (perm == null) return null;
        return ResourcePermissionResponse.builder()
                .id(perm.getId())
                .resourceType(perm.getResourceType())
                .resourceId(perm.getResourceId())
                .userId(perm.getUser() != null ? perm.getUser().getId() : null)
                .username(perm.getUser() != null ? perm.getUser().getUsername() : "EVERYONE")
                .permissionCode(perm.getPermissionCode())
                .allow(perm.getAllow())
                .createdBy(perm.getCreatedBy())
                .createdAt(perm.getCreatedAt())
                .build();
    }
}
