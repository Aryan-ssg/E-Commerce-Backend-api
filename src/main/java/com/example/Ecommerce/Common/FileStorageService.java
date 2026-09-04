package com.example.Ecommerce.Common;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    private Path storageLocation;

    @PostConstruct
    public void init() throws IOException {
        storageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(storageLocation);
    }

    /**
     * Stores the file on disk and returns the relative URL path that can be used
     * to retrieve it (e.g. "/uploads/products/{uuid}.jpg").
     */
    public String storeFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File size must not exceed 5 MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG, WebP and GIF images are accepted");
        }

        String ext = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif"  -> ".gif";
            default -> "";
        };

        String filename = UUID.randomUUID() + ext;
        try {
            Files.copy(file.getInputStream(), storageLocation.resolve(filename));
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }

        return "/uploads/products/" + filename;
    }

    /** Deletes a previously stored file. No-op if the path is null or invalid. */
    public void deleteFile(String url) {
        if (url == null || url.isBlank()) return;
        try {
            Path path = storageLocation.resolve(Paths.get(URI.create("file:" + url).getPath()).getFileName());
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
        }
    }
}
