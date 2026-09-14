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
public class DocumentContentResponse {
    private Long id;
    private Long documentId;
    private String fileName;
    private String title;
    private String fileType;
    private String extractedText;
    private Integer charCount;
    private Integer wordCount;
    private Integer chunkCount;
    private Integer embeddingCount;
    private boolean hasText;
    private String status;
    private LocalDateTime extractedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public Integer getCharCount() { return charCount; }
    public void setCharCount(Integer charCount) { this.charCount = charCount; }

    public Integer getWordCount() { return wordCount; }
    public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }

    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }

    public Integer getEmbeddingCount() { return embeddingCount; }
    public void setEmbeddingCount(Integer embeddingCount) { this.embeddingCount = embeddingCount; }

    public boolean isHasText() { return hasText; }
    public void setHasText(boolean hasText) { this.hasText = hasText; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getExtractedAt() { return extractedAt; }
    public void setExtractedAt(LocalDateTime extractedAt) { this.extractedAt = extractedAt; }

    public static DocumentContentResponseBuilder builder() {
        return new DocumentContentResponseBuilder();
    }

    public static class DocumentContentResponseBuilder {
        private Long id;
        private Long documentId;
        private String fileName;
        private String title;
        private String fileType;
        private String extractedText;
        private Integer charCount;
        private Integer wordCount;
        private Integer chunkCount;
        private Integer embeddingCount;
        private boolean hasText;
        private String status;
        private LocalDateTime extractedAt;

        DocumentContentResponseBuilder() {}

        public DocumentContentResponseBuilder id(Long id) { this.id = id; return this; }
        public DocumentContentResponseBuilder documentId(Long documentId) { this.documentId = documentId; return this; }
        public DocumentContentResponseBuilder fileName(String fileName) { this.fileName = fileName; return this; }
        public DocumentContentResponseBuilder title(String title) { this.title = title; return this; }
        public DocumentContentResponseBuilder fileType(String fileType) { this.fileType = fileType; return this; }
        public DocumentContentResponseBuilder extractedText(String extractedText) { this.extractedText = extractedText; return this; }
        public DocumentContentResponseBuilder charCount(Integer charCount) { this.charCount = charCount; return this; }
        public DocumentContentResponseBuilder wordCount(Integer wordCount) { this.wordCount = wordCount; return this; }
        public DocumentContentResponseBuilder chunkCount(Integer chunkCount) { this.chunkCount = chunkCount; return this; }
        public DocumentContentResponseBuilder embeddingCount(Integer embeddingCount) { this.embeddingCount = embeddingCount; return this; }
        public DocumentContentResponseBuilder hasText(boolean hasText) { this.hasText = hasText; return this; }
        public DocumentContentResponseBuilder status(String status) { this.status = status; return this; }
        public DocumentContentResponseBuilder extractedAt(LocalDateTime extractedAt) { this.extractedAt = extractedAt; return this; }

        public DocumentContentResponse build() {
            DocumentContentResponse res = new DocumentContentResponse();
            res.setId(this.id);
            res.setDocumentId(this.documentId);
            res.setFileName(this.fileName);
            res.setTitle(this.title);
            res.setFileType(this.fileType);
            res.setExtractedText(this.extractedText);
            res.setCharCount(this.charCount);
            res.setWordCount(this.wordCount);
            res.setChunkCount(this.chunkCount);
            res.setEmbeddingCount(this.embeddingCount);
            res.setHasText(this.hasText);
            res.setStatus(this.status);
            res.setExtractedAt(this.extractedAt);
            return res;
        }
    }
}
