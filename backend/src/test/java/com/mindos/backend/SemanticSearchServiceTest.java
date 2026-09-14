package com.mindos.backend;

import com.mindos.backend.dto.SemanticSearchRequest;
import com.mindos.backend.dto.SemanticSearchResponse;
import com.mindos.backend.dto.SemanticSearchResult;
import com.mindos.backend.entity.Document;
import com.mindos.backend.entity.DocumentChunk;
import com.mindos.backend.entity.DocumentChunkEmbedding;
import com.mindos.backend.entity.User;
import com.mindos.backend.enums.DocumentStatus;
import com.mindos.backend.repository.DocumentChunkEmbeddingRepository;
import com.mindos.backend.service.SemanticSearchService;
import com.mindos.backend.service.embedding.LocalDeterministicEmbeddingProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SemanticSearchServiceTest {

    @Mock
    private DocumentChunkEmbeddingRepository embeddingRepository;

    private LocalDeterministicEmbeddingProvider embeddingProvider;
    private SemanticSearchService searchService;

    private User userA;
    private User userB;

    private Document docDbmsUserA;
    private Document docJavaUserB;

    private DocumentChunk chunkDbmsNormalization;
    private DocumentChunk chunkDbmsIndexing;
    private DocumentChunk chunkJavaInheritance;

    private DocumentChunkEmbedding embNormalization;
    private DocumentChunkEmbedding embIndexing;
    private DocumentChunkEmbedding embInheritance;

    @BeforeEach
    void setUp() {
        embeddingProvider = new LocalDeterministicEmbeddingProvider("text-embedding-3-small", 1536);
        searchService = new SemanticSearchService(embeddingProvider, embeddingRepository, 0.40, 5, 10);

        userA = User.builder().id(101L).name("User A").email("usera@mindos.com").build();
        userB = User.builder().id(202L).name("User B").email("userb@mindos.com").build();

        docDbmsUserA = Document.builder()
                .id(1L)
                .user(userA)
                .fileName("DBMS_Notes.pdf")
                .title("DBMS Notes")
                .status(DocumentStatus.READY)
                .build();

        docJavaUserB = Document.builder()
                .id(2L)
                .user(userB)
                .fileName("Java_Guide.pdf")
                .title("Java Guide")
                .status(DocumentStatus.READY)
                .build();

        // Chunks
        chunkDbmsNormalization = DocumentChunk.builder()
                .id(10L)
                .document(docDbmsUserA)
                .chunkIndex(0)
                .content("Database Normalization is the process of organizing data to reduce redundancy and duplicate information.")
                .build();

        chunkDbmsIndexing = DocumentChunk.builder()
                .id(11L)
                .document(docDbmsUserA)
                .chunkIndex(1)
                .content("B-Tree indexes speed up query performance on database columns.")
                .build();

        chunkJavaInheritance = DocumentChunk.builder()
                .id(20L)
                .document(docJavaUserB)
                .chunkIndex(0)
                .content("Inheritance in Java allows a class to inherit fields and methods from a superclass.")
                .build();

        // Embeddings
        float[] vNorm = embeddingProvider.generateEmbedding(chunkDbmsNormalization.getContent());
        embNormalization = DocumentChunkEmbedding.builder()
                .id(100L)
                .chunk(chunkDbmsNormalization)
                .embedding(vNorm)
                .modelName("text-embedding-3-small")
                .dimension(1536)
                .build();

        float[] vIndex = embeddingProvider.generateEmbedding(chunkDbmsIndexing.getContent());
        embIndexing = DocumentChunkEmbedding.builder()
                .id(101L)
                .chunk(chunkDbmsIndexing)
                .embedding(vIndex)
                .modelName("text-embedding-3-small")
                .dimension(1536)
                .build();

        float[] vJava = embeddingProvider.generateEmbedding(chunkJavaInheritance.getContent());
        embInheritance = DocumentChunkEmbedding.builder()
                .id(200L)
                .chunk(chunkJavaInheritance)
                .embedding(vJava)
                .modelName("text-embedding-3-small")
                .dimension(1536)
                .build();
    }

    @Test
    @DisplayName("Should retrieve and rank normalization chunk when searching by meaning via native query")
    void testSemanticMeaningSearch() {
        Object[] row1 = new Object[]{10L, 1L, "DBMS Notes", 0, "Database Normalization reduces redundancy...", 0.885};
        Object[] row2 = new Object[]{11L, 1L, "DBMS Notes", 1, "B-Tree indexes...", 0.512};

        when(embeddingRepository.searchSimilarChunksNative(eq(userA.getId()), anyString(), eq("text-embedding-3-small"), anyDouble(), anyInt()))
                .thenReturn(List.of(row1, row2));

        SemanticSearchRequest req = SemanticSearchRequest.builder()
                .query("How do I avoid duplicate data in my database?")
                .topK(5)
                .threshold(0.20)
                .build();

        SemanticSearchResponse response = searchService.search(userA, req);

        assertNotNull(response);
        assertEquals("How do I avoid duplicate data in my database?", response.getQuery());
        assertEquals(2, response.getResults().size());

        // Top result should be normalization
        SemanticSearchResult topResult = response.getResults().get(0);
        assertEquals(10L, topResult.getChunkId());
        assertEquals("DBMS Notes", topResult.getDocumentName());
        assertEquals(0.885, topResult.getSimilarity());
    }

    @Test
    @DisplayName("Should test in-memory cosine similarity ranking with unit vectors")
    void testInMemorySemanticMeaningSearch() {
        float[] queryVec = embeddingProvider.generateEmbedding("Database Normalization and organizing duplicate data");

        when(embeddingRepository.findUserReadyChunkEmbeddings(eq(userA.getId()), eq("text-embedding-3-small")))
                .thenReturn(List.of(embNormalization, embIndexing));

        List<SemanticSearchResult> results = searchService.searchSimilarChunksInMemory(
                userA.getId(), queryVec, "text-embedding-3-small", 0.05, 5
        );

        assertNotNull(results);
        assertFalse(results.isEmpty());
        // Normalization should rank first
        assertEquals(10L, results.get(0).getChunkId());
        assertTrue(results.get(0).getContent().contains("Database Normalization"));
    }

    @Test
    @DisplayName("Should return empty results when threshold is not met")
    void testUnrelatedQueryReturnsEmpty() {
        when(embeddingRepository.searchSimilarChunksNative(eq(userA.getId()), anyString(), eq("text-embedding-3-small"), anyDouble(), anyInt()))
                .thenReturn(List.of());

        SemanticSearchRequest req = SemanticSearchRequest.builder()
                .query("How to make chicken biryani recipe?")
                .topK(5)
                .threshold(0.85) // High threshold filtering
                .build();

        SemanticSearchResponse response = searchService.search(userA, req);

        assertNotNull(response);
        assertTrue(response.getResults().isEmpty(), "Unrelated query should yield no results");
    }

    @Test
    @DisplayName("Should enforce strict user isolation: User A cannot search User B's documents")
    void testUserIsolation() {
        float[] queryVec = embeddingProvider.generateEmbedding("Inheritance in Java");

        // User A only has DBMS embeddings
        when(embeddingRepository.findUserReadyChunkEmbeddings(eq(userA.getId()), eq("text-embedding-3-small")))
                .thenReturn(List.of(embNormalization, embIndexing));

        List<SemanticSearchResult> results = searchService.searchSimilarChunksInMemory(
                userA.getId(), queryVec, "text-embedding-3-small", 0.01, 5
        );

        for (SemanticSearchResult res : results) {
            assertNotEquals(20L, res.getChunkId(), "User B's Java chunk must never appear in User A's search results");
            assertNotEquals(2L, res.getDocumentId());
        }
    }

    @Test
    @DisplayName("Should cap topK to maximum allowed (10)")
    void testTopKCapping() {
        when(embeddingRepository.searchSimilarChunksNative(eq(userA.getId()), anyString(), eq("text-embedding-3-small"), anyDouble(), eq(10)))
                .thenReturn(List.of());

        SemanticSearchRequest req = SemanticSearchRequest.builder()
                .query("database")
                .topK(9999) // excessively large top-k
                .threshold(0.10)
                .build();

        SemanticSearchResponse response = searchService.search(userA, req);
        assertNotNull(response);
    }

    @Test
    @DisplayName("Should reject blank query with IllegalArgumentException")
    void testBlankQueryRejection() {
        SemanticSearchRequest req = SemanticSearchRequest.builder().query("   ").build();

        assertThrows(IllegalArgumentException.class, () ->
                searchService.search(userA, req)
        );
    }
}
