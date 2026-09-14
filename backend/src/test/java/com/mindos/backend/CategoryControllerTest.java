package com.mindos.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.controller.CategoryController;
import com.mindos.backend.dto.CategoryRequest;
import com.mindos.backend.dto.CategoryResponse;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.CategoryService;
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

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
public class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @MockBean
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(1L).name("Test User").email("9876543210@mindos.com").passwordHash("pass").build();
        when(userService.getOrCreateUserByMobileOrId(any(), any())).thenReturn(mockUser);
    }

    @Test
    @DisplayName("GET /api/categories returns user's category list")
    void testGetCategories() throws Exception {
        CategoryResponse cat1 = CategoryResponse.builder().id(1L).userId(1L).name("Project").description("Software projects").createdAt(LocalDateTime.now()).taskCount(3).build();
        CategoryResponse cat2 = CategoryResponse.builder().id(2L).userId(1L).name("Study").description("Exam study").createdAt(LocalDateTime.now()).taskCount(1).build();

        when(categoryService.getUserCategories(any(User.class))).thenReturn(List.of(cat1, cat2));

        mockMvc.perform(get("/api/categories")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Project"))
                .andExpect(jsonPath("$[1].name").value("Study"));
    }

    @Test
    @DisplayName("POST /api/categories creates new category")
    void testCreateCategory() throws Exception {
        CategoryRequest request = new CategoryRequest("Placement", "Campus placement preparation");
        CategoryResponse response = CategoryResponse.builder().id(3L).userId(1L).name("Placement").description("Campus placement preparation").build();

        when(categoryService.createCategory(any(User.class), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/categories")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Placement"));
    }

    @Test
    @DisplayName("PUT /api/categories/{id} updates category")
    void testUpdateCategory() throws Exception {
        CategoryRequest request = new CategoryRequest("Software Project", "Updated description");
        CategoryResponse response = CategoryResponse.builder().id(1L).userId(1L).name("Software Project").description("Updated description").build();

        when(categoryService.updateCategory(any(User.class), eq(1L), any(CategoryRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/categories/1")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Software Project"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} safely deletes unused category")
    void testDeleteCategorySuccess() throws Exception {
        doNothing().when(categoryService).deleteCategory(any(User.class), eq(1L));

        mockMvc.perform(delete("/api/categories/1")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Category deleted successfully"));
    }

    @Test
    @DisplayName("DELETE /api/categories/{id} returns 409 Conflict when category has tasks")
    void testDeleteCategoryInUseConflict() throws Exception {
        doThrow(new IllegalStateException("Cannot delete category 'Project' because it is currently assigned to 2 task(s). Please reassign or update those tasks first."))
                .when(categoryService).deleteCategory(any(User.class), eq(1L));

        mockMvc.perform(delete("/api/categories/1")
                .header("X-User-Mobile", "9876543210")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Cannot delete category 'Project' because it is currently assigned to 2 task(s). Please reassign or update those tasks first."));
    }
}
