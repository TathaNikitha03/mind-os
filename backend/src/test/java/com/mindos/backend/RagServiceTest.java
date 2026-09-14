package com.mindos.backend;

import com.mindos.backend.dto.*;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.LlmService;
import com.mindos.backend.service.RagService;
import com.mindos.backend.service.SemanticSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RagServiceTest {

    @Mock
    private SemanticSearchService semanticSearchService;

    @Mock
    private LlmService llmService;

    private RagService ragService;
    private User testUser;

    @BeforeEach
    void setUp() {
        ragService = new RagService(semanticSearchService, llmService);
        testUser = User.builder().id(101L).name("Nikitha").email("nikitha@mindos.com").build();
    }

    @Test
    @DisplayName("Should retrieve relevant chunks, call LLM, and return grounded answer with sources")
    void testGroundedRagAnswer() {
        SemanticSearchResult chunk1 = SemanticSearchResult.builder()
                .chunkId(10L)
                .documentId(1L)
                .documentName("DBMS Notes.pdf")
                .chunkIndex(0)
                .content("Database Normalization organizes tables to reduce redundancy.")
                .similarity(0.92)
                .build();

        SemanticSearchResult chunk2 = SemanticSearchResult.builder()
                .chunkId(11L)
                .documentId(1L)
                .documentName("DBMS Notes.pdf")
                .chunkIndex(1)
                .content("First Normal Form eliminates repeating groups.")
                .similarity(0.85)
                .build();

        SemanticSearchResponse searchResponse = SemanticSearchResponse.builder()
                .query("What is normalization?")
                .results(List.of(chunk1, chunk2))
                .totalResults(2)
                .modelName("text-embedding-3-small")
                .build();

        when(semanticSearchService.search(eq(testUser), any(SemanticSearchRequest.class)))
                .thenReturn(searchResponse);

        when(llmService.generateAnswer(eq("What is normalization?"), anyString()))
                .thenReturn("Normalization is a database technique that organizes tables to reduce redundancy and eliminates repeating groups.");

        when(llmService.getActiveModelName()).thenReturn("local-grounded-synthesis");

        RagQuestionRequest req = RagQuestionRequest.builder()
                .question("What is normalization?")
                .topK(5)
                .build();

        RagAnswerResponse response = ragService.ask(testUser, req);

        assertNotNull(response);
        assertEquals("What is normalization?", response.getQuestion());
        assertTrue(response.isHasContext());
        assertTrue(response.getAnswer().contains("Normalization"));
        assertEquals(2, response.getSources().size());
        assertEquals(10L, response.getSources().get(0).getChunkId());
        assertEquals("DBMS Notes.pdf", response.getSources().get(0).getDocumentName());
        assertEquals(0.92, response.getSources().get(0).getSimilarity());

        // Verify LLM service was called with formatted context
        verify(llmService, times(1)).generateAnswer(eq("What is normalization?"), anyString());
    }

    @Test
    @DisplayName("Should NOT call LLM when no relevant context is retrieved and return controlled no-context message")
    void testNoContextDoesNotCallLlm() {
        SemanticSearchResponse emptySearch = SemanticSearchResponse.builder()
                .query("What are the symptoms of influenza?")
                .results(List.of())
                .totalResults(0)
                .modelName("text-embedding-3-small")
                .build();

        when(semanticSearchService.search(eq(testUser), any(SemanticSearchRequest.class)))
                .thenReturn(emptySearch);

        when(llmService.getActiveModelName()).thenReturn("local-grounded-synthesis");

        RagQuestionRequest req = RagQuestionRequest.builder()
                .question("What are the symptoms of influenza?")
                .build();

        RagAnswerResponse response = ragService.ask(testUser, req);

        assertNotNull(response);
        assertFalse(response.isHasContext());
        assertEquals(RagService.NO_CONTEXT_MESSAGE, response.getAnswer());
        assertTrue(response.getSources().isEmpty());

        // CRITICAL CHECK: LLM was NEVER called
        verify(llmService, never()).generateAnswer(anyString(), anyString());
    }

    @Test
    @DisplayName("Should reject blank question with IllegalArgumentException")
    void testBlankQuestionRejection() {
        RagQuestionRequest req = RagQuestionRequest.builder().question("   ").build();

        assertThrows(IllegalArgumentException.class, () ->
                ragService.ask(testUser, req)
        );
    }
}
