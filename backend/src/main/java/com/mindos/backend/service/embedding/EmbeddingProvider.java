package com.mindos.backend.service.embedding;

import java.util.List;

public interface EmbeddingProvider {

    /**
     * Generate single vector embedding for the input text.
     */
    float[] generateEmbedding(String text);

    /**
     * Generate vector embeddings for a list of chunk texts.
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Get the name/identifier of the active embedding model.
     */
    String getModelName();

    /**
     * Get the expected dimension of the embedding vectors.
     */
    int getDimension();
}
