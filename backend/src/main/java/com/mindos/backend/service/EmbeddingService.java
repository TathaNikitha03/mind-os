package com.mindos.backend.service;

import com.mindos.backend.entity.Document;
import com.mindos.backend.entity.DocumentChunk;
import com.mindos.backend.entity.DocumentChunkEmbedding;
import com.mindos.backend.repository.DocumentChunkEmbeddingRepository;
import com.mindos.backend.service.embedding.EmbeddingProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final EmbeddingProvider embeddingProvider;
    private final DocumentChunkEmbeddingRepository embeddingRepository;

    public EmbeddingService(EmbeddingProvider embeddingProvider,
                            DocumentChunkEmbeddingRepository embeddingRepository) {
        this.embeddingProvider = embeddingProvider;
        this.embeddingRepository = embeddingRepository;
    }

    /**
     * Generate embeddings for all document chunks and persist vectors in PostgreSQL.
     * Idempotent & reprocessing safe (cleans prior embeddings before inserting new ones).
     *
     * @param document Associated parent document
     * @param chunks   List of generated document chunks
     * @return List of persisted DocumentChunkEmbedding entities
     */
    @Transactional
    public List<DocumentChunkEmbedding> generateAndSaveEmbeddings(Document document, List<DocumentChunk> chunks) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("Valid document entity is required.");
        }

        // Reprocessing safety: delete existing embeddings for this document
        embeddingRepository.deleteByDocumentId(document.getId());

        if (chunks == null || chunks.isEmpty()) {
            log.warn("No chunks provided to generate embeddings for document ID: {}", document.getId());
            return List.of();
        }

        log.info("Generating embeddings for {} chunks of document ID: {} using model: '{}'",
                chunks.size(), document.getId(), embeddingProvider.getModelName());

        List<String> texts = chunks.stream()
                .map(DocumentChunk::getContent)
                .toList();

        List<float[]> vectors = embeddingProvider.generateEmbeddings(texts);

        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException(String.format(
                    "Embedding generation count mismatch: expected %d vectors but got %d",
                    chunks.size(), vectors.size()));
        }

        int expectedDimension = embeddingProvider.getDimension();
        List<DocumentChunkEmbedding> entities = new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {
            DocumentChunk chunk = chunks.get(i);
            float[] vector = vectors.get(i);

            // Dimension validation
            if (vector == null) {
                throw new IllegalStateException("Embedding vector for chunk index " + chunk.getChunkIndex() + " is null.");
            }

            if (vector.length != expectedDimension) {
                throw new IllegalStateException(String.format(
                        "Embedding dimension mismatch for chunk %d: expected %d but received %d",
                        chunk.getChunkIndex(), expectedDimension, vector.length));
            }

            DocumentChunkEmbedding dce = DocumentChunkEmbedding.builder()
                    .chunk(chunk)
                    .embedding(vector)
                    .modelName(embeddingProvider.getModelName())
                    .dimension(vector.length)
                    .build();

            entities.add(dce);
        }

        List<DocumentChunkEmbedding> saved = embeddingRepository.saveAll(entities);
        log.info("Successfully persisted {} vector embeddings for document ID: {}", saved.size(), document.getId());
        return saved;
    }

    /**
     * Get the count of embeddings currently persisted for a document.
     */
    @Transactional(readOnly = true)
    public long getEmbeddingCountForDocument(Long documentId) {
        if (documentId == null) return 0;
        return embeddingRepository.countByDocumentId(documentId);
    }

    /**
     * Delete embeddings for a document.
     */
    @Transactional
    public void deleteEmbeddingsByDocument(Long documentId) {
        if (documentId != null) {
            embeddingRepository.deleteByDocumentId(documentId);
        }
    }

    public String getActiveModelName() {
        return embeddingProvider.getModelName();
    }

    public int getActiveDimension() {
        return embeddingProvider.getDimension();
    }
}
