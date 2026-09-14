package com.mindos.backend.controller;

import com.mindos.backend.dto.SemanticSearchRequest;
import com.mindos.backend.dto.SemanticSearchResponse;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.SemanticSearchService;
import com.mindos.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin(origins = "*")
public class KnowledgeSearchController {

    private final SemanticSearchService semanticSearchService;
    private final UserService userService;

    public KnowledgeSearchController(SemanticSearchService semanticSearchService, UserService userService) {
        this.semanticSearchService = semanticSearchService;
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
     * POST /api/knowledge/search
     * Perform semantic similarity vector retrieval on user's knowledge base.
     */
    @PostMapping("/search")
    public ResponseEntity<?> searchKnowledge(
            @Valid @RequestBody SemanticSearchRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            SemanticSearchResponse response = semanticSearchService.search(user, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", e.getMessage()));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Knowledge search is temporarily unavailable."));
        }
    }
}
