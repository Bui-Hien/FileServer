package com.buihien.fileserver.file;

import com.buihien.fileserver.file.dto.FileResponse;
import com.buihien.fileserver.file.dto.FileVersionResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileRepository fileRepository;
    private final FileVersionRepository fileVersionRepository;

    /**
     * API tải lên một tệp tin.
     * Hỗ trợ lưu trữ trực tiếp vào thư mục gốc hoặc thư mục đích chỉ định.
     */
    @PostMapping("/upload")
    public ResponseEntity<FileResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            HttpServletRequest request) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String ipAddress = getClientIp(request);
        FileResponse response = fileService.uploadFile(file, folderId, ipAddress);
        return ResponseEntity.ok(response);
    }

    /**
     * API tải lên tệp tin phân mảnh (Chunked Upload).
     * Phục vụ cho việc tải lên tệp tin lớn mà không gây tốn tài nguyên bộ nhớ JVM (tránh OutOfMemoryError).
     */
    @PostMapping("/upload-chunk")
    public ResponseEntity<FileResponse> uploadChunk(
            @RequestParam("file") MultipartFile chunk,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("chunkIndex") int chunkIndex,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("fileName") String fileName,
            @RequestParam(value = "folderId", required = false) Long folderId,
            HttpServletRequest request) {

        if (chunk.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String ipAddress = getClientIp(request);
        FileResponse response = fileService.uploadChunk(
                chunk,
                uploadId,
                chunkIndex,
                totalChunks,
                fileName,
                folderId,
                ipAddress
        );

        if (response == null) {
            // Trả về HTTP 202 Accepted để báo hiệu Chunk đã nhận thành công nhưng tệp chưa được ghép hoàn chỉnh
            return ResponseEntity.accepted().build();
        }

        return ResponseEntity.ok(response);
    }

    /**
     * API tải xuống một tệp tin với luồng dữ liệu thô (Raw stream) và các tiêu đề Header phù hợp.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<InputStreamResource> downloadFile(
            @PathVariable Long id,
            HttpServletRequest request) {

        FileEntity fileEntity = fileRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tệp tin hoặc tệp tin đã bị xóa với ID: " + id));

        String ipAddress = getClientIp(request);
        InputStream inputStream = fileService.downloadFile(id, ipAddress);

        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntity.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType() != null ? fileEntity.getMimeType() : "application/octet-stream"))
                .contentLength(fileEntity.getSize())
                .body(resource);
    }

    /**
     * API xóa mềm tệp tin.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable Long id, HttpServletRequest request) {
        String ipAddress = getClientIp(request);
        fileService.deleteFile(id, ipAddress);
        return ResponseEntity.noContent().build();
    }

    /**
     * API lấy lịch sử các phiên bản cũ của tệp tin.
     */
    @GetMapping("/{id}/versions")
    public ResponseEntity<List<FileVersionResponse>> getFileVersions(@PathVariable Long id) {
        List<FileVersionResponse> versions = fileService.getFileVersions(id);
        return ResponseEntity.ok(versions);
    }

    /**
     * API tải xuống một phiên bản lịch sử cụ thể của tệp tin.
     */
    @GetMapping("/versions/{versionId}/download")
    public ResponseEntity<InputStreamResource> downloadFileVersion(
            @PathVariable Long versionId,
            HttpServletRequest request) {

        FileVersion fileVersion = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiên bản tệp tin với ID: " + versionId));

        FileEntity fileEntity = fileVersion.getFile();
        String ipAddress = getClientIp(request);
        InputStream inputStream = fileService.downloadFileVersion(versionId, ipAddress);

        InputStreamResource resource = new InputStreamResource(inputStream);

        // Đặt tên file tải xuống theo định dạng: [Tên gốc]_v[Số phiên bản].[Đuôi mở rộng]
        String originalName = fileEntity.getFileName();
        String baseName = originalName.contains(".") ? originalName.substring(0, originalName.lastIndexOf(".")) : originalName;
        String ext = fileEntity.getExtension() != null && !fileEntity.getExtension().isEmpty() ? "." + fileEntity.getExtension() : "";
        String downloadName = baseName + "_v" + fileVersion.getVersion() + ext;

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadName + "\"")
                .contentType(MediaType.parseMediaType(fileEntity.getMimeType() != null ? fileEntity.getMimeType() : "application/octet-stream"))
                .contentLength(fileVersion.getSize())
                .body(resource);
    }

    /**
     * API lấy danh sách toàn bộ tệp tin trong một thư mục chỉ định.
     */
    @GetMapping
    public ResponseEntity<List<FileResponse>> getFilesByFolder(@RequestParam(value = "folderId", required = false) Long folderId) {
        List<FileResponse> files = fileService.getFilesByFolder(folderId);
        return ResponseEntity.ok(files);
    }

    // Helper: Trích xuất địa chỉ IP thực tế của Client gửi request
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
