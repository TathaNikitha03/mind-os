package com.mindos.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.controller.RagController;
import com.mindos.backend.dto.RagAnswerResponse;
import com.mindos.backend.dto.RagQuestionRequest;
import com.mindos.backend.dto.RagSource;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.RagService;
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

@WebMvcTest(controllers = RagController.class)
@AutoConfigureMockMvc(addFilters = false)
public class RagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RagService ragService;

    @MockBean
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(101L).name("Nikitha").email("9876543210@mindos.com").passwordHash("pass").build();
        when(userService.getOrCreateUserByMobileOrId(any(), any())).thenReturn(mockUser);
    }

    @Test
    @DisplayName("POST /api/knowledge/ask returns HTTP 200 with grounded answer and sources")
    void testAskKnowledgeSuccess() throws Exception {
        RagQuestionRequest request = RagQuestionRequest.builder()
                .question("What is normalization?")
                .topK(5)
                .build();

        RagSource source = RagSource.builder()
                .chunkId(10L)
                .documentId(1L)
                .documentName("DBMS Notes.pdf")
                .chunkIndex(0)
                .similarity(0.92)
                .build();

        RagAnswerResponse response = RagAnswerResponse.builder()
                .question("What is normalization?")
                .answer("Normalization reduces data redundancy in database tables.")
                .sources(List.of(source))
                .hasContext(true)
                .modelName("gpt-4o-mini")
                .build();

        when(ragService.ask(any(User.class), any(RagQuestionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/knowledge/ask")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("What is normalization?"))
                .andExpect(jsonPath("$.hasContext").value(true))
                .andExpect(jsonPath("$.answer").value("Normalization reduces data redundancy in database tables."))
                .andExpect(jsonPath("$.sources[0].documentName").value("DBMS Notes.pdf"))
                .andExpect(jsonPath("$.sources[0].similarity").value(0.92));
    }

    @Test
    @DisplayName("POST /api/knowledge/ask rejects blank question with HTTP 400")
    void testAskKnowledgeBlankQuestion() throws Exception {
        RagQuestionRequest request = RagQuestionRequest.builder()
                .question("   ")
                .build();

        mockMvc.perform(post("/api/knowledge/ask")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
