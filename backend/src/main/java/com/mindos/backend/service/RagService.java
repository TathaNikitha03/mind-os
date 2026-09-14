package com.mindos.backend.service;

import com.mindos.backend.dto.*;
import com.mindos.backend.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    public static final String NO_CONTEXT_MESSAGE = "I couldn't find enough information about this in your knowledge base.";

    private final SemanticSearchService semanticSearchService;
    private final LlmService llmService;

    public RagService(SemanticSearchService semanticSearchService, LlmService llmService) {
        this.semanticSearchService = semanticSearchService;
        this.llmService = llmService;
    }

    /**
     * Complete RAG Q&A Pipeline:
     * 1. Validate user and question.
     * 2. Perform semantic vector retrieval against user's knowledge base.
     * 3. If no relevant chunks exist: return immediate controlled message (NO LLM invocation).
     * 4. If relevant chunks exist: build structured context.
     * 5. Call LLM for grounded answer synthesis.
     * 6. Return grounded answer with verified source citations.
     *
     * @param user    Authenticated user
     * @param request Question request parameters
     * @return Grounded answer response with sources
     */
    public RagAnswerResponse ask(User user, RagQuestionRequest request) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("User must be authenticated.");
        }

        if (request == null || request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            throw new IllegalArgumentException("Question cannot be blank.");
        }

        String rawQuestion = request.getQuestion().trim();
        log.info("[RAG] Question received for user ID {}: '{}'", user.getId(), rawQuestion);

        // 1. Semantic Retrieval
        SemanticSearchRequest searchReq = SemanticSearchRequest.builder()
                .query(rawQuestion)
                .topK(request.getTopK())
                .threshold(request.getThreshold())
                .build();

        SemanticSearchResponse searchResp;
        try {
            searchResp = semanticSearchService.search(user, searchReq);
        } catch (Exception e) {
            log.error("[RAG] Retrieval failed: {}", e.getMessage(), e);
            throw e;
        }
        
        List<SemanticSearchResult> chunks = searchResp.getResults();
        log.info("[RAG] Retrieved chunks: {}", chunks != null ? chunks.size() : 0);

        // 2. No-Context Guard: Do NOT call LLM if no relevant chunks are found
        if (chunks == null || chunks.isEmpty()) {
            log.info("[RAG] No relevant context found. Returning no-context message.");
            return RagAnswerResponse.builder()
                    .question(rawQuestion)
                    .answer(NO_CONTEXT_MESSAGE)
                    .sources(List.of())
                    .hasContext(false)
                    .modelName(llmService.getActiveModelName())
                    .build();
        }

        // 3. Build Context String
        String context = buildContext(chunks);

        // 4. Call LLM Service for Grounded Answer
        log.info("[RAG] Calling LLM provider '{}'", llmService.getActiveModelName());
        String answer;
        try {
            answer = llmService.generateAnswer(rawQuestion, context);
            log.info("[RAG] LLM response status: SUCCESS");
        } catch (Exception e) {
            log.error("[RAG] LLM call failed: {}", e.getMessage(), e);
            throw e;
        }

        log.info("[RAG] Answer generated successfully");

        // 5. Map Source Citations
        List<RagSource> sources = new ArrayList<>(chunks.size());
        for (SemanticSearchResult chunk : chunks) {
            sources.add(RagSource.builder()
                    .documentId(chunk.getDocumentId())
                    .documentName(chunk.getDocumentName())
                    .chunkId(chunk.getChunkId())
                    .chunkIndex(chunk.getChunkIndex())
                    .similarity(chunk.getSimilarity())
                    .build());
        }

        return RagAnswerResponse.builder()
                .question(rawQuestion)
                .answer(answer)
                .sources(sources)
                .hasContext(true)
                .modelName(llmService.getActiveModelName())
                .build();
    }

    /**
     * Formats retrieved chunks into clean, structured context for the LLM.
     */
    public String buildContext(List<SemanticSearchResult> chunks) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            SemanticSearchResult c = chunks.get(i);
            sb.append(String.format("--- SOURCE %d ---\n", i + 1));
            sb.append(String.format("Document: %s (Section %d)\n", c.getDocumentName(), c.getChunkIndex() + 1));
            sb.append(c.getContent()).append("\n\n");
        }
        return sb.toString().trim();
    }
}
