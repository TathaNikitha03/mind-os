package com.mindos.backend;

import com.mindos.backend.dto.TaskStatisticsResponse;
import com.mindos.backend.entity.Category;
import com.mindos.backend.entity.Task;
import com.mindos.backend.entity.TaskDependency;
import com.mindos.backend.entity.User;
import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;
import com.mindos.backend.repository.CategoryRepository;
import com.mindos.backend.repository.TagRepository;
import com.mindos.backend.repository.TaskDependencyRepository;
import com.mindos.backend.repository.TaskRepository;
import com.mindos.backend.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TaskStatisticsTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TagRepository tagRepository;

    @Mock
    private TaskDependencyRepository taskDependencyRepository;

    private TaskService taskService;

    private User userA;
    private User userB;

    private Category projectCat;
    private Category studyCat;

    @BeforeEach
    void setUp() {
        taskService = new TaskService(taskRepository, categoryRepository, tagRepository, taskDependencyRepository);

        userA = new User();
        userA.setId(101L);
        userA.setName("Nikitha UserA");
        userA.setEmail("usera@example.com");

        userB = new User();
        userB.setId(202L);
        userB.setName("Alex UserB");
        userB.setEmail("userb@example.com");

        projectCat = Category.builder().id(1L).user(userA).name("Project").build();
        studyCat = Category.builder().id(2L).user(userA).name("Study").build();
    }

    @Test
    @DisplayName("Should return zero statistics when user has zero tasks")
    void testZeroTasksStatistics() {
        when(taskRepository.findByUserId(userA.getId())).thenReturn(new ArrayList<>());

        TaskStatisticsResponse stats = taskService.getTaskStatistics(userA);

        assertNotNull(stats);
        assertEquals(0, stats.getTotalTasks());
        assertEquals(0, stats.getCompletedTasks());
        assertEquals(0, stats.getInProgressTasks());
        assertEquals(0, stats.getTodoTasks());
        assertEquals(0, stats.getOverdueTasks());
        assertEquals(0, stats.getTodayTasks());
        assertEquals(0, stats.getBlockedTasks());
        assertEquals(0.0, stats.getCompletionPercentage());
        assertTrue(stats.getTodayTasksList().isEmpty());
        assertTrue(stats.getUpcomingTasksList().isEmpty());
        assertTrue(stats.getOverdueTasksList().isEmpty());
        assertTrue(stats.getBlockedTasksList().isEmpty());
    }

    @Test
    @DisplayName("Should correctly calculate all 7 statistics, progress percentage, priorities, and categories for scenario dataset")
    void testFullTaskStatisticsCalculation() {
        LocalDateTime today = LocalDateTime.now();
        LocalDateTime yesterday = today.minusDays(1);
        LocalDateTime nextWeek = today.plusDays(7);

        // Task 1: Design Database (COMPLETED, HIGH, Project)
        Task t1 = Task.builder()
                .id(1L).user(userA).title("Design Database")
                .status(TaskStatus.COMPLETED).priority(TaskPriority.HIGH)
                .category(projectCat).dueDate(yesterday)
                .build();

        // Task 2: Build Backend API (IN_PROGRESS, HIGH, Project) - Depends on Task 1 (which is completed)
        Task t2 = Task.builder()
                .id(2L).user(userA).title("Build Backend API")
                .status(TaskStatus.IN_PROGRESS).priority(TaskPriority.HIGH)
                .category(projectCat).dueDate(nextWeek)
                .build();

        // Task 3: Build Frontend (TODO, MEDIUM, Project) - Depends on Task 2 (which is IN_PROGRESS -> blocked)
        Task t3 = Task.builder()
                .id(3L).user(userA).title("Build Frontend")
                .status(TaskStatus.TODO).priority(TaskPriority.MEDIUM)
                .category(projectCat).dueDate(nextWeek)
                .build();

        // Task 4: Practice DSA (TODO, LOW, Study) - Due today
        Task t4 = Task.builder()
                .id(4L).user(userA).title("Practice DSA")
                .status(TaskStatus.TODO).priority(TaskPriority.LOW)
                .category(studyCat).dueDate(today)
                .build();

        // Task 5: Complete Assignment (TODO, HIGH, Study) - Due yesterday (overdue)
        Task t5 = Task.builder()
                .id(5L).user(userA).title("Complete Assignment")
                .status(TaskStatus.TODO).priority(TaskPriority.HIGH)
                .category(studyCat).dueDate(yesterday)
                .build();

        List<Task> userATasks = List.of(t1, t2, t3, t4, t5);
        when(taskRepository.findByUserId(userA.getId())).thenReturn(userATasks);

        // Dependency: Task 2 depends on Task 1 (completed) -> not blocked
        when(taskDependencyRepository.findByTaskId(1L)).thenReturn(List.of());
        when(taskDependencyRepository.findByTaskId(2L)).thenReturn(List.of(
                TaskDependency.builder().id(10L).task(t2).dependsOnTask(t1).build()
        ));
        // Dependency: Task 3 depends on Task 2 (in_progress) -> blocked!
        when(taskDependencyRepository.findByTaskId(3L)).thenReturn(List.of(
                TaskDependency.builder().id(11L).task(t3).dependsOnTask(t2).build()
        ));
        when(taskDependencyRepository.findByTaskId(4L)).thenReturn(List.of());
        when(taskDependencyRepository.findByTaskId(5L)).thenReturn(List.of());

        TaskStatisticsResponse stats = taskService.getTaskStatistics(userA);

        assertNotNull(stats);
        assertEquals(5, stats.getTotalTasks(), "Total tasks should be 5");
        assertEquals(1, stats.getCompletedTasks(), "Completed tasks should be 1");
        assertEquals(1, stats.getInProgressTasks(), "In Progress tasks should be 1");
        assertEquals(3, stats.getTodoTasks(), "TODO tasks should be 3");
        assertEquals(1, stats.getOverdueTasks(), "Overdue tasks should be 1 (Task 5, Task 1 is completed so not overdue)");
        assertEquals(1, stats.getTodayTasks(), "Today's tasks should be 1 (Task 4)");
        assertEquals(1, stats.getBlockedTasks(), "Blocked tasks should be 1 (Task 3)");

        // 1 / 5 * 100 = 20.0%
        assertEquals(20.0, stats.getCompletionPercentage(), "Completion percentage should be 20.0%");

        // Priority breakdown: HIGH: 3 (t1, t2, t5), MEDIUM: 1 (t3), LOW: 1 (t4)
        assertEquals(3L, stats.getPriorityBreakdown().get("HIGH"));
        assertEquals(1L, stats.getPriorityBreakdown().get("MEDIUM"));
        assertEquals(1L, stats.getPriorityBreakdown().get("LOW"));

        // Category breakdown: Project: 3, Study: 2
        assertEquals(3L, stats.getCategoryBreakdown().get("Project"));
        assertEquals(2L, stats.getCategoryBreakdown().get("Study"));

        // Lists validation
        assertEquals(1, stats.getTodayTasksList().size());
        assertEquals("Practice DSA", stats.getTodayTasksList().get(0).getTitle());

        assertEquals(1, stats.getOverdueTasksList().size());
        assertEquals("Complete Assignment", stats.getOverdueTasksList().get(0).getTitle());

        assertEquals(1, stats.getBlockedTasksList().size());
        assertEquals("Build Frontend", stats.getBlockedTasksList().get(0).getTitle());
        assertTrue(stats.getBlockedTasksList().get(0).isBlocked());
    }

    @Test
    @DisplayName("Should update statistics and progress percentage when task status changes from TODO to COMPLETED")
    void testRefreshStatisticsAfterStatusChange() {
        LocalDateTime today = LocalDateTime.now();

        Task t1 = Task.builder().id(1L).user(userA).title("Task 1").status(TaskStatus.COMPLETED).dueDate(today).build();
        Task t2 = Task.builder().id(2L).user(userA).title("Task 2").status(TaskStatus.TODO).dueDate(today).build();

        // Initial: 1 completed out of 2 = 50%
        when(taskRepository.findByUserId(userA.getId())).thenReturn(List.of(t1, t2));
        TaskStatisticsResponse initialStats = taskService.getTaskStatistics(userA);
        assertEquals(1, initialStats.getCompletedTasks());
        assertEquals(1, initialStats.getTodoTasks());
        assertEquals(50.0, initialStats.getCompletionPercentage());

        // Now update t2 to COMPLETED
        t2.setStatus(TaskStatus.COMPLETED);
        TaskStatisticsResponse updatedStats = taskService.getTaskStatistics(userA);
        assertEquals(2, updatedStats.getCompletedTasks());
        assertEquals(0, updatedStats.getTodoTasks());
        assertEquals(100.0, updatedStats.getCompletionPercentage());
    }

    @Test
    @DisplayName("Should strictly isolate statistics between User A and User B")
    void testUserIsolation() {
        Task userATask = Task.builder().id(1L).user(userA).title("UserA Task").status(TaskStatus.COMPLETED).build();
        Task userBTask = Task.builder().id(2L).user(userB).title("UserB Task").status(TaskStatus.TODO).build();

        when(taskRepository.findByUserId(userA.getId())).thenReturn(List.of(userATask));
        when(taskRepository.findByUserId(userB.getId())).thenReturn(List.of(userBTask));

        TaskStatisticsResponse statsA = taskService.getTaskStatistics(userA);
        TaskStatisticsResponse statsB = taskService.getTaskStatistics(userB);

        assertEquals(1, statsA.getTotalTasks());
        assertEquals(1, statsA.getCompletedTasks());
        assertEquals(100.0, statsA.getCompletionPercentage());

        assertEquals(1, statsB.getTotalTasks());
        assertEquals(0, statsB.getCompletedTasks());
        assertEquals(1, statsB.getTodoTasks());
        assertEquals(0.0, statsB.getCompletionPercentage());
    }
}
