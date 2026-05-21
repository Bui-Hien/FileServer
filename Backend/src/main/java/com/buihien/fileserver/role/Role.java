package com.buihien.fileserver.role;

import com.buihien.fileserver.permission.Permission;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Thực thể quản lý các Vai Trò (Role) của người dùng trong hệ thống phục vụ
 * phân quyền tĩnh RBAC.
 * Ánh xạ tới bảng "roles" trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    /**
     * Khóa chính duy nhất tự tăng của vai trò (Auto Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã code định danh viết hoa duy nhất cho vai trò (ví dụ: "ADMIN", "EDITOR",
     * "VIEWER").
     * Ràng buộc: Không được rỗng và không được phép trùng lặp.
     */
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    /**
     * Tên hiển thị thân thiện của vai trò (ví dụ: "Quản trị viên", "Biên tập viên",
     * "Người xem").
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Danh sách các quyền hạn chi tiết (Permissions) liên kết với vai trò này (Quan
     * hệ Nhiều-Nhiều).
     * Được ánh xạ qua bảng liên kết trung gian "role_permissions".
     * Sử dụng cơ chế Lazy Loading để tối ưu truy vấn dữ liệu.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_permissions", joinColumns = @JoinColumn(name = "role_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
