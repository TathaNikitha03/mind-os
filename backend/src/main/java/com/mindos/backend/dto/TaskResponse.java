package com.mindos.backend.dto;

import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;

import java.time.LocalDateTime;
import java.util.List;

public class TaskResponse {
    private Long id;
    private Long userId;
    private String title;
    private String description;
    private TaskPriority priority;
    private TaskStatus status;
    private String dueDate;
    private String dueTime;
    private Integer estimatedMinutes;
    private String category;
    private List<String> tags;
    private int dependencyCount;
    private int uncompletedDependencyCount;
    private boolean blocked;
    private int documentCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;

    public TaskResponse() {}

    public static TaskResponseBuilder builder() { return new TaskResponseBuilder(); }

    public static class TaskResponseBuilder {
        private Long id;
        private Long userId;
        private String title;
        private String description;
        private TaskPriority priority;
        private TaskStatus status;
        private String dueDate;
        private String dueTime;
        private Integer estimatedMinutes;
        private String category;
        private List<String> tags;
        private int dependencyCount;
        private int uncompletedDependencyCount;
        private boolean blocked;
        private int documentCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime completedAt;

        public TaskResponseBuilder id(Long id) { this.id = id; return this; }
        public TaskResponseBuilder userId(Long userId) { this.userId = userId; return this; }
        public TaskResponseBuilder title(String title) { this.title = title; return this; }
        public TaskResponseBuilder description(String description) { this.description = description; return this; }
        public TaskResponseBuilder priority(TaskPriority priority) { this.priority = priority; return this; }
        public TaskResponseBuilder status(TaskStatus status) { this.status = status; return this; }
        public TaskResponseBuilder dueDate(String dueDate) { this.dueDate = dueDate; return this; }
        public TaskResponseBuilder dueTime(String dueTime) { this.dueTime = dueTime; return this; }
        public TaskResponseBuilder estimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; return this; }
        public TaskResponseBuilder category(String category) { this.category = category; return this; }
        public TaskResponseBuilder tags(List<String> tags) { this.tags = tags; return this; }
        public TaskResponseBuilder dependencyCount(int dependencyCount) { this.dependencyCount = dependencyCount; return this; }
        public TaskResponseBuilder uncompletedDependencyCount(int uncompletedDependencyCount) { this.uncompletedDependencyCount = uncompletedDependencyCount; return this; }
        public TaskResponseBuilder blocked(boolean blocked) { this.blocked = blocked; return this; }
        public TaskResponseBuilder documentCount(int documentCount) { this.documentCount = documentCount; return this; }
        public TaskResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public TaskResponseBuilder updatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; return this; }
        public TaskResponseBuilder completedAt(LocalDateTime completedAt) { this.completedAt = completedAt; return this; }

        public TaskResponse build() {
            TaskResponse r = new TaskResponse();
            r.setId(id);
            r.setUserId(userId);
            r.setTitle(title);
            r.setDescription(description);
            r.setPriority(priority);
            r.setStatus(status);
            r.setDueDate(dueDate);
            r.setDueTime(dueTime);
            r.setEstimatedMinutes(estimatedMinutes);
            r.setCategory(category);
            r.setTags(tags);
            r.setDependencyCount(dependencyCount);
            r.setUncompletedDependencyCount(uncompletedDependencyCount);
            r.setBlocked(blocked);
            r.setDocumentCount(documentCount);
            r.setCreatedAt(createdAt);
            r.setUpdatedAt(updatedAt);
            r.setCompletedAt(completedAt);
            return r;
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }
    public Integer getEstimatedMinutes() { return estimatedMinutes; }
    public void setEstimatedMinutes(Integer estimatedMinutes) { this.estimatedMinutes = estimatedMinutes; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public int getDependencyCount() { return dependencyCount; }
    public void setDependencyCount(int dependencyCount) { this.dependencyCount = dependencyCount; }
    public int getUncompletedDependencyCount() { return uncompletedDependencyCount; }
    public void setUncompletedDependencyCount(int uncompletedDependencyCount) { this.uncompletedDependencyCount = uncompletedDependencyCount; }
    public boolean isBlocked() { return blocked; }
    public void setBlocked(boolean blocked) { this.blocked = blocked; }
    public int getDocumentCount() { return documentCount; }
    public void setDocumentCount(int documentCount) { this.documentCount = documentCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
