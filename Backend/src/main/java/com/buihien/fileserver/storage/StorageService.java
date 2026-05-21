package com.buihien.fileserver.storage;

import java.io.InputStream;

public interface StorageService {

    /**
     * Tải tệp tin lên hệ thống lưu trữ.
     *
     * @param inputStream Luồng dữ liệu của tệp tin.
     * @param path Đường dẫn đích trên hệ thống lưu trữ.
     * @return Đường dẫn lưu trữ thực tế.
     */
    String upload(InputStream inputStream, String path);

    /**
     * Tải tệp tin xuống từ hệ thống lưu trữ.
     *
     * @param path Đường dẫn của tệp tin trên hệ thống lưu trữ.
     * @return Luồng dữ liệu của tệp tin.
     */
    InputStream download(String path);

    /**
     * Xóa tệp tin khỏi hệ thống lưu trữ.
     *
     * @param path Đường dẫn của tệp tin trên hệ thống lưu trữ.
     */
    void delete(String path);
}
