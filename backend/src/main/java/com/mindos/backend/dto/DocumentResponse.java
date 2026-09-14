package com.mindos.backend.dto;

import java.time.LocalDateTime;

public class DocumentResponse {

    private Long id;
    private Long userId;
    private Long categoryId;
    private String categoryName;
    private String fileName;
    private String title;
    private String fileType;
    private Long fileSize;
    private String description;
    private String storageUrl;
    private String status;
    private LocalDateTime uploadedAt;
    private LocalDateTime updatedAt;

    public DocumentResponse() {}

    public static DocumentResponseBuilder builder() {
        return new DocumentResponseBuilder();
    }

    public static class DocumentResponseBuilder {
        private Long id;
        private Long userId;
        private Long categoryId;
        private String categoryName;
        private String fileName;
        private String title;
        private String fileType;
        private Long fileSize;
        private String description;
        private String storageUrl;
        private String status;
        private LocalDateTime uploadedAt;
        private LocalDateTime updatedAt;

        public DocumentResponseBuilder id(Long id) { this.id = id; return this; }
        public DocumentResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public DocumentResponseBuilder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }
        public DocumentResponseBuilder categoryName(String categoryName) { this.categoryName = categoryName; return this; }
        public DocumentResponseBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public DocumentResponseBuilder title(String title) { this.title = title; return this; }
        public DocumentResponseBuilder fileType(String fileType) { this.fileType = fileType; return this; }
        public DocumentResponseBuilder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public DocumentResponseBuilder description(String description) { this.description = description; return this; }
        public DocumentResponseBuilder storageUrl(String storageUrl) { this.storageUrl = storageUrl; return this; }
        public DocumentResponseBuilder status(String status) { this.status = status; return this; }
        public DocumentResponseBuilder uploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; return this; }
        public DocumentResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public DocumentResponse build() {
            DocumentResponse r = new DocumentResponse();
            r.setId(id);
            r.setUserId(userId);
            r.setCategoryId(categoryId);
            r.setCategoryName(categoryName);
            r.setFileName(fileName);
            r.setTitle(title);
            r.setFileType(fileType);
            r.setFileSize(fileSize);
            r.setDescription(description);
            r.setStorageUrl(storageUrl);
            r.setStatus(status);
            r.setUploadedAt(uploadedAt);
            r.setUpdatedAt(updatedAt);
            return r;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStorageUrl() { return storageUrl; }
    public void setStorageUrl(String storageUrl) { this.storageUrl = storageUrl; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
