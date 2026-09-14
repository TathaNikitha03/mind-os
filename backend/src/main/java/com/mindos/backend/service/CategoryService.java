package com.mindos.backend.service;

import com.mindos.backend.dto.CategoryRequest;
import com.mindos.backend.dto.CategoryResponse;
import com.mindos.backend.entity.Category;
import com.mindos.backend.entity.User;
import com.mindos.backend.repository.CategoryRepository;
import com.mindos.backend.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TaskRepository taskRepository;



    @Transactional(readOnly = true)
    public List<CategoryResponse> getUserCategories(User user) {
        List<Category> categories = categoryRepository.findByUserId(user.getId());
        return categories.stream().map(c -> {
            long taskCount = taskRepository.countByCategoryId(c.getId());
            return CategoryResponse.builder()
                    .id(c.getId())
                    .userId(c.getUser().getId())
                    .name(c.getName())
                    .description(c.getDescription())
                    .createdAt(c.getCreatedAt())
                    .taskCount(taskCount)
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(User user, Long categoryId) {
        Category category = getCategoryAndVerifyOwnership(user, categoryId);
        long taskCount = taskRepository.countByCategoryId(category.getId());
        return CategoryResponse.builder()
                .id(category.getId())
                .userId(category.getUser().getId())
                .name(category.getName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .taskCount(taskCount)
                .build();
    }

    @Transactional
    public CategoryResponse createCategory(User user, CategoryRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        String name = request.getName().trim();

        // Unique per user rule
        if (categoryRepository.existsByUserIdAndName(user.getId(), name)) {
            throw new IllegalArgumentException("Category '" + name + "' already exists in your workspace.");
        }

        Category category = Category.builder()
                .user(user)
                .name(name)
                .description(request.getDescription() != null ? request.getDescription().trim() : "")
                .build();

        Category saved = categoryRepository.save(category);

        return CategoryResponse.builder()
                .id(saved.getId())
                .userId(saved.getUser().getId())
                .name(saved.getName())
                .description(saved.getDescription())
                .createdAt(saved.getCreatedAt())
                .taskCount(0)
                .build();
    }

    @Transactional
    public CategoryResponse updateCategory(User user, Long categoryId, CategoryRequest request) {
        Category category = getCategoryAndVerifyOwnership(user, categoryId);

        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name cannot be empty.");
        }

        String newName = request.getName().trim();

        // Check if new name conflicts with an existing category of this user
        Optional<Category> existing = categoryRepository.findByUserIdAndName(user.getId(), newName);
        if (existing.isPresent() && !existing.get().getId().equals(categoryId)) {
            throw new IllegalArgumentException("Category '" + newName + "' already exists in your workspace.");
        }

        category.setName(newName);
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription().trim());
        }

        Category updated = categoryRepository.save(category);
        long taskCount = taskRepository.countByCategoryId(updated.getId());

        return CategoryResponse.builder()
                .id(updated.getId())
                .userId(updated.getUser().getId())
                .name(updated.getName())
                .description(updated.getDescription())
                .createdAt(updated.getCreatedAt())
                .taskCount(taskCount)
                .build();
    }

    @Transactional
    public void deleteCategory(User user, Long categoryId) {
        Category category = getCategoryAndVerifyOwnership(user, categoryId);

        // Safe deletion: Check if category is currently assigned to tasks
        long taskCount = taskRepository.countByCategoryId(categoryId);
        if (taskCount > 0) {
            throw new IllegalStateException("Cannot delete category '" + category.getName() + "' because it is currently assigned to " + taskCount + " task(s). Please reassign or update those tasks first.");
        }

        categoryRepository.delete(category);
    }

    private Category getCategoryAndVerifyOwnership(User user, Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + categoryId));

        if (!category.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Unauthorized: You do not have permission to access or modify this category.");
        }

        return category;
    }
}
