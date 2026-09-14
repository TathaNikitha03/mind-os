package com.mindos.backend.service.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("localLlmProvider")
public class LocalDeterministicLlmProvider implements LlmProvider {

    private static final Logger log = LoggerFactory.getLogger(LocalDeterministicLlmProvider.class);

    private final String modelName;

    public LocalDeterministicLlmProvider(@Value("${llm.model:local-grounded-synthesis}") String modelName) {
        this.modelName = modelName;
        log.info("Initialized LocalDeterministicLlmProvider with model: {}", modelName);
    }

    @Override
    public String generateAnswer(String systemPrompt, String userQuestion, String context) {
        if (context == null || context.trim().isEmpty()) {
            return "I couldn't find enough information about this in your knowledge base.";
        }

        // Extract key sentence statements directly from the grounded context
        String[] lines = context.split("\n");
        List<String> contentLines = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--- SOURCE") || trimmed.startsWith("Document:") || trimmed.startsWith("Section:")) {
                continue;
            }
            contentLines.add(trimmed);
        }

        if (contentLines.isEmpty()) {
            return "Based on your knowledge base: " + context.trim();
        }

        StringBuilder answer = new StringBuilder();
        answer.append("Based on the information in your knowledge base:\n\n");

        for (int i = 0; i < Math.min(contentLines.size(), 3); i++) {
            answer.append(contentLines.get(i)).append("\n\n");
        }

        return answer.toString().trim();
    }

    @Override
    public String getProviderName() {
        return "local";
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
