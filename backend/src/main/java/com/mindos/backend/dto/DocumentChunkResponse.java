package com.mindos.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunkResponse {
    private Long id;
    private Long documentId;
    private Integer chunkIndex;
    private String content;
    private Integer charCount;
    private Integer wordCount;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getCharCount() { return charCount; }
    public void setCharCount(Integer charCount) { this.charCount = charCount; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static DocumentChunkResponseBuilder builder() {
        return new DocumentChunkResponseBuilder();
    }

    public static class DocumentChunkResponseBuilder {
        private Long id;
        private Long documentId;
        private Integer chunkIndex;
        private String content;
        private Integer charCount;
        private Integer wordCount;
        private LocalDateTime createdAt;

        DocumentChunkResponseBuilder() {}

        public DocumentChunkResponseBuilder id(Long id) { this.id = id; return this; }
        public DocumentChunkResponseBuilder documentId(Long documentId) { this.documentId = documentId; return this; }
        public DocumentChunkResponseBuilder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public DocumentChunkResponseBuilder content(String content) { this.content = content; return this; }
        public DocumentChunkResponseBuilder charCount(Integer charCount) { this.charCount = charCount; return this; }
        public DocumentChunkResponseBuilder wordCount(Integer wordCount) { this.wordCount = wordCount; return this; }
        public DocumentChunkResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public DocumentChunkResponse build() {
            DocumentChunkResponse res = new DocumentChunkResponse();
            res.setId(this.id);
            res.setDocumentId(this.documentId);
            res.setChunkIndex(this.chunkIndex);
            res.setContent(this.content);
            res.setCharCount(this.charCount);
            res.setWordCount(this.wordCount);
            res.setCreatedAt(this.createdAt);
            return res;
        }
    }
}
