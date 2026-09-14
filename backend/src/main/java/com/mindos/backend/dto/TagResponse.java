package com.mindos.backend.dto;

import java.time.LocalDateTime;

public class TagResponse {
    private Long id;
    private Long userId;
    private String name;
    private LocalDateTime createdAt;
    private long taskCount;

    public TagResponse() {}

    public TagResponse(Long id, Long userId, String name, LocalDateTime createdAt, long taskCount) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.createdAt = createdAt;
        this.taskCount = taskCount;
    }

    public static TagResponseBuilder builder() {
        return new TagResponseBuilder();
    }

    public static class TagResponseBuilder {
        private Long id;
        private Long userId;
        private String name;
        private LocalDateTime createdAt;
        private long taskCount;

        public TagResponseBuilder id(Long id) { this.id = id; return this; }
        public TagResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public TagResponseBuilder name(String name) { this.name = name; return this; }
        public TagResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TagResponseBuilder taskCount(long taskCount) { this.taskCount = taskCount; return this; }

        public TagResponse build() {
            return new TagResponse(id, userId, name, createdAt, taskCount);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public long getTaskCount() { return taskCount; }
    public void setTaskCount(long taskCount) { this.taskCount = taskCount; }
}
