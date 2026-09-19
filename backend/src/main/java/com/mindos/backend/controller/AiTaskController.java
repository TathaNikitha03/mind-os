package com.mindos.backend.controller;

import com.mindos.backend.dto.AiTaskAnalyzeRequest;
import com.mindos.backend.dto.AiTaskSuggestionResponse;
import com.mindos.backend.service.AiTaskService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks/ai")
@CrossOrigin(origins = "*")
public class AiTaskController {

    private final AiTaskService aiTaskService;

    public AiTaskController(AiTaskService aiTaskService) {
        this.aiTaskService = aiTaskService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeTask(@Valid @RequestBody AiTaskAnalyzeRequest request) {
        try {
            AiTaskSuggestionResponse response = aiTaskService.analyzeGoal(request.getPrompt());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}
