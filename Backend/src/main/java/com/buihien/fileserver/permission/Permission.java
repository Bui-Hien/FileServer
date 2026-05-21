package com.buihien.fileserver.permission;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Thực thể quản lý các Quyền Hạn chi tiết (Permission) đối với các hành động
 * trên tài nguyên.
 * Ánh xạ tới bảng "permissions" trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    /**
     * Khóa chính duy nhất tự tăng của quyền hạn (Auto Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã code định danh viết hoa duy nhất cho quyền hạn (
     * ví dụ: "FILE_READ", "FILE_WRITE", "FILE_DELETE", "FOLDER_CREATE",
     * "FOLDER_DELETE", "FOLDER_READ").
     * Ràng buộc: Không được rỗng và không trùng lặp.
     */
    @Column(name = "code", nullable = false, unique = true)
    private String code;

    /**
     * Mô tả chi tiết tên quyền hiển thị bằng tiếng Việt thân thiện (ví dụ: "Quyền
     * đọc tệp tin", "Quyền tạo thư mục").
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "name", nullable = false)
    private String name;
}
