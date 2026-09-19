package com.mindos.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class AiTaskSuggestionResponse {
    private String analysisSummary;
    private List<TaskSuggestionDto> suggestedTasks;
}
