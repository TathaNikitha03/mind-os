package com.mindos.backend;

import com.mindos.backend.dto.TagRequest;
import com.mindos.backend.dto.TagResponse;
import com.mindos.backend.dto.TaskRequest;
import com.mindos.backend.dto.TaskResponse;
import com.mindos.backend.entity.Category;
import com.mindos.backend.entity.Tag;
import com.mindos.backend.entity.Task;
import com.mindos.backend.entity.User;
import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;
import com.mindos.backend.repository.CategoryRepository;
import com.mindos.backend.repository.TagRepository;
import com.mindos.backend.repository.TaskRepository;
import com.mindos.backend.service.TagService;
import com.mindos.backend.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CategoryRepository categoryRepository;

    private TagService tagService;
    private TaskService taskService;

    private User userA;
    private User userB;

    @BeforeEach
    void setUp() {
        tagService = new TagService(tagRepository, taskRepository);
        taskService = new TaskService(taskRepository, categoryRepository, tagRepository, null);

        userA = User.builder().id(1L).name("User A").email("9876543210@mindos.com").passwordHash("pass123").build();
        userB = User.builder().id(2L).name("User B").email("9123456780@mindos.com").passwordHash("pass123").build();
    }

    @Test
    @DisplayName("TEST 1: User A creates 'java', 'backend', 'springboot' tags")
    void testCreateAndListTags() {
        Tag tag1 = Tag.builder().id(1L).user(userA).name("java").build();
        Tag tag2 = Tag.builder().id(2L).user(userA).name("backend").build();
        Tag tag3 = Tag.builder().id(3L).user(userA).name("springboot").build();

        when(tagRepository.existsByUserIdAndName(userA.getId(), "java")).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(tag1);
        TagResponse res1 = tagService.createTag(userA, new TagRequest("java"));
        assertEquals("java", res1.getName());

        when(tagRepository.findByUserIdOrderByNameAsc(userA.getId())).thenReturn(List.of(tag2, tag1, tag3));
        List<TagResponse> userTags = tagService.getUserTags(userA);
        assertEquals(3, userTags.size());
        assertEquals("backend", userTags.get(0).getName());
        assertEquals("java", userTags.get(1).getName());
        assertEquals("springboot", userTags.get(2).getName());
    }

    @Test
    @DisplayName("Tag Normalization: 'Java', 'JAVA', '#java' normalized to 'java' and no duplicates created")
    void testTagNormalizationAndDeduplication() {
        assertEquals("java", TagService.normalizeTagName("Java"));
        assertEquals("java", TagService.normalizeTagName(" JAVA "));
        assertEquals("java", TagService.normalizeTagName("###java"));
        assertEquals("springboot", TagService.normalizeTagName("#SpringBoot"));

        // Blank tag check
        assertThrows(IllegalArgumentException.class, () -> {
            tagService.createTag(userA, new TagRequest("  ###  "));
        });

        // Duplicate tag check for same user
        when(tagRepository.existsByUserIdAndName(userA.getId(), "java")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> {
            tagService.createTag(userA, new TagRequest("JAVA"));
        });
    }

    @Test
    @DisplayName("TEST 2 & 3: Create Task with multiple tags (java, backend, springboot)")
    void testCreateTaskWithMultipleTags() {
        Category projectCat = Category.builder().id(10L).user(userA).name("Project").build();
        when(categoryRepository.findByUserIdAndName(userA.getId(), "Project")).thenReturn(Optional.of(projectCat));

        Tag javaTag = Tag.builder().id(1L).user(userA).name("java").build();
        Tag backendTag = Tag.builder().id(2L).user(userA).name("backend").build();
        Tag sbTag = Tag.builder().id(3L).user(userA).name("springboot").build();

        when(tagRepository.findByUserIdAndName(userA.getId(), "java")).thenReturn(Optional.of(javaTag));
        when(tagRepository.findByUserIdAndName(userA.getId(), "backend")).thenReturn(Optional.of(backendTag));
        when(tagRepository.findByUserIdAndName(userA.getId(), "springboot")).thenReturn(Optional.of(sbTag));

        Task savedTask = Task.builder()
                .id(100L)
                .user(userA)
                .category(projectCat)
                .title("Build Spring Boot API")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .tags(new ArrayList<>(List.of(javaTag, backendTag, sbTag)))
                .build();

        when(taskRepository.save(any(Task.class))).thenReturn(savedTask);

        TaskRequest request = new TaskRequest();
        request.setTitle("Build Spring Boot API");
        request.setCategory("Project");
        request.setPriority(TaskPriority.HIGH);
        request.setTags(List.of("java", "backend", "springboot"));

        TaskResponse res = taskService.createTask(userA, request);
        assertNotNull(res);
        assertEquals("Build Spring Boot API", res.getTitle());
        assertEquals("Project", res.getCategory());
        assertEquals(3, res.getTags().size());
        assertTrue(res.getTags().contains("java"));
        assertTrue(res.getTags().contains("backend"));
        assertTrue(res.getTags().contains("springboot"));
    }

    @Test
    @DisplayName("TEST 4: Edit Task ??? Remove 'backend' and add 'api'")
    void testEditTaskTags() {
        Category projectCat = Category.builder().id(10L).user(userA).name("Project").build();
        Tag javaTag = Tag.builder().id(1L).user(userA).name("java").build();
        Tag backendTag = Tag.builder().id(2L).user(userA).name("backend").build();
        Tag sbTag = Tag.builder().id(3L).user(userA).name("springboot").build();
        Tag apiTag = Tag.builder().id(4L).user(userA).name("api").build();

        Task existingTask = Task.builder()
                .id(100L)
                .user(userA)
                .category(projectCat)
                .title("Build Spring Boot API")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.TODO)
                .tags(new ArrayList<>(List.of(javaTag, backendTag, sbTag)))
                .build();

        when(taskRepository.findById(100L)).thenReturn(Optional.of(existingTask));
        when(categoryRepository.findByUserIdAndName(userA.getId(), "Project")).thenReturn(Optional.of(projectCat));
        when(tagRepository.findByUserIdAndName(userA.getId(), "java")).thenReturn(Optional.of(javaTag));
        when(tagRepository.findByUserIdAndName(userA.getId(), "springboot")).thenReturn(Optional.of(sbTag));
        when(tagRepository.findByUserIdAndName(userA.getId(), "api")).thenReturn(Optional.of(apiTag));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        TaskRequest updateReq = new TaskRequest();
        updateReq.setTitle("Build Spring Boot API");
        updateReq.setCategory("Project");
        updateReq.setPriority(TaskPriority.HIGH);
        updateReq.setTags(List.of("java", "springboot", "api"));

        TaskResponse updated = taskService.updateTask(userA, 100L, updateReq);
        assertEquals(3, updated.getTags().size());
        assertTrue(updated.getTags().contains("java"));
        assertTrue(updated.getTags().contains("springboot"));
        assertTrue(updated.getTags().contains("api"));
        assertFalse(updated.getTags().contains("backend"));
    }

    @Test
    @DisplayName("TEST 5: Reuse existing 'java' tag without creating duplicate")
    void testReuseExistingTag() {
        Tag existingJavaTag = Tag.builder().id(1L).user(userA).name("java").build();
        when(tagRepository.findByUserIdAndName(userA.getId(), "java")).thenReturn(Optional.of(existingJavaTag));

        Task newTask = Task.builder()
                .id(101L)
                .user(userA)
                .title("Second Java Task")
                .tags(List.of(existingJavaTag))
                .build();
        when(taskRepository.save(any(Task.class))).thenReturn(newTask);

        TaskRequest req = new TaskRequest();
        req.setTitle("Second Java Task");
        req.setTags(List.of("JAVA"));

        TaskResponse res = taskService.createTask(userA, req);
        assertEquals(1, res.getTags().size());
        assertEquals("java", res.getTags().get(0));

        // Verify tagRepository.save was NOT called to create duplicate tag
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("TEST 7: Delete tag ??? removes tag from tasks but tasks are NOT deleted")
    void testDeleteTagPreservesTasks() {
        Tag backendTag = Tag.builder().id(2L).user(userA).name("backend").build();
        when(tagRepository.findById(2L)).thenReturn(Optional.of(backendTag));

        Task task1 = Task.builder().id(100L).user(userA).title("Task 1").tags(new ArrayList<>(List.of(backendTag))).build();
        Task task2 = Task.builder().id(101L).user(userA).title("Task 2").tags(new ArrayList<>(List.of(backendTag))).build();

        when(taskRepository.findByTagsId(2L)).thenReturn(List.of(task1, task2));

        tagService.deleteTag(userA, 2L);

        // Verify tag was removed from task1 and task2
        assertTrue(task1.getTags().isEmpty());
        assertTrue(task2.getTags().isEmpty());

        // Verify tasks were saved and NOT deleted
        verify(taskRepository, times(2)).save(any(Task.class));
        verify(taskRepository, never()).delete(any(Task.class));
        verify(tagRepository, times(1)).delete(backendTag);
    }

    @Test
    @DisplayName("TEST 8: Security & User Isolation ??? User B cannot modify or delete User A's tag")
    void testUserIsolation() {
        Tag userATag = Tag.builder().id(1L).user(userA).name("java").build();
        when(tagRepository.findById(1L)).thenReturn(Optional.of(userATag));

        // User B tries to update User A's tag
        assertThrows(SecurityException.class, () -> {
            tagService.updateTag(userB, 1L, new TagRequest("hacked"));
        });

        // User B tries to delete User A's tag
        assertThrows(SecurityException.class, () -> {
            tagService.deleteTag(userB, 1L);
        });

        verify(tagRepository, never()).delete(any());
    }
}
