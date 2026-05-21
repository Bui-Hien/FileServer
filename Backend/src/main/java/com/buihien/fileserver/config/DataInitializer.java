package com.buihien.fileserver.config;

import com.buihien.fileserver.permission.Permission;
import com.buihien.fileserver.permission.PermissionRepository;
import com.buihien.fileserver.role.Role;
import com.buihien.fileserver.role.RoleRepository;
import com.buihien.fileserver.user.User;
import com.buihien.fileserver.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

/**
 * Lớp khởi tạo dữ liệu mẫu (Data Seeding) tự động chạy khi máy chủ khởi động
 * thành công.
 * Giúp thiết lập sẵn danh mục Quyền hạn (Permissions), Vai trò (Roles), liên
 * kết RBAC và tài khoản người dùng mặc định.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("===> Bắt đầu quá trình kiểm tra và khởi tạo dữ liệu mặc định (Data Seeding)...");

        // 1. KHỞI TẠO QUYỀN HẠN MẶC ĐỊNH (Permissions)
        Permission fileRead = ensurePermission("FILE_READ", "Đọc và tải xuống tệp tin");
        Permission fileWrite = ensurePermission("FILE_WRITE", "Tải lên và cập nhật phiên bản tệp tin");
        Permission fileDelete = ensurePermission("FILE_DELETE", "Xóa mềm tệp tin");
        Permission fileShare = ensurePermission("FILE_SHARE", "Chia sẻ tệp tin cho cá nhân/mọi người");
        Permission folderCreate = ensurePermission("FOLDER_CREATE", "Tạo thư mục con");
        Permission folderDelete = ensurePermission("FOLDER_DELETE", "Xóa thư mục");
        Permission userManage = ensurePermission("USER_MANAGE", "Quản lý người dùng");

        // 2. KHỞI TẠO VAI TRÒ MẶC ĐỊNH VÀ GÁN QUYỀN (Roles & RBAC)

        // Vai trò ADMIN (Toàn quyền)
        Role adminRole = ensureRole("ADMIN", "Quản trị viên tối cao", Set.of(
                fileRead, fileWrite, fileDelete, fileShare, folderCreate, folderDelete, userManage));

        // Vai trò EDITOR (Đọc, ghi, chia sẻ, tạo thư mục)
        Role editorRole = ensureRole("EDITOR", "Biên tập viên tài nguyên", Set.of(
                fileRead, fileWrite, fileShare, folderCreate));

        // Vai trò VIEWER (Chỉ được phép đọc file)
        Role viewerRole = ensureRole("VIEWER", "Người chỉ xem", Set.of(
                fileRead));

        // 3. KHỞI TẠO TÀI KHOẢN NGƯỜI DÙNG MẪU (Default Users)

        // Tài khoản admin: Quota 100 GB
        ensureUser("admin", "admin@fileserver.com", "123456", "Super Administrator", 100L * 1024L * 1024L * 1024L,
                Set.of(adminRole));

        // Tài khoản editor1: Quota 20 GB
        ensureUser("editor1", "editor1@fileserver.com", "123456", "Editor Staff 01", 20L * 1024L * 1024L * 1024L,
                Set.of(editorRole));

        // Tài khoản viewer1: Quota 5 GB
        ensureUser("viewer1", "viewer1@fileserver.com", "123456", "Viewer Client 01", 5L * 1024L * 1024L * 1024L,
                Set.of(viewerRole));

        log.info("===> Hoàn thành quá trình khởi tạo dữ liệu mẫu thành công! Hệ thống sẵn sàng hoạt động.");
    }

    // Helper: Đảm bảo Permission tồn tại trong DB, tránh tạo trùng lặp
    private Permission ensurePermission(String code, String name) {
        Optional<Permission> permOpt = permissionRepository.findByCode(code);
        if (permOpt.isPresent()) {
            return permOpt.get();
        }
        Permission newPerm = Permission.builder()
                .code(code)
                .name(name)
                .build();
        Permission saved = permissionRepository.save(newPerm);
        log.info("   -> Đã khởi tạo Quyền hạn: {} ({})", code, name);
        return saved;
    }

    // Helper: Đảm bảo Role tồn tại trong DB và cập nhật chính xác danh sách Quyền
    // hạn (RBAC)
    private Role ensureRole(String code, String name, Set<Permission> permissions) {
        Optional<Role> roleOpt = roleRepository.findByCode(code);
        Role role;
        Set<Permission> mutablePermissions = new java.util.HashSet<>(permissions);
        if (roleOpt.isPresent()) {
            role = roleOpt.get();
            // Cập nhật lại danh mục quyền hạn nếu có thay đổi
            role.setPermissions(mutablePermissions);
        } else {
            role = Role.builder()
                    .code(code)
                    .name(name)
                    .permissions(mutablePermissions)
                    .build();
        }
        Role saved = roleRepository.save(role);
        log.info("   -> Đã cấu hình Vai trò: {} (gán {} quyền)", code, permissions.size());
        return saved;
    }

    // Helper: Đảm bảo tài khoản User tồn tại trong DB và cấu hình các vai trò +
    // quota dung lượng tương ứng
    private void ensureUser(String username, String email, String password, String fullName, Long maxStorage,
            Set<Role> roles) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        Set<Role> mutableRoles = new java.util.HashSet<>(roles);
        if (userOpt.isPresent()) {
            User existingUser = userOpt.get();
            // Cập nhật vai trò mới nhất
            existingUser.setRoles(mutableRoles);
            existingUser.setMaxStorage(maxStorage);
            userRepository.save(existingUser);
            return;
        }

        User newUser = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password)) // Mã hóa BCrypt khi lưu
                .fullName(fullName)
                .status("ACTIVE")
                .usedStorage(0L)
                .maxStorage(maxStorage)
                .roles(mutableRoles)
                .build();
        userRepository.save(newUser);
        log.info("   -> Đã khởi tạo người dùng: {} | Quota: {} GB | Vai trò: {}",
                username, maxStorage / (1024L * 1024L * 1024L), roles.stream().map(Role::getCode).toList());
    }
}
