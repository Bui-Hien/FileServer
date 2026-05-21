package com.buihien.fileserver.user;

import com.buihien.fileserver.user.dto.UserCreateRequest;
import com.buihien.fileserver.user.dto.UserResponse;
import com.buihien.fileserver.user.dto.UserUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * API tạo mới người dùng.
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@RequestBody UserCreateRequest request) {
        if (request.getUsername() == null || request.getEmail() == null || request.getPassword() == null) {
            return ResponseEntity.badRequest().build();
        }
        UserResponse response = userService.createUser(request);
        return ResponseEntity.ok(response);
    }

    /**
     * API cập nhật thông tin người dùng.
     */
    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UserUpdateRequest request) {
        if (request.getEmail() == null) {
            return ResponseEntity.badRequest().build();
        }
        UserResponse response = userService.updateUser(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * API lấy thông tin chi tiết của người dùng theo ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        UserResponse response = userService.getUserById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * API lấy danh sách toàn bộ người dùng trong hệ thống.
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    /**
     * API gán các vai trò (roles) trực tiếp cho người dùng.
     */
    @PostMapping("/{id}/roles")
    public ResponseEntity<UserResponse> assignRoles(
            @PathVariable Long id,
            @RequestBody Set<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        UserResponse response = userService.assignRoles(id, roleCodes);
        return ResponseEntity.ok(response);
    }
}
