package com.mindos.backend.controller;

import com.mindos.backend.dto.RagAnswerResponse;
import com.mindos.backend.dto.RagQuestionRequest;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.RagService;
import com.mindos.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin(origins = "*")
public class RagController {

    private final RagService ragService;
    private final UserService userService;

    public RagController(RagService ragService, UserService userService) {
        this.ragService = ragService;
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

    /**
     * POST /api/knowledge/ask
     * Complete RAG Q&A endpoint: retrieve user context and synthesize grounded answer with citations.
     */
    @PostMapping("/ask")
    public ResponseEntity<?> askKnowledge(
            @Valid @RequestBody RagQuestionRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            RagAnswerResponse response = ragService.ask(user, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "AI service is temporarily unavailable. " + e.getMessage()));
        }
    }
}
