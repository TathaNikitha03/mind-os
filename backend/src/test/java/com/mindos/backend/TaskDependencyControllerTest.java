package com.mindos.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.dto.TaskDependencyRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class TaskDependencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String USER_MOBILE = "9112223330";
    private static final String USER_NAME = "DepTestUser";

    private String createTaskAndGetId(String title) throws Exception {
        String body = "{\"title\":\"" + title + "\",\"priority\":\"MEDIUM\",\"status\":\"TODO\"}";
        String response = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME)
                .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    @Test
    public void testGetDependencies_Empty() throws Exception {
        String taskId = createTaskAndGetId("Standalone Task Dep Test");
        mockMvc.perform(get("/api/tasks/" + taskId + "/dependencies")
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    public void testAddDependency_Success() throws Exception {
        String taskAId = createTaskAndGetId("Design Database DEP");
        String taskBId = createTaskAndGetId("Build Backend API DEP");

        TaskDependencyRequest req = new TaskDependencyRequest(Long.parseLong(taskAId));

        mockMvc.perform(post("/api/tasks/" + taskBId + "/dependencies")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taskId").value(Long.parseLong(taskBId)))
                .andExpect(jsonPath("$.dependsOnTaskId").value(Long.parseLong(taskAId)));
    }

    @Test
    public void testAddDependency_SelfReference_Rejected() throws Exception {
        String taskId = createTaskAndGetId("Self Ref Task DEP");

        TaskDependencyRequest req = new TaskDependencyRequest(Long.parseLong(taskId));

        mockMvc.perform(post("/api/tasks/" + taskId + "/dependencies")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    public void testAddDependency_CircularDependency_Rejected() throws Exception {
        String taskAId = createTaskAndGetId("Circular A DEP");
        String taskBId = createTaskAndGetId("Circular B DEP");

        // A depends on B
        TaskDependencyRequest reqAB = new TaskDependencyRequest(Long.parseLong(taskBId));
        mockMvc.perform(post("/api/tasks/" + taskAId + "/dependencies")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME)
                .content(objectMapper.writeValueAsString(reqAB)))
                .andExpect(status().isCreated());

        // Try: B depends on A (creates cycle)
        TaskDependencyRequest reqBA = new TaskDependencyRequest(Long.parseLong(taskAId));
        mockMvc.perform(post("/api/tasks/" + taskBId + "/dependencies")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME)
                .content(objectMapper.writeValueAsString(reqBA)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Circular")));
    }

    @Test
    public void testAddDependency_Duplicate_Rejected() throws Exception {
        String taskAId = createTaskAndGetId("Dup A DEP");
        String taskBId = createTaskAndGetId("Dup B DEP");

        TaskDependencyRequest req = new TaskDependencyRequest(Long.parseLong(taskAId));

        // Add once
        mockMvc.perform(post("/api/tasks/" + taskBId + "/dependencies")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        // Add again - should be rejected
        mockMvc.perform(post("/api/tasks/" + taskBId + "/dependencies")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testRemoveDependency_TasksNotDeleted() throws Exception {
        String taskAId = createTaskAndGetId("Remove Dep A DEP");
        String taskBId = createTaskAndGetId("Remove Dep B DEP");

        // Add dependency
        TaskDependencyRequest req = new TaskDependencyRequest(Long.parseLong(taskAId));
        String depResponse = mockMvc.perform(post("/api/tasks/" + taskBId + "/dependencies")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String depId = objectMapper.readTree(depResponse).get("id").asText();

        // Remove dependency
        mockMvc.perform(delete("/api/tasks/" + taskBId + "/dependencies/" + depId)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME))
                .andExpect(status().isOk());

        // Verify task A still exists
        mockMvc.perform(get("/api/tasks/" + taskAId)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(taskAId)));

        // Verify task B still exists
        mockMvc.perform(get("/api/tasks/" + taskBId)
                .header("X-User-Mobile", USER_MOBILE)
                .header("X-User-Name", USER_NAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(Long.parseLong(taskBId)));
    }

    @Test
    public void testUserIsolation_CrossUserDependency_Rejected() throws Exception {
        // User A creates a task
        String userAMobile = "9991110001";
        String taskAResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", userAMobile)
                .header("X-User-Name", "User A Isolation")
                .content("{\"title\":\"User A Task ISO\",\"priority\":\"LOW\",\"status\":\"TODO\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskAId = objectMapper.readTree(taskAResponse).get("id").asText();

        // User B creates a task
        String userBMobile = "9991110002";
        String taskBResponse = mockMvc.perform(post("/api/tasks")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", userBMobile)
                .header("X-User-Name", "User B Isolation")
                .content("{\"title\":\"User B Task ISO\",\"priority\":\"LOW\",\"status\":\"TODO\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String taskBId = objectMapper.readTree(taskBResponse).get("id").asText();

        // User B tries to make their task depend on User A's task
        TaskDependencyRequest req = new TaskDependencyRequest(Long.parseLong(taskAId));
        mockMvc.perform(post("/api/tasks/" + taskBId + "/dependencies")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-User-Mobile", userBMobile)
                .header("X-User-Name", "User B Isolation")
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is4xxClientError());
    }
}
