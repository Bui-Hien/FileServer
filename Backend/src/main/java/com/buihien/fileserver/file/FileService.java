package com.buihien.fileserver.file;

import com.buihien.fileserver.acl.PermissionService;
import com.buihien.fileserver.auth.AuthService;
import com.buihien.fileserver.folder.Folder;
import com.buihien.fileserver.folder.FolderRepository;
import com.buihien.fileserver.file.dto.FileResponse;
import com.buihien.fileserver.file.dto.FileVersionResponse;
import com.buihien.fileserver.user.User;
import com.buihien.fileserver.user.UserRepository;
import com.buihien.fileserver.storage.StorageService;
import com.buihien.fileserver.audit.AuditLog;
import com.buihien.fileserver.audit.AuditLogRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;
    private final FolderRepository folderRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final StorageService storageService;
    private final AuthService authService;
    private final PermissionService permissionService;

    // Danh sách các đuôi mở rộng bị cấm để tránh tấn công Remote Code Execution
    // (RCE)
    private static final List<String> BLOCKED_EXTENSIONS = Arrays.asList(
            "exe", "bat", "sh", "msi", "cmd", "vbs", "js", "jsp", "php", "asp", "aspx", "jar", "py", "pl", "scr");

    // Danh sách MIME Type bị cấm
    private static final List<String> BLOCKED_MIME_TYPES = Arrays.asList(
            "application/x-msdownload", // EXE
            "application/x-sh", // SH
            "application/x-msi", // MSI
            "application/javascript", // JS
            "application/x-jsp", // JSP
            "application/x-php" // PHP
    );

    public FileService(
            FileRepository fileRepository,
            FileVersionRepository fileVersionRepository,
            FolderRepository folderRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            StorageService storageService,
            AuthService authService,
            @Lazy PermissionService permissionService) {
        this.fileRepository = fileRepository;
        this.fileVersionRepository = fileVersionRepository;
        this.folderRepository = folderRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.storageService = storageService;
        this.authService = authService;
        this.permissionService = permissionService;
    }

    /**
     * Tải lên một tệp tin lên hệ thống.
     * Áp dụng kiểm tra hạn mức dung lượng (Quota) của User.
     * Tự động kích hoạt cơ chế Versioning (lưu lịch sử bản ghi cũ) nếu trùng tên
     * tệp tin trong cùng thư mục.
     */
    @Transactional
    public FileResponse uploadFile(MultipartFile file, Long folderId, String ipAddress) {
        User currentUser = authService.getCurrentUser();
        long fileSize = file.getSize();

        // 1. KIỂM TRA BẢO MẬT: Validate tệp tin đầu vào
        validateFileSecurity(file);

        // 2. KIỂM TRA QUYỀN HẠN: Kiểm tra xem người dùng có quyền ghi file (FILE_WRITE)
        // trong thư mục đích không
        if (folderId != null) {
            boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FOLDER", folderId,
                    "FILE_WRITE");
            if (!hasPermission) {
                throw new SecurityException(
                        "Từ chối truy cập! Bạn không có quyền tải tệp tin (FILE_WRITE) vào thư mục này.");
            }
        }

        // 3. Kiểm tra hạn mức dung lượng (Quota)
        if (currentUser.getUsedStorage() + fileSize > currentUser.getMaxStorage()) {
            throw new RuntimeException("Tải file thất bại! Bạn đã vượt quá hạn mức dung lượng cho phép ("
                    + (currentUser.getMaxStorage() / (1024 * 1024 * 1024)) + " GB).");
        }

        // 4. Tính toán mã hash checksum toàn vẹn của tệp tin (sử dụng MD5)
        String checksum = calculateChecksum(file);

        // 5. Tìm thư mục đích
        Folder folder = null;
        if (folderId != null) {
            folder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy thư mục đích với ID: " + folderId));
        }

        // 6. KIỂM TRA TRÙNG LẶP CHECKSUM (Data Deduplication & Content-Addressable
        // Storage)
        Optional<FileEntity> existingFileWithSameChecksumOpt = fileRepository
                .findFirstByChecksumAndIsDeletedFalse(checksum);

        // Sử dụng cấu trúc lưu trữ phi tập trung và trung lập (Content-Addressable
        // Storage - CAS):
        // Không lưu đường dẫn chứa ID của người dùng để tránh ô nhiễm thư mục chéo và
        // vi phạm bảo mật truy cập.
        String fileExt = getFileExtension(file.getOriginalFilename());
        String storagePath = "store/" + checksum.substring(0, 2) + "/" + checksum
                + (fileExt.isEmpty() ? "" : "." + fileExt);
        boolean isDeduplicated = existingFileWithSameChecksumOpt.isPresent();

        // 7. Lưu trữ tệp tin vật lý (Chỉ tải lên đĩa nếu file có checksum mới hoàn
        // toàn)
        if (!isDeduplicated) {
            try (InputStream is = file.getInputStream()) {
                storageService.upload(is, storagePath);
            } catch (Exception e) {
                throw new RuntimeException("Lỗi xảy ra khi lưu trữ tệp tin: " + e.getMessage(), e);
            }
        }

        // 8. Kiểm tra xem file trùng tên trong cùng thư mục đã tồn tại chưa
        List<FileEntity> existingFiles = fileRepository.findByFolderIdAndIsDeletedFalse(folderId);
        Optional<FileEntity> duplicateFileOpt = existingFiles.stream()
                .filter(f -> f.getFileName().equalsIgnoreCase(file.getOriginalFilename()))
                .findFirst();

        FileEntity savedFileEntity;

        if (duplicateFileOpt.isPresent()) {
            // Cơ chế VERSIONING: Sao lưu phiên bản cũ và cập nhật thông tin phiên bản mới
            FileEntity oldFile = duplicateFileOpt.get();

            // Sao lưu phiên bản cũ vào bảng file_versions
            FileVersion fileVersion = FileVersion.builder()
                    .file(oldFile)
                    .version(oldFile.getVersion())
                    .storagePath(oldFile.getStoragePath())
                    .checksum(oldFile.getChecksum())
                    .size(oldFile.getSize())
                    .createdBy(currentUser.getId())
                    .build();
            fileVersionRepository.save(fileVersion);

            // Cập nhật tệp tin gốc với siêu dữ liệu mới
            oldFile.setStoragePath(storagePath);
            oldFile.setSize(fileSize);
            oldFile.setChecksum(checksum);
            oldFile.setMimeType(file.getContentType());
            oldFile.setVersion(oldFile.getVersion() + 1);
            oldFile.setExtension(getFileExtension(file.getOriginalFilename()));
            oldFile.setUpdatedAt(LocalDateTime.now());

            savedFileEntity = fileRepository.save(oldFile);
        } else {
            // Tạo một tệp tin mới hoàn toàn
            FileEntity newFile = FileEntity.builder()
                    .fileName(file.getOriginalFilename())
                    .extension(getFileExtension(file.getOriginalFilename()))
                    .mimeType(file.getContentType())
                    .size(fileSize)
                    .storagePath(storagePath)
                    .checksum(checksum)
                    .version(1)
                    .folder(folder)
                    .owner(currentUser)
                    .status("ACTIVE")
                    .isDeleted(false)
                    .build();

            savedFileEntity = fileRepository.save(newFile);
        }

        // 9. Cập nhật hạn mức dung lượng đã dùng của người dùng
        currentUser.setUsedStorage(currentUser.getUsedStorage() + fileSize);
        userRepository.save(currentUser);

        // 10. Lưu nhật ký hoạt động (Audit Log)
        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action("UPLOAD_FILE")
                .resourceType("FILE")
                .resourceId(savedFileEntity.getId())
                .ipAddress(ipAddress)
                .build());

        return mapToResponse(savedFileEntity);
    }

    /**
     * Tải xuống tệp tin từ hệ thống lưu trữ vật lý.
     */
    @Transactional
    public InputStream downloadFile(Long id, String ipAddress) {
        User currentUser = authService.getCurrentUser();
        FileEntity fileEntity = fileRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tệp tin hoặc tệp tin đã bị xóa với ID: " + id));

        // KIỂM TRA QUYỀN HẠN: Kiểm tra xem người dùng hiện tại có quyền đọc file
        // (FILE_READ) không
        boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FILE", fileEntity.getId(),
                "FILE_READ");
        if (!hasPermission) {
            throw new SecurityException("Từ chối truy cập! Bạn không có quyền đọc/tải xuống tệp tin này.");
        }

        // Tải luồng dữ liệu từ storage
        InputStream is = storageService.download(fileEntity.getStoragePath());

        // Lưu nhật ký hoạt động (Audit Log)
        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action("DOWNLOAD_FILE")
                .resourceType("FILE")
                .resourceId(fileEntity.getId())
                .ipAddress(ipAddress)
                .build());

        return is;
    }

    @Transactional
    public void verifyDownloadPermissionAndLog(Long id, String ipAddress) {
        User currentUser = authService.getCurrentUser();
        FileEntity fileEntity = fileRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tệp tin hoặc tệp tin đã bị xóa với ID: " + id));

        boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FILE", fileEntity.getId(),
                "FILE_READ");
        if (!hasPermission) {
            throw new SecurityException("Từ chối truy cập! Bạn không có quyền đọc/tải xuống tệp tin này.");
        }

        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action("DOWNLOAD_FILE")
                .resourceType("FILE")
                .resourceId(fileEntity.getId())
                .ipAddress(ipAddress)
                .build());
    }

    /**
     * Tải xuống một phiên bản lịch sử cũ của tệp tin.
     */
    @Transactional
    public InputStream downloadFileVersion(Long versionId, String ipAddress) {
        User currentUser = authService.getCurrentUser();
        FileVersion fileVersion = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bản tệp tin với ID: " + versionId));

        FileEntity fileEntity = fileVersion.getFile();

        // KIỂM TRA QUYỀN HẠN: Kiểm tra quyền đọc file gốc
        boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FILE", fileEntity.getId(),
                "FILE_READ");
        if (!hasPermission) {
            throw new SecurityException("Từ chối truy cập! Bạn không có quyền đọc/tải xuống tệp tin này.");
        }

        // Tải luồng dữ liệu từ storage sử dụng storagePath của version
        InputStream is = storageService.download(fileVersion.getStoragePath());

        // Lưu nhật ký hoạt động (Audit Log)
        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action("DOWNLOAD_FILE_VERSION")
                .resourceType("FILE")
                .resourceId(fileEntity.getId())
                .ipAddress(ipAddress)
                .build());

        return is;
    }

    @Transactional
    public void verifyDownloadVersionPermissionAndLog(Long versionId, String ipAddress) {
        User currentUser = authService.getCurrentUser();
        FileVersion fileVersion = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bản tệp tin với ID: " + versionId));

        FileEntity fileEntity = fileVersion.getFile();

        boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FILE", fileEntity.getId(),
                "FILE_READ");
        if (!hasPermission) {
            throw new SecurityException("Từ chối truy cập! Bạn không có quyền đọc/tải xuống tệp tin này.");
        }

        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action("DOWNLOAD_FILE_VERSION")
                .resourceType("FILE")
                .resourceId(fileEntity.getId())
                .ipAddress(ipAddress)
                .build());
    }

    /**
     * Xóa mềm tệp tin khỏi hệ thống (Soft Delete).
     * Giải phóng dung lượng Quota tương ứng cho người dùng.
     */
    @Transactional
    public void deleteFile(Long id, String ipAddress) {
        User currentUser = authService.getCurrentUser();
        FileEntity fileEntity = fileRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tệp tin với ID: " + id));

        // KIỂM TRA QUYỀN HẠN: Kiểm tra xem người dùng hiện tại có quyền xóa file
        // (FILE_DELETE) không
        boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FILE", fileEntity.getId(),
                "FILE_DELETE");
        if (!hasPermission) {
            throw new SecurityException("Từ chối truy cập! Bạn không có quyền xóa tệp tin này.");
        }

        // Soft delete file
        fileEntity.setIsDeleted(true);
        fileEntity.setStatus("DELETED");
        fileRepository.save(fileEntity);

        // Giải phóng dung lượng Quota của User sở hữu file
        User owner = fileEntity.getOwner();
        long newUsedStorage = Math.max(0, owner.getUsedStorage() - fileEntity.getSize());
        owner.setUsedStorage(newUsedStorage);
        userRepository.save(owner);

        // Lưu nhật ký hoạt động (Audit Log)
        auditLogRepository.save(AuditLog.builder()
                .user(currentUser)
                .action("DELETE_FILE")
                .resourceType("FILE")
                .resourceId(fileEntity.getId())
                .ipAddress(ipAddress)
                .build());
    }

    /**
     * Lấy danh sách lịch sử các phiên bản cũ của tệp tin.
     */
    @Transactional(readOnly = true)
    public List<FileVersionResponse> getFileVersions(Long fileId) {
        User currentUser = authService.getCurrentUser();

        // KIỂM TRA QUYỀN HẠN: Kiểm tra quyền xem file (FILE_READ)
        boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FILE", fileId, "FILE_READ");
        if (!hasPermission) {
            throw new SecurityException("Từ chối truy cập! Bạn không có quyền xem lịch sử phiên bản của tệp tin này.");
        }

        return fileVersionRepository.findByFileIdOrderByVersionDesc(fileId)
                .stream()
                .map(v -> FileVersionResponse.builder()
                        .id(v.getId())
                        .fileId(v.getFile().getId())
                        .version(v.getVersion())
                        .storagePath(v.getStoragePath())
                        .checksum(v.getChecksum())
                        .size(v.getSize())
                        .createdBy(v.getCreatedBy())
                        .createdAt(v.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Lấy toàn bộ tệp tin hoạt động trong một thư mục cụ thể.
     */
    @Transactional(readOnly = true)
    public List<FileResponse> getFilesByFolder(Long folderId) {
        User currentUser = authService.getCurrentUser();

        // KIỂM TRA QUYỀN HẠN: Chỉ áp dụng với thư mục vật lý (folderId > 0)
        if (folderId != null && folderId > 0) {
            boolean hasPermission = permissionService.checkPermission(currentUser.getId(), "FOLDER", folderId,
                    "FILE_READ");
            if (!hasPermission) {
                throw new SecurityException("Từ chối truy cập! Bạn không có quyền xem nội dung thư mục này.");
            }
        }

        if (folderId != null) {
            if (folderId > 0) {
                // A. Thư mục vật lý: Trả về tệp tin thuộc thư mục này mà người dùng sở hữu hoặc
                // được chia sẻ
                return fileRepository.findByFolderIdAndIsDeletedFalse(folderId)
                        .stream()
                        .filter(file -> file.getOwner().getId().equals(currentUser.getId())
                                || permissionService.checkPermission(currentUser.getId(), "FILE", file.getId(),
                                        "FILE_READ"))
                        .map(this::mapToResponse)
                        .collect(Collectors.toList());
            } else {
                // B. Thư mục ảo của người chia sẻ (folderId < 0, ID đại diện là -ownerId)
                Long sharerId = -folderId;
                List<Long> sharedFileIds = permissionService.getSharedResourceIds(currentUser.getId(), "FILE");
                if (sharedFileIds != null && !sharedFileIds.isEmpty()) {
                    List<FileEntity> sharedFiles = fileRepository.findAllById(sharedFileIds).stream()
                            .filter(file -> !file.getIsDeleted()
                                    && file.getOwner().getId().equals(sharerId)) // Chỉ lấy file thuộc sở hữu của người
                                                                                 // chia sẻ này
                            .toList();

                    // Lọc ra các file ở Root của người chia sẻ, hoặc các file ở thư mục con mà
                    // người dùng không có quyền truy cập thư mục cha
                    return sharedFiles.stream()
                            .filter(file -> {
                                if (file.getFolder() == null)
                                    return true;
                                return !permissionService.checkPermission(currentUser.getId(), "FOLDER",
                                        file.getFolder().getId(), "FILE_READ");
                            })
                            .map(this::mapToResponse)
                            .collect(Collectors.toList());
                }
                return new java.util.ArrayList<>();
            }
        } else {
            // C. Thư mục gốc Root (folderId == null)
            // Chỉ lấy các tệp tin ở Root thuộc sở hữu của chính User hiện tại
            List<FileEntity> rootFiles = fileRepository.findByFolderIdAndIsDeletedFalse(null);
            return rootFiles.stream()
                    .filter(file -> file.getOwner().getId().equals(currentUser.getId()))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
    }

    /**
     * Ánh xạ thực thể File sang DTO FileResponse.
     */
    public FileResponse mapToResponse(FileEntity file) {
        if (file == null)
            return null;
        return FileResponse.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .extension(file.getExtension())
                .mimeType(file.getMimeType())
                .size(file.getSize())
                .storagePath(file.getStoragePath())
                .checksum(file.getChecksum())
                .version(file.getVersion())
                .folderId(file.getFolder() != null ? file.getFolder().getId() : null)
                .ownerId(file.getOwner().getId())
                .ownerUsername(file.getOwner().getUsername())
                .status(file.getStatus())
                .isDeleted(file.getIsDeleted())
                .createdAt(file.getCreatedAt())
                .updatedAt(file.getUpdatedAt())
                .build();
    }

    /**
     * Hàm KIỂM TRA BẢO MẬT & VALIDATE TỆP TIN đầu vào.
     * Chống tải lên tệp tin độc hại nguy cơ Remote Code Execution (RCE) và Path
     * Traversal.
     */
    private void validateFileSecurity(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        // 1. Kiểm tra rỗng hoặc không có tên
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Không thể tải lên tệp tin không có tên!");
        }

        // 2. Chống tấn công Directory Traversal (ngăn chặn ký tự "..")
        if (originalFilename.contains("..")) {
            throw new SecurityException("Cảnh báo bảo mật: Tên tệp tin chứa ký tự không hợp lệ (path traversal)!");
        }

        // 3. Kiểm tra phần đuôi mở rộng bị cấm (Blacklist Extension)
        String extension = getFileExtension(originalFilename).toLowerCase();
        if (BLOCKED_EXTENSIONS.contains(extension)) {
            throw new SecurityException(
                    "Từ chối tải tệp! Định dạng file ." + extension + " bị cấm vì lý do an toàn bảo mật.");
        }

        // 4. Kiểm tra MIME Type thực tế của tệp tin để chống giả mạo đuôi mở rộng
        String mimeType = file.getContentType();
        if (mimeType != null) {
            String cleanMime = mimeType.toLowerCase().trim();
            if (BLOCKED_MIME_TYPES.contains(cleanMime)) {
                throw new SecurityException(
                        "Từ chối tải tệp! Định dạng MIME Type (" + mimeType + ") bị cấm vì lý do an toàn bảo mật.");
            }
        }
    }

    // Helper: Tính toán mã hash checksum MD5 (Tối ưu hóa tránh OutOfMemoryError bằng cách dùng Stream Chunk Buffer)
    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (InputStream is = file.getInputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, bytesRead);
                }
            }
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tính toán mã hash checksum: " + e.getMessage());
        }
    }

    // Helper: Lấy phần mở rộng của file
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    @Transactional
    public FileResponse uploadChunk(
            MultipartFile chunk,
            String uploadId,
            int chunkIndex,
            int totalChunks,
            String fileName,
            Long folderId,
            String ipAddress) {
        
        // 1. Tạo thư mục tạm lưu các chunk với đường dẫn tuyệt đối để tránh việc Tomcat tự phân giải sai thư mục tạm của nó
        java.nio.file.Path tempDirAbsolutePath = java.nio.file.Paths.get("temp-upload", "chunks", uploadId).toAbsolutePath().normalize();
        java.io.File tempDir = tempDirAbsolutePath.toFile();
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // 2. Lưu chunk hiện tại bằng đường dẫn tuyệt đối
        java.io.File chunkFile = tempDirAbsolutePath.resolve(String.valueOf(chunkIndex)).toFile();
        try {
            chunk.transferTo(chunkFile);
        } catch (Exception e) {
            throw new RuntimeException("Lỗi lưu trữ chunk thứ " + chunkIndex + ": " + e.getMessage(), e);
        }

        // 3. Kiểm tra xem tất cả các chunk đã được tải lên chưa
        boolean allUploaded = true;
        for (int i = 0; i < totalChunks; i++) {
            java.io.File f = tempDirAbsolutePath.resolve(String.valueOf(i)).toFile();
            if (!f.exists()) {
                allUploaded = false;
                break;
            }
        }

        if (!allUploaded) {
            // Chưa tải lên đủ, trả về null để Frontend biết chưa đến lúc ghép file
            return null;
        }

        // 4. Ghép toàn bộ các chunk lại theo thứ tự bằng đường dẫn tuyệt đối
        java.io.File mergedFile = java.nio.file.Paths.get("temp-upload", "chunks", uploadId + "_merged").toAbsolutePath().normalize().toFile();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(mergedFile);
             java.io.BufferedOutputStream bos = new java.io.BufferedOutputStream(fos)) {
            for (int i = 0; i < totalChunks; i++) {
                java.io.File f = tempDirAbsolutePath.resolve(String.valueOf(i)).toFile();
                try (java.io.FileInputStream fis = new java.io.FileInputStream(f);
                     java.io.BufferedInputStream bis = new java.io.BufferedInputStream(fis)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = bis.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi ghép các mảnh tệp tin: " + e.getMessage(), e);
        }

        // 5. Khởi tạo đối tượng Mock MultipartFile từ file đã ghép để tái sử dụng toàn bộ logic nghiệp vụ tiêu chuẩn
        MultipartFile mergedMultipartFile = new LocalMultipartFile(mergedFile, fileName, chunk.getContentType());

        try {
            // Tái sử dụng 100% logic kiểm tra quyền, hạn mức, RCE scan, trùng lặp checksum và ghi log
            return uploadFile(mergedMultipartFile, folderId, ipAddress);
        } finally {
            // Dọn dẹp tệp tin tạm và thư mục chunk
            try {
                for (int i = 0; i < totalChunks; i++) {
                    java.io.File f = tempDirAbsolutePath.resolve(String.valueOf(i)).toFile();
                    f.delete();
                }
                tempDir.delete();
                mergedFile.delete();
            } catch (Exception e) {
                // Bỏ qua lỗi dọn dẹp tạm
            }
        }
    }

    private static class LocalMultipartFile implements MultipartFile {
        private final java.io.File file;
        private final String name;
        private final String contentType;

        public LocalMultipartFile(java.io.File file, String name, String contentType) {
            this.file = file;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getOriginalFilename() {
            return name;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return file.length() == 0;
        }

        @Override
        public long getSize() {
            return file.length();
        }

        @Override
        public byte[] getBytes() throws java.io.IOException {
            return java.nio.file.Files.readAllBytes(file.toPath());
        }

        @Override
        public java.io.InputStream getInputStream() throws java.io.IOException {
            return new java.io.FileInputStream(file);
        }

        @Override
        public void transferTo(java.io.File dest) throws java.io.IOException, IllegalStateException {
            java.nio.file.Files.copy(file.toPath(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
