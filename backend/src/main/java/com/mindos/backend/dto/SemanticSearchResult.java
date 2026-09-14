package com.mindos.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SemanticSearchResult {

    private Long chunkId;
    private Long documentId;
    private String documentName;
    private Integer chunkIndex;
    private String content;
    private Double similarity;

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }

    public static SemanticSearchResultBuilder builder() {
        return new SemanticSearchResultBuilder();
    }

    public static class SemanticSearchResultBuilder {
        private Long chunkId;
        private Long documentId;
        private String documentName;
        private Integer chunkIndex;
        private String content;
        private Double similarity;

        SemanticSearchResultBuilder() {}

        public SemanticSearchResultBuilder chunkId(Long chunkId) { this.chunkId = chunkId; return this; }
        public SemanticSearchResultBuilder documentId(Long documentId) { this.documentId = documentId; return this; }
        public SemanticSearchResultBuilder documentName(String documentName) { this.documentName = documentName; return this; }
        public SemanticSearchResultBuilder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public SemanticSearchResultBuilder content(String content) { this.content = content; return this; }
        public SemanticSearchResultBuilder similarity(Double similarity) { this.similarity = similarity; return this; }

        public SemanticSearchResult build() {
            SemanticSearchResult res = new SemanticSearchResult();
            res.setChunkId(this.chunkId);
            res.setDocumentId(this.documentId);
            res.setDocumentName(this.documentName);
            res.setChunkIndex(this.chunkIndex);
            res.setContent(this.content);
            res.setSimilarity(this.similarity);
            return res;
        }
    }
}
