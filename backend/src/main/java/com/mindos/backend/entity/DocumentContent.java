package com.mindos.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_contents",
       uniqueConstraints = {
           @UniqueConstraint(name = "uk_document_contents_doc_id", columnNames = {"document_id"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false, unique = true)
    private Document document;

    @Column(name = "extracted_text", columnDefinition = "TEXT", nullable = false)
    private String extractedText;

    @Column(name = "char_count")
    private Integer charCount;

    @Column(name = "word_count")
    private Integer wordCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Explicit constructor and methods for JDK 23 compatibility
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) {
        this.extractedText = extractedText;
        if (extractedText != null) {
            this.charCount = extractedText.length();
            this.wordCount = extractedText.trim().isEmpty() ? 0 : extractedText.trim().split("\\s+").length;
        } else {
            this.charCount = 0;
            this.wordCount = 0;
        }
    }

    public Integer getCharCount() { return charCount; }
    public void setCharCount(Integer charCount) { this.charCount = charCount; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static DocumentContentBuilder builder() {
        return new DocumentContentBuilder();
    }

    public static class DocumentContentBuilder {
        private Long id;
        private Document document;
        private String extractedText;
        private Integer charCount;
        private Integer wordCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        DocumentContentBuilder() {}

        public DocumentContentBuilder id(Long id) { this.id = id; return this; }
        public DocumentContentBuilder document(Document document) { this.document = document; return this; }
        public DocumentContentBuilder extractedText(String extractedText) {
            this.extractedText = extractedText;
            if (extractedText != null) {
                this.charCount = extractedText.length();
                this.wordCount = extractedText.trim().isEmpty() ? 0 : extractedText.trim().split("\\s+").length;
            }
            return this;
        }
        public DocumentContentBuilder charCount(Integer charCount) { this.charCount = charCount; return this; }
        public DocumentContentBuilder wordCount(Integer wordCount) { this.wordCount = wordCount; return this; }
        public DocumentContentBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public DocumentContentBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public DocumentContent build() {
            DocumentContent content = new DocumentContent();
            content.setId(this.id);
            content.setDocument(this.document);
            content.setExtractedText(this.extractedText);
            content.setCharCount(this.charCount);
            content.setWordCount(this.wordCount);
            content.setCreatedAt(this.createdAt);
            content.setUpdatedAt(this.updatedAt);
            return content;
        }
    }
}
