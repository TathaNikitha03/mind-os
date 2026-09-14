package com.mindos.backend.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service("openAiLlmProvider")
public class OpenAiLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenAiLlmProvider.class);

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private final double temperature;
    private final int maxTokens;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenAiLlmProvider(
            @Value("${llm.api-key:}") String apiKey,
            @Value("${llm.model:gpt-4o-mini}") String modelName,
            @Value("${llm.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${llm.temperature:0.2}") double temperature,
            @Value("${llm.max-tokens:1000}") int maxTokens) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = baseUrl;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String generateAnswer(String systemPrompt, String userQuestion, String context) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalStateException("OpenAI API key is missing. Please configure LLM_API_KEY.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey.trim());

            String userContent = String.format("""
                    CONTEXT INFORMATION FROM USER'S KNOWLEDGE BASE:
                    %s

                    USER QUESTION:
                    %s
                    """, context, userQuestion);

            Map<String, Object> requestPayload = Map.of(
                    "model", modelName,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userContent)
                    ),
                    "temperature", temperature,
                    "max_tokens", maxTokens
            );

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestPayload, headers);
            String url = baseUrl + "/chat/completions";

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("OpenAI API returned status: " + response.getStatusCode());
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.path("choices");

            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("OpenAI API response missing 'choices'.");
            }

            String content = choices.get(0).path("message").path("content").asText();
            return content != null ? content.trim() : "";
        } catch (Exception e) {
            log.error("Failed to generate answer from OpenAI LLM: {}", e.getMessage());
            throw new RuntimeException("AI service is temporarily unavailable.", e);
        }
    }

    @Override
    public String getProviderName() {
        return "openai";
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
