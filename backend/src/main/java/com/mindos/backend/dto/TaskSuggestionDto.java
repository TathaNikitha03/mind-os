package com.mindos.backend.dto;

import com.mindos.backend.enums.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskSuggestionDto {
    private String title;
    private String description;
    private TaskPriority priority;
    private Integer estimatedMinutes;
    private String suggestedCategory;

    public TaskSuggestionDto(String title, String description, TaskPriority priority, Integer estimatedMinutes) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.estimatedMinutes = estimatedMinutes;
    }
}
