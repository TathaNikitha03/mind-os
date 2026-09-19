package com.mindos.backend.controller;

import com.mindos.backend.dto.DocumentResponse;
import com.mindos.backend.dto.TaskResponse;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.TaskDocumentService;
import com.mindos.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class TaskDocumentController {

    private final TaskDocumentService taskDocumentService;
    private final UserService userService;

    public TaskDocumentController(TaskDocumentService taskDocumentService, UserService userService) {
        this.taskDocumentService = taskDocumentService;
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

    // 1. GET /api/tasks/{taskId}/documents - List attached documents for task
    @GetMapping("/api/tasks/{taskId}/documents")
    public ResponseEntity<?> getDocumentsForTask(
            @PathVariable("taskId") Long taskId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            List<DocumentResponse> documents = taskDocumentService.getDocumentsForTask(user, taskId);
            return ResponseEntity.ok(documents);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 2. POST /api/tasks/{taskId}/documents/{documentId} - Attach document to task
    @PostMapping("/api/tasks/{taskId}/documents/{documentId}")
    public ResponseEntity<?> attachDocumentToTask(
            @PathVariable("taskId") Long taskId,
            @PathVariable("documentId") Long documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            DocumentResponse response = taskDocumentService.attachDocumentToTask(user, taskId, documentId);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 3. DELETE /api/tasks/{taskId}/documents/{documentId} - Unlink document from task
    @DeleteMapping("/api/tasks/{taskId}/documents/{documentId}")
    public ResponseEntity<?> detachDocumentFromTask(
            @PathVariable("taskId") Long taskId,
            @PathVariable("documentId") Long documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            taskDocumentService.detachDocumentFromTask(user, taskId, documentId);
            return ResponseEntity.ok(Map.of("message", "Document detached from task successfully."));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 4. GET /api/documents/{documentId}/tasks - List tasks linked to document
    @GetMapping("/api/documents/{documentId}/tasks")
    public ResponseEntity<?> getTasksForDocument(
            @PathVariable("documentId") Long documentId,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            List<TaskResponse> tasks = taskDocumentService.getTasksForDocument(user, documentId);
            return ResponseEntity.ok(tasks);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
