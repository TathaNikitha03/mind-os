package com.mindos.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.controller.KnowledgeSearchController;
import com.mindos.backend.dto.SemanticSearchRequest;
import com.mindos.backend.dto.SemanticSearchResponse;
import com.mindos.backend.dto.SemanticSearchResult;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.SemanticSearchService;
import com.mindos.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = KnowledgeSearchController.class)
@AutoConfigureMockMvc(addFilters = false)
public class KnowledgeSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SemanticSearchService semanticSearchService;

    @MockBean
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(101L).name("Nikitha").email("9876543210@mindos.com").passwordHash("pass").build();
        when(userService.getOrCreateUserByMobileOrId(any(), any())).thenReturn(mockUser);
    }

    @Test
    @DisplayName("POST /api/knowledge/search returns ranked semantic search results")
    void testSemanticSearchEndpointSuccess() throws Exception {
        SemanticSearchRequest request = SemanticSearchRequest.builder()
                .query("How to reduce duplicate data?")
                .topK(5)
                .build();

        SemanticSearchResult res = SemanticSearchResult.builder()
                .chunkId(12L)
                .documentId(4L)
                .documentName("DBMS Notes.pdf")
                .chunkIndex(2)
                .content("Database Normalization reduces redundancy...")
                .similarity(0.89)
                .build();

        SemanticSearchResponse response = SemanticSearchResponse.builder()
                .query("How to reduce duplicate data?")
                .results(List.of(res))
                .totalResults(1)
                .modelName("text-embedding-3-small")
                .build();

        when(semanticSearchService.search(any(User.class), any(SemanticSearchRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/knowledge/search")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("How to reduce duplicate data?"))
                .andExpect(jsonPath("$.totalResults").value(1))
                .andExpect(jsonPath("$.results[0].chunkId").value(12))
                .andExpect(jsonPath("$.results[0].documentName").value("DBMS Notes.pdf"))
                .andExpect(jsonPath("$.results[0].similarity").value(0.89));
    }

    @Test
    @DisplayName("POST /api/knowledge/search rejects blank query with HTTP 400")
    void testSemanticSearchBlankQuery() throws Exception {
        SemanticSearchRequest request = SemanticSearchRequest.builder()
                .query("   ")
                .build();

        mockMvc.perform(post("/api/knowledge/search")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
