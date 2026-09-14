package com.mindos.backend.service;

import com.mindos.backend.entity.Document;
import com.mindos.backend.entity.DocumentChunk;
import com.mindos.backend.repository.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentChunkingService {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunkingService.class);

    // Target chunking parameters: 500-800 words with 100-150 words overlap
    public static final int TARGET_CHUNK_WORDS = 600;
    public static final int CHUNK_OVERLAP_WORDS = 120;

    private final DocumentChunkRepository documentChunkRepository;

    public DocumentChunkingService(DocumentChunkRepository documentChunkRepository) {
        this.documentChunkRepository = documentChunkRepository;
    }

    /**
     * Chunk extracted text and persist sequential overlapping chunks into PostgreSQL.
     * Idempotent & reprocessing safe (cleans prior chunks before inserting new ones).
     */
    @Transactional
    public List<DocumentChunk> processAndSaveChunks(Document document, String extractedText) {
        if (document == null || document.getId() == null) {
            throw new IllegalArgumentException("Valid document entity is required for chunking.");
        }

        // Reprocessing safety: Delete existing chunks for this document
        documentChunkRepository.deleteByDocumentId(document.getId());

        if (extractedText == null || extractedText.trim().isEmpty()) {
            log.warn("Cannot create chunks for document ID {}: extracted text is empty.", document.getId());
            return List.of();
        }

        List<String> rawChunks = chunkText(extractedText, TARGET_CHUNK_WORDS, CHUNK_OVERLAP_WORDS);
        if (rawChunks.isEmpty()) {
            log.warn("No chunks generated for document ID {}", document.getId());
            return List.of();
        }

        List<DocumentChunk> entities = new ArrayList<>();
        for (int i = 0; i < rawChunks.size(); i++) {
            String chunkContent = rawChunks.get(i);
            DocumentChunk chunk = DocumentChunk.builder()
                    .document(document)
                    .chunkIndex(i)
                    .content(chunkContent)
                    .build();
            entities.add(chunk);
        }

        List<DocumentChunk> saved = documentChunkRepository.saveAll(entities);
        log.info("Successfully created {} chunks for document ID: {}", saved.size(), document.getId());
        return saved;
    }

    /**
     * Deterministic, word-boundary preserving chunking algorithm with overlap.
     *
     * @param text         Clean extracted plain text
     * @param targetWords  Target words per chunk (~500-800 words)
     * @param overlapWords Overlapping words between consecutive chunks (~100-150 words)
     * @return Ordered list of chunk strings
     */
    public List<String> chunkText(String text, int targetWords, int overlapWords) {
        if (text == null || text.trim().isEmpty()) {
            return List.of();
        }

        String cleaned = text.trim();
        String[] words = cleaned.split("\\s+");

        if (words.length == 0) {
            return List.of();
        }

        // If total document words are less than or equal to target, return single chunk (chunk 0)
        if (words.length <= targetWords) {
            return List.of(cleaned);
        }

        List<String> chunks = new ArrayList<>();
        int step = Math.max(1, targetWords - overlapWords);
        int startIndex = 0;

        while (startIndex < words.length) {
            int endIndex = Math.min(startIndex + targetWords, words.length);

            // Reconstruct the slice of words
            StringBuilder chunkBuilder = new StringBuilder();
            for (int i = startIndex; i < endIndex; i++) {
                chunkBuilder.append(words[i]);
                if (i < endIndex - 1) {
                    chunkBuilder.append(" ");
                }
            }

            String chunkStr = chunkBuilder.toString().trim();
            if (!chunkStr.isEmpty()) {
                chunks.add(chunkStr);
            }

            if (endIndex >= words.length) {
                break;
            }

            startIndex += step;
        }

        return chunks;
    }

    /**
     * Helper overload with default parameters
     */
    public List<String> chunkText(String text) {
        return chunkText(text, TARGET_CHUNK_WORDS, CHUNK_OVERLAP_WORDS);
    }
}
