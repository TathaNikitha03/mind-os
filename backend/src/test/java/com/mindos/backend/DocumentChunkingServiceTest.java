package com.mindos.backend;

import com.mindos.backend.entity.Document;
import com.mindos.backend.entity.DocumentChunk;
import com.mindos.backend.repository.DocumentChunkRepository;
import com.mindos.backend.service.DocumentChunkingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentChunkingServiceTest {

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    private DocumentChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        chunkingService = new DocumentChunkingService(documentChunkRepository);
    }

    @Test
    @DisplayName("Should create single chunk (index 0) for short document text")
    void testShortDocumentChunking() {
        String shortText = "Database Management Systems (DBMS) organize and query data efficiently.";
        List<String> chunks = chunkingService.chunkText(shortText, 600, 120);

        assertEquals(1, chunks.size());
        assertEquals(shortText, chunks.get(0));
    }

    @Test
    @DisplayName("Should create multiple sequential overlapping chunks for long document text")
    void testLongDocumentChunkingWithOverlap() {
        // Generate a 1500-word text string
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 1500; i++) {
            sb.append("Word").append(i).append(" ");
        }
        String longText = sb.toString().trim();

        // 500 words per chunk with 100 words overlap -> step = 400 words
        List<String> chunks = chunkingService.chunkText(longText, 500, 100);

        assertTrue(chunks.size() >= 3);

        // Check chunk 0
        String chunk0 = chunks.get(0);
        assertTrue(chunk0.startsWith("Word1 "));
        assertTrue(chunk0.contains("Word500"));

        // Check chunk 1 starts with overlapping words from chunk 0 (starts at word 401)
        String chunk1 = chunks.get(1);
        assertTrue(chunk1.startsWith("Word401 "));
        assertTrue(chunk1.contains("Word500")); // verifies overlap with chunk 0

        // Check chunk 2 starts at word 801
        String chunk2 = chunks.get(2);
        assertTrue(chunk2.startsWith("Word801 "));
        assertTrue(chunk2.contains("Word900")); // verifies overlap with chunk 1
    }

    @Test
    @DisplayName("Should return empty list for null or whitespace text")
    void testEmptyTextChunking() {
        assertTrue(chunkingService.chunkText(null).isEmpty());
        assertTrue(chunkingService.chunkText("   \n\n  ").isEmpty());
    }

    @Test
    @DisplayName("Should save sequential chunks to PostgreSQL and delete old chunks on reprocessing")
    void testProcessAndSaveChunksReprocessing() {
        Document doc = Document.builder().id(10L).fileName("DBMS.pdf").build();

        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 1200; i++) {
            sb.append("Term").append(i).append(" ");
        }

        when(documentChunkRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<DocumentChunk> saved = chunkingService.processAndSaveChunks(doc, sb.toString());

        assertNotNull(saved);
        assertTrue(saved.size() >= 2);

        // Verify sequential chunkIndex starting from 0
        for (int i = 0; i < saved.size(); i++) {
            assertEquals(i, saved.get(i).getChunkIndex());
            assertEquals(doc, saved.get(i).getDocument());
            assertNotNull(saved.get(i).getContent());
            assertTrue(saved.get(i).getWordCount() > 0);
        }

        // Verify old chunks are deleted prior to saving (reprocessing safety)
        verify(documentChunkRepository, times(1)).deleteByDocumentId(10L);
        verify(documentChunkRepository, times(1)).saveAll(anyList());
    }
}
