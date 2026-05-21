package com.buihien.fileserver.acl;

import com.buihien.fileserver.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể quản lý Danh Sách Quyền Hạn Truy Cập Chi Tiết (ACL - Access Control List) trên từng Tài nguyên.
 * Cho phép phân quyền động, chi tiết tới cấp độ từng tệp tin hoặc thư mục cụ thể cho từng người dùng,
 * vượt lên trên cơ chế phân quyền theo vai trò tĩnh (RBAC).
 * Ánh xạ tới bảng "resource_permissions" trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "resource_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResourcePermission {

    /**
     * Khóa chính duy nhất tự tăng của bản ghi phân quyền (Auto Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Loại tài nguyên cụ thể đang được phân quyền (ví dụ: "FILE" hoặc "FOLDER").
     * Ràng buộc: Không được để trống.
     */
    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    /**
     * ID định danh duy nhất của tài nguyên (ID của FileEntity hoặc Folder tương ứng).
     * Ràng buộc: Không được để trống.
     */
    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    /**
     * Người dùng được gán quyền hạn cụ thể này đối với tài nguyên nói trên (Quan hệ Nhiều-Một).
     * Ràng buộc: Không được để trống.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private User user;

    /**
     * Mã quyền hạn hành động cụ thể được gán (ví dụ: "FILE_READ", "FILE_WRITE", "FILE_DELETE", "FOLDER_DELETE").
     * Ràng buộc: Không được để trống.
     */
    @Column(name = "permission_code", nullable = false)
    private String permissionCode;

    /**
     * Xác định xem quyền này là "Cho phép" (Allow = true) hay "Chặn trực tiếp" (Deny = false).
     * Mặc định là cho phép (true). Quyền chặn trực tiếp (Deny) thường có độ ưu tiên cao nhất khi kiểm tra.
     * Ràng buộc: Không được để trống.
     */
    @Builder.Default
    @Column(name = "allow", nullable = false)
    private Boolean allow = true;

    /**
     * ID của người dùng (User ID) là người quản trị hoặc chủ sở hữu tài nguyên đã thiết lập quyền này.
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * Thời điểm bản ghi cấp quyền này được khởi tạo trên hệ thống.
     * Ràng buộc: Không được rỗng và không thể sửa sau khi lưu.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thiết lập tự động thời gian tạo bản ghi quyền trước khi lưu cơ sở dữ liệu.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.allow == null) {
            this.allow = true;
        }
    }
}
