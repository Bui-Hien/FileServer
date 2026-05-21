package com.buihien.fileserver.user;

import com.buihien.fileserver.role.Role;
import com.buihien.fileserver.role.RoleRepository;
import com.buihien.fileserver.user.dto.UserCreateRequest;
import com.buihien.fileserver.user.dto.UserResponse;
import com.buihien.fileserver.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    /**
     * Tạo mới người dùng trong hệ thống kèm theo gán các vai trò ban đầu.
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Tên đăng nhập đã tồn tại trong hệ thống: " + request.getUsername());
        }

        Set<Role> roles = new HashSet<>();
        if (request.getRoleCodes() != null) {
            for (String code : request.getRoleCodes()) {
                roleRepository.findByCode(code).ifPresent(roles::add);
            }
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword())) // BCrypt hashed
                .fullName(request.getFullName())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .usedStorage(0L)
                .maxStorage(request.getMaxStorage() != null ? request.getMaxStorage() : 10L * 1024L * 1024L * 1024L)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        return mapToResponse(savedUser);
    }

    /**
     * Cập nhật thông tin chi tiết của người dùng.
     */
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setStatus(request.getStatus());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getMaxStorage() != null) {
            user.setMaxStorage(request.getMaxStorage());
        }

        if (request.getRoleCodes() != null) {
            Set<Role> roles = new HashSet<>();
            for (String code : request.getRoleCodes()) {
                roleRepository.findByCode(code).ifPresent(roles::add);
            }
            user.setRoles(roles);
        }

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    /**
     * Lấy thông tin chi tiết một người dùng theo ID.
     */
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
        return mapToResponse(user);
    }

    /**
     * Lấy toàn bộ danh sách người dùng hoạt động trong hệ thống.
     */
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Gán trực tiếp danh sách vai trò cho một người dùng.
     */
    @Transactional
    public UserResponse assignRoles(Long id, Set<String> roleCodes) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));

        Set<Role> roles = new HashSet<>();
        for (String code : roleCodes) {
            roleRepository.findByCode(code).ifPresent(roles::add);
        }
        user.setRoles(roles);

        User updatedUser = userRepository.save(user);
        return mapToResponse(updatedUser);
    }

    /**
     * Ánh xạ thực thể User sang DTO UserResponse.
     */
    public UserResponse mapToResponse(User user) {
        if (user == null)
            return null;

        String formattedUsed = formatBytes(user.getUsedStorage());
        String formattedMax = formatBytes(user.getMaxStorage());
        String rolesStr = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(", "));

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .status(user.getStatus())
                .usedStorage(user.getUsedStorage())
                .maxStorage(user.getMaxStorage())
                .formattedUsedStorage(formattedUsed)
                .formattedMaxStorage(formattedMax)
                .roleNames(rolesStr)
                .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    private String formatBytes(Long bytes) {
        if (bytes == null || bytes == 0)
            return "0 Bytes";
        String[] units = new String[] { "Bytes", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        return String.format("%.2f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
