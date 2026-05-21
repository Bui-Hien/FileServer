package com.buihien.fileserver.folder;

import com.buihien.fileserver.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể quản lý các Thư Mục (Folder) trong cấu trúc cây phân cấp của File Server.
 * Ánh xạ tới bảng "folders" trong cơ sở dữ liệu.
 * Sử dụng cơ chế Materialized Path giúp tối ưu hóa hiệu năng truy vấn thư mục con.
 */
@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    /**
     * Khóa chính duy nhất tự tăng của thư mục (Auto Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên hiển thị của thư mục.
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Liên kết tự tham chiếu tới Thư mục cha (Self-Referencing Parent).
     * Quan hệ Nhiều-Một (Many-to-One). Nếu bằng NULL thì thư mục này nằm ở thư mục gốc (Root).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    /**
     * Đường dẫn định danh đầy đủ tính từ thư mục gốc phục vụ Materialized Path (ví dụ: "/root/documents/projects/java").
     * Giúp tìm kiếm toàn bộ thư mục con nhanh chóng bằng một câu truy vấn duy nhất qua toán tử LIKE mà không cần đệ quy.
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "path", nullable = false)
    private String path;

    /**
     * Người dùng sở hữu và có toàn quyền tối cao đối với thư mục này (Quan hệ Nhiều-Một).
     * Ràng buộc: Không được rỗng (mọi thư mục phải thuộc sở hữu của một tài khoản).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Đánh dấu xóa mềm (Soft Delete).
     * true: Đang nằm trong thùng rác và bị ẩn khỏi giao diện quản lý thông thường.
     * false: Trạng thái hiển thị hoạt động bình thường.
     */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * Thời điểm thư mục được khởi tạo trong hệ thống.
     * Ràng buộc: Không được rỗng và không thể chỉnh sửa lại sau khi tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm thư mục được cập nhật đổi tên hoặc di chuyển gần nhất.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Phương thức tự động gọi trước khi dữ liệu được thêm mới vào DB (PrePersist).
     * Thiết lập giá trị mặc định cho thời gian tạo và cờ xóa mềm.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    /**
     * Phương thức tự động gọi trước khi cập nhật dữ liệu trong DB (PreUpdate).
     * Cập nhật thời gian chỉnh sửa mới nhất.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
