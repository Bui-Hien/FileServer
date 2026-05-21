package com.buihien.fileserver.auth;

/**
 * Lớp static lưu giữ thông tin người dùng hiện tại đang đăng nhập để phục vụ
 * test và development.
 * Không sử dụng cơ chế bảo mật phức tạp như Spring Security / JWT để đơn giản
 * hóa quá trình chạy thử.
 */
public class LoginContext {

    /**
     * Tên đăng nhập của người dùng hiện tại.
     * Mặc định khởi tạo là "admin" để chạy thử thuận tiện.
     */
    public static String CURRENT_USER = "admin";
}
