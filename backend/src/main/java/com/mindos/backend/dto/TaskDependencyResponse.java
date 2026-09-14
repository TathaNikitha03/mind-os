package com.mindos.backend.dto;

import com.mindos.backend.enums.TaskPriority;
import com.mindos.backend.enums.TaskStatus;

import java.time.LocalDateTime;

public class TaskDependencyResponse {
    private Long id;
    private Long taskId;
    private String taskTitle;
    private Long dependsOnTaskId;
    private String dependsOnTaskTitle;
    private TaskPriority dependsOnTaskPriority;
    private TaskStatus dependsOnTaskStatus;
    private String dependsOnTaskCategory;
    private boolean isCompleted;
    private LocalDateTime createdAt;

    public TaskDependencyResponse() {}

    public TaskDependencyResponse(Long id, Long taskId, String taskTitle, Long dependsOnTaskId,
                                  String dependsOnTaskTitle, TaskPriority dependsOnTaskPriority,
                                  TaskStatus dependsOnTaskStatus, String dependsOnTaskCategory,
                                  boolean isCompleted, LocalDateTime createdAt) {
        this.id = id;
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.dependsOnTaskId = dependsOnTaskId;
        this.dependsOnTaskTitle = dependsOnTaskTitle;
        this.dependsOnTaskPriority = dependsOnTaskPriority;
        this.dependsOnTaskStatus = dependsOnTaskStatus;
        this.dependsOnTaskCategory = dependsOnTaskCategory;
        this.isCompleted = isCompleted;
        this.createdAt = createdAt;
    }

    public static TaskDependencyResponseBuilder builder() {
        return new TaskDependencyResponseBuilder();
    }

    public static class TaskDependencyResponseBuilder {
        private Long id;
        private Long taskId;
        private String taskTitle;
        private Long dependsOnTaskId;
        private String dependsOnTaskTitle;
        private TaskPriority dependsOnTaskPriority;
        private TaskStatus dependsOnTaskStatus;
        private String dependsOnTaskCategory;
        private boolean isCompleted;
        private LocalDateTime createdAt;

        public TaskDependencyResponseBuilder id(Long id) { this.id = id; return this; }
        public TaskDependencyResponseBuilder taskId(Long taskId) { this.taskId = taskId; return this; }
        public TaskDependencyResponseBuilder taskTitle(String taskTitle) { this.taskTitle = taskTitle; return this; }
        public TaskDependencyResponseBuilder dependsOnTaskId(Long dependsOnTaskId) { this.dependsOnTaskId = dependsOnTaskId; return this; }
        public TaskDependencyResponseBuilder dependsOnTaskTitle(String dependsOnTaskTitle) { this.dependsOnTaskTitle = dependsOnTaskTitle; return this; }
        public TaskDependencyResponseBuilder dependsOnTaskPriority(TaskPriority dependsOnTaskPriority) { this.dependsOnTaskPriority = dependsOnTaskPriority; return this; }
        public TaskDependencyResponseBuilder dependsOnTaskStatus(TaskStatus dependsOnTaskStatus) { this.dependsOnTaskStatus = dependsOnTaskStatus; return this; }
        public TaskDependencyResponseBuilder dependsOnTaskCategory(String dependsOnTaskCategory) { this.dependsOnTaskCategory = dependsOnTaskCategory; return this; }
        public TaskDependencyResponseBuilder isCompleted(boolean isCompleted) { this.isCompleted = isCompleted; return this; }
        public TaskDependencyResponseBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }

        public TaskDependencyResponse build() {
            return new TaskDependencyResponse(id, taskId, taskTitle, dependsOnTaskId,
                    dependsOnTaskTitle, dependsOnTaskPriority, dependsOnTaskStatus,
                    dependsOnTaskCategory, isCompleted, createdAt);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTaskId() { return taskId; }
    public void setTaskId(Long taskId) { this.taskId = taskId; }

    public String getTaskTitle() { return taskTitle; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }

    public Long getDependsOnTaskId() { return dependsOnTaskId; }
    public void setDependsOnTaskId(Long dependsOnTaskId) { this.dependsOnTaskId = dependsOnTaskId; }

    public String getDependsOnTaskTitle() { return dependsOnTaskTitle; }
    public void setDependsOnTaskTitle(String dependsOnTaskTitle) { this.dependsOnTaskTitle = dependsOnTaskTitle; }

    public TaskPriority getDependsOnTaskPriority() { return dependsOnTaskPriority; }
    public void setDependsOnTaskPriority(TaskPriority dependsOnTaskPriority) { this.dependsOnTaskPriority = dependsOnTaskPriority; }

    public TaskStatus getDependsOnTaskStatus() { return dependsOnTaskStatus; }
    public void setDependsOnTaskStatus(TaskStatus dependsOnTaskStatus) { this.dependsOnTaskStatus = dependsOnTaskStatus; }

    public String getDependsOnTaskCategory() { return dependsOnTaskCategory; }
    public void setDependsOnTaskCategory(String dependsOnTaskCategory) { this.dependsOnTaskCategory = dependsOnTaskCategory; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
