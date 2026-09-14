package com.mindos.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.controller.TagController;
import com.mindos.backend.dto.TagRequest;
import com.mindos.backend.dto.TagResponse;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.TagService;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TagController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TagService tagService;

    @MockBean
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("Test User").email("9876543210@mindos.com").passwordHash("pass").build();
        when(userService.getOrCreateUserByMobileOrId(any(), any())).thenReturn(mockUser);
    }

    @Test
    @DisplayName("GET /api/tags returns user's tag list")
    void testGetTags() throws Exception {
        TagResponse t1 = TagResponse.builder().id(1L).userId(1L).name("java").createdAt(LocalDateTime.now()).taskCount(4).build();
        TagResponse t2 = TagResponse.builder().id(2L).userId(1L).name("springboot").createdAt(LocalDateTime.now()).taskCount(2).build();

        when(tagService.getUserTags(any(User.class))).thenReturn(List.of(t1, t2));

        mockMvc.perform(get("/api/tags")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("java"))
                .andExpect(jsonPath("$[1].name").value("springboot"));
    }

    @Test
    @DisplayName("POST /api/tags creates a new tag")
    void testCreateTag() throws Exception {
        TagRequest request = new TagRequest("backend");
        TagResponse response = TagResponse.builder().id(3L).userId(1L).name("backend").build();

        when(tagService.createTag(any(User.class), any(TagRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/tags")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("backend"));
    }

    @Test
    @DisplayName("PUT /api/tags/{id} updates a tag")
    void testUpdateTag() throws Exception {
        TagRequest request = new TagRequest("spring-boot");
        TagResponse response = TagResponse.builder().id(2L).userId(1L).name("spring-boot").build();

        when(tagService.updateTag(any(User.class), eq(2L), any(TagRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/tags/2")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("spring-boot"));
    }

    @Test
    @DisplayName("DELETE /api/tags/{id} deletes tag")
    void testDeleteTag() throws Exception {
        doNothing().when(tagService).deleteTag(any(User.class), eq(1L));

        mockMvc.perform(delete("/api/tags/1")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Tag deleted successfully"));
    }
}
