package com.mindos.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.controller.TaskController;
import com.mindos.backend.dto.TaskResponse;
import com.mindos.backend.dto.TaskStatisticsResponse;
import com.mindos.backend.entity.User;
import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;
import com.mindos.backend.service.TaskService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TaskStatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TaskService taskService;

    @MockBean
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id(101L).name("Nikitha").email("9876543210@mindos.com").passwordHash("pass").build();
        when(userService.getOrCreateUserByMobileOrId(any(), any())).thenReturn(mockUser);
    }

    @Test
    @DisplayName("GET /api/tasks/statistics returns 200 OK with calculated task statistics payload")
    void testGetTaskStatisticsEndpoint() throws Exception {
        TaskResponse sampleTask = TaskResponse.builder()
                .id(1L)
                .title("Design Database")
                .status(TaskStatus.COMPLETED)
                .priority(TaskPriority.HIGH)
                .category("Project")
                .dueDate("2026-09-01")
                .build();

        TaskStatisticsResponse response = TaskStatisticsResponse.builder()
                .totalTasks(5)
                .completedTasks(1)
                .inProgressTasks(1)
                .todoTasks(3)
                .overdueTasks(1)
                .todayTasks(1)
                .blockedTasks(1)
                .completionPercentage(20.0)
                .priorityBreakdown(Map.of("HIGH", 3L, "MEDIUM", 1L, "LOW", 1L, "high", 3L, "medium", 1L, "low", 1L))
                .categoryBreakdown(Map.of("Project", 3L, "Study", 2L))
                .todayTasksList(List.of(sampleTask))
                .build();

        when(taskService.getTaskStatistics(any(User.class))).thenReturn(response);

        mockMvc.perform(get("/api/tasks/statistics")
                        .header("X-User-Mobile", "9876543210")
                        .header("X-User-Name", "Nikitha")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTasks").value(5))
                .andExpect(jsonPath("$.completedTasks").value(1))
                .andExpect(jsonPath("$.inProgressTasks").value(1))
                .andExpect(jsonPath("$.todoTasks").value(3))
                .andExpect(jsonPath("$.overdueTasks").value(1))
                .andExpect(jsonPath("$.todayTasks").value(1))
                .andExpect(jsonPath("$.blockedTasks").value(1))
                .andExpect(jsonPath("$.completionPercentage").value(20.0))
                .andExpect(jsonPath("$.priorityBreakdown.HIGH").value(3))
                .andExpect(jsonPath("$.categoryBreakdown.Project").value(3))
                .andExpect(jsonPath("$.todayTasksList[0].title").value("Design Database"));
    }
}
