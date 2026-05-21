package com.buihien.fileserver.file;

import com.buihien.fileserver.folder.Folder;
import com.buihien.fileserver.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể quản lý siêu dữ liệu (Metadata) của các Tệp Tin (File) lưu trữ trên hệ thống.
 * Tệp tin vật lý thực tế được lưu trữ trên Object Storage (MinIO).
 * Ánh xạ tới bảng "files" trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    /**
     * Khóa chính duy nhất tự tăng của tệp tin (Auto Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tên hiển thị đầy đủ của tệp tin kèm phần mở rộng (ví dụ: "report.docx").
     * Ràng buộc: Không được để trống.
     */
    @Column(name = "file_name", nullable = false)
    private String fileName;

    /**
     * Phần mở rộng (Extension) của tệp tin được bóc tách từ tên tệp (ví dụ: "docx", "pdf", "png").
     */
    @Column(name = "extension")
    private String extension;

    /**
     * Định dạng MIME Type chuẩn của tệp tin để đảm bảo an ninh mạng và hiển thị phù hợp trên trình duyệt (ví dụ: "application/pdf").
     */
    @Column(name = "mime_type")
    private String mimeType;

    /**
     * Dung lượng của tệp tin thực tế tính bằng Bytes (được dùng để kiểm tra tính hạn mức Quota của User).
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "size", nullable = false)
    private Long size;

    /**
     * Đường dẫn vật lý của tệp tin thực tế được lưu trữ trên MinIO Bucket.
     * Cấu trúc thường gặp: "users/{userId}/{year}/{month}/{random_uuid_file_name}"
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    /**
     * Mã hash kiểm tra toàn vẹn dữ liệu (MD5 hoặc SHA-256) của tệp tin.
     * Giúp hệ thống phát hiện trùng lặp file (Deduplication) để tiết kiệm lưu trữ vật lý và tránh file hỏng.
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "checksum", nullable = false)
    private String checksum;

    /**
     * Phiên bản hiện hành của tệp tin. Số phiên bản tăng dần lên khi người dùng thực hiện tải ghi đè file.
     * Mặc định ban đầu khi tạo mới là phiên bản 1.
     */
    @Builder.Default
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * Thư mục cha chứa tệp tin này (Quan hệ Nhiều-Một).
     * Nếu bằng NULL, tệp tin này nằm trực tiếp ở thư mục gốc (Root).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    /**
     * Người dùng tải lên và sở hữu tệp tin này (Quan hệ Nhiều-Một).
     * Ràng buộc: Không được rỗng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    /**
     * Trạng thái hoạt động của tệp tin (ví dụ: "ACTIVE" - Hoạt động bình thường, "ARCHIVED" - Đã lưu trữ lâu dài).
     */
    @Column(name = "status")
    private String status;

    /**
     * Đánh dấu xóa mềm (Soft Delete) đối với tệp tin.
     * true: Đã bị xóa và di chuyển vào thùng rác (không hiện thị thông thường nhưng chưa bị xóa khỏi MinIO và DB).
     * false: Trạng thái tệp tin hoạt động bình thường.
     */
    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    /**
     * Thời điểm tệp tin được upload lên hệ thống.
     * Ràng buộc: Không được để trống và không được cập nhật lại sau khi tạo.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm tệp tin hoặc metadata của nó được cập nhật sửa đổi gần nhất.
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Thiết lập giá trị mặc định cho thời gian khởi tạo và trạng thái phiên bản ban đầu trước khi lưu DB.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.version == null) {
            this.version = 1;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    /**
     * Cập nhật thời gian chỉnh sửa mới nhất trước khi lưu thay đổi DB.
     */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
