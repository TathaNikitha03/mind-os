package com.mindos.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiTaskAnalyzeRequest {
    @NotBlank(message = "Prompt cannot be blank")
    private String prompt;
}
