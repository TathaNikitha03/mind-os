package com.mindos.backend.controller;

import com.mindos.backend.dto.CategoryRequest;
import com.mindos.backend.dto.CategoryResponse;
import com.mindos.backend.entity.User;
import com.mindos.backend.service.CategoryService;
import com.mindos.backend.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;



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

    // 1. GET /api/categories - List categories for authenticated user
    @GetMapping
    public ResponseEntity<?> getUserCategories(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            List<CategoryResponse> categories = categoryService.getUserCategories(user);
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // 2. GET /api/categories/{id} - Get single category for authenticated user
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            CategoryResponse response = categoryService.getCategoryById(user, id);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 3. POST /api/categories - Create category for authenticated user
    @PostMapping
    public ResponseEntity<?> createCategory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader,
            @Valid @RequestBody CategoryRequest request
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            CategoryResponse response = categoryService.createCategory(user, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 4. PUT /api/categories/{id} - Update category for authenticated user
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader,
            @Valid @RequestBody CategoryRequest request
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            CategoryResponse response = categoryService.updateCategory(user, id, request);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // 5. DELETE /api/categories/{id} - Delete category safely
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(
            @PathVariable("id") Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Mobile", required = false) String userMobileHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userNameHeader
    ) {
        try {
            User user = getAuthenticatedUser(authHeader, userIdHeader, userMobileHeader, userNameHeader);
            categoryService.deleteCategory(user, id);
            return ResponseEntity.ok(Map.of("message", "Category deleted successfully", "id", id));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
