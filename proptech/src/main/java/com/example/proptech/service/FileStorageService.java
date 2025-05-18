package com.example.proptech.service;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.springframework.core.io.Resource;


public interface FileStorageService {
    void init(); // Tạo thư mục nếu chưa có
    String store(MultipartFile file, String subFolder); // Trả về tên file đã lưu (hoặc đường dẫn tương đối)
    // Stream<Path> loadAll(String subFolder); // (Tùy chọn)
    // Path load(String subFolder, String filename); // (Tùy chọn)
    Resource loadAsResource(String subFolder, String filename); // (Tùy chọn)
    void delete(String subFolder, String filename);
    // void deleteAll(String subFolder); // (Tùy chọn)
}