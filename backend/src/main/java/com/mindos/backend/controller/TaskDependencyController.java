package com.mindos.backend.controller;

import com.mindos.backend.dto.TaskDependencyRequest;
import com.mindos.backend.dto.TaskDependencyResponse;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.TaskDependencyService;
import com.mindos.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks/{taskId}/dependencies")
@CrossOrigin(origins = "*")
public class TaskDependencyController {

    private final TaskDependencyService taskDependencyService;
    private final UserService userService;

    public TaskDependencyController(TaskDependencyService taskDependencyService, UserService userService) {
        this.taskDependencyService = taskDependencyService;
        this.userService = userService;
    }

    private User getAuthenticatedUser(String authHeader, String userIdHeader, String userMobileHeader, String userNameHeader) {
        String identifier = "default_user";
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            identifier = userIdHeader;
        } else if (userMobileHeader != null && !userMobileHeader.isBlank()) {
            identifier = userMobileHeader;
        } else if (authHeader != null && !authHeader.isBlank()) {
            identifier = authHeader.replace("Bearer ", "").trim();
        }
        return userService.getOrCreateUserByMobileOrId(identifier, userNameHeader);
    }

    // 1. GET /api/tasks/{taskId}/dependencies - List prerequisites for this task
    @GetMapping
    public ResponseEntity<?> getDependencies(
            @PathVariable("taskId") Long taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            List<TaskDependencyResponse> dependencies = taskDependencyService.getDependencies(user, taskId);
            return ResponseEntity.ok(dependencies);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 2. GET /api/tasks/{taskId}/dependencies/dependents - List tasks waiting for this task
    @GetMapping("/dependents")
    public ResponseEntity<?> getDependents(
            @PathVariable("taskId") Long taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            List<TaskDependencyResponse> dependents = taskDependencyService.getDependents(user, taskId);
            return ResponseEntity.ok(dependents);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 3. POST /api/tasks/{taskId}/dependencies - Add a prerequisite dependency
    @PostMapping
    public ResponseEntity<?> addDependency(
            @PathVariable("taskId") Long taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader,
            @Valid @RequestBody TaskDependencyRequest request
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            TaskDependencyResponse response = taskDependencyService.addDependency(user, taskId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 4. DELETE /api/tasks/{taskId}/dependencies/{dependencyId} - Remove a dependency
    @DeleteMapping("/{dependencyId}")
    public ResponseEntity<?> removeDependency(
            @PathVariable("taskId") Long taskId,
            @PathVariable("dependencyId") Long dependencyId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            taskDependencyService.removeDependency(user, taskId, dependencyId);
            return ResponseEntity.ok(Map.of("message", "Dependency removed successfully", "id", dependencyId));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
