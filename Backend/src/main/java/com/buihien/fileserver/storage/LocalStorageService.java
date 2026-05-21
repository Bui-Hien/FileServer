package com.buihien.fileserver.storage;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Triển khai lưu trữ tệp tin trên thư mục tạm của server để phát triển và test trước khi cấu hình MinIO.
 */
@Service
public class LocalStorageService implements StorageService {

    // Thư mục lưu trữ tệp tin tạm thời trên ổ đĩa server
    private final String uploadDir = "temp-upload";

    public LocalStorageService() {
        // Tự động tạo thư mục lưu trữ nếu chưa tồn tại
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public String upload(InputStream inputStream, String relativePath) {
        try {
            // Chuẩn hóa và làm sạch path để tránh tấn công Directory Traversal
            Path targetPath = Paths.get(uploadDir, relativePath).normalize();
            
            // Tạo các thư mục cha nếu chưa tồn tại (ví dụ: temp-upload/users/1/2026/05/)
            Files.createDirectories(targetPath.getParent());

            // Ghi luồng dữ liệu vào file vật lý
            try (FileOutputStream out = new FileOutputStream(targetPath.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // Trả về đường dẫn tương đối để lưu vào database
            return relativePath;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xảy ra khi lưu trữ tệp tin cục bộ trên server: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String relativePath) {
        try {
            Path targetPath = Paths.get(uploadDir, relativePath).normalize();
            File file = targetPath.toFile();
            if (!file.exists()) {
                throw new RuntimeException("Tệp tin không tồn tại trên máy chủ lưu trữ: " + relativePath);
            }
            return new FileInputStream(file);
        } catch (IOException e) {
            throw new RuntimeException("Lỗi xảy ra khi tải xuống tệp tin cục bộ từ server: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path targetPath = Paths.get(uploadDir, relativePath).normalize();
            File file = targetPath.toFile();
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xảy ra khi xóa tệp tin cục bộ từ server: " + e.getMessage(), e);
        }
    }
}
