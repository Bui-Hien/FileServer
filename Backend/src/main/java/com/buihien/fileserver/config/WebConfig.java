package com.buihien.fileserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Web MVC cho phép chia sẻ tài nguyên nguồn gốc chéo (CORS).
 * Giúp Frontend (chạy ở cổng 5173 hoặc bất kỳ nguồn nào khác) có thể kết nối
 * mượt mà tới các cổng API Backend 8080 mà không bị trình duyệt chặn.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*") // Cho phép toàn bộ các Domain/Origin gọi API
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH") // Cho phép tất cả các HTTP method
                .allowedHeaders("*") // Cho phép toàn bộ Request Header
                .exposedHeaders("Authorization", "Content-Disposition") // Tiết lộ Header tải file
                .allowCredentials(true) // Cho phép gửi Cookies / JWT credentials nếu cần
                .maxAge(3600); // Lưu trữ cấu hình Preflight trong cache trình duyệt 1 tiếng
    }
}
