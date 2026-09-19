package com.mindos.backend.dto;

import com.mindos.backend.enums.TaskPriority;
import lombok.Data;

@Data
public class TaskSuggestionDto {
    private String title;
    private String description;
    private TaskPriority priority;
    private Integer estimatedMinutes;
}
