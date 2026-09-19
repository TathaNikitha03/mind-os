package com.mindos.backend;

import com.mindos.backend.dto.CategoryRequest;
import com.mindos.backend.dto.CategoryResponse;
import com.mindos.backend.dto.TaskRequest;
import com.mindos.backend.dto.TaskResponse;
import com.mindos.backend.entity.Category;
import com.mindos.backend.entity.Task;
import com.mindos.backend.entity.User;
import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;
import com.mindos.backend.repository.CategoryRepository;
import com.mindos.backend.repository.TagRepository;
import com.mindos.backend.repository.TaskRepository;
import com.mindos.backend.repository.UserRepository;
import com.mindos.backend.service.CategoryService;
import com.mindos.backend.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private UserRepository userRepository;

    private CategoryService categoryService;
    private TaskService taskService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryService(categoryRepository, taskRepository);
        taskService = new TaskService(taskRepository, categoryRepository, tagRepository, null);

        userA = User.builder().id(1L).name("User A").email("9876543210@mindos.com").passwordHash("pass123").build();
        userB = User.builder().id(2L).name("User B").email("9123456780@mindos.com").passwordHash("pass123").build();
    }

    @Test
    @DisplayName("TEST 1 & 2: User A creates 'Project' and 'Study' categories, verifies both appear")
    void testCreateAndListCategories() {
        Category projectCat = Category.builder().id(101L).user(userA).name("Project").description("Projects").build();
        Category studyCat = Category.builder().id(102L).user(userA).name("Study").description("Studies").build();

        when(categoryRepository.existsByUserIdAndName(userA.getId(), "Project")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(projectCat);

        CategoryResponse res1 = categoryService.createCategory(userA, new CategoryRequest("Project", "Projects"));
        assertNotNull(res1);
        assertEquals("Project", res1.getName());

        when(categoryRepository.existsByUserIdAndName(userA.getId(), "Study")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenReturn(studyCat);

        CategoryResponse res2 = categoryService.createCategory(userA, new CategoryRequest("Study", "Studies"));
        assertNotNull(res2);
        assertEquals("Study", res2.getName());

        when(categoryRepository.findByUserId(userA.getId())).thenReturn(List.of(projectCat, studyCat));
        when(taskRepository.countByCategoryId(101L)).thenReturn(0L);
        when(taskRepository.countByCategoryId(102L)).thenReturn(0L);

        List<CategoryResponse> userCategories = categoryService.getUserCategories(userA);
        assertEquals(2, userCategories.size());
        assertEquals("Project", userCategories.get(0).getName());
        assertEquals("Study", userCategories.get(1).getName());
    }

    @Test
    @DisplayName("Category Rule 1 & 2: Cannot create empty or duplicate category name for same user")
    void testCategoryValidationRules() {
        // Empty name
        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(userA, new CategoryRequest("  ", "Description"));
        });

        // Duplicate name
        when(categoryRepository.existsByUserIdAndName(userA.getId(), "Study")).thenReturn(true);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(userA, new CategoryRequest("Study", "Another Study"));
        });
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    @DisplayName("TEST 3 & 4: Create Task under 'Project' category and verify mapping")
    void testCreateTaskWithCategory() {
        Category projectCat = Category.builder().id(101L).user(userA).name("Project").build();
        when(categoryRepository.findByUserIdAndName(userA.getId(), "Project")).thenReturn(Optional.of(projectCat));

        Task savedTask = Task.builder()
                .id(201L)
                .user(userA)
                .category(projectCat)
                .title("Build Spring Boot API")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .dueDate(LocalDateTime.now().plusDays(1))
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskRequest taskReq = new TaskRequest();
        taskReq.setTitle("Build Spring Boot API");
        taskReq.setCategory("Project");
        taskReq.setPriority(TaskPriority.HIGH);

        TaskResponse taskRes = taskService.createTask(userA, taskReq);
        assertEquals("Build Spring Boot API", taskRes.getTitle());
        assertEquals("Project", taskRes.getCategory());
    }

    @Test
    @DisplayName("TEST 5: Edit Task Category from 'Project' to 'Study'")
    void testEditTaskCategory() {
        Category projectCat = Category.builder().id(101L).user(userA).name("Project").build();
        Category studyCat = Category.builder().id(102L).user(userA).name("Study").build();

        Task existingTask = Task.builder()
                .id(201L)
                .user(userA)
                .category(projectCat)
                .title("Build Spring Boot API")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .dueDate(LocalDateTime.now())
                .build();

        when(taskRepository.findById(201L)).thenReturn(Optional.of(existingTask));
        when(categoryRepository.findByUserIdAndName(userA.getId(), "Study")).thenReturn(Optional.of(studyCat));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TaskRequest updateReq = new TaskRequest();
        updateReq.setTitle("Build Spring Boot API");
        updateReq.setCategory("Study");
        updateReq.setPriority(TaskPriority.HIGH);

        TaskResponse updated = taskService.updateTask(userA, 201L, updateReq);
        assertEquals("Study", updated.getCategory());
    }

    @Test
    @DisplayName("TEST 6: Edit Category Name to 'Software Project'")
    void testEditCategoryName() {
        Category projectCat = Category.builder().id(101L).user(userA).name("Project").build();
        when(categoryRepository.findById(101L)).thenReturn(Optional.of(projectCat));
        when(categoryRepository.findByUserIdAndName(userA.getId(), "Software Project")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));

        CategoryResponse updatedCat = categoryService.updateCategory(userA, 101L, new CategoryRequest("Software Project", "Updated description"));
        assertEquals("Software Project", updatedCat.getName());
    }

    @Test
    @DisplayName("TEST 7: Safe Delete - Cannot delete category in use by tasks and tasks are NOT deleted")
    void testDeleteCategoryInUse() {
        Category projectCat = Category.builder().id(101L).user(userA).name("Project").build();
        when(categoryRepository.findById(101L)).thenReturn(Optional.of(projectCat));
        when(taskRepository.countByCategoryId(101L)).thenReturn(2L);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(userA, 101L);
        });

        assertTrue(ex.getMessage().contains("Cannot delete category 'Project' because it is currently assigned to 2 task(s)"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("TEST 8: User isolation - User B cannot modify or delete User A's category")
    void testUserIsolation() {
        Category projectCat = Category.builder().id(101L).user(userA).name("Project").build();
        when(categoryRepository.findById(101L)).thenReturn(Optional.of(projectCat));

        // User B tries to update User A's category
        assertThrows(SecurityException.class, () -> {
            categoryService.updateCategory(userB, 101L, new CategoryRequest("Hacked Project", "desc"));
        });

        // User B tries to delete User A's category
        assertThrows(SecurityException.class, () -> {
            categoryService.deleteCategory(userB, 101L);
        });

        verify(categoryRepository, never()).delete(any());
    }
}
