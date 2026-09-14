package com.mindos.backend.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service("localStorageService")
public class LocalStorageService implements StorageService {

    private final Path fileStorageLocation;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx", "doc", "txt");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    public LocalStorageService(@Value("${storage.local.upload-dir:${file.upload-dir:uploads/documents}}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (Exception ex) {
            throw new RuntimeException("Could not create the local upload directory: " + this.fileStorageLocation, ex);
        }
    }

    @Override
    public String storeFile(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Cannot store empty file.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds 10 MB limit.");
        }

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Invalid path sequence in filename: " + originalFilename);
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file type: " + extension + ". Supported formats: PDF, DOCX, TXT.");
        }

        try {
            Path userDir = this.fileStorageLocation.resolve("user_" + userId).normalize();
            Files.createDirectories(userDir);

            String storedFileName = UUID.randomUUID().toString() + "_" + originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path targetLocation = userDir.resolve(storedFileName);

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return "user_" + userId + "/" + storedFileName;
        } catch (IOException ex) {
            throw new RuntimeException("Failed to store file " + originalFilename, ex);
        }
    }

    @Override
    public Resource loadFileAsResource(String relativeFilePath) {
        try {
            Path filePath = this.fileStorageLocation.resolve(relativeFilePath).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IllegalArgumentException("File not found or unreadable: " + relativeFilePath);
            }
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid file path: " + relativeFilePath, ex);
        }
    }

    @Override
    public boolean deleteFile(String relativeFilePath) {
        if (relativeFilePath == null || relativeFilePath.trim().isEmpty()) {
            return false;
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(relativeFilePath).normalize();
            return Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println("Warning: Could not delete physical file: " + relativeFilePath + " - " + ex.getMessage());
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return "LOCAL";
    }

    public static String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }
}
