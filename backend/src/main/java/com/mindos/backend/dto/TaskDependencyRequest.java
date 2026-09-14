package com.mindos.backend.dto;

import jakarta.validation.constraints.NotNull;

public class TaskDependencyRequest {

    @NotNull(message = "dependsOnTaskId is required")
    private Long dependsOnTaskId;

    public TaskDependencyRequest() {}

    public TaskDependencyRequest(Long dependsOnTaskId) {
        this.dependsOnTaskId = dependsOnTaskId;
    }

    public Long getDependsOnTaskId() { return dependsOnTaskId; }
    public void setDependsOnTaskId(Long dependsOnTaskId) { this.dependsOnTaskId = dependsOnTaskId; }
}
