package com.mindos.backend;

import com.mindos.backend.entity.Document;
import com.mindos.backend.entity.DocumentChunk;
import com.mindos.backend.entity.DocumentChunkEmbedding;
import com.mindos.backend.repository.DocumentChunkEmbeddingRepository;
import com.mindos.backend.service.EmbeddingService;
import com.mindos.backend.service.embedding.EmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmbeddingServiceTest {

    @Mock
    private EmbeddingProvider embeddingProvider;

    @Mock
    private DocumentChunkEmbeddingRepository embeddingRepository;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(embeddingProvider, embeddingRepository);
    }

    @Test
    @DisplayName("Should generate and persist 1536-dim embeddings for all document chunks")
    void testGenerateAndSaveEmbeddingsSuccess() {
        Document doc = Document.builder().id(10L).fileName("DBMS.pdf").build();
        DocumentChunk chunk0 = DocumentChunk.builder().id(101L).document(doc).chunkIndex(0).content("Chunk 0 text").build();
        DocumentChunk chunk1 = DocumentChunk.builder().id(102L).document(doc).chunkIndex(1).content("Chunk 1 text").build();

        float[] v0 = new float[1536];
        v0[0] = 0.5f;
        float[] v1 = new float[1536];
        v1[0] = 0.8f;

        when(embeddingProvider.getModelName()).thenReturn("text-embedding-3-small");
        when(embeddingProvider.getDimension()).thenReturn(1536);
        when(embeddingProvider.generateEmbeddings(List.of("Chunk 0 text", "Chunk 1 text")))
                .thenReturn(List.of(v0, v1));
        when(embeddingRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<DocumentChunkEmbedding> saved = embeddingService.generateAndSaveEmbeddings(doc, List.of(chunk0, chunk1));

        assertNotNull(saved);
        assertEquals(2, saved.size());
        assertEquals("text-embedding-3-small", saved.get(0).getModelName());
        assertEquals(1536, saved.get(0).getDimension());
        assertEquals(1536, saved.get(1).getDimension());

        // Verify reprocessing safety: old embeddings for document are removed first
        verify(embeddingRepository, times(1)).deleteByDocumentId(10L);
        verify(embeddingRepository, times(1)).saveAll(anyList());
    }

    @Test
    @DisplayName("Should reject and fail when embedding dimension does not match configured dimension")
    void testDimensionMismatchThrowsException() {
        Document doc = Document.builder().id(10L).fileName("DBMS.pdf").build();
        DocumentChunk chunk0 = DocumentChunk.builder().id(101L).document(doc).chunkIndex(0).content("Chunk 0 text").build();

        // Returns 768-dim vector when 1536 is expected
        float[] mismatchedVector = new float[768];

        when(embeddingProvider.getModelName()).thenReturn("text-embedding-3-small");
        when(embeddingProvider.getDimension()).thenReturn(1536);
        when(embeddingProvider.generateEmbeddings(List.of("Chunk 0 text")))
                .thenReturn(List.of(mismatchedVector));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                embeddingService.generateAndSaveEmbeddings(doc, List.of(chunk0))
        );

        assertTrue(ex.getMessage().contains("dimension mismatch"));
        verify(embeddingRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should safely return empty list when no chunks are provided")
    void testEmptyChunksHandling() {
        Document doc = Document.builder().id(10L).fileName("DBMS.pdf").build();
        List<DocumentChunkEmbedding> result = embeddingService.generateAndSaveEmbeddings(doc, List.of());

        assertTrue(result.isEmpty());
        verify(embeddingRepository, times(1)).deleteByDocumentId(10L);
        verify(embeddingRepository, never()).saveAll(anyList());
    }
}
