package com.buihien.fileserver.audit;

import com.buihien.fileserver.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể quản lý Nhật Ký Hệ Thống (Audit Log) ghi nhận toàn bộ lịch sử thao tác dữ liệu.
 * Giúp người quản trị giám sát, kiểm tra vết, phát hiện hành vi thất thoát dữ liệu hoặc phá hoại hệ thống.
 * Ánh xạ tới bảng "audit_logs" trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    /**
     * Khóa chính duy nhất tự tăng của bản ghi nhật ký (Auto Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Người dùng đã thực hiện hành động này (Quan hệ Nhiều-Một).
     * Nếu bằng NULL tức là hành động được thực hiện bởi khách ẩn danh (Anonymous) hoặc hệ thống tự động.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * Tên hành động nghiệp vụ chi tiết (ví dụ: "UPLOAD_FILE", "DOWNLOAD_FILE", "DELETE_FILE", "CREATE_FOLDER", "SHARE_FILE").
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "action", nullable = false)
    private String action;

    /**
     * Loại tài nguyên chịu sự tác động trực tiếp của hành động (ví dụ: "FILE", "FOLDER").
     */
    @Column(name = "resource_type")
    private String resourceType;

    /**
     * ID định danh của tài nguyên chịu tác động (ID của File hoặc Folder tương ứng).
     */
    @Column(name = "resource_id")
    private Long resourceId;

    /**
     * Địa chỉ IP của máy khách (Client IP Address) nơi gửi yêu cầu API thực hiện hành động.
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /**
     * Thời điểm chính xác hành động nghiệp vụ này diễn ra.
     * Ràng buộc: Không được rỗng và không thể thay đổi sau khi ghi nhận.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Tự động gán dấu thời gian xảy ra hành động trước khi lưu dữ liệu nhật ký vào cơ sở dữ liệu.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
