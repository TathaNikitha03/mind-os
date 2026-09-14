package com.mindos.backend.service.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@Service("localEmbeddingProvider")
public class LocalDeterministicEmbeddingProvider implements EmbeddingProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalDeterministicEmbeddingProvider.class);

    private final String modelName;
    private final int dimension;

    public LocalDeterministicEmbeddingProvider(
            @Value("${embedding.model:text-embedding-3-small}") String modelName,
            @Value("${embedding.dimension:1536}") int dimension) {
        this.modelName = modelName;
        this.dimension = dimension;
        log.info("Initialized LocalDeterministicEmbeddingProvider: model={}, dimension={}", modelName, dimension);
    }

    @Override
    public float[] generateEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty for embedding generation.");
        }

        float[] vector = new float[dimension];
        String normalized = text.toLowerCase().trim();
        String[] tokens = normalized.split("\\s+");

        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");

            // Seed semantic vectors from n-grams and tokens
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                byte[] hash = sha256.digest(token.getBytes(StandardCharsets.UTF_8));

                for (int b = 0; b < hash.length; b++) {
                    int slot = Math.abs((token.hashCode() * 31 + b * 17 + i)) % dimension;
                    vector[slot] += (float) (hash[b] / 128.0);
                }
            }
        } catch (NoSuchAlgorithmException e) {
            // Fallback to polynomial hash
            for (int i = 0; i < tokens.length; i++) {
                int slot = Math.abs(tokens[i].hashCode()) % dimension;
                vector[slot] += 1.0f;
            }
        }

        // L2 Unit Normalization (crucial for cosine similarity metrics)
        double sumSq = 0.0;
        for (float v : vector) {
            sumSq += v * v;
        }

        double norm = Math.sqrt(sumSq);
        if (norm > 0.000001) {
            for (int i = 0; i < vector.length; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        } else {
            vector[0] = 1.0f; // Unit fallback
        }

        return vector;
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(generateEmbedding(text));
        }
        return results;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public int getDimension() {
        return dimension;
    }
}
