package com.buihien.fileserver.file;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Thực thể quản lý Lịch Sử Phiên Bản cũ của Tệp Tin (File Versioning).
 * Khi tệp tin chính bị tải đè, metadata của phiên bản cũ sẽ được lưu lại tại
 * bảng này
 * để người dùng có thể tải về các bản cũ hoặc khôi phục dữ liệu lịch sử.
 * Ánh xạ tới bảng "file_versions" trong cơ sở dữ liệu.
 */
@Entity
@Table(name = "file_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileVersion {

    /**
     * Khóa chính duy nhất tự tăng của bản ghi phiên bản (Auto Increment).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tệp tin gốc mà phiên bản lịch sử này trực thuộc (Quan hệ Nhiều-Một).
     * Ràng buộc: Không được rỗng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    /**
     * Số phiên bản cụ thể (ví dụ: phiên bản 1, phiên bản 2).
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "version", nullable = false)
    private Integer version;

    /**
     * Đường dẫn lưu trữ vật lý của phiên bản lịch sử này trên hệ thống lưu trữ
     * MinIO.
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "storage_path", nullable = false)
    private String storagePath;

    /**
     * Mã hash kiểm tra toàn vẹn (checksum MD5/SHA-256) của riêng phiên bản lịch sử
     * này.
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "checksum", nullable = false)
    private String checksum;

    /**
     * Dung lượng của phiên bản lịch sử này tính bằng Bytes.
     * Ràng buộc: Không được rỗng.
     */
    @Column(name = "size", nullable = false)
    private Long size;

    /**
     * ID của người dùng (User ID) đã thực hiện tải lên và tạo ra phiên bản này.
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * Thời điểm phiên bản lịch sử này được tạo ra do hành động tải đè tệp tin gốc.
     * Ràng buộc: Không được rỗng và không thể sửa lại sau khi ghi.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thiết lập tự động thời gian lưu trữ phiên bản trước khi ghi nhận vào cơ sở dữ
     * liệu.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
