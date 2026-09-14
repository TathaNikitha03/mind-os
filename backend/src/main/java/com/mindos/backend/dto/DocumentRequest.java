package com.mindos.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class DocumentRequest {

    @NotBlank(message = "File name is required")
    private String fileName;

    private String title;
    private String fileType;
    private Long fileSize;
    private String description;
    private String storageUrl;
    private String status;
    private Long categoryId;

    public DocumentRequest() {}

    public static DocumentRequestBuilder builder() {
        return new DocumentRequestBuilder();
    }

    public static class DocumentRequestBuilder {
        private String fileName;
        private String title;
        private String fileType;
        private Long fileSize;
        private String description;
        private String storageUrl;
        private String status;
        private Long categoryId;

        public DocumentRequestBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public DocumentRequestBuilder title(String title) { this.title = title; return this; }
        public DocumentRequestBuilder fileType(String fileType) { this.fileType = fileType; return this; }
        public DocumentRequestBuilder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public DocumentRequestBuilder description(String description) { this.description = description; return this; }
        public DocumentRequestBuilder storageUrl(String storageUrl) { this.storageUrl = storageUrl; return this; }
        public DocumentRequestBuilder status(String status) { this.status = status; return this; }
        public DocumentRequestBuilder categoryId(Long categoryId) { this.categoryId = categoryId; return this; }

        public DocumentRequest build() {
            DocumentRequest r = new DocumentRequest();
            r.setFileName(fileName);
            r.setTitle(title);
            r.setFileType(fileType);
            r.setFileSize(fileSize);
            r.setDescription(description);
            r.setStorageUrl(storageUrl);
            r.setStatus(status);
            r.setCategoryId(categoryId);
            return r;
        }
    }

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
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
}
