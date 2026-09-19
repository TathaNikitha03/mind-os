package com.mindos.backend;

import com.mindos.backend.dto.TaskDependencyRequest;
import com.mindos.backend.dto.TaskDependencyResponse;
import com.mindos.backend.entity.Task;
import com.mindos.backend.entity.User;
import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;
import com.mindos.backend.repository.TaskDependencyRepository;
import com.mindos.backend.repository.TaskRepository;
import com.mindos.backend.service.TaskDependencyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskDependencyServiceTest {

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskDependencyService taskDependencyService;

    private User user;
    private Task taskA;
    private Task taskB;
    private Task taskC;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("Test User");
        user.setEmail("test@example.com");

        taskA = new Task();
        taskA.setId(10L);
        taskA.setTitle("Design Database");
        taskA.setUser(user);
        taskA.setStatus(TaskStatus.TODO);
        taskA.setPriority(TaskPriority.HIGH);

        taskB = new Task();
        taskB.setId(20L);
        taskB.setTitle("Build Backend API");
        taskB.setUser(user);
        taskB.setStatus(TaskStatus.TODO);
        taskB.setPriority(TaskPriority.MEDIUM);

        taskC = new Task();
        taskC.setId(30L);
        taskC.setTitle("Frontend Integration");
        taskC.setUser(user);
        taskC.setStatus(TaskStatus.TODO);
        taskC.setPriority(TaskPriority.LOW);
    }

    @Test
    void testAddDependency_Success() {
        when(taskRepository.findById(20L)).thenReturn(Optional.of(taskB));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskA));
        when(taskDependencyRepository.existsByTaskIdAndDependsOnTaskId(20L, 10L)).thenReturn(false);
        when(taskDependencyRepository.findByTaskId(10L)).thenReturn(Collections.emptyList());

        com.mindos.backend.entity.TaskDependency saved = com.mindos.backend.entity.TaskDependency.builder()
                .id(1L).task(taskB).dependsOnTask(taskA).build();
        when(taskDependencyRepository.save(any())).thenReturn(saved);

        TaskDependencyRequest req = new TaskDependencyRequest(10L);
        TaskDependencyResponse response = taskDependencyService.addDependency(user, 20L, req);

        assertNotNull(response);
        assertEquals(20L, response.getTaskId());
        assertEquals(10L, response.getDependsOnTaskId());
        assertEquals("Design Database", response.getDependsOnTaskTitle());
    }

    @Test
    void testAddDependency_SelfReference_Throws() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                taskDependencyService.addDependency(user, 10L, new TaskDependencyRequest(10L)));
        assertTrue(ex.getMessage().contains("itself"));
    }

    @Test
    void testAddDependency_Duplicate_Throws() {
        when(taskRepository.findById(20L)).thenReturn(Optional.of(taskB));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskA));
        when(taskDependencyRepository.existsByTaskIdAndDependsOnTaskId(20L, 10L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                taskDependencyService.addDependency(user, 20L, new TaskDependencyRequest(10L)));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void testAddDependency_CircularDirect_Throws() {
        // Existing dependency: A(10) -> B(20)  (A depends on B)
        com.mindos.backend.entity.TaskDependency depAonB = com.mindos.backend.entity.TaskDependency.builder()
                .id(5L).task(taskA).dependsOnTask(taskB).build();

        // Setup mocks for: addDependency(user, taskB.id=20, prerequisite=taskA.id=10)
        // i.e., "B depends on A"
        // Step 1: verify ownership on both tasks
        when(taskRepository.findById(20L)).thenReturn(Optional.of(taskB));
        when(taskRepository.findById(10L)).thenReturn(Optional.of(taskA));
        // Step 2: not a duplicate
        when(taskDependencyRepository.existsByTaskIdAndDependsOnTaskId(20L, 10L)).thenReturn(false);
        // Step 3: isReachable(startTaskId=A=10, targetTaskId=B=20)
        //   Start at A(10). A's deps => [B(20)]. B == target => reachable => CIRCULAR
        when(taskDependencyRepository.findByTaskId(10L)).thenReturn(List.of(depAonB));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                taskDependencyService.addDependency(user, 20L, new TaskDependencyRequest(10L)));
        assertTrue(ex.getMessage().contains("Circular") || ex.getMessage().contains("circular"),
                "Expected circular dependency message, got: " + ex.getMessage());
    }

    @Test
    void testAddDependency_CrossUserTask_SecurityException() {
        User otherUser = new User();
        otherUser.setId(99L);
        otherUser.setName("Other User");

        Task otherTask = new Task();
        otherTask.setId(50L);
        otherTask.setTitle("Other User Task");
        otherTask.setUser(otherUser);
        otherTask.setStatus(TaskStatus.TODO);
        otherTask.setPriority(TaskPriority.LOW);

        when(taskRepository.findById(20L)).thenReturn(Optional.of(taskB));
        when(taskRepository.findById(50L)).thenReturn(Optional.of(otherTask));

        assertThrows(SecurityException.class, () ->
                taskDependencyService.addDependency(user, 20L, new TaskDependencyRequest(50L)));
    }

    @Test
    void testGetDependencies_ReturnsCorrectList() {
        com.mindos.backend.entity.TaskDependency dep = com.mindos.backend.entity.TaskDependency.builder()
                .id(1L).task(taskB).dependsOnTask(taskA).build();

        when(taskRepository.findById(20L)).thenReturn(Optional.of(taskB));
        when(taskDependencyRepository.findByTaskId(20L)).thenReturn(List.of(dep));

        List<TaskDependencyResponse> result = taskDependencyService.getDependencies(user, 20L);

        assertEquals(1, result.size());
        assertEquals("Design Database", result.get(0).getDependsOnTaskTitle());
        assertFalse(result.get(0).isCompleted());
    }

    @Test
    void testIsTaskBlocked_ReturnsTrueWhenDepsIncomplete() {
        com.mindos.backend.entity.TaskDependency dep = com.mindos.backend.entity.TaskDependency.builder()
                .id(1L).task(taskB).dependsOnTask(taskA).build();

        when(taskDependencyRepository.findByTaskId(20L)).thenReturn(List.of(dep));
        assertTrue(taskDependencyService.isTaskBlocked(20L));
    }

    @Test
    void testIsTaskBlocked_ReturnsFalseWhenDepsComplete() {
        taskA.setStatus(TaskStatus.COMPLETED);
        com.mindos.backend.entity.TaskDependency dep = com.mindos.backend.entity.TaskDependency.builder()
                .id(1L).task(taskB).dependsOnTask(taskA).build();

        when(taskDependencyRepository.findByTaskId(20L)).thenReturn(List.of(dep));
        assertFalse(taskDependencyService.isTaskBlocked(20L));
    }
}
