package com.mindos.backend;

import com.mindos.backend.service.embedding.LocalDeterministicEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDeterministicEmbeddingProviderTest {

    private LocalDeterministicEmbeddingProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LocalDeterministicEmbeddingProvider("text-embedding-3-small", 1536);
    }

    @Test
    @DisplayName("Should generate vector with exact configured dimension (1536)")
    void testExactDimension() {
        float[] vector = provider.generateEmbedding("PostgreSQL pgvector storage for RAG");
        assertNotNull(vector);
        assertEquals(1536, vector.length);
        assertEquals(1536, provider.getDimension());
        assertEquals("text-embedding-3-small", provider.getModelName());
    }

    @Test
    @DisplayName("Should produce normalized unit vector (L2 norm ~= 1.0)")
    void testL2UnitNormalization() {
        float[] vector = provider.generateEmbedding("Relational database management systems and indexing");
        double sumSq = 0.0;
        for (float v : vector) {
            sumSq += v * v;
        }
        double norm = Math.sqrt(sumSq);
        assertEquals(1.0, norm, 0.001, "Vector must be unit normalized for cosine similarity");
    }

    @Test
    @DisplayName("Should be deterministic: identical input yields identical vector")
    void testDeterministicEmbedding() {
        String text = "Normalization removes data redundancy in relational tables.";
        float[] v1 = provider.generateEmbedding(text);
        float[] v2 = provider.generateEmbedding(text);

        assertArrayEquals(v1, v2);
    }

    @Test
    @DisplayName("Should generate batch embeddings matching input size")
    void testBatchEmbedding() {
        List<String> texts = List.of("First chunk text", "Second chunk text", "Third chunk text");
        List<float[]> embeddings = provider.generateEmbeddings(texts);

        assertEquals(3, embeddings.size());
        for (float[] v : embeddings) {
            assertEquals(1536, v.length);
        }
    }
}
