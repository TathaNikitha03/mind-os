package com.mindos.backend.entity;

import com.mindos.backend.util.VectorConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunk_embeddings",
       uniqueConstraints = {
           @UniqueConstraint(name = "unique_chunk_model_embedding", columnNames = {"chunk_id", "model_name"})
       })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunkEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id", nullable = false)
    private DocumentChunk chunk;

    @Convert(converter = VectorConverter.class)
    @Column(name = "embedding", columnDefinition = "text", nullable = false)
    private float[] embedding;

    @Column(name = "model_name", nullable = false, length = 100)
    private String modelName;

    @Column(name = "dimension", nullable = false)
    private Integer dimension;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DocumentChunk getChunk() { return chunk; }
    public void setChunk(DocumentChunk chunk) { this.chunk = chunk; }

    public float[] getEmbedding() { return embedding; }
    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
        if (embedding != null) {
            this.dimension = embedding.length;
        }
    }

    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }

    public Integer getDimension() { return dimension; }
    public void setDimension(Integer dimension) { this.dimension = dimension; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public static DocumentChunkEmbeddingBuilder builder() {
        return new DocumentChunkEmbeddingBuilder();
    }

    public static class DocumentChunkEmbeddingBuilder {
        private Long id;
        private DocumentChunk chunk;
        private float[] embedding;
        private String modelName;
        private Integer dimension;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        DocumentChunkEmbeddingBuilder() {}

        public DocumentChunkEmbeddingBuilder id(Long id) { this.id = id; return this; }
        public DocumentChunkEmbeddingBuilder chunk(DocumentChunk chunk) { this.chunk = chunk; return this; }
        public DocumentChunkEmbeddingBuilder embedding(float[] embedding) {
            this.embedding = embedding;
            if (embedding != null) {
                this.dimension = embedding.length;
            }
            return this;
        }
        public DocumentChunkEmbeddingBuilder modelName(String modelName) { this.modelName = modelName; return this; }
        public DocumentChunkEmbeddingBuilder dimension(Integer dimension) { this.dimension = dimension; return this; }
        public DocumentChunkEmbeddingBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public DocumentChunkEmbeddingBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }

        public DocumentChunkEmbedding build() {
            DocumentChunkEmbedding dce = new DocumentChunkEmbedding();
            dce.setId(this.id);
            dce.setChunk(this.chunk);
            dce.setEmbedding(this.embedding);
            dce.setModelName(this.modelName);
            dce.setDimension(this.dimension != null ? this.dimension : (this.embedding != null ? this.embedding.length : 1536));
            dce.setCreatedAt(this.createdAt);
            dce.setUpdatedAt(this.updatedAt);
            return dce;
        }
    }
}
