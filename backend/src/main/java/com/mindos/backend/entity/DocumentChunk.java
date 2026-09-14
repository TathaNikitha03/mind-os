package com.mindos.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunks",
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_document_chunk", columnNames = {"document_id", "chunk_index"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Document getDocument() { return document; }
    public void setDocument(Document document) { this.document = document; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) {
        this.content = content;
        if (content != null) {
            this.charCount = content.length();
            this.wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
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

    public static DocumentChunkBuilder builder() {
        return new DocumentChunkBuilder();
    }

    public static class DocumentChunkBuilder {
        private Long id;
        private Document document;
        private Integer chunkIndex;
        private String content;
        private Integer charCount;
        private Integer wordCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        DocumentChunkBuilder() {}

        public DocumentChunkBuilder id(Long id) { this.id = id; return this; }
        public DocumentChunkBuilder document(Document document) { this.document = document; return this; }
        public DocumentChunkBuilder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public DocumentChunkBuilder content(String content) {
            this.content = content;
            if (content != null) {
                this.charCount = content.length();
                this.wordCount = content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length;
            }
            return this;
        }
        public DocumentChunkBuilder charCount(Integer charCount) { this.charCount = charCount; return this; }
        public DocumentChunkBuilder wordCount(Integer wordCount) { this.wordCount = wordCount; return this; }
        public DocumentChunkBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public DocumentChunkBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public DocumentChunk build() {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setId(this.id);
            chunk.setDocument(this.document);
            chunk.setChunkIndex(this.chunkIndex);
            chunk.setContent(this.content);
            chunk.setCharCount(this.charCount);
            chunk.setWordCount(this.wordCount);
            chunk.setCreatedAt(this.createdAt);
            chunk.setUpdatedAt(this.updatedAt);
            return chunk;
        }
    }
}
