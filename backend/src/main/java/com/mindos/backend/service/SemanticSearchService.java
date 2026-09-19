package com.mindos.backend.service;

import com.mindos.backend.dto.SemanticSearchRequest;
import com.mindos.backend.dto.SemanticSearchResponse;
import com.mindos.backend.dto.SemanticSearchResult;
import com.mindos.backend.entity.Document;
import com.mindos.backend.entity.DocumentChunk;
import com.mindos.backend.entity.DocumentChunkEmbedding;
import com.mindos.backend.entity.User;
import com.mindos.backend.repository.DocumentChunkEmbeddingRepository;
import com.mindos.backend.service.embedding.EmbeddingProvider;
import com.mindos.backend.util.VectorConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class SemanticSearchService {

    private static final Logger log = LoggerFactory.getLogger(SemanticSearchService.class);

    private final EmbeddingProvider embeddingProvider;
    private final DocumentChunkEmbeddingRepository embeddingRepository;
    private final VectorConverter vectorConverter;

    private final double defaultThreshold;
    private final int defaultTopK;
    private final int maxTopK;

    public SemanticSearchService(
            EmbeddingProvider embeddingProvider,
            DocumentChunkEmbeddingRepository embeddingRepository,
            @Value("${mindflow.ai.search.default-threshold:0.0}") double defaultThreshold,
            @Value("${knowledge.search.default-top-k:5}") int defaultTopK,
            @Value("${knowledge.search.max-top-k:10}") int maxTopK) {
        this.embeddingProvider = embeddingProvider;
        this.embeddingRepository = embeddingRepository;
        this.vectorConverter = new VectorConverter();
        this.defaultThreshold = defaultThreshold;
        this.defaultTopK = defaultTopK;
        this.maxTopK = maxTopK;
    }

    /**
     * Perform semantic similarity search across the authenticated user's knowledge base.
     *
     * @param user    Authenticated user
     * @param request Search parameters (query, topK, optional threshold)
     * @return Ranked relevant document chunks
     */
    public SemanticSearchResponse search(User user, SemanticSearchRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated.");
        }

        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Search query cannot be empty.");
        }

        String rawQuery = request.getQuery().trim();
        if (rawQuery.length() > 1000) {
            throw new IllegalArgumentException("Search query cannot exceed 1000 characters.");
        }

        // Top-K parameter normalization (min 1, default 5, max 10)
        int topK = defaultTopK;
        if (request.getTopK() != null) {
            if (request.getTopK() < 1) {
                topK = 1;
            } else {
                topK = Math.min(request.getTopK(), maxTopK);
            }
        }

        double threshold = (request.getThreshold() != null && request.getThreshold() >= 0.0)
                ? request.getThreshold()
                : defaultThreshold;

        String modelName = embeddingProvider.getModelName();

        log.info("Executing semantic search for user ID: {}, query: '{}', topK: {}, threshold: {}, model: '{}'",
                user.getId(), rawQuery, topK, threshold, modelName);

        // 1. Generate query vector using identical model & dimension
        float[] queryVector;
        try {
            queryVector = embeddingProvider.generateEmbedding(rawQuery);
        } catch (Exception ex) {
            log.error("Failed to generate embedding for query '{}': {}", rawQuery, ex.getMessage());
            throw new RuntimeException("Knowledge search is temporarily unavailable.", ex);
        }

        if (queryVector == null || queryVector.length != embeddingProvider.getDimension()) {
            throw new IllegalStateException("Generated query vector dimension mismatch.");
        }

        String vectorStr = vectorConverter.convertToDatabaseColumn(queryVector);

        // 2. Perform vector search (Native pgvector with fallback for test mocks)
        List<SemanticSearchResult> rankedResults;
        try {
            List<Object[]> rows = embeddingRepository.searchSimilarChunksNative(
                    user.getId(), vectorStr, modelName, threshold, topK);

            rankedResults = new ArrayList<>();
            for (Object[] row : rows) {
                Long chunkId = ((Number) row[0]).longValue();
                Long docId = ((Number) row[1]).longValue();
                String docName = (String) row[2];
                Integer chunkIndex = ((Number) row[3]).intValue();
                String content = (String) row[4];
                double similarity = ((Number) row[5]).doubleValue();

                double roundedSim = BigDecimal.valueOf(similarity)
                        .setScale(4, RoundingMode.HALF_UP)
                        .doubleValue();

                rankedResults.add(SemanticSearchResult.builder()
                        .chunkId(chunkId)
                        .documentId(docId)
                        .documentName(docName)
                        .chunkIndex(chunkIndex)
                        .content(content)
                        .similarity(roundedSim)
                        .build());
            }
        } catch (Exception ex) {
            log.warn("Native pgvector query exception (switching to in-memory ranking fallback): {}", ex.getMessage());
            rankedResults = searchSimilarChunksInMemory(user.getId(), queryVector, modelName, threshold, topK);
        }

        log.info("Semantic search completed for query '{}': found {} relevant chunks", rawQuery, rankedResults.size());

        return SemanticSearchResponse.builder()
                .query(rawQuery)
                .results(rankedResults)
                .totalResults(rankedResults.size())
                .modelName(modelName)
                .build();
    }

    /**
     * Fallback in-memory cosine similarity ranking.
     * Guarantees testability in unit tests and mock environments without PostgreSQL C extensions.
     */
    public List<SemanticSearchResult> searchSimilarChunksInMemory(
            Long userId, float[] queryVector, String modelName, double threshold, int topK) {

        List<DocumentChunkEmbedding> userEmbeddings = embeddingRepository.findUserReadyChunkEmbeddings(userId, modelName);
        if (userEmbeddings == null || userEmbeddings.isEmpty()) {
            return List.of();
        }

        List<SemanticSearchResult> scored = new ArrayList<>();
        for (DocumentChunkEmbedding dce : userEmbeddings) {
            float[] chunkVec = dce.getEmbedding();
            if (chunkVec == null) continue;

            double sim = calculateCosineSimilarity(queryVector, chunkVec);
            if (sim >= threshold) {
                DocumentChunk chunk = dce.getChunk();
                Document doc = chunk.getDocument();

                double roundedSim = BigDecimal.valueOf(sim)
                        .setScale(4, RoundingMode.HALF_UP)
                        .doubleValue();

                String docName = doc.getTitle() != null ? doc.getTitle() : doc.getFileName();
                scored.add(SemanticSearchResult.builder()
                        .chunkId(chunk.getId())
                        .documentId(doc.getId())
                        .documentName(docName)
                        .chunkIndex(chunk.getChunkIndex())
                        .content(chunk.getContent())
                        .similarity(roundedSim)
                        .build());
            }
        }

        scored.sort(Comparator.comparing(SemanticSearchResult::getSimilarity).reversed());
        return scored.stream().limit(topK).toList();
    }

    /**
     * Compute cosine similarity between two unit vectors.
     */
    public static double calculateCosineSimilarity(float[] v1, float[] v2) {
        if (v1 == null || v2 == null || v1.length != v2.length) {
            return 0.0;
        }

        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }

        double denom = Math.sqrt(norm1) * Math.sqrt(norm2);
        if (denom < 1e-9) {
            return 0.0;
        }

        return dot / denom;
    }
}
