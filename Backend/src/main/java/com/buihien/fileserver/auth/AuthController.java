package com.buihien.fileserver.auth;

import com.buihien.fileserver.auth.dto.LoginRequest;
import com.buihien.fileserver.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * API thực hiện đăng nhập bảo mật bằng mật khẩu, trả về cặp mã Access/Refresh Token.
     */
    @PostMapping("/login")
    public ResponseEntity<com.buihien.fileserver.auth.dto.TokenResponse> login(@RequestBody com.buihien.fileserver.auth.dto.LoginRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        com.buihien.fileserver.auth.dto.TokenResponse response = authService.authenticate(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API làm mới Access Token bằng Refresh Token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<com.buihien.fileserver.auth.dto.TokenResponse> refresh(@RequestBody com.buihien.fileserver.auth.dto.RefreshTokenRequest request) {
        if (request.getRefreshToken() == null || request.getRefreshToken().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        com.buihien.fileserver.auth.dto.TokenResponse response = authService.refresh(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Lấy thông tin người dùng đang đăng nhập hiện hành.
     */
    @GetMapping("/me")
    public ResponseEntity<User> getMe() {
        User user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
}
