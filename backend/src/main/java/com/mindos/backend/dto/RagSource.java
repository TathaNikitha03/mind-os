package com.mindos.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagSource {

    private Long documentId;
    private String documentName;
    private Long chunkId;
    private Integer chunkIndex;
    private Double similarity;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }

    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }

    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }

    public Double getSimilarity() { return similarity; }
    public void setSimilarity(Double similarity) { this.similarity = similarity; }

    public static RagSourceBuilder builder() {
        return new RagSourceBuilder();
    }

    public static class RagSourceBuilder {
        private Long documentId;
        private String documentName;
        private Long chunkId;
        private Integer chunkIndex;
        private Double similarity;

        RagSourceBuilder() {}

        public RagSourceBuilder documentId(Long documentId) { this.documentId = documentId; return this; }
        public RagSourceBuilder documentName(String documentName) { this.documentName = documentName; return this; }
        public RagSourceBuilder chunkId(Long chunkId) { this.chunkId = chunkId; return this; }
        public RagSourceBuilder chunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; return this; }
        public RagSourceBuilder similarity(Double similarity) { this.similarity = similarity; return this; }

        public RagSource build() {
            RagSource src = new RagSource();
            src.setDocumentId(this.documentId);
            src.setDocumentName(this.documentName);
            src.setChunkId(this.chunkId);
            src.setChunkIndex(this.chunkIndex);
            src.setSimilarity(this.similarity);
            return src;
        }
    }
}
