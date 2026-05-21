package com.buihien.fileserver.user;

import com.buihien.fileserver.role.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Thực thể quản lý thông tin Người Dùng (User) và hạn mức dung lượng (Quota) trong hệ thống.
 * Ánh xạ tới bảng "users" trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Khóa chính của bảng người dùng, tự động tăng (Auto Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên đăng nhập của người dùng.
     * Ràng buộc: Không được phép rỗng (nullable = false) và là duy nhất (unique = true).
     */
    @Column(name = "username", nullable = false, unique = true)
    private String username;

    /**
     * Địa chỉ Email của người dùng phục vụ cho liên hệ và xác thực.
     * Ràng buộc: Không được phép rỗng và là duy nhất.
     */
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    /**
     * Mật khẩu đăng nhập của tài khoản (luôn luôn được băm/mã hóa bảo mật trước khi lưu).
     * Ràng buộc: Không được để rỗng.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Họ và tên đầy đủ của người dùng.
     */
    @Column(name = "full_name")
    private String fullName;

    /**
     * Trạng thái tài khoản người dùng (ví dụ: ACTIVE - Hoạt động, INACTIVE - Tạm khóa).
     */
    @Column(name = "status")
    private String status;

    /**
     * Dung lượng lưu trữ thực tế mà người dùng đã sử dụng (được tính bằng đơn vị Bytes).
     * Mặc định khi khởi tạo tài khoản mới là 0 Bytes.
     */
    @Builder.Default
    @Column(name = "used_storage")
    private Long usedStorage = 0L;

    /**
     * Hạn mức dung lượng tối đa (Quota) mà người dùng được phép upload lên hệ thống (đơn vị Bytes).
     * Mặc định là 10GB = 10 * 1024 * 1024 * 1024 Bytes.
     */
    @Builder.Default
    @Column(name = "max_storage")
    private Long maxStorage = 10L * 1024L * 1024L * 1024L;

    /**
     * Thời điểm tài khoản được khởi tạo trên hệ thống.
     * Ràng buộc: Không được phép rỗng và không thể chỉnh sửa sau khi tạo (updatable = false).
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm thông tin tài khoản được cập nhật lần cuối cùng.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Danh sách các vai trò (Roles) được gán cho người dùng này (Quan hệ Nhiều-Nhiều).
     * Sử dụng FetchType.LAZY để tối ưu hóa hiệu năng, dữ liệu chỉ được tải khi thực sự cần.
     * Ánh xạ thông qua bảng trung gian "user_roles".
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * Phương thức tự động gọi trước khi dữ liệu được thêm mới vào DB (PrePersist).
     * Dùng để thiết lập mặc định thời gian khởi tạo và dung lượng lưu trữ ban đầu.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.usedStorage == null) {
            this.usedStorage = 0L;
        }
        if (this.maxStorage == null) {
            this.maxStorage = 10L * 1024L * 1024L * 1024L;
        }
    }

    /**
     * Phương thức tự động gọi trước khi dữ liệu được cập nhật trong DB (PreUpdate).
     * Dùng để cập nhật lại dấu thời gian chỉnh sửa mới nhất.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
