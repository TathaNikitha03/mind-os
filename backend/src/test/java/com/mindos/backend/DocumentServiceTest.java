
package com.mindos.backend;

import com.mindos.backend.dto.DocumentChunkResponse;
import com.mindos.backend.dto.DocumentContentResponse;
import com.mindos.backend.dto.DocumentResponse;
import com.mindos.backend.entity.*;
import com.mindos.backend.enums.DocumentStatus;
import com.mindos.backend.repository.CategoryRepository;
import com.mindos.backend.repository.DocumentChunkRepository;
import com.mindos.backend.repository.DocumentContentRepository;
import com.mindos.backend.repository.DocumentRepository;
import com.mindos.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DocumentServiceTest {

        @Mock
        private DocumentRepository documentRepository;

        @Mock
        private CategoryRepository categoryRepository;

        @Mock
        private DocumentContentRepository documentContentRepository;

        @Mock
        private DocumentChunkRepository documentChunkRepository;

        @Mock
        private FileStorageService fileStorageService;

        @Mock
        private DocumentTextExtractionService textExtractionService;

        @Mock
        private DocumentChunkingService chunkingService;

        @Mock
        private EmbeddingService embeddingService;

        private DocumentService documentService;

        private User userA;
        private User userB;

        @BeforeEach
        void setUp() {
                documentService = new DocumentService(
                                documentRepository,
                                categoryRepository,
                                documentContentRepository,
                                documentChunkRepository,
                                fileStorageService,
                                textExtractionService,
                                chunkingService,
                                embeddingService);

                userA = User.builder().id(101L).name("User A").email("usera@example.com").build();
                userB = User.builder().id(202L).name("User B").email("userb@example.com").build();
        }

        @Test
        @DisplayName("Should upload file, extract text, chunk document, generate embeddings, and mark as READY")
        void testUploadDocumentFileWithFullPipeline() {
                MockMultipartFile mockFile = new MockMultipartFile(
                                "file", "DBMS Notes.pdf", "application/pdf", "Sample PDF Binary Data".getBytes());

                when(fileStorageService.storeFile(eq(userA.getId()), any())).thenReturn("user_101/stored_dbms.pdf");
                when(textExtractionService.extractText(any(), eq("PDF")))
                                .thenReturn("Database Management Systems: Normalization and Queries.");

                Document savedDoc = Document.builder()
                                .id(1L)
                                .user(userA)
                                .fileName("DBMS Notes.pdf")
                                .title("DBMS Notes")
                                .fileType("PDF")
                                .fileSize((long) "Sample PDF Binary Data".getBytes().length)
                                .fileUrl("user_101/stored_dbms.pdf")
                                .description("DBMS study guide")
                                .status(DocumentStatus.READY)
                                .createdAt(LocalDateTime.now())
                                .build();

                when(documentRepository.save(any(Document.class))).thenReturn(savedDoc);

                DocumentChunk chunk = DocumentChunk.builder().id(10L).chunkIndex(0)
                                .content("Database Management Systems: Normalization and Queries.").build();
                when(chunkingService.processAndSaveChunks(any(), anyString())).thenReturn(List.of(chunk));
                when(embeddingService.generateAndSaveEmbeddings(any(), anyList()))
                                .thenReturn(List.of(DocumentChunkEmbedding.builder().id(100L).chunk(chunk).build()));

                DocumentResponse response = documentService.uploadDocument(userA, mockFile, "DBMS Notes",
                                "DBMS study guide", null);

                assertNotNull(response);
                assertEquals(1L, response.getId());
                assertEquals("DBMS Notes.pdf", response.getFileName());
                assertEquals("PDF", response.getFileType());
                assertEquals("user_101/stored_dbms.pdf", response.getStorageUrl());
                assertEquals("READY", response.getStatus());

                verify(fileStorageService, times(1)).storeFile(eq(userA.getId()), eq(mockFile));
                verify(textExtractionService, times(1)).extractText(any(), eq("PDF"));
                verify(documentContentRepository, times(1)).save(any(DocumentContent.class));
                verify(chunkingService, times(1)).processAndSaveChunks(any(), anyString());
                verify(embeddingService, times(1)).generateAndSaveEmbeddings(any(), anyList());
        }

        @Test
        @DisplayName("Should retrieve document chunks when owned by user")
        void testGetDocumentChunksSuccess() {
                Document doc = Document.builder().id(1L).user(userA).fileName("DBMS Notes.pdf").build();
                DocumentChunk chunk0 = DocumentChunk.builder().id(101L).document(doc).chunkIndex(0)
                                .content("Chunk 0 content").build();
                DocumentChunk chunk1 = DocumentChunk.builder().id(102L).document(doc).chunkIndex(1)
                                .content("Chunk 1 content").build();

                when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
                when(documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(1L))
                                .thenReturn(List.of(chunk0, chunk1));

                List<DocumentChunkResponse> chunks = documentService.getDocumentChunks(userA, 1L);

                assertEquals(2, chunks.size());
                assertEquals(0, chunks.get(0).getChunkIndex());
                assertEquals(1, chunks.get(1).getChunkIndex());
                assertEquals("Chunk 0 content", chunks.get(0).getContent());
        }

        @Test
        @DisplayName("Should throw SecurityException when User B attempts to access User A's document chunks")
        void testGetDocumentChunksUnauthorized() {
                Document userADoc = Document.builder().id(1L).user(userA).fileName("UserA Notes.pdf").build();

                when(documentRepository.findById(1L)).thenReturn(Optional.of(userADoc));

                assertThrows(SecurityException.class, () -> documentService.getDocumentChunks(userB, 1L));
        }

        @Test
        @DisplayName("Should retrieve document content preview, chunk count, and embedding count when owned by user")
        void testGetDocumentContentSuccess() {
                Document doc = Document.builder()
                                .id(1L)
                                .user(userA)
                                .fileName("DBMS Notes.pdf")
                                .title("DBMS Notes")
                                .fileType("PDF")
                                .status(DocumentStatus.READY)
                                .build();

                DocumentContent content = DocumentContent.builder()
                                .id(10L)
                                .document(doc)
                                .extractedText("Extracted text content for test")
                                .build();

                when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
                when(documentContentRepository.findByDocumentId(1L)).thenReturn(Optional.of(content));
                when(documentChunkRepository.countByDocumentId(1L)).thenReturn(3L);
                when(embeddingService.getEmbeddingCountForDocument(1L)).thenReturn(3L);

                DocumentContentResponse response = documentService.getDocumentContent(userA, 1L);

                assertNotNull(response);
                assertEquals("Extracted text content for test", response.getExtractedText());
                assertTrue(response.isHasText());
                assertEquals(5, response.getWordCount());
                assertEquals(3, response.getChunkCount());
                assertEquals(3, response.getEmbeddingCount());
        }

        @Test
        @DisplayName("Should throw SecurityException when User B attempts to access User A's document content")
        void testGetDocumentContentUnauthorized() {
                Document userADoc = Document.builder()
                                .id(1L).user(userA).fileName("UserA Notes.pdf").build();

                when(documentRepository.findById(1L)).thenReturn(Optional.of(userADoc));

                assertThrows(SecurityException.class, () -> documentService.getDocumentContent(userB, 1L));
        }

        @Test
        @DisplayName("Should retrieve only authenticated user's documents")
        void testGetUserDocuments() {
                Document doc1 = Document.builder()
                                .id(1L).user(userA).fileName("DBMS Notes.pdf").title("DBMS Notes").fileType("PDF")
                                .fileSize(1843200L).status(DocumentStatus.READY)
                                .build();
                Document doc2 = Document.builder()
                                .id(2L).user(userA).fileName("Project Architecture.docx").title("Project Architecture")
                                .fileType("DOCX").fileSize(850000L).status(DocumentStatus.READY)
                                .build();

                when(documentRepository.findByUserIdOrderByCreatedAtDesc(userA.getId()))
                                .thenReturn(List.of(doc1, doc2));

                List<DocumentResponse> list = documentService.getUserDocuments(userA);

                assertEquals(2, list.size());
                assertEquals("DBMS Notes.pdf", list.get(0).getFileName());
                assertEquals("Project Architecture.docx", list.get(1).getFileName());
        }

        @Test
        @DisplayName("Should load document file as Resource when owned by user")
        void testGetDocumentFileSuccess() {
                Document doc = Document.builder()
                                .id(1L).user(userA).fileName("DBMS Notes.pdf").fileUrl("user_101/stored_dbms.pdf")
                                .status(DocumentStatus.READY)
                                .build();

                when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
                Resource mockResource = new ByteArrayResource("PDF Content".getBytes());
                when(fileStorageService.loadFileAsResource("user_101/stored_dbms.pdf")).thenReturn(mockResource);

                Resource resource = documentService.getDocumentFile(userA, 1L);
                assertNotNull(resource);
                verify(fileStorageService, times(1)).loadFileAsResource("user_101/stored_dbms.pdf");
        }

        @Test
        @DisplayName("Should delete document from PostgreSQL, storage file, content, chunks, and embeddings")
        void testDeleteDocumentSuccess() {
                Document userADoc = Document.builder()
                                .id(1L).user(userA).fileName("UserA Notes.pdf").fileUrl("user_101/stored.pdf").build();

                when(documentRepository.findById(1L)).thenReturn(Optional.of(userADoc));

                documentService.deleteDocument(userA, 1L);

                verify(embeddingService, times(1)).deleteEmbeddingsByDocument(1L);
                verify(documentChunkRepository, times(1)).deleteByDocumentId(1L);
                verify(documentContentRepository, times(1)).deleteByDocumentId(1L);
                verify(fileStorageService, times(1)).deleteFile("user_101/stored.pdf");
                verify(documentRepository, times(1)).delete(userADoc);
        }
}
