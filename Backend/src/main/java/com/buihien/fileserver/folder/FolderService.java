package com.buihien.fileserver.folder;

import com.buihien.fileserver.acl.PermissionService;
import com.buihien.fileserver.auth.AuthService;
import com.buihien.fileserver.folder.dto.FolderCreateRequest;
import com.buihien.fileserver.folder.dto.FolderResponse;
import com.buihien.fileserver.user.User;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FolderService {

    private final FolderRepository folderRepository;
    private final AuthService authService;
    private final PermissionService permissionService;

    // Sử dụng @Lazy để tránh mối quan hệ vòng (Circular Dependency) giữa
    // FolderService và PermissionService
    public FolderService(FolderRepository folderRepository, AuthService authService,
            @Lazy PermissionService permissionService) {
        this.folderRepository = folderRepository;
        this.authService = authService;
        this.permissionService = permissionService;
    }

    /**
     * Tạo thư mục mới.
     * Cây thư mục sử dụng cấu trúc Materialized Path để quản lý đường dẫn đầy đủ
     * dạng chuỗi.
     */
    @Transactional
    public FolderResponse createFolder(FolderCreateRequest request) {
        User currentUser = authService.getCurrentUser();

        Folder parent = null;
        String path;

        if (request.getParentId() != null) {
            parent = folderRepository.findById(request.getParentId())
                    .orElseThrow(
                            () -> new RuntimeException("Không tìm thấy thư mục cha với ID: " + request.getParentId()));

            // KIỂM TRA QUYỀN HẠN: Kiểm tra xem người dùng có quyền tạo thư mục con
            // (FOLDER_CREATE) trong thư mục cha này không
            boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FOLDER", parent.getId(),
                    "FOLDER_CREATE");
            if (!hasPermission) {
                throw new SecurityException(
                        "Từ chối truy cập! Bạn không có quyền tạo thư mục mới (FOLDER_CREATE) trong thư mục này.");
            }

            path = parent.getPath() + "/" + request.getName();
        } else {
            // Nằm ở thư mục gốc (Root) - cho phép mọi người dùng tạo thư mục cá nhân của
            // mình
            path = "/root/" + request.getName();
        }

        Folder folder = Folder.builder()
                .name(request.getName())
                .parent(parent)
                .path(path)
                .owner(currentUser)
                .isDeleted(false)
                .build();

        Folder savedFolder = folderRepository.save(folder);
        return mapToResponse(savedFolder);
    }

    /**
     * Lấy toàn bộ thư mục thuộc sở hữu của người dùng hiện tại HOẶC các thư mục
     * được chia sẻ cho họ.
     */
    @Transactional(readOnly = true)
    public List<FolderResponse> getFolders() {
        User currentUser = authService.getCurrentUser();

        // 1. Lấy tất cả thư mục vật lý hoạt động mà người dùng có quyền truy cập
        List<Folder> allFolders = folderRepository.findAll().stream()
                .filter(folder -> !folder.getIsDeleted())
                .filter(folder -> folder.getOwner().getId().equals(currentUser.getId())
                        || permissionService.checkPermission(currentUser.getId(), "FOLDER", folder.getId(),
                                "FILE_READ"))
                .collect(Collectors.toList());

        // 2. Chuyển đổi sang Response DTOs và gán lại parentId cho các thư mục con được
        // chia sẻ mà cha của nó không truy cập được
        List<FolderResponse> responses = allFolders.stream()
                .map(folder -> {
                    FolderResponse res = mapToResponse(folder);
                    // Nếu không phải do User sở hữu và thư mục cha (nếu có) không truy cập được bởi
                    // User này
                    if (!folder.getOwner().getId().equals(currentUser.getId())) {
                        res.setName("(Share) " + folder.getName());
                        boolean parentAccessible = folder.getParent() != null
                                && allFolders.stream().anyMatch(p -> p.getId().equals(folder.getParent().getId()));
                        if (!parentAccessible) {
                            res.setParentId(-folder.getOwner().getId());
                        }
                    }
                    return res;
                })
                .collect(Collectors.toCollection(java.util.ArrayList::new));

        // 3. Tạo các thư mục ảo ở Root (parentId == null) đại diện cho từng người dùng
        // đã chia sẻ tài nguyên
        List<User> sharers = permissionService.getSharedResourceOwners(currentUser.getId());
        for (User sharer : sharers) {
            FolderResponse virtualFolder = FolderResponse.builder()
                    .id(-sharer.getId())
                    .name("(Share) " + sharer.getUsername()) // Tên của thư mục ảo kèm tiền tố (Share)
                    .parentId(null) // Nằm ở Root
                    .path("/" + sharer.getUsername())
                    .ownerId(sharer.getId())
                    .ownerUsername(sharer.getUsername())
                    .isDeleted(false)
                    .build();
            responses.add(virtualFolder);
        }

        return responses;
    }

    /**
     * Xóa mềm thư mục (Soft Delete) và toàn bộ các thư mục con của nó dựa trên
     * Materialized Path.
     */
    @Transactional
    public void deleteFolder(Long id) {
        User currentUser = authService.getCurrentUser();
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thư mục với ID: " + id));

        // KIỂM TRA QUYỀN HẠN: Kiểm tra quyền xóa thư mục (FOLDER_DELETE)
        boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FOLDER", folder.getId(),
                "FOLDER_DELETE");
        if (!hasPermission) {
            throw new SecurityException("Từ chối truy cập! Bạn không có quyền xóa thư mục này (FOLDER_DELETE).");
        }

        // Soft delete thư mục hiện tại
        folder.setIsDeleted(true);
        folderRepository.save(folder);

        // Tìm kiếm và xóa mềm toàn bộ các thư mục con cháu dựa trên đường dẫn
        // Materialized Path
        List<Folder> subFolders = folderRepository.findByPathStartingWithAndIsDeletedFalse(folder.getPath() + "/");
        for (Folder sub : subFolders) {
            sub.setIsDeleted(true);
            folderRepository.save(sub);
        }
    }

    /**
     * Đổi tên thư mục và đồng bộ lại đường dẫn Materialized Path cho tất cả thư mục con bên trong.
     */
    @Transactional
    public FolderResponse renameFolder(Long id, String newName) {
        User currentUser = authService.getCurrentUser();
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thư mục với ID: " + id));

        // KIỂM TRA QUYỀN HẠN: Chỉ chủ sở hữu thư mục hoặc ADMIN mới được đổi tên thư mục
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(role -> role.getCode().equalsIgnoreCase("ADMIN"));
        boolean isOwner = folder.getOwner().getId().equals(currentUser.getId());
        
        if (!isOwner && !isAdmin) {
            throw new SecurityException("Từ chối truy cập! Bạn không phải chủ sở hữu để đổi tên thư mục này.");
        }

        String oldPath = folder.getPath();
        String parentPath = folder.getParent() != null ? folder.getParent().getPath() : "/root";
        String newPath = parentPath + "/" + newName;

        // Cập nhật tên và đường dẫn của thư mục hiện tại
        folder.setName(newName);
        folder.setPath(newPath);
        folderRepository.save(folder);

        // Cập nhật đường dẫn của toàn bộ thư mục con cháu bên dưới
        List<Folder> subFolders = folderRepository.findByPathStartingWithAndIsDeletedFalse(oldPath + "/");
        for (Folder sub : subFolders) {
            String subRelativePath = sub.getPath().substring(oldPath.length());
            sub.setPath(newPath + subRelativePath);
            folderRepository.save(sub);
        }

        return mapToResponse(folder);
    }

    /**
     * Ánh xạ thực thể Folder sang DTO FolderResponse.
     */
    public FolderResponse mapToResponse(Folder folder) {
        if (folder == null)
            return null;
        return FolderResponse.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .path(folder.getPath())
                .ownerId(folder.getOwner().getId())
                .ownerUsername(folder.getOwner().getUsername())
                .isDeleted(folder.getIsDeleted())
                .createdAt(folder.getCreatedAt())
                .updatedAt(folder.getUpdatedAt())
                .build();
    }
}
