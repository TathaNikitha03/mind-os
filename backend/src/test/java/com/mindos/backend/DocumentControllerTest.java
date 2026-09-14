package com.mindos.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.controller.DocumentController;
import com.mindos.backend.dto.DocumentChunkResponse;
import com.mindos.backend.dto.DocumentContentResponse;
import com.mindos.backend.dto.DocumentRequest;
import com.mindos.backend.dto.DocumentResponse;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.DocumentService;
import com.mindos.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DocumentController.class)
@AutoConfigureMockMvc(addFilters = false)
public class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(101L).name("Nikitha").email("9876543210@mindos.com").passwordHash("pass").build();
        when(userService.getOrCreateUserByMobileOrId(any(), any())).thenReturn(mockUser);
    }

    @Test
    @DisplayName("GET /api/documents returns user's document list")
    void testGetDocuments() throws Exception {
        DocumentResponse doc1 = DocumentResponse.builder()
                .id(1L).userId(101L).fileName("DBMS Notes.pdf").title("DBMS Notes").fileType("PDF").fileSize(1843200L).status("READY").uploadedAt(LocalDateTime.now())
                .build();
        DocumentResponse doc2 = DocumentResponse.builder()
                .id(2L).userId(101L).fileName("Project Architecture.docx").title("Project Architecture").fileType("DOCX").fileSize(850000L).status("READY").uploadedAt(LocalDateTime.now())
                .build();

        when(documentService.getUserDocuments(any(User.class))).thenReturn(List.of(doc1, doc2));

        mockMvc.perform(get("/api/documents")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fileName").value("DBMS Notes.pdf"))
                .andExpect(jsonPath("$[1].fileType").value("DOCX"));
    }

    @Test
    @DisplayName("POST /api/documents/upload multipart file upload stores file and returns 201 Created")
    void testUploadMultipartFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "MindOS_Guide.pdf", "application/pdf", "Dummy PDF content".getBytes()
        );

        DocumentResponse response = DocumentResponse.builder()
                .id(10L)
                .userId(101L)
                .fileName("MindOS_Guide.pdf")
                .title("MindOS Guide")
                .fileType("PDF")
                .fileSize((long) "Dummy PDF content".getBytes().length)
                .storageUrl("user_101/stored_guide.pdf")
                .status("READY")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(documentService.uploadDocument(any(User.class), any(), eq("MindOS Guide"), eq("Quick guide"), eq(null)))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .param("title", "MindOS Guide")
                        .param("description", "Quick guide")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.fileName").value("MindOS_Guide.pdf"))
                .andExpect(jsonPath("$.storageUrl").value("user_101/stored_guide.pdf"));
    }

    @Test
    @DisplayName("GET /api/documents/{id}/content returns extracted plain text preview")
    void testGetDocumentContent() throws Exception {
        DocumentContentResponse content = DocumentContentResponse.builder()
                .id(1L)
                .documentId(10L)
                .fileName("DBMS Notes.pdf")
                .extractedText("Database management content sample")
                .charCount(32)
                .wordCount(4)
                .chunkCount(2)
                .hasText(true)
                .status("READY")
                .build();

        when(documentService.getDocumentContent(any(User.class), eq(10L))).thenReturn(content);

        mockMvc.perform(get("/api/documents/10/content")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasText").value(true))
                .andExpect(jsonPath("$.extractedText").value("Database management content sample"))
                .andExpect(jsonPath("$.wordCount").value(4))
                .andExpect(jsonPath("$.chunkCount").value(2));
    }

    @Test
    @DisplayName("GET /api/documents/{id}/chunks returns ordered list of chunks")
    void testGetDocumentChunks() throws Exception {
        DocumentChunkResponse c0 = DocumentChunkResponse.builder().id(1L).documentId(10L).chunkIndex(0).content("Chunk 0 text").wordCount(3).build();
        DocumentChunkResponse c1 = DocumentChunkResponse.builder().id(2L).documentId(10L).chunkIndex(1).content("Chunk 1 text").wordCount(3).build();

        when(documentService.getDocumentChunks(any(User.class), eq(10L))).thenReturn(List.of(c0, c1));

        mockMvc.perform(get("/api/documents/10/chunks")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].chunkIndex").value(0))
                .andExpect(jsonPath("$[1].chunkIndex").value(1));
    }

    @Test
    @DisplayName("POST /api/documents/{id}/extract re-extracts document text and chunks")
    void testReExtractDocumentText() throws Exception {
        DocumentContentResponse content = DocumentContentResponse.builder()
                .id(1L)
                .documentId(10L)
                .fileName("DBMS Notes.pdf")
                .extractedText("Re-extracted document text successfully")
                .chunkCount(2)
                .hasText(true)
                .status("READY")
                .build();

        when(documentService.extractDocumentText(any(User.class), eq(10L))).thenReturn(content);

        mockMvc.perform(post("/api/documents/10/extract")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasText").value(true))
                .andExpect(jsonPath("$.extractedText").value("Re-extracted document text successfully"));
    }

    @Test
    @DisplayName("GET /api/documents/{id}/download downloads file with proper Content-Disposition")
    void testDownloadDocumentFile() throws Exception {
        DocumentResponse doc = DocumentResponse.builder()
                .id(1L)
                .fileName("DBMS_Notes.pdf")
                .fileType("PDF")
                .storageUrl("user_101/stored_dbms.pdf")
                .build();

        Resource resource = new ByteArrayResource("PDF Content Stream".getBytes());

        when(documentService.getDocumentById(any(User.class), eq(1L))).thenReturn(doc);
        when(documentService.getDocumentFile(any(User.class), eq(1L))).thenReturn(resource);

        mockMvc.perform(get("/api/documents/1/download")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"DBMS_Notes.pdf\""))
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().string("PDF Content Stream"));
    }

    @Test
    @DisplayName("DELETE /api/documents/{id} removes document metadata and physical file")
    void testDeleteDocument() throws Exception {
        doNothing().when(documentService).deleteDocument(any(User.class), eq(1L));

        mockMvc.perform(delete("/api/documents/1")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Document deleted successfully"));
    }
}
