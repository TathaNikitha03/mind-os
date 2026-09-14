package com.mindos.backend.entity;

import com.mindos.backend.enums.DocumentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(length = 255)
    private String title;

    @Column(name = "file_type", length = 100)
    private String fileType;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentStatus status = DocumentStatus.READY;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentEntity> entities;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentChunk> chunks;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiExtraction> aiExtractions;

    public Document() {}

    public static DocumentBuilder builder() {
        return new DocumentBuilder();
    }

    public static class DocumentBuilder {
        private Long id;
        private User user;
        private Category category;
        private String fileName;
        private String title;
        private String fileType;
        private String fileUrl;
        private Long fileSize;
        private String description;
        private DocumentStatus status = DocumentStatus.READY;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public DocumentBuilder id(Long id) { this.id = id; return this; }
        public DocumentBuilder user(User user) { this.user = user; return this; }
        public DocumentBuilder category(Category category) { this.category = category; return this; }
        public DocumentBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public DocumentBuilder title(String title) { this.title = title; return this; }
        public DocumentBuilder fileType(String fileType) { this.fileType = fileType; return this; }
        public DocumentBuilder fileUrl(String fileUrl) { this.fileUrl = fileUrl; return this; }
        public DocumentBuilder fileSize(Long fileSize) { this.fileSize = fileSize; return this; }
        public DocumentBuilder description(String description) { this.description = description; return this; }
        public DocumentBuilder status(DocumentStatus status) { this.status = status; return this; }
        public DocumentBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public DocumentBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public Document build() {
            Document d = new Document();
            d.setId(id);
            d.setUser(user);
            d.setCategory(category);
            d.setFileName(fileName);
            d.setTitle(title);
            d.setFileType(fileType);
            d.setFileUrl(fileUrl);
            d.setFileSize(fileSize);
            d.setDescription(description);
            if (status != null) d.setStatus(status);
            d.setCreatedAt(createdAt);
            d.setUpdatedAt(updatedAt);
            return d;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public List<DocumentEntity> getEntities() { return entities; }
    public void setEntities(List<DocumentEntity> entities) { this.entities = entities; }
    public List<DocumentChunk> getChunks() { return chunks; }
    public void setChunks(List<DocumentChunk> chunks) { this.chunks = chunks; }
    public List<AiExtraction> getAiExtractions() { return aiExtractions; }
    public void setAiExtractions(List<AiExtraction> aiExtractions) { this.aiExtractions = aiExtractions; }
}
