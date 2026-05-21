package com.buihien.fileserver.auth;

import com.buihien.fileserver.user.User;
import com.buihien.fileserver.user.UserRepository;
import com.buihien.fileserver.role.Role;
import com.buihien.fileserver.role.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.buihien.fileserver.config.JwtService jwtService;

    /**
     * Xác thực thông tin đăng nhập bằng mật khẩu và sinh cặp mã Access/Refresh
     * Token.
     */
    @Transactional(readOnly = true)
    public com.buihien.fileserver.auth.dto.TokenResponse authenticate(
            com.buihien.fileserver.auth.dto.LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Tên đăng nhập hoặc mật khẩu không chính xác");
        }

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        return generateTokensForUser(user);
    }

    /**
     * Làm mới Access Token sử dụng Refresh Token còn hiệu lực.
     */
    @Transactional(readOnly = true)
    public com.buihien.fileserver.auth.dto.TokenResponse refresh(
            com.buihien.fileserver.auth.dto.RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (jwtService.isTokenExpired(token)) {
            throw new RuntimeException("Refresh Token đã hết hạn");
        }

        String tokenType = jwtService.getTokenType(token);
        if (!"REFRESH".equals(tokenType)) {
            throw new RuntimeException("Mã token không hợp lệ");
        }

        String username = jwtService.extractUsername(token);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông tin người dùng"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new RuntimeException("Tài khoản đã bị vô hiệu hóa");
        }

        return generateTokensForUser(user);
    }

    private com.buihien.fileserver.auth.dto.TokenResponse generateTokensForUser(User user) {
        java.util.List<String> roles = user.getRoles().stream()
                .map(Role::getCode)
                .toList();

        java.util.List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(com.buihien.fileserver.permission.Permission::getCode)
                .distinct()
                .toList();

        String accessToken = jwtService.generateAccessToken(user.getUsername(), roles, permissions);
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        return com.buihien.fileserver.auth.dto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(user.getUsername())
                .roles(new java.util.HashSet<>(roles))
                .permissions(new java.util.HashSet<>(permissions))
                .build();
    }

    /**
     * Thực hiện đăng nhập bằng cách lưu username vào biến static LoginContext.
     * Đồng thời tự động khởi tạo User trong cơ sở dữ liệu nếu chưa tồn tại.
     */
    @Transactional
    public User login(String username) {
        LoginContext.CURRENT_USER = username;

        // Tìm kiếm người dùng trong DB
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            return userOpt.get();
        }

        // Đảm bảo có sẵn Role tương ứng trong DB
        Role defaultRole = roleRepository.findByCode("ADMIN")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .code("ADMIN")
                        .name("Quản trị viên")
                        .build()));

        // Tự động tạo người dùng mẫu nếu chưa tồn tại
        User newUser = User.builder()
                .username(username)
                .email(username + "@example.com")
                .password(passwordEncoder.encode("123456")) // Mật khẩu mẫu
                .fullName("User " + username)
                .status("ACTIVE")
                .usedStorage(0L)
                .maxStorage(10L * 1024L * 1024L * 1024L) // 10 GB
                .roles(Set.of(defaultRole))
                .build();

        return userRepository.save(newUser);
    }

    /**
     * Lấy thực thể Người Dùng hiện tại đang đăng nhập từ cơ sở dữ liệu.
     */
    @Transactional
    public User getCurrentUser() {
        String username = LoginContext.CURRENT_USER;
        if (username == null) {
            username = "admin"; // Default fallback
        }

        final String finalUsername = username;
        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    // Tự động khởi tạo user nếu chưa có
                    return login(finalUsername);
                });
    }
}
