package com.mindos.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindos.backend.dto.AiTaskSuggestionResponse;
import com.mindos.backend.service.llm.LlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiTaskService {

    private static final Logger log = LoggerFactory.getLogger(AiTaskService.class);

    private final LlmProvider llmProvider;
    private final ObjectMapper objectMapper;

    public static final String TASK_ASSISTANT_SYSTEM_PROMPT = """
            You are a smart Task Assistant for MIND OS.
            Break down the user's goal into a structured, actionable task list.
            
            Return ONLY a raw JSON object with this exact structure (no markdown fences, no backticks, no extra text):
            {
              "analysisSummary": "A brief 1-2 sentence overview of what needs to be done.",
              "suggestedTasks": [
                {
                  "title": "Clear actionable title",
                  "description": "Details on how to accomplish this step",
                  "priority": "HIGH", // HIGH, MEDIUM, or LOW
                  "estimatedMinutes": 30 // integer
                }
              ]
            }
            """;

    public AiTaskService(LlmProvider llmProvider, ObjectMapper objectMapper) {
        this.llmProvider = llmProvider;
        this.objectMapper = objectMapper;
    }

    public AiTaskSuggestionResponse analyzeGoal(String userGoal) {
        if (userGoal == null || userGoal.trim().isEmpty()) {
            throw new IllegalArgumentException("Goal cannot be blank.");
        }

        try {
            log.info("Analyzing task goal: {}", userGoal);
            // Passing empty string for context since this relies entirely on the model's knowledge
            String rawJson = llmProvider.generateAnswer(TASK_ASSISTANT_SYSTEM_PROMPT, userGoal, "");
            
            // Clean up any potential markdown formatting the LLM might have returned despite instructions
            rawJson = rawJson.replaceAll("^`json\\\\s*", "").replaceAll("`$", "").trim();
            
            return objectMapper.readValue(rawJson, AiTaskSuggestionResponse.class);
        } catch (Exception e) {
            log.error("Failed to analyze goal: {}", e.getMessage());
            throw new RuntimeException("Failed to generate task breakdown. Please try again.");
        }
    }
}
