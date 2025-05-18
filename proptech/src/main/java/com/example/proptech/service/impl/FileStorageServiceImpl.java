package com.example.proptech.service.impl;

import com.example.proptech.exception.BadRequestException;
import com.example.proptech.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct; // Sử dụng PostConstruct
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${proptech.app.upload.base-dir:upload-dir}") // Cấu hình trong application.properties
    private String uploadBaseDir;

    private Path rootLocation;

    @Override
    @PostConstruct // Được gọi sau khi bean được khởi tạo
    public void init() {
        this.rootLocation = Paths.get(uploadBaseDir);
        try {
            Files.createDirectories(rootLocation);
        } catch (IOException e) {
            throw new BadRequestException("Could not initialize storage location", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subFolder) {
        if (file.isEmpty()) {
            throw new BadRequestException("Failed to store empty file.");
        }
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + extension;

        try {
            Path targetFolder = this.rootLocation.resolve(subFolder);
            Files.createDirectories(targetFolder); // Tạo thư mục con nếu chưa có

            Path destinationFile = targetFolder.resolve(storedFilename)
                    .normalize().toAbsolutePath();

            if (!destinationFile.getParent().equals(targetFolder.toAbsolutePath())) {
                throw new BadRequestException("Cannot store file outside current directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
            // Trả về đường dẫn tương đối để lưu vào DB, ví dụ: "listings/uuid.jpg"
            return Paths.get(subFolder).resolve(storedFilename).toString().replace("\\", "/");
        } catch (IOException e) {
            throw new BadRequestException("Failed to store file " + originalFilename, e);
        }
    }

    @Override
    public Resource loadAsResource(String subFolder, String filename) {
        try {
            Path file = rootLocation.resolve(subFolder).resolve(filename);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new BadRequestException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new BadRequestException("Could not read file: " + filename, e);
        }
    }

    @Override
    public void delete(String subFolder, String filename) {
        try {
            Path file = rootLocation.resolve(subFolder).resolve(filename);
            FileSystemUtils.deleteRecursively(file);
        } catch (IOException e) {
            // Log lỗi hoặc throw exception nếu cần
            System.err.println("Could not delete file: " + filename + " Error: " + e.getMessage());
        }
    }
    // Implement các phương thức khác nếu cần
}