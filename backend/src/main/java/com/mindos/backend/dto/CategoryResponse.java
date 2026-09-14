package com.mindos.backend.dto;

import java.time.LocalDateTime;

public class CategoryResponse {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private long taskCount;

    public CategoryResponse() {}

    public static CategoryResponseBuilder builder() {
        return new CategoryResponseBuilder();
    }

    public static class CategoryResponseBuilder {
        private Long id;
        private Long userId;
        private String name;
        private String description;
        private LocalDateTime createdAt;
        private long taskCount;

        public CategoryResponseBuilder id(Long id) { this.id = id; return this; }
        public CategoryResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public CategoryResponseBuilder name(String name) { this.name = name; return this; }
        public CategoryResponseBuilder description(String description) { this.description = description; return this; }
        public CategoryResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public CategoryResponseBuilder taskCount(long taskCount) { this.taskCount = taskCount; return this; }

        public CategoryResponse build() {
            CategoryResponse r = new CategoryResponse();
            r.setId(id);
            r.setUserId(userId);
            r.setName(name);
            r.setDescription(description);
            r.setCreatedAt(createdAt);
            r.setTaskCount(taskCount);
            return r;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public long getTaskCount() { return taskCount; }
    public void setTaskCount(long taskCount) { this.taskCount = taskCount; }
}
